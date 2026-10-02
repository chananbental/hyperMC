package org.openu.hyperltl.mc;

import org.openu.hyperltl.automaton.BoolConst;
import org.openu.hyperltl.automaton.NBA;
import org.openu.hyperltl.model.SystemModel;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ProductConstructionWithDecompositionTest {

    @Test
    void storesDecompositionForProductStates() {
        NBA left = new NBA(
                List.of(),
                Set.of(0),
                Map.of(
                        0, List.of(new NBA.Transition(0, 0, new BoolConst(true)))
                ),
                Set.of(0),
                1
        );

        SystemModel model = new SystemModel(
                Set.of("a"),
                "s0",
                Map.of(
                        "s0", new SystemModel.State(Set.of("a"), Set.of("s1")),
                        "s1", new SystemModel.State(Set.of("a"), Set.of("s1"))
                )
        );

        BuiltSystemAutomaton right = new SystemAutomatonBuilder().buildForTraceVariable(model, "pi1");

        ProductBuildResult result = ProductConstructionWithDecomposition.intersect(left, right);

        assertNotNull(result.nba());
        assertFalse(result.decomposition().isEmpty());

        for (var entry : result.decomposition().entrySet()) {
            ProductStateComponents c = entry.getValue();
            assertTrue(c.leftState() >= 0);
            assertTrue(c.rightState() >= 0);
            assertTrue(c.phase() == 0 || c.phase() == 1);
        }
    }
}