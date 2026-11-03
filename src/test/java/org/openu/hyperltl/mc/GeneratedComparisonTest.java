package org.openu.hyperltl.mc;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.openu.hyperltl.approx.*;
import org.openu.hyperltl.formula.HyperLTLParser;
import org.openu.hyperltl.model.SystemModel;
import org.openu.hyperltl.spot.SpotAutomatonUtil;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;

@EnabledIfSystemProperty(named = "generated.compare", matches = "true")
class GeneratedComparisonTest {
    private static final long DEFAULT_SEED = 20261012L;
    record Case(String id, SystemModel model, String formula, String prefix) {}

    @Test
    void compareSeededCasesWithAutoHyperAndApproximationStrategies() throws Exception {
        long seed = Long.getLong("generated.seed", DEFAULT_SEED);
        Path root = Path.of("build", "generated-comparison", Long.toString(seed)).toAbsolutePath();
        Files.createDirectories(root);
        Path executable = Path.of("AutoHyper", "app", "AutoHyper.exe").toAbsolutePath();
        assertTrue(Files.isRegularFile(executable), "Build AutoHyper first");
        List<String> results = new ArrayList<>(List.of(
                "seed\tcase\tstates\tprefix\tformula\tAutoHyper\texact\tscc\treachability"));
        List<String> failures = new ArrayList<>();
        int falseAlternating = 0;
        for (Case c : cases(seed)) {
            Path directory = root.resolve(c.id());
            Files.createDirectories(directory);
            Path model = directory.resolve("system.txt");
            Path property = directory.resolve("formula.hq");
            Files.writeString(model, AutoHyperComparisonTest.explicitSystem(c.model()));
            Files.writeString(property, AutoHyperComparisonTest.autoHyperFormula(c.model(), c.formula()));
            Files.writeString(directory.resolve("formula.txt"), c.formula());
            Files.writeString(directory.resolve("seed.txt"), Long.toString(seed));
            // Native JSON supports loading a saved case directly with SystemModelLoader.
            new tools.jackson.databind.ObjectMapper().writeValue(directory.resolve("system.json").toFile(), c.model());
            Process process = new ProcessBuilder(executable.toString(), "--explicit", "--comp", "--no-bisim",
                    model.toString(), property.toString()).directory(directory.toFile())
                    .redirectErrorStream(true).redirectOutput(directory.resolve("autohyper.log").toFile()).start();
            String reference;
            if (!process.waitFor(60, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                reference = "ERROR: timeout";
            } else {
                String output = Files.readString(directory.resolve("autohyper.log"));
                reference = process.exitValue() == 0 ? output.lines().map(String::trim)
                        .filter(line -> line.equals("SAT") || line.equals("UNSAT"))
                        .findFirst().orElse("ERROR: no verdict") : "ERROR: AutoHyper exit " + process.exitValue();
            }
            String exact = check(c, ExactApproximationStrategy::new);
            String scc = check(c, SccApproximationStrategy::new);
            String reachability = check(c, ReachabilityApproximationStrategy::new);
            String row = String.join("\t", Long.toString(seed), c.id(), Integer.toString(c.model().states().size()),
                    c.prefix(), c.formula(), reference, exact, scc, reachability);
            results.add(row);
            Files.write(root.resolve("results.tsv"), results);
            boolean mismatch = reference.startsWith("ERROR") || !reference.equals(exact);
            for (String approximate : List.of(scc, reachability)) {
                if (!approximate.startsWith("INCONCLUSIVE") && !approximate.equals(exact)) mismatch = true;
                if (approximate.startsWith("ERROR")) mismatch = true;
            }
            if (mismatch) {
                failures.add(row);
                Files.writeString(directory.resolve("FAILURE.txt"), row);
            }
            if (reference.equals("UNSAT") && c.prefix().contains("A") && c.prefix().contains("E")) falseAlternating++;
        }
        Files.write(root.resolve("failures.tsv"), failures);
        assertEquals(65, results.size());
        assertTrue(failures.isEmpty(), "Generated discrepancies (inputs saved under " + root + "):\n"
                + String.join("\n", failures));
        assertTrue(falseAlternating > 0, "Expected negative alternating coverage");
    }

    private static String check(Case c, Supplier<ApproximationStrategy> factory) {
        try {
            var checker = new HyperLTLModelChecker(new SpotAutomatonUtil(),
                    new ApproximationCoordinator(factory.get(), factory.get()));
            return checker.check(c.model(), new HyperLTLParser().parse(c.formula())).holds() ? "SAT" : "UNSAT";
        } catch (Exception exception) {
            String message = String.valueOf(exception.getMessage()).replace('\n', ' ').replace('\r', ' ').replace('\t', ' ');
            boolean inconclusive = exception instanceof IllegalStateException &&
                    (message.equals("Inconclusive: approximation refinement made no progress.") ||
                    message.equals("Inconclusive: refinement rounds exhausted without a conclusive verdict."));
            return (inconclusive ? "INCONCLUSIVE: " : "ERROR: ") + message;
        }
    }

    static List<Case> cases(long seed) {
        Random random = new Random(seed);
        List<Case> cases = new ArrayList<>();
        List<String> prefixes = List.of("AA", "EE", "AE", "EA", "AEA", "EAE", "AEAE", "EAEA");
        for (int modelIndex = 0; modelIndex < 8; modelIndex++) {
            int count = 1 + modelIndex / 2;
            Map<String, SystemModel.State> states = new LinkedHashMap<>();
            for (int i = 0; i < count; i++) {
                Set<String> labels = new LinkedHashSet<>();
                for (String ap : List.of("a", "b")) if (random.nextBoolean()) labels.add(ap);
                Set<String> successors = new LinkedHashSet<>();
                for (int j = 0; j < count; j++) if (random.nextDouble() < 0.35) successors.add("s" + j);
                if (successors.isEmpty()) successors.add("s" + random.nextInt(count));
                // Include a reachable branch with two cycles among the larger models.
                if (modelIndex % 2 == 1 && count >= 3) {
                    if (i == 0) successors.addAll(Set.of("s1", "s2"));
                    if (i == 1 || i == 2) successors.add("s" + i);
                }
                states.put("s" + i, new SystemModel.State(labels, successors));
            }
            SystemModel model = new SystemModel(Set.of("a", "b"), "s0", states);
            for (int prefixIndex = 0; prefixIndex < prefixes.size(); prefixIndex++) {
                String prefix = prefixes.get(prefixIndex);
                String first = "a_p", last = "b_" + "pqrs".charAt(prefix.length() - 1);
                if (random.nextBoolean()) first = "!" + first;
                if (random.nextBoolean()) last = "!" + last;
                List<String> bodies = List.of("X(" + first + " <-> " + last + ")",
                        "(" + first + ") U (" + last + ")", "G F(" + first + " & " + last + ")",
                        "G(" + first + " -> F(" + last + "))", "F G(" + first + " <-> " + last + ")",
                        "X((" + first + ") U (" + last + "))", "G F(" + first + " <-> " + last + ")",
                        "G(a_p <-> !a_" + "pqrs".charAt(prefix.length() - 1) + ")");
                String body = bodies.get((modelIndex + prefixIndex) % bodies.size());
                StringBuilder quantified = new StringBuilder();
                for (int i = 0; i < prefix.length(); i++) {
                    char variable = "pqrs".charAt(i);
                    quantified.append(prefix.charAt(i) == 'A' ? "forall " : "exists ").append(variable).append(". ");
                    if (i > 0 && i < prefix.length() - 1) {
                        body = "(" + body + ") & " + (random.nextBoolean() ? "X" : "G F")
                                + "(" + (random.nextBoolean() ? "a_" : "!b_") + variable + ")";
                    }
                }
                cases.add(new Case(String.format(Locale.ROOT, "case-%02d", cases.size() + 1), model,
                        quantified + body, prefix));
            }
        }
        return List.copyOf(cases);
    }
}
