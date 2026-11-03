package org.openu.hyperltl.mc;

import org.openu.hyperltl.ModelCheckingResult;
import org.openu.hyperltl.approx.ApproximationCoordinator;
import org.openu.hyperltl.approx.ExactApproximationStrategy;
import org.openu.hyperltl.automaton.BoolConst;
import org.openu.hyperltl.automaton.NBA;
import org.openu.hyperltl.formula.HyperLTLParser;
import org.openu.hyperltl.model.SystemModel;
import org.openu.hyperltl.spot.SpotAutomatonUtil;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class HyperLTLModelCheckerTest {

    @Test
    void returnsHoldsWhenFinalAutomatonIsEmpty() throws Exception {
        SystemModel model = new SystemModel(
                Set.of("a"),
                "s0",
                Map.of(
                        "s0", new SystemModel.State(Set.of("a"), Set.of("s0"))
                )
        );

        HyperLTLParser parser = new HyperLTLParser();

        SpotAutomatonUtil spot = new FakeSpotAutomatonUtil(
                new NBA(
                        List.of("a_pi1"),
                        Set.of(0),
                        Map.of(0, List.of(new NBA.Transition(0, 0, new BoolConst(true)))),
                        Set.of(0),
                        1
                ),
                true
        );

        HyperLTLModelChecker checker = new HyperLTLModelChecker(spot, new ApproximationCoordinator(new ExactApproximationStrategy(), new ExactApproximationStrategy()));

        ModelCheckingResult result = checker.check(model, parser.parse("forall pi1. G(a_pi1)"));

        assertTrue(result.holds());
        assertNull(result.counterexample());
    }

    @Test
    void recomputesApproximationsBeforeDetectingStalledRefinement() {
        SystemModel original = new SystemModel(Set.of("a"), "s0",
                Map.of("s0", new SystemModel.State(Set.of("a"), Set.of())));
        SystemModel approximate = new SystemModel(Set.of("a"), "s0",
                Map.of("s0", new SystemModel.State(Set.of("a"), Set.of("s0"))));
        java.util.ArrayList<Integer> rounds = new java.util.ArrayList<>();
        org.openu.hyperltl.approx.ApproximationStrategy strategy =
                new org.openu.hyperltl.approx.ApproximationStrategy() {
                    @Override
                    public SystemModel approximate(SystemModel model,
                            org.openu.hyperltl.approx.ApproximationType type, int round) {
                        rounds.add(round);
                        return approximate;
                    }
                    @Override
                    public org.openu.hyperltl.approx.ApproximationStrategy refine(
                            List<org.openu.hyperltl.trace.TraceWitness> witnesses) {
                        return this;
                    }
                    @Override
                    public String name() { return "fixed-over"; }
                };
        NBA nba = new NBA(List.of("a_pi1"), Set.of(0),
                Map.of(0, List.of(new NBA.Transition(0, 0, new BoolConst(true)))), Set.of(0), 1);
        HyperLTLModelChecker checker = new HyperLTLModelChecker(
                new FakeSpotAutomatonUtil(nba, false) {
                    @Override
                    public boolean isEmpty(NBA automaton) {
                        return new AutomatonLassoExtractor().extract(automaton).isEmpty();
                    }
                },
                new ApproximationCoordinator(new ExactApproximationStrategy(), strategy));
        IllegalStateException failure = assertThrows(IllegalStateException.class,
                () -> checker.check(original, new HyperLTLParser().parse("forall pi1. G(a_pi1)")));
        assertTrue(failure.getMessage().contains("no progress"));
        assertEquals(List.of(0, 1), rounds);
    }

    static class FakeSpotAutomatonUtil extends SpotAutomatonUtil {
        private final NBA initial;
        private final boolean empty;

        FakeSpotAutomatonUtil(NBA initial, boolean empty) {
            this.initial = initial;
            this.empty = empty;
        }

        @Override
        public NBA ltlToNba(String ltlFormula) {
            return initial;
        }

        @Override
        public NBA complement(NBA nba) {
            return nba;
        }

        @Override
        public boolean isEmpty(NBA nba) {
            return empty;
        }
    }
}
