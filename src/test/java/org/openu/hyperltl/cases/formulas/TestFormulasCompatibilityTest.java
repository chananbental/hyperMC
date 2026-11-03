package org.openu.hyperltl.cases.formulas;

import org.junit.jupiter.api.Test;
import org.openu.hyperltl.formula.HyperLTLParser;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class TestFormulasCompatibilityTest {
    private final Path root = Path.of(System.getProperty("hypermc.projectRoot", ".")).toAbsolutePath();

    @Test
    void everyQuantifiedTraceAppearsInTheBody() {
        for (var formula : TestFormulas.formulas.getFormulas()) {
            for (var quantifier : formula.quantifiers()) {
                assertTrue(formula.body().atomicPropositions().stream()
                                .anyMatch(ap -> ap.endsWith("_" + quantifier.variable())),
                        () -> "Unused trace variable in AutoHyper input: " + formula.prettyPrint());
            }
        }
    }

    @Test
    void autoHyperFilesMatchJavaFixtures() throws Exception {
        Path directory = root.resolve("AutoHyper/benchmarks/my_tests/compare-to-my-solver/formulas");
        assumeTrue(Files.isDirectory(directory), "Local AutoHyper checkout is optional");
        int checked = 0;
        var suites = TestFormulas.formulas.getFormulaSuits();
        for (int alternations = 0; alternations < suites.size(); alternations++) {
            var group = (MetaFormulaSuit) suites.get(alternations);
            for (var block : group.getFormulaSuits()) {
                var formulas = block.getFormulas();
                for (int index = 0; index < formulas.size(); index++) {
                    Path file = directory.resolve(alternations + "-alternations")
                            .resolve(block.getClass().getSimpleName()).resolve("formula-" + (index + 1) + ".hq");
                    String nativeText = Files.readString(file)
                            .replaceAll("\"([a-zA-Z0-9_]+)\"_(pi\\d+)", "$1_$2");
                    assertEquals(formulas.get(index), new HyperLTLParser().parse(nativeText), file.toString());
                    checked++;
                }
            }
        }
        assertEquals(24, checked);
    }

    @Test
    void verifiedBenchmarkSnapshotsUseCurrentFormulaTexts() throws Exception {
        var current = TestFormulas.formulas.getFormulas().stream().map(formula ->
                formula.quantifiers().stream().map(q -> q.formalPrint()).collect(Collectors.joining(" "))
                        + " " + formula.body().toSpotString()).collect(Collectors.toSet());
        for (String snapshot : new String[]{"autohyper-benchmark-results.tsv", "approximation-benchmark-results.tsv"}) {
            var lines = Files.readAllLines(root.resolve("docs").resolve(snapshot));
            int column = snapshot.startsWith("approximation") ? 2 : 1;
            var counts = new HashMap<String, Integer>();
            for (String line : lines.subList(1, lines.size())) {
                String text = line.split("\t", -1)[column];
                assertTrue(current.contains(text), () -> "Stale formula in " + snapshot + ": " + text);
                counts.merge(text, 1, Integer::sum);
            }
            assertEquals(current, counts.keySet(), snapshot);
            int repetitions = column == 1 ? 6 : 12;
            counts.forEach((formula, count) -> assertEquals(repetitions, count, formula));
        }
    }
}
