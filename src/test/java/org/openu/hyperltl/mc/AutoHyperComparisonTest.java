package org.openu.hyperltl.mc;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.openu.hyperltl.approx.ApproximationCoordinator;
import org.openu.hyperltl.approx.ExactApproximationStrategy;
import org.openu.hyperltl.formula.HyperLTLParser;
import org.openu.hyperltl.model.SystemModel;
import org.openu.hyperltl.spot.SpotAutomatonUtil;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.LinkedHashMap;
import java.util.regex.Pattern;

import org.openu.hyperltl.cases.systems.TestSystems;
import org.openu.hyperltl.cases.formulas.TestFormulas;

import static org.junit.jupiter.api.Assertions.*;

/** Differential diagnostic: enable with JAVA_TOOL_OPTIONS=-Dautohyper.compare=true. */
@EnabledIfSystemProperty(named = "autohyper.compare", matches = "true")
class AutoHyperComparisonTest {
    @Test
    void compareExactVerdictsWithAutoHyper() throws Exception {
        Map<String, SystemModel> models = Map.of(
                "always-a", new SystemModel(Set.of("a"), "s0", Map.of(
                        "s0", new SystemModel.State(Set.of("a"), Set.of("s0")))),
                "never-a", new SystemModel(Set.of("a"), "s0", Map.of(
                        "s0", new SystemModel.State(Set.of(), Set.of("s0")))),
                "branching", new SystemModel(Set.of("a"), "s0", Map.of(
                        "s0", new SystemModel.State(Set.of(), Set.of("s1", "s2")),
                        "s1", new SystemModel.State(Set.of("a"), Set.of("s1")),
                        "s2", new SystemModel.State(Set.of(), Set.of("s2")))));
        List<String> formulas = List.of(
                "forall p. G(a_p)", "exists p. G(a_p)",
                "forall p. F(a_p)", "exists p. F(a_p)",
                "forall p. forall q. G(a_p <-> a_q)",
                "exists p. exists q. F(a_p & a_q)",
                "forall p. exists q. G(a_p <-> a_q)",
                "exists p. forall q. G(a_p <-> a_q)",
                "forall p. exists q. forall r. G(a_q <-> a_r) & G(a_p | !a_p)",
                "exists p. forall q. exists r. G(a_q <-> a_r) & G(a_p | !a_p)",
                "forall p. exists q. forall r. exists s. G(a_r <-> a_s) & G(a_p | !a_p) & G(a_q | !a_q)",
                "exists p. forall q. exists r. forall s. G(a_r <-> a_s) & G(a_p | !a_p) & G(a_q | !a_q)");
        compare(Path.of("build", "autohyper-comparison"), models, formulas);
    }

    @Test
    void compareExisting144BenchmarkCasesWithAutoHyper() throws Exception {
        Map<String, SystemModel> models = new LinkedHashMap<>();
        for (int i = 0; i < TestSystems.systems.size(); i++) {
            models.put("system-" + (i + 1), TestSystems.systems.get(i));
        }
        var formulas = TestFormulas.formulas.getFormulas().stream().map(f ->
                f.quantifiers().stream().map(q -> q.formalPrint())
                        .collect(java.util.stream.Collectors.joining(" ")) + " " + f.body().toSpotString()).toList();
        assertEquals(144, models.size() * formulas.size());
        compare(Path.of("build", "autohyper-benchmarks"), models, formulas);
    }

    private static void compare(Path outputDirectory, Map<String, SystemModel> models,
                                List<String> formulas) throws Exception {
        Path directory = outputDirectory.toAbsolutePath();
        Files.createDirectories(directory);
        Path executable = Path.of("AutoHyper", "app", "AutoHyper.exe").toAbsolutePath();
        assertTrue(Files.isRegularFile(executable), "Build AutoHyper first");
        List<String> rows = new ArrayList<>(List.of("system\tformula\tAutoHyper\thyperMC"));
        Files.write(directory.resolve("results.tsv"), rows);
        List<String> mismatches = new ArrayList<>();
        int caseNumber = 0;
        for (var entry : models.entrySet().stream().sorted(Map.Entry.comparingByKey()).toList()) {
            SystemModel model = entry.getValue();
            Path system = directory.resolve(entry.getKey() + ".txt");
            Files.writeString(system, explicitSystem(model));
            for (String formula : formulas) {
                Path property = directory.resolve("formula-" + (++caseNumber) + ".hq");
                Files.writeString(property, autoHyperFormula(model, formula));
                Path log = directory.resolve("autohyper-" + caseNumber + ".log");
                Process process = new ProcessBuilder(executable.toString(), "--explicit", "--comp",
                        "--no-bisim", system.toString(), property.toString())
                        .directory(directory.toFile())
                        .redirectErrorStream(true).redirectOutput(log.toFile()).start();
                if (!process.waitFor(60, TimeUnit.SECONDS)) {
                    process.destroyForcibly();
                    fail("AutoHyper timed out: " + log);
                }
                String output = Files.readString(log);
                String reference = process.exitValue() == 0 ? output.lines().map(String::trim)
                        .filter(line -> line.equals("SAT") || line.equals("UNSAT"))
                        .findFirst().orElse("ERROR: no verdict") : "ERROR: see " + log.getFileName();
                String actual;
                try {
                    var checker = new HyperLTLModelChecker(new SpotAutomatonUtil(),
                            new ApproximationCoordinator(new ExactApproximationStrategy(),
                                    new ExactApproximationStrategy()));
                    actual = checker.check(model, new HyperLTLParser().parse(formula)).holds()
                            ? "SAT" : "UNSAT";
                } catch (Exception exception) {
                    actual = "ERROR: " + exception.getMessage().replace('\n', ' ').replace('\r', ' ');
                }
                String row = entry.getKey() + "\t" + formula + "\t" + reference + "\t" + actual;
                rows.add(row);
                Files.write(directory.resolve("results.tsv"), rows);
                if (!reference.equals(actual) || reference.startsWith("ERROR")) mismatches.add(row);
            }
        }
        assertTrue(mismatches.isEmpty(), "AutoHyper discrepancies:\n" + String.join("\n", mismatches));
    }

    static String autoHyperFormula(SystemModel model, String text) {
        var formula = new HyperLTLParser().parse(text);
        String body = formula.body().toSpotString();
        for (String atom : formula.body().atomicPropositions().stream()
                .sorted(java.util.Comparator.comparingInt(String::length).reversed()).toList()) {
            String replacement = null;
            for (var q : formula.quantifiers()) {
                for (String ap : model.atomicPropositions()) {
                    if (atom.equals(ap + "_" + q.variable())) replacement = "{\"" + ap + "\"_" + q.variable() + "}";
                }
            }
            if (replacement == null) throw new IllegalArgumentException("Unknown atom: " + atom);
            body = body.replaceAll("(?<![A-Za-z0-9_])" + Pattern.quote(atom) + "(?![A-Za-z0-9_])",
                    java.util.regex.Matcher.quoteReplacement(replacement));
        }
        // The local AutoHyper parser requires all quantified variables to occur.
        String ap = model.atomicPropositions().stream().sorted().findFirst().orElseThrow();
        StringBuilder prefix = new StringBuilder();
        for (var q : formula.quantifiers()) {
            prefix.append(q.quantifier() == org.openu.hyperltl.formula.Quantifier.FORALL ? "forall " : "exists ")
                    .append(q.variable()).append(". ");
            String atom = "{\"" + ap + "\"_" + q.variable() + "}";
            body = "(" + body + ") & G(" + atom + " | !" + atom + ")";
        }
        return prefix + body;
    }

    static String explicitSystem(SystemModel model) {
        List<String> states = model.states().keySet().stream().sorted().toList();
        var aps = model.atomicPropositions().stream().sorted().toList();
        StringBuilder result = new StringBuilder("Variables:");
        for (String ap : aps) result.append(" (\"").append(ap).append("\" Bool)");
        result.append("\nInit: ").append(states.indexOf(model.initialState())).append("\n--BODY--\n");
        for (String name : states) {
            var state = model.states().get(name);
            assertFalse(state.transitions().isEmpty(), "AutoHyper explicit inputs must be total: " + name);
            result.append("State: ").append(states.indexOf(name)).append(" {");
            for (String ap : aps) result.append("(\"").append(ap).append("\" ")
                    .append(state.labels().contains(ap)).append(") ");
            result.append("}\n");
            for (String successor : state.transitions().stream().sorted().toList()) {
                result.append(states.indexOf(successor)).append(' ');
            }
            result.append('\n');
        }
        return result.append("--END--\n").toString();
    }
}
