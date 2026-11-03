package org.openu.hyperltl.mc;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.openu.hyperltl.approx.*;
import org.openu.hyperltl.cases.formulas.TestFormulas;
import org.openu.hyperltl.cases.systems.TestSystems;
import org.openu.hyperltl.spot.SpotAutomatonUtil;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/** Compare approximate verdicts with the exact results previously verified by AutoHyper. */
@EnabledIfSystemProperty(named = "approximation.compare", matches = "true")
class ApproximationBenchmarkComparisonTest {
    @Test
    void compareSccAndReachabilityWithVerifiedExactBenchmarks() throws Exception {
        Map<String, String> baseline = new HashMap<>();
        for (String line : Files.readAllLines(Path.of("docs", "autohyper-benchmark-results.tsv")).subList(1, 145)) {
            String[] columns = line.split("\t", -1);
            assertEquals(4, columns.length, line);
            assertTrue(columns[2].equals("SAT") || columns[2].equals("UNSAT"), line);
            assertEquals(columns[2], columns[3], "Exact baseline disagrees with AutoHyper");
            assertNull(baseline.put(columns[0] + "\t" + columns[1], columns[3]), "Duplicate baseline case");
        }
        assertEquals(144, baseline.size());
        Map<String, Supplier<ApproximationStrategy>> strategies = Map.of(
                "scc", SccApproximationStrategy::new,
                "reachability", ReachabilityApproximationStrategy::new);
        Path directory = Path.of("build", "approximation-benchmarks");
        Files.createDirectories(directory);
        Path output = directory.resolve("results.tsv");
        List<String> rows = new ArrayList<>(List.of(
                "strategy\tsystem\tformula\texact\tstatus\tverdict\trefinementRound\telapsedMs\tdetail"));
        Files.write(output, rows);
        List<String> failures = new ArrayList<>();
        for (String strategyName : strategies.keySet().stream().sorted().toList()) {
            Supplier<ApproximationStrategy> factory = strategies.get(strategyName);
            for (int systemIndex = 0; systemIndex < TestSystems.systems.size(); systemIndex++) {
                String systemName = "system-" + (systemIndex + 1);
                var system = TestSystems.systems.get(systemIndex);
                for (var formula : TestFormulas.formulas.getFormulas()) {
                    String text = formula.quantifiers().stream().map(q -> q.formalPrint())
                            .collect(Collectors.joining(" ")) + " " + formula.body().toSpotString();
                    String exact = baseline.get(systemName + "\t" + text);
                    assertNotNull(exact, "Benchmark has changed; refresh verified baseline: " + text);
                    String status, verdict = "-", round = "-", detail = "";
                    long start = System.nanoTime();
                    try {
                        // Every case gets fresh, separate under/over strategies: refinement
                        // in one case must not affect the next case or the other strategy.
                        var checker = new HyperLTLModelChecker(new SpotAutomatonUtil(),
                                new ApproximationCoordinator(factory.get(), factory.get()));
                        var result = checker.check(system, formula);
                        verdict = result.holds() ? "SAT" : "UNSAT";
                        round = Integer.toString(result.refinementRounds());
                        status = exact.equals(verdict) ? "AGREES" : "DISAGREES";
                    } catch (Exception exception) {
                        detail = exception.getClass().getSimpleName() + ": " + exception.getMessage();
                        String message = exception.getMessage();
                        status = exception instanceof IllegalStateException && message != null
                                && message.equals("Inconclusive: approximation refinement made no progress.")
                                ? "INCONCLUSIVE_STALLED"
                                : exception instanceof IllegalStateException && message != null
                                && message.equals("Inconclusive: refinement rounds exhausted without a conclusive verdict.")
                                ? "INCONCLUSIVE_EXHAUSTED" : "ERROR";
                    }
                    long elapsedMs = (System.nanoTime() - start) / 1_000_000;
                    detail = detail.replace('\t', ' ').replace('\n', ' ').replace('\r', ' ');
                    String row = String.join("\t", strategyName, systemName, text, exact,
                            status, verdict, round, Long.toString(elapsedMs), detail);
                    rows.add(row);
                    Files.write(output, rows);
                    if (status.equals("DISAGREES") || status.equals("ERROR")) failures.add(row);
                }
            }
        }
        assertEquals(289, rows.size(), "Expected 288 approximate checks and a header");
        assertTrue(failures.isEmpty(), "Approximation failures:\n" + String.join("\n", failures));
    }
}
