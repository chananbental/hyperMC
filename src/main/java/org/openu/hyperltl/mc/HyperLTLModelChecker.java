package org.openu.hyperltl.mc;

import org.openu.hyperltl.HyperCounterexample;
import org.openu.hyperltl.ModelCheckingResult;
import org.openu.hyperltl.approx.ApproximationCoordinator;
import org.openu.hyperltl.approx.ApproximationPair;
import org.openu.hyperltl.automaton.NBA;
import org.openu.hyperltl.formula.HyperLTLFormula;
import org.openu.hyperltl.formula.Quantifier;
import org.openu.hyperltl.model.SystemModel;
import org.openu.hyperltl.spot.SpotAutomatonUtil;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public final class HyperLTLModelChecker {
    private final SpotAutomatonUtil spot;
    private final SystemAutomatonBuilder systemAutomatonBuilder = new SystemAutomatonBuilder();
    private final ApproximationCoordinator approximationCoordinator;

    public HyperLTLModelChecker(SpotAutomatonUtil spot, ApproximationCoordinator approximationCoordinator) {
        this.spot = spot;
        this.approximationCoordinator = approximationCoordinator;
    }

    public ModelCheckingResult check(SystemModel originalSystem, HyperLTLFormula formula) {
        if (formula.quantifiers().isEmpty()) {
            throw new IllegalArgumentException("A HyperLTL sentence must have a quantifier prefix.");
        }
        var variables = new HashSet<String>();
        for (var q : formula.quantifiers()) {
            if (!variables.add(q.variable())) throw new IllegalArgumentException("Duplicate trace variable: " + q.variable());
        }
        // current represents the remaining subformula, or its complement when negated.
        // Universal elimination projects violations: not(forall x. P) = exists x. not P.
        boolean initiallyNegated = formula.quantifiers().getLast().quantifier() == Quantifier.FORALL;
        String body = formula.body().toSpotString();
        NBA initial = spot.ltlToNba(initiallyNegated ? "!(" + body + ")" : body);
        ApproximationPair previous = null;
        for (int round = 0; round < 5; round++) {
            ApproximationPair pair = approximationCoordinator.approximate(originalSystem, round);
            if (pair.equals(previous)) {
                throw new IllegalStateException("Inconclusive: approximation refinement made no progress.");
            }
            previous = pair;
            // Existential domains increase truth; universal domains decrease it.
            // The two evaluations bound exact truth even across alternation.
            Evaluation lower = evaluate(initial, initiallyNegated, formula, pair, true);
            if (lower.holds()) return result(true, null, round);
            Evaluation upper = pair.underApproximation().equals(pair.overApproximation())
                    ? lower : evaluate(initial, initiallyNegated, formula, pair, false);
            if (!upper.holds()) {
                HyperCounterexample counterexample = null;
                if (formula.quantifiers().getFirst().quantifier() == Quantifier.FORALL) {
                    var lasso = new AutomatonLassoExtractor().extract(upper.automaton());
                    if (lasso.isPresent()) {
                        var witnesses = new WitnessReconstructionEngine().reconstructWitnesses(
                                new ReconstructionContext(upper.steps()), lasso.get(), originalSystem);
                        if (!witnesses.isEmpty()) counterexample = new HyperCounterexample(witnesses);
                    }
                }
                return result(false, counterexample, round);
            }
            approximationCoordinator.refine(List.of());
        }
        throw new IllegalStateException("Inconclusive: refinement rounds exhausted without a conclusive verdict.");
    }

    private Evaluation evaluate(NBA initial, boolean negated, HyperLTLFormula formula,
                                ApproximationPair pair, boolean lowerBound) {
        NBA current = initial;
        List<QuantifierIntegrationStep> steps = new ArrayList<>();
        for (int i = formula.quantifiers().size() - 1; i >= 0; i--) {
            var q = formula.quantifiers().get(i);
            boolean targetNegated = q.quantifier() == Quantifier.FORALL;
            boolean complemented = negated != targetNegated;
            if (complemented) current = spot.complement(current);
            negated = targetNegated;
            boolean useUnder = (q.quantifier() == Quantifier.EXISTS) == lowerBound;
            SystemModel domain = useUnder ? pair.underApproximation() : pair.overApproximation();
            BuiltSystemAutomaton built = systemAutomatonBuilder.buildForTraceVariable(domain, q.variable());
            NBA before = current;
            ProductBuildResult product = ProductConstructionWithDecomposition.eliminate(before, built, domain, q.variable());
            current = product.nba();
            steps.add(new QuantifierIntegrationStep(steps.size(), q.variable(), q.quantifier(),
                    complemented, domain, built, before, current, product.decomposition()));
        }
        if (!current.atomicPropositions().isEmpty()) {
            throw new IllegalArgumentException("Unbound or unknown atomic propositions: " + current.atomicPropositions());
        }
        boolean empty = spot.isEmpty(current);
        return new Evaluation(negated ? empty : !empty, current, List.copyOf(steps));
    }

    private ModelCheckingResult result(boolean holds, HyperCounterexample counterexample, int round) {
        return new ModelCheckingResult(holds,
                "The HyperLTL formula " + (holds ? "holds" : "does not hold")
                        + " in the system using strategy '" + approximationCoordinator.name() + "'.",
                counterexample, round);
    }

    private record Evaluation(boolean holds, NBA automaton, List<QuantifierIntegrationStep> steps) {}
}
