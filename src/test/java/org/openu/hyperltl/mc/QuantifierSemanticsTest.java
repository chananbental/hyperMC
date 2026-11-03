package org.openu.hyperltl.mc;

import org.junit.jupiter.api.Test;
import org.openu.hyperltl.approx.*;
import org.openu.hyperltl.formula.HyperLTLParser;
import org.openu.hyperltl.formula.HyperLTLFormula;
import org.openu.hyperltl.model.SystemModel;
import org.openu.hyperltl.spot.SpotAutomatonUtil;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class QuantifierSemanticsTest {

    @Test
    void sccRefinementProvesTheSixPreviouslyStalledUniversalProperties() {
        for (int systemIndex : List.of(1, 3, 5)) {
            var model = org.openu.hyperltl.cases.systems.TestSystems.systems.get(systemIndex);
            for (String text : List.of("forall pi1. forall pi2. G(a_pi1 -> F(a_pi2))",
                    "forall pi1. forall pi2. G((a_pi1 & b_pi1) -> F(c_pi2))")) {
                var result = new HyperLTLModelChecker(new SpotAutomatonUtil(),
                        new ApproximationCoordinator(new SccApproximationStrategy(), new SccApproximationStrategy()))
                        .check(model, new HyperLTLParser().parse(text));
                assertTrue(result.holds(), "system-" + (systemIndex + 1) + ": " + text);
                assertTrue(result.refinementRounds() > 0);
            }
        }
    }
    private final SystemModel branching = new SystemModel(Set.of("a"), "s0", Map.of(
            "s0", new SystemModel.State(Set.of(), Set.of("s1", "s2")),
            "s1", new SystemModel.State(Set.of("a"), Set.of("s1")),
            "s2", new SystemModel.State(Set.of(), Set.of("s2"))));

    @Test
    void exactVerdictsRespectQuantifierPolarityAndAlternation() {
        List<String> formulas = List.of(
                "forall p. G(a_p)", "exists p. G(a_p)",
                "forall p. F(a_p)", "exists p. F(a_p)",
                "forall p. forall q. G(a_p <-> a_q)",
                "exists p. exists q. F(a_p & a_q)",
                "forall p. exists q. G(a_p <-> a_q)",
                "exists p. forall q. G(a_p <-> a_q)",
                "forall p. exists q. forall r. G(a_q <-> a_r)",
                "exists p. forall q. exists r. G(a_q <-> a_r)",
                "forall p. exists q. forall r. exists s. G(a_r <-> a_s)",
                "exists p. forall q. exists r. forall s. G(a_r <-> a_s)");
        List<Boolean> expected = List.of(false, false, false, true, false, true,
                true, false, false, true, true, false);
        for (int i = 0; i < formulas.size(); i++) {
            var result = checker(new ExactApproximationStrategy()).check(branching,
                    new HyperLTLParser().parse(formulas.get(i)));
            assertEquals(expected.get(i), result.holds(), formulas.get(i));
            if (result.counterexample() != null) {
                for (var witness : result.counterexample().traces()) {
                    assertFalse(witness.loop().isEmpty());
                    var parsed = new HyperLTLParser().parse(formulas.get(i));
                    var quantifier = parsed.quantifiers().stream()
                            .filter(q -> q.variable().equals(witness.variable())).findFirst().orElseThrow();
                    assertTrue(new HyperTupleValidator().existsAsConcreteTuple(branching,
                            new HyperLTLFormula(List.of(quantifier), parsed.body()), List.of(witness)),
                            "Invalid counterexample trace: " + witness);
                }
            }
        }
    }

    @Test
    void singleStateSatisfactionIsNotInverted() {
        for (boolean label : List.of(false, true)) {
            SystemModel model = new SystemModel(Set.of("a"), "s0", Map.of(
                    "s0", new SystemModel.State(label ? Set.of("a") : Set.of(), Set.of("s0"))));
            for (String quantifier : List.of("forall", "exists")) {
                var result = checker(new ExactApproximationStrategy()).check(model,
                        new HyperLTLParser().parse(quantifier + " p. G(a_p)"));
                assertEquals(label, result.holds());
                if (quantifier.equals("exists")) assertNull(result.counterexample());
            }
        }
    }

    @Test
    void alternatingCounterexampleContainsOnlyOutermostUniversalBlock() {
        var result = checker(new ExactApproximationStrategy()).check(branching,
                new HyperLTLParser().parse("forall p. forall q. exists r. G(a_p <-> a_q)"));
        assertFalse(result.holds());
        assertNotNull(result.counterexample());
        assertEquals(List.of("p", "q"), result.counterexample().traces().stream()
                .map(witness -> witness.variable()).toList());
    }

    @Test
    void emptyInnerProductDoesNotDecideOuterQuantifiers() {
        SystemModel deadlock = new SystemModel(Set.of("a"), "s0", Map.of(
                "s0", new SystemModel.State(Set.of(), Set.of())));
        var checker = checker(new ExactApproximationStrategy());
        assertFalse(checker.check(deadlock, new HyperLTLParser().parse(
                "exists p. forall q. G(a_q)")).holds());
        assertTrue(checker.check(deadlock, new HyperLTLParser().parse(
                "forall p. exists q. G(a_q)")).holds());
    }

    @Test
    void sccBoundsRefineRatherThanRejectAMissingExistentialWitness() {
        var formula = new HyperLTLParser().parse("exists p. F(a_p)");
        var result = checker(new SccApproximationStrategy()).check(branching, formula);
        assertTrue(result.holds());
        assertTrue(result.refinementRounds() > 0);
    }

    @Test
    void reachabilityBoundsNeitherInventFrontierLoopsNorLoseFutureBehavior() {
        SystemModel chain = new SystemModel(Set.of("a"), "s0", Map.of(
                "s0", new SystemModel.State(Set.of(), Set.of("s1")),
                "s1", new SystemModel.State(Set.of(), Set.of("s2")),
                "s2", new SystemModel.State(Set.of(), Set.of("s3")),
                "s3", new SystemModel.State(Set.of("a"), Set.of("s3"))));
        for (String quantifier : List.of("exists", "forall")) {
            assertFalse(checker(new ReachabilityApproximationStrategy()).check(chain,
                    new HyperLTLParser().parse(quantifier + " p. G(!a_p)")).holds());
        }
        var result = checker(new ReachabilityApproximationStrategy()).check(chain,
                new HyperLTLParser().parse("exists p. F(a_p)"));
        assertTrue(result.holds());
        assertTrue(result.refinementRounds() > 0);
    }

    private HyperLTLModelChecker checker(ApproximationStrategy strategy) {
        return new HyperLTLModelChecker(new SpotAutomatonUtil(),
                new ApproximationCoordinator(strategy, strategy));
    }
}
