package org.openu.hyperltl.mc;

import org.openu.hyperltl.formula.HyperLTLFormula;
import org.openu.hyperltl.formula.HyperLTLParser;
import org.openu.hyperltl.model.SystemModel;
import org.openu.hyperltl.trace.TraceStep;
import org.openu.hyperltl.trace.TraceWitness;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class HyperTupleValidatorTest {

    private final HyperTupleValidator validator = new HyperTupleValidator();
    private final HyperLTLParser parser = new HyperLTLParser();

    @Test
    void acceptsConcreteTupleThatExistsInOriginalSystem() {
        SystemModel model = new SystemModel(
                Set.of("a", "b"),
                "s0",
                Map.of(
                        "s0", new SystemModel.State(Set.of("a"), Set.of("s1")),
                        "s1", new SystemModel.State(Set.of("b"), Set.of("s1"))
                )
        );

        HyperLTLFormula formula = parser.parse("forall pi1. exists pi2. G(a_pi1 -> F(b_pi2))");

        TraceWitness pi1 = new TraceWitness(
                "pi1",
                List.of(
                        new TraceStep("s0", Set.of("a_pi1")),
                        new TraceStep("s1", Set.of("b_pi1"))
                ),
                List.of(
                        new TraceStep("s1", Set.of("b_pi1"))
                )
        );

        TraceWitness pi2 = new TraceWitness(
                "pi2",
                List.of(
                        new TraceStep("s0", Set.of("a_pi2")),
                        new TraceStep("s1", Set.of("b_pi2"))
                ),
                List.of(
                        new TraceStep("s1", Set.of("b_pi2"))
                )
        );

        assertTrue(validator.existsAsConcreteTuple(model, formula, List.of(pi1, pi2)));
    }

    @Test
    void rejectsTupleWhenVariableMissing() {
        SystemModel model = new SystemModel(
                Set.of("a"),
                "s0",
                Map.of(
                        "s0", new SystemModel.State(Set.of("a"), Set.of("s0"))
                )
        );

        HyperLTLFormula formula = parser.parse("forall pi1. exists pi2. G(a_pi1)");

        TraceWitness pi1 = new TraceWitness(
                "pi1",
                List.of(
                        new TraceStep("s0", Set.of("a_pi1"))
                ),
                List.of(
                        new TraceStep("s0", Set.of("a_pi1"))
                )
        );

        assertFalse(validator.existsAsConcreteTuple(model, formula, List.of(pi1)));
    }

    @Test
    void rejectsTupleWhenLoopDoesNotExist() {
        SystemModel model = new SystemModel(
                Set.of("a", "b"),
                "s0",
                Map.of(
                        "s0", new SystemModel.State(Set.of("a"), Set.of("s1")),
                        "s1", new SystemModel.State(Set.of("b"), Set.of())
                )
        );

        HyperLTLFormula formula = parser.parse("forall pi1. G(a_pi1)");

        TraceWitness pi1 = new TraceWitness(
                "pi1",
                List.of(
                        new TraceStep("s0", Set.of("a_pi1")),
                        new TraceStep("s1", Set.of("b_pi1"))
                ),
                List.of(
                        new TraceStep("s1", Set.of("b_pi1"))
                )
        );

        assertFalse(validator.existsAsConcreteTuple(model, formula, List.of(pi1)));
    }
}