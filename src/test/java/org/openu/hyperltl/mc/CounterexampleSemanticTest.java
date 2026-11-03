package org.openu.hyperltl.mc;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.openu.hyperltl.approx.*;
import org.openu.hyperltl.formula.HyperLTLParser;
import org.openu.hyperltl.formula.HyperLTLFormula;
import org.openu.hyperltl.formula.Quantifier;
import org.openu.hyperltl.model.SystemModel;
import org.openu.hyperltl.spot.SpotAutomatonUtil;
import org.openu.hyperltl.trace.TraceStep;
import org.openu.hyperltl.trace.TraceWitness;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;

@EnabledIfSystemProperty(named = "counterexample.compare", matches = "true")
class CounterexampleSemanticTest {
    @Test
    void fixedDomainsDistinguishAValidTracePairFromARealCounterexample() throws Exception {
        SystemModel model = new SystemModel(Set.of("a"), "s0", Map.of(
                "s0", new SystemModel.State(Set.of(), Set.of("s1", "s2")),
                "s1", new SystemModel.State(Set.of("a"), Set.of("s1")),
                "s2", new SystemModel.State(Set.of(), Set.of("s2"))));
        var formula = new HyperLTLParser().parse("forall p. forall q. X(a_p <-> a_q)");
        Path root = Path.of("build", "counterexample-comparison", "control").toAbsolutePath();
        Path executable = Path.of("AutoHyper", "app", "AutoHyper.exe").toAbsolutePath();
        assertEquals("UNSAT", fixedVerdict(model, formula, Map.of(), root.resolve("original"), executable));
        Map<String, TraceWitness> sameTracePair = new LinkedHashMap<>();
        for (String variable : List.of("p", "q")) {
            TraceWitness witness = new TraceWitness(variable, List.of(new TraceStep("s0", Set.of())),
                    List.of(new TraceStep("s1", Set.of("a_" + variable))));
            validateLasso(model, witness);
            sameTracePair.put(variable, witness);
        }
        assertEquals("SAT", fixedVerdict(model, formula, sameTracePair, root.resolve("non-counterexample"), executable));
    }
    @Test
    void outerUniversalCounterexamplesRefuteTheRemainingQuantifiedProperty() throws Exception {
        long seed = 20261012L;
        Map<String, String> reference = new HashMap<>();
        for (String line : Files.readAllLines(Path.of("docs", "generated-comparison-results.tsv")).subList(1, 65)) {
            String[] columns = line.split("\t", -1);
            assertEquals(Long.toString(seed), columns[0]);
            assertEquals(columns[5], columns[6]);
            reference.put(columns[1] + "\t" + columns[4], columns[5]);
        }
        Path root = Path.of("build", "counterexample-comparison", Long.toString(seed)).toAbsolutePath();
        Files.createDirectories(root);
        Path executable = Path.of("AutoHyper", "app", "AutoHyper.exe").toAbsolutePath();
        assertTrue(Files.isRegularFile(executable));
        List<String> rows = new ArrayList<>(List.of("seed\tcase\tstrategy\tprefix\tstatus\tAutoHyperFixedTraces\tdetail"));
        List<String> failures = new ArrayList<>();
        Map<String, Supplier<ApproximationStrategy>> strategies = Map.of(
                "exact", ExactApproximationStrategy::new, "scc", SccApproximationStrategy::new,
                "reachability", ReachabilityApproximationStrategy::new);
        for (var c : GeneratedComparisonTest.cases(seed)) {
            String expected = reference.get(c.id() + "\t" + c.formula());
            assertNotNull(expected, "Refresh the generated reference if generation changes");
            if (!c.prefix().startsWith("A") || !expected.equals("UNSAT")) continue;
            var formula = new HyperLTLParser().parse(c.formula());
            var outer = formula.quantifiers().stream().takeWhile(q -> q.quantifier() == Quantifier.FORALL).toList();
            for (String name : strategies.keySet().stream().sorted().toList()) {
                Path directory = root.resolve(c.id()).resolve(name);
                Files.createDirectories(directory);
                String status = "VALID", fixedVerdict = "-", detail = "";
                try {
                    var factory = strategies.get(name);
                    var result = new HyperLTLModelChecker(new SpotAutomatonUtil(),
                            new ApproximationCoordinator(factory.get(), factory.get())).check(c.model(), formula);
                    if (result.holds()) throw new IllegalStateException("Expected a false formula");
                    if (result.counterexample() == null) throw new IllegalStateException("Missing counterexample");
                    var witnesses = result.counterexample().traces();
                    new tools.jackson.databind.ObjectMapper().writeValue(directory.resolve("witnesses.json").toFile(), witnesses);
                    Map<String, TraceWitness> byVariable = new LinkedHashMap<>();
                    for (var witness : witnesses) {
                        if (byVariable.put(witness.variable(), witness) != null) throw new IllegalStateException("Duplicate witness variable");
                        validateLasso(c.model(), witness);
                    }
                    Set<String> expectedVariables = new HashSet<>();
                    outer.forEach(q -> expectedVariables.add(q.variable()));
                    if (!byVariable.keySet().equals(expectedVariables)) throw new IllegalStateException("Wrong outer-block variables");
                    fixedVerdict = fixedVerdict(c.model(), formula, byVariable, directory, executable);
                    if (!fixedVerdict.equals("UNSAT")) throw new IllegalStateException("Returned traces do not refute the quantified property");
                } catch (Exception failure) {
                    status = "FAILED";
                    detail = failure.toString().replace('\t', ' ').replace('\n', ' ').replace('\r', ' ');
                    Files.writeString(directory.resolve("FAILURE.txt"), detail);
                }
                String row = String.join("\t", Long.toString(seed), c.id(), name, c.prefix(), status, fixedVerdict, detail);
                rows.add(row);
                Files.write(root.resolve("results.tsv"), rows);
                if (status.equals("FAILED")) failures.add(row);
            }
        }
        Files.write(root.resolve("failures.tsv"), failures);
        assertEquals(61, rows.size(), "Expected twenty false universal-leading cases across three strategies");
        assertTrue(failures.isEmpty(), "Invalid counterexamples:\n" + String.join("\n", failures));
    }

    private static String fixedVerdict(SystemModel model, HyperLTLFormula formula,
                                       Map<String, TraceWitness> fixed, Path directory, Path executable) throws Exception {
        Files.createDirectories(directory);
        Path original = directory.resolve("original.txt");
        Path property = directory.resolve("formula.hq");
        String text = formula.quantifiers().stream().map(q -> q.formalPrint())
                .collect(java.util.stream.Collectors.joining(" ")) + " " + formula.body().toSpotString();
        Files.writeString(original, AutoHyperComparisonTest.explicitSystem(model));
        Files.writeString(property, AutoHyperComparisonTest.autoHyperFormula(model, text));
        Files.writeString(directory.resolve("formula.txt"), text);
        List<String> command = new ArrayList<>(List.of(executable.toString(), "--explicit", "--comp", "--no-bisim"));
        for (var q : formula.quantifiers()) {
            TraceWitness witness = fixed.get(q.variable());
            Path domain = original;
            if (witness != null) {
                domain = directory.resolve("trace-" + q.variable() + ".txt");
                Files.writeString(domain, AutoHyperComparisonTest.explicitSystem(singletonDomain(model, witness)));
            }
            command.add(domain.toString());
        }
        command.add(property.toString());
        Files.write(directory.resolve("command.txt"), command);
        Process process = new ProcessBuilder(command).directory(directory.toFile()).redirectErrorStream(true)
                .redirectOutput(directory.resolve("autohyper.log").toFile()).start();
        if (!process.waitFor(60, TimeUnit.SECONDS)) {
            process.destroyForcibly();
            throw new IllegalStateException("AutoHyper timeout");
        }
        String output = Files.readString(directory.resolve("autohyper.log"));
        if (process.exitValue() != 0) throw new IllegalStateException("AutoHyper error: " + output);
        return output.lines().map(String::trim).filter(s -> s.equals("SAT") || s.equals("UNSAT"))
                .findFirst().orElseThrow(() -> new IllegalStateException("No AutoHyper verdict"));
    }

    /** Independent structural check: no time-zero skipping or foreign propositions allowed. */
    private static void validateLasso(SystemModel model, TraceWitness witness) {
        if (witness.loop().isEmpty()) throw new IllegalStateException("Empty loop");
        List<TraceStep> steps = new ArrayList<>(witness.prefix());
        steps.addAll(witness.loop());
        if (!steps.getFirst().state().equals(model.initialState())) throw new IllegalStateException("Wrong time-zero state");
        for (int i = 0; i < steps.size(); i++) {
            TraceStep step = steps.get(i);
            var state = model.states().get(step.state());
            if (state == null) throw new IllegalStateException("Unknown witness state");
            Set<String> expected = new HashSet<>();
            for (String ap : state.labels()) expected.add(ap + "_" + witness.variable());
            if (!step.valuation().equals(expected)) throw new IllegalStateException("Wrong witness valuation");
            String next = i + 1 < steps.size() ? steps.get(i + 1).state() : witness.loop().getFirst().state();
            if (!state.transitions().contains(next)) throw new IllegalStateException("Non-system transition: " + step.state() + " -> " + next);
        }
    }

    /** Each position gets its own state so repeated original states preserve timing. */
    private static SystemModel singletonDomain(SystemModel original, TraceWitness witness) {
        List<TraceStep> steps = new ArrayList<>(witness.prefix());
        steps.addAll(witness.loop());
        Map<String, SystemModel.State> states = new LinkedHashMap<>();
        for (int i = 0; i < steps.size(); i++) {
            int next = i + 1 < steps.size() ? i + 1 : witness.prefix().size();
            states.put("position" + i, new SystemModel.State(original.states().get(steps.get(i).state()).labels(),
                    Set.of("position" + next)));
        }
        return new SystemModel(original.atomicPropositions(), "position0", states);
    }
}
