package org.openu.hyperltl.mc;

import org.openu.hyperltl.formula.HyperLTLFormula;
import org.openu.hyperltl.model.SystemModel;
import org.openu.hyperltl.trace.TraceStep;
import org.openu.hyperltl.trace.TraceWitness;

import java.util.*;
import java.util.stream.Collectors;

public final class HyperTupleValidator {
    public boolean existsAsConcreteTuple(SystemModel model,
                                         HyperLTLFormula formula,
                                         List<TraceWitness> tuple) {
        if (tuple == null || tuple.isEmpty()) {
            return false;
        }

        Map<String, TraceWitness> byVariable = new LinkedHashMap<>();
        for (TraceWitness witness : tuple) {
            if (byVariable.put(witness.variable(), witness) != null) {
                return false;
            }
        }

        List<String> expectedVariables = formula.quantifiers().stream()
                .map(q -> q.variable())
                .toList();

        if (!byVariable.keySet().containsAll(expectedVariables)) {
            return false;
        }

        for (String variable : expectedVariables) {
            TraceWitness witness = byVariable.get(variable);
            if (witness == null) {
                return false;
            }
            if (!existsConcreteLasso(model, witness)) {
                return false;
            }
        }

        return true;
    }

    private boolean existsConcreteLasso(SystemModel model, TraceWitness witness) {
        if (witness.loop().isEmpty()) {
            return false;
        }

        List<String> prefixStates = witness.prefix().stream().map(TraceStep::state).toList();
        List<String> loopStates = witness.loop().stream().map(TraceStep::state).toList();

        // 1. Prefix states must match valuations and be connected by real transitions,
        //    starting from the model initial state.
        if (!prefixStates.isEmpty()) {
            if (!prefixStates.getFirst().equals(model.initialState())) {
                return false;
            }
            if (!matchesValuations(model, witness.variable(), witness.prefix())) {
                return false;
            }
            if (!hasPath(model, prefixStates)) {
                return false;
            }
        }

        // 2. Loop states must match valuations and form a real cycle.
        if (!matchesValuations(model, witness.variable(), witness.loop())) {
            return false;
        }
        if (!hasPath(model, loopStates)) {
            return false;
        }
        if (!hasTransition(model, loopStates.getLast(), loopStates.getFirst())) {
            return false;
        }

        // 3. Prefix end must connect into loop start.
        if (!prefixStates.isEmpty()) {
            if (!hasTransition(model, prefixStates.getLast(), loopStates.getFirst())) {
                return false;
            }
        } else {
            if (!model.initialState().equals(loopStates.getFirst())
                    && !hasTransition(model, model.initialState(), loopStates.getFirst())) {
                return false;
            }
        }

        return true;
    }

    private boolean matchesValuations(SystemModel model, String variable, List<TraceStep> steps) {
        for (TraceStep step : steps) {
            SystemModel.State state = model.states().get(step.state());
            if (state == null) {
                return false;
            }
            Set<String> expected = normalizeValuation(step.valuation(), variable);
            if (!state.labels().equals(expected)) {
                return false;
            }
        }
        return true;
    }

    private boolean hasPath(SystemModel model, List<String> states) {
        if (states.size() <= 1) {
            return true;
        }
        for (int i = 0; i + 1 < states.size(); i++) {
            if (!hasTransition(model, states.get(i), states.get(i + 1))) {
                return false;
            }
        }
        return true;
    }

    private boolean hasTransition(SystemModel model, String from, String to) {
        SystemModel.State state = model.states().get(from);
        return state != null && state.transitions().contains(to);
    }

    private Set<String> normalizeValuation(Set<String> witnessValuation, String variable) {
        String suffix = "_" + variable;
        return witnessValuation.stream()
                .filter(ap -> ap.endsWith(suffix))
                .map(ap -> ap.substring(0, ap.length() - suffix.length()))
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }
}