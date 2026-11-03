package org.openu.hyperltl.approx;

import org.openu.hyperltl.model.SystemModel;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class SccApproximationStrategyTest {

    @Test
    void overRefinementRemovesOnlyAddedEdgesAndConvergesToOriginal() {
        SystemModel original = new SystemModel(Set.of("a"), "x", Map.of(
                "x", new SystemModel.State(Set.of(), Set.of("y")),
                "y", new SystemModel.State(Set.of("a"), Set.of("x", "z")),
                "z", new SystemModel.State(Set.of("a"), Set.of()),
                "unreachable", new SystemModel.State(Set.of(), Set.of())));
        SccApproximationStrategy strategy = new SccApproximationStrategy();
        SystemModel previous = strategy.approximate(original, ApproximationType.OVER, 0);
        assertTrue(previous.states().get("x").transitions().contains("z"));
        assertTrue(previous.states().get("z").transitions().contains("z"));
        for (int round = 1; round <= 2; round++) {
            SystemModel current = strategy.approximate(original, ApproximationType.OVER, round);
            assertEquals(original.initialState(), current.initialState());
            assertEquals(original.states().keySet(), current.states().keySet());
            for (String state : original.states().keySet()) {
                assertEquals(original.states().get(state).labels(), current.states().get(state).labels());
                assertTrue(current.states().get(state).transitions().containsAll(original.states().get(state).transitions()));
                assertTrue(previous.states().get(state).transitions().containsAll(current.states().get(state).transitions()));
            }
            assertEquals(original.states().get("x"), current.states().get("x"));
            previous = current;
        }
        assertEquals(original, previous);
    }

    @Test
    void explicitRefinementTightensOverApproximationWithoutChangingRound() {
        SystemModel model = new SystemModel(Set.of("a"), "s0", Map.of(
                "s0", new SystemModel.State(Set.of(), Set.of("s1")),
                "s1", new SystemModel.State(Set.of("a"), Set.of("s1"))));
        SccApproximationStrategy strategy = new SccApproximationStrategy();
        assertTrue(strategy.approximate(model, ApproximationType.OVER, 0)
                .states().get("s0").transitions().contains("s0"));
        strategy.refine(java.util.List.of());
        assertEquals(model, strategy.approximate(model, ApproximationType.OVER, 0));
    }

    @Test
    void underApproximationIsSubgraphAndOverApproximationContainsOriginalStates() {
        SystemModel model = new SystemModel(
                Set.of("a", "b"),
                "s0",
                Map.of(
                        "s0", new SystemModel.State(Set.of("a"), Set.of("s1")),
                        "s1", new SystemModel.State(Set.of("b"), Set.of("s1", "s2")),
                        "s2", new SystemModel.State(Set.of(), Set.of("s2"))
                )
        );

        SccApproximationStrategy strategy = new SccApproximationStrategy();
        SystemModel under = strategy.approximate(model, ApproximationType.UNDER, 0);
        SystemModel over = strategy.approximate(model, ApproximationType.OVER, 0);

        assertTrue(under.states().containsKey("s0"));
        assertTrue(over.states().keySet().containsAll(model.states().keySet()));

        for (var entry : under.states().entrySet()) {
            String state = entry.getKey();
            assertTrue(model.states().containsKey(state));
            assertEquals(model.states().get(state).labels(), entry.getValue().labels());
            assertTrue(model.states().get(state).transitions().containsAll(entry.getValue().transitions()));
        }
    }

    @Test
    void refineExpandsSearchFrontier() {
        SystemModel model = new SystemModel(
                Set.of("a"),
                "s0",
                Map.of(
                        "s0", new SystemModel.State(Set.of("a"), Set.of("s1")),
                        "s1", new SystemModel.State(Set.of("a"), Set.of("s2")),
                        "s2", new SystemModel.State(Set.of("a"), Set.of("s2"))
                )
        );

        SccApproximationStrategy strategy = new SccApproximationStrategy();

        int before = strategy.approximate(model, ApproximationType.UNDER, 0).states().size();
        strategy.refine(java.util.List.of());
        int after = strategy.approximate(model, ApproximationType.UNDER, 0).states().size();

        assertTrue(after >= before);
    }
}
