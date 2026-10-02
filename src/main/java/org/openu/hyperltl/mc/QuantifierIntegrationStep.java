package org.openu.hyperltl.mc;

import org.openu.hyperltl.automaton.NBA;
import org.openu.hyperltl.formula.Quantifier;
import org.openu.hyperltl.model.SystemModel;

import java.util.Map;

public record QuantifierIntegrationStep(
        int stepIndex,
        String variable,
        Quantifier quantifier,
        boolean complementedBeforeStep,
        SystemModel chosenApproximation,
        BuiltSystemAutomaton systemAutomaton,
        NBA automatonBeforeStep,
        NBA automatonAfterStep,
        Map<Integer, ProductStateComponents> productStateDecomposition
) {
}