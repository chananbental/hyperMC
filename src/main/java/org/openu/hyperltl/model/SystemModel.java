package org.openu.hyperltl.model;

import java.util.Map;
import java.util.Set;

public record SystemModel(Set<String> atomicPropositions,
                          String initialState,
                          Map<String, State> states) {

    public SystemModel {
        atomicPropositions = Set.copyOf(atomicPropositions);
        states = Map.copyOf(states);

        if (!states.containsKey(initialState)) {
            throw new IllegalArgumentException("Initial state not found: " + initialState);
        }

        validate(atomicPropositions, states);
    }

    private static void validate(Set<String> atomicPropositions, Map<String, State> states) {
        for (var entry : states.entrySet()) {
            String stateName = entry.getKey();
            State state = entry.getValue();

            if (!atomicPropositions.containsAll(state.labels())) {
                throw new IllegalArgumentException(
                        "State " + stateName + " uses unknown atomic propositions: " + state.labels()
                );
            }

            for (String tgt : state.transitions()) {
                if (!states.containsKey(tgt)) {
                    throw new IllegalArgumentException(
                            "State " + stateName + " transitions to unknown state: " + tgt
                    );
                }
            }
        }
    }

    public record State(Set<String> labels, Set<String> transitions) {

        public State {
            labels = Set.copyOf(labels);
            transitions = Set.copyOf(transitions);
        }

    }
}
