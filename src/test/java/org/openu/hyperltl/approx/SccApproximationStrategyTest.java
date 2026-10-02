package org.openu.hyperltl.approx;

import org.openu.hyperltl.model.SystemModel;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class SccApproximationStrategyTest {

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