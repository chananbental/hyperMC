package org.openu;

import org.openu.hyperltl.ModelCheckingResult;
import org.openu.hyperltl.approx.*;
import org.openu.hyperltl.formula.HyperLTLFormula;
import org.openu.hyperltl.formula.HyperLTLParser;
import org.openu.hyperltl.mc.HyperLTLModelChecker;
import org.openu.hyperltl.model.SystemModel;
import org.openu.hyperltl.model.SystemModelLoader;
import org.openu.hyperltl.spot.SpotAutomatonUtil;

import java.nio.file.Path;
import java.util.Locale;

import static org.openu.hyperltl.utils.Timer.runAndTime;

public final class Main {

    static void main(String[] args) throws Exception {
        if (args.length < 4) {
            System.err.println("Usage: java -jar hyperltl-mc.jar <system.json> <hyperltl-formula> <under-approximation-method> <over-approximation-method>");
            System.err.println("Approximation methods: exact, reachable-under-over, scc-under-over");
            System.exit(2);
        }

        Path systemFile = Path.of(args[0]);
        String formulaText = args[1];
        String underMethod = args[2].toLowerCase(Locale.ROOT);
        String overMethod = args[3].toLowerCase(Locale.ROOT);

        SystemModel system = SystemModelLoader.load(systemFile);
        HyperLTLFormula formula = new HyperLTLParser().parse(formulaText);

        ApproximationStrategy underApproximationStrategy = switch (underMethod) {
            case "exact" -> new ExactApproximationStrategy();
            case "reachable" -> new ReachabilityApproximationStrategy();
            case "scc" -> new SccApproximationStrategy();
            default -> throw new IllegalArgumentException("Unknown under approximation method: " + underMethod);
        };

        ApproximationStrategy overApproximationStrategy = switch (overMethod) {
            case "exact" -> new ExactApproximationStrategy();
            case "reachable" -> new ReachabilityApproximationStrategy();
            case "scc" -> new SccApproximationStrategy();
            default -> throw new IllegalArgumentException("Unknown over approximation method: " + overMethod);
        };

        SpotAutomatonUtil spot = new SpotAutomatonUtil();

        HyperLTLModelChecker checker = new HyperLTLModelChecker(spot, new ApproximationCoordinator(underApproximationStrategy, overApproximationStrategy));

        ModelCheckingResult result = runAndTime(() -> checker.check(system, formula),
                elapsedTime -> IO.println(elapsedTime + " time to check " + elapsedTime));

        System.out.println(result.prettyPrint());
    }
}
