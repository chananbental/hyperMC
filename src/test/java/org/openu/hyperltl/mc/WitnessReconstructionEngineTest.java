package org.openu.hyperltl.mc;

import org.openu.hyperltl.automaton.BoolConst;
import org.openu.hyperltl.automaton.NBA;
import org.openu.hyperltl.formula.Quantifier;
import org.openu.hyperltl.model.SystemModel;
import org.openu.hyperltl.trace.TraceWitness;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class WitnessReconstructionEngineTest {

    @Test
    void reconstructsWitnessFromRecordedProductDecomposition_usingStateBasedConvention() {
        SystemModel model = new SystemModel(
                Set.of("a", "b"),
                "s0",
                Map.of(
                        "s0", new SystemModel.State(Set.of("a"), Set.of("s1")),
                        "s1", new SystemModel.State(Set.of("b"), Set.of("s1"))
                )
        );

        BuiltSystemAutomaton built = new SystemAutomatonBuilder().buildForTraceVariable(model, "pi1");

        NBA before = new NBA(
                List.of(),
                Set.of(0),
                Map.of(0, List.of(new NBA.Transition(0, 0, new BoolConst(true)))),
                Set.of(0),
                1
        );

        NBA after = new NBA(
                built.nba().atomicPropositions(),
                Set.of(0),
                Map.of(
                        0, List.of(new NBA.Transition(0, 1, new BoolConst(true))),
                        1, List.of(new NBA.Transition(1, 1, new BoolConst(true)))
                ),
                Set.of(1),
                2
        );

        QuantifierIntegrationStep step = new QuantifierIntegrationStep(
                0,
                "pi1",
                Quantifier.EXISTS,
                false,
                model,
                built,
                before,
                after,
                Map.of(
                        0, new ProductStateComponents(0, 0, 0),
                        1, new ProductStateComponents(0, 1, 0)
                )
        );

        ReconstructionContext context = new ReconstructionContext(List.of(step));
        AutomatonLasso lasso = new AutomatonLasso(
                List.of(0, 1),
                List.of(1, 1)
        );

        WitnessReconstructionEngine engine = new WitnessReconstructionEngine();
        List<TraceWitness> witnesses = engine.reconstructWitnesses(context, lasso, model);

        assertEquals(1, witnesses.size());
        TraceWitness witness = witnesses.getFirst();

        assertEquals("pi1", witness.variable());

        assertEquals(2, witness.prefix().size());
        assertEquals("s0", witness.prefix().get(0).state());
        assertEquals(Set.of("a_pi1"), witness.prefix().get(0).valuation());
        assertEquals("s1", witness.prefix().get(1).state());
        assertEquals(Set.of("b_pi1"), witness.prefix().get(1).valuation());

        assertEquals(1, witness.loop().size());
        assertEquals("s1", witness.loop().getFirst().state());
        assertEquals(Set.of("b_pi1"), witness.loop().getFirst().valuation());
    }
}