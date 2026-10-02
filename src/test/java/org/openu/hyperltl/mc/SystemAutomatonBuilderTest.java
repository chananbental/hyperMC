package org.openu.hyperltl.mc;

import org.openu.hyperltl.automaton.BoolAnd;
import org.openu.hyperltl.automaton.BoolAtom;
import org.openu.hyperltl.automaton.BoolConst;
import org.openu.hyperltl.automaton.BoolNot;
import org.openu.hyperltl.model.SystemModel;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class SystemAutomatonBuilderTest {

    @Test
    void buildsAutomatonAndStateMapping() {
        SystemModel model = new SystemModel(
                Set.of("a", "b"),
                "s0",
                Map.of(
                        "s0", new SystemModel.State(Set.of("a"), Set.of("s1")),
                        "s1", new SystemModel.State(Set.of("b"), Set.of("s1"))
                )
        );

        BuiltSystemAutomaton built = new SystemAutomatonBuilder().buildForTraceVariable(model, "pi1");

        assertEquals(List.of("a_pi1", "b_pi1"), built.nba().atomicPropositions());
        assertEquals(2, built.nba().stateCount());
        assertEquals("s0", built.automatonStateToSystemState().get(0));
        assertEquals("s1", built.automatonStateToSystemState().get(1));

        assertEquals(1, built.nba().initialStates().size());
        int init = built.nba().initialStates().iterator().next();
        assertEquals(0, init);

        assertEquals(1, built.nba().outgoing(0).size());
        var t = built.nba().outgoing(0).getFirst();
        assertEquals(1, t.to());
        assertTrue(t.label() instanceof BoolAnd || t.label() instanceof BoolConst || t.label() instanceof BoolAtom || t.label() instanceof BoolNot);
    }
}