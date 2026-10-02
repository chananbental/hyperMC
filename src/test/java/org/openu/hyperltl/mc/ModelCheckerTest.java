package org.openu.hyperltl.mc;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.openu.hyperltl.ModelCheckingResult;
import org.openu.hyperltl.approx.*;
import org.openu.hyperltl.formula.HyperLTLFormula;
import org.openu.hyperltl.model.SystemModel;
import org.openu.hyperltl.spot.SpotAutomatonUtil;
import tools.jackson.databind.ObjectMapper;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.openu.hyperltl.cases.formulas.TestFormulas.formulas;
import static org.openu.hyperltl.cases.systems.TestSystems.systems;
import static org.openu.hyperltl.utils.Timer.runAndTime;

@Disabled("activate when running beckmark")
public class ModelCheckerTest {

    SpotAutomatonUtil spot = new SpotAutomatonUtil();

    ExactApproximationStrategy exactStrategy = new ExactApproximationStrategy();
    ReachabilityApproximationStrategy reachabilityStrategy = new ReachabilityApproximationStrategy();
    SccApproximationStrategy sccStrategy = new SccApproximationStrategy();

    List<ApproximationStrategy> strategies = List.of(exactStrategy, reachabilityStrategy, sccStrategy);

    ObjectMapper mapper = new ObjectMapper();

    @Test
    void name() throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {

            Sheet sheet = workbook.createSheet("Data");

            // Header
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("over");
            header.createCell(1).setCellValue("under");
            header.createCell(2).setCellValue("system");
            header.createCell(3).setCellValue("formula");
            header.createCell(4).setCellValue("formula spread");
            header.createCell(5).setCellValue("result");
            header.createCell(6).setCellValue("total time");
            header.createCell(7).setCellValue("number of refinement rounds");

            int currentRowNumber = 1;
            int totalNumber = strategies.size() * strategies.size() * systems.size() * formulas.getFormulas().size();

            Map<String, Map<String, Map<String, Map<String, Map<String, Object>>>>> results = new HashMap<>();
            for (ApproximationStrategy overApproximationStrategy : strategies) {
                Map<String, Map<String, Map<String, Map<String, Object>>>> innerStrategyResults = new HashMap<>(0);
                results.put(overApproximationStrategy.name(), new HashMap<>());
                for (ApproximationStrategy underApproximationStrategy : strategies) {
                    results.get(overApproximationStrategy.name()).put(underApproximationStrategy.name(), new HashMap<>());
                    IO.println("starting strategies [over:" + overApproximationStrategy + ", under:" + underApproximationStrategy.name() + "]");
                    //Arrange
                    HyperLTLModelChecker checker = new HyperLTLModelChecker(
                            spot,
                            new ApproximationCoordinator(
                                    underApproximationStrategy,
                                    overApproximationStrategy
                            )
                    );

                    Map<String, Map<String, Map<String, Object>>> systemResults = new HashMap<>();
                    for (int i = 0; i < systems.size(); i++) {
                        SystemModel system = systems.get(i);
                        IO.println("starting system #" + i);
                        Map<String, Map<String, Object>> formulaResults = new HashMap<>();
                        List<HyperLTLFormula> formulasFormulas = formulas.getFormulas();
                        for (HyperLTLFormula formula : formulasFormulas) {
                            IO.println("starting case " + currentRowNumber + "/" + totalNumber);
                            Map<String, Object> caseResult = new HashMap<>();

                            // First data row
                            Row row = sheet.createRow(currentRowNumber);
                            currentRowNumber++;

                            row.createCell(0).setCellValue(overApproximationStrategy.name());
                            row.createCell(1).setCellValue(underApproximationStrategy.name());
                            row.createCell(2).setCellValue("system-" + (i + 1));
                            row.createCell(3).setCellValue(formula.prettyPrint());
                            row.createCell(4).setCellValue(formula.formalPrint());
                            try {

                                ModelCheckingResult result = runAndTime(() -> checker.check(system, formula),
                                        elapsedTime -> caseResult.put("elapsedTime", elapsedTime));
                                caseResult.put("result", result);
                                row.createCell(5).setCellValue(result.holds() ? "SAT" : "UNSAT");
                                row.createCell(6).setCellValue((Double) caseResult.get("elapsedTime"));
                                row.createCell(7).setCellValue(result.refinementRounds());
                            } catch (Exception e) {
                                caseResult.put("result", "failed");
                                caseResult.put("reason", e.getMessage());
                                row.createCell(5).setCellValue("failed: " + e.getMessage());
                                row.createCell(6).setCellValue("-");
                                row.createCell(7).setCellValue("-");
                            }

                            formulaResults.put(formula.toString(), caseResult);
                        }
                        systemResults.put("system" + i, formulaResults);
                    }
                    innerStrategyResults.put(underApproximationStrategy.name(), systemResults);
                }
                results.put(overApproximationStrategy.name(), innerStrategyResults);
            }

            Files.writeString(Path.of("results.json"), mapper.writeValueAsString(results));

            try (FileOutputStream output = new FileOutputStream("results.xlsx")) {
                sheet.autoSizeColumn(0);
                sheet.autoSizeColumn(1);
                sheet.autoSizeColumn(2);
                sheet.autoSizeColumn(3);
                sheet.autoSizeColumn(4);
                sheet.autoSizeColumn(5);
                sheet.autoSizeColumn(6);
                workbook.write(output);
            }
        }
    }
}
