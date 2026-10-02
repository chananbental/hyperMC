package org.openu.hyperltl.mc;

import org.openu.hyperltl.HyperCounterexample;
import org.openu.hyperltl.ModelCheckingResult;
import org.openu.hyperltl.approx.ApproximationCoordinator;
import org.openu.hyperltl.approx.ApproximationPair;
import org.openu.hyperltl.automaton.NBA;
import org.openu.hyperltl.formula.HyperLTLFormula;
import org.openu.hyperltl.formula.QuantifiedVariable;
import org.openu.hyperltl.model.SystemModel;
import org.openu.hyperltl.spot.SpotAutomatonUtil;
import org.openu.hyperltl.trace.TraceWitness;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.openu.hyperltl.utils.Timer.runAndTime;

public final class HyperLTLModelChecker {

    private final SpotAutomatonUtil spot;
    private final SystemAutomatonBuilder systemAutomatonBuilder = new SystemAutomatonBuilder();
    private final ApproximationCoordinator approximationCoordinator;
    private final HyperTupleValidator hyperTupleValidator = new HyperTupleValidator();

    public HyperLTLModelChecker(SpotAutomatonUtil spot,
                                ApproximationCoordinator approximationCoordinator) {
        this.spot = spot;
        this.approximationCoordinator = approximationCoordinator;
    }

    public ModelCheckingResult check(SystemModel originalSystem, HyperLTLFormula formula) {
        NBA initialNba = spot.ltlToNba(formula.body().toSpotString());

        int maxRefinements = 5;
        int refinementRound = 0;
        ApproximationPair previousApproximations = null;
        for (; refinementRound < maxRefinements; refinementRound++) {
            ApproximationPair approximations = approximationCoordinator.approximate(originalSystem, refinementRound);
            if (approximations.equals(previousApproximations)) {
                throw new IllegalStateException("Inconclusive: approximation refinement made no progress.");
            }
            previousApproximations = approximations;

            NBA current = initialNba;
            List<QuantifierIntegrationStep> recordedSteps = new ArrayList<>();

            List<QuantifiedVariable> qs = formula.quantifiers();
            for (int i = qs.size() - 1; i >= 0; i--) {
                QuantifiedVariable currentQ = qs.get(i);
                QuantifiedVariable previousQ = i > 0 ? qs.get(i - 1) : null;

                boolean complementedBeforeStep = false;
                if (previousQ != null && currentQ.quantifier() != previousQ.quantifier()) {
                    current = spot.complement(current);
                    complementedBeforeStep = true;
                }

                SystemModel chosenApprox = switch (currentQ.quantifier()) {
                    case EXISTS -> approximations.underApproximation();
                    case FORALL -> approximations.overApproximation();
                };

                BuiltSystemAutomaton systemForVar =
                        systemAutomatonBuilder.buildForTraceVariable(chosenApprox, currentQ.variable());

                NBA beforeStep = current;
                ProductBuildResult product =
                        ProductConstructionWithDecomposition.intersect(beforeStep, systemForVar);

                current = product.nba();

                recordedSteps.add(new QuantifierIntegrationStep(
                        recordedSteps.size(),
                        currentQ.variable(),
                        currentQ.quantifier(),
                        complementedBeforeStep,
                        chosenApprox,
                        systemForVar,
                        beforeStep,
                        current,
                        product.decomposition()
                ));

                if (spot.isEmpty(current)) {
                    return new ModelCheckingResult(
                            true,
                            "The HyperLTL formula holds in the system using strategy '" + approximationCoordinator.name() + "'.",
                            null, refinementRound
                    );
                }
            }

            AutomatonLassoExtractor lassoExtractor = new AutomatonLassoExtractor();
            NBA finalCurrent = current;
            Optional<AutomatonLasso> lassoOpt = runAndTime(() -> lassoExtractor.extract(finalCurrent),
                    elapsedTime -> IO.println(elapsedTime + " time to extract lasso " + elapsedTime));

            if (lassoOpt.isEmpty()) {
                throw new IllegalStateException("Inconclusive: the non-empty automaton has no extracted accepting lasso.");
            }

            ReconstructionContext reconstructionContext = new ReconstructionContext(recordedSteps);
            WitnessReconstructionEngine reconstructionEngine = new WitnessReconstructionEngine();
            List<TraceWitness> reconstructed =
                    runAndTime(() -> reconstructionEngine.reconstructWitnesses(reconstructionContext, lassoOpt.get(), originalSystem),
                            elapsedTime -> IO.println(elapsedTime + " time to reconstruct " + elapsedTime));

            if (reconstructed.isEmpty()) {
                approximationCoordinator.refine(reconstructed);
                continue;
            }

            boolean realTuple = hyperTupleValidator.existsAsConcreteTuple(originalSystem, formula, reconstructed);
            if (realTuple) {
                return new ModelCheckingResult(
                        false,
                        "A real HyperLTL counterexample tuple was found.",
                        new HyperCounterexample(reconstructed),
                        refinementRound
                );
            }

            approximationCoordinator.refine(reconstructed);
        }

        throw new IllegalStateException("Inconclusive: refinement rounds exhausted without a conclusive verdict.");
    }
}
