package org.openu.hyperltl.automaton;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public record NBA(List<String> atomicPropositions, Set<Integer> initialStates,
                  Map<Integer, List<Transition>> transitions, Set<Integer> acceptingStates, int stateCount) {

    public NBA(List<String> atomicPropositions,
               Set<Integer> initialStates,
               Map<Integer, List<Transition>> transitions,
               Set<Integer> acceptingStates,
               int stateCount) {
        this.atomicPropositions = List.copyOf(atomicPropositions);
        this.initialStates = Set.copyOf(initialStates);
        this.stateCount = stateCount;
        this.acceptingStates = Set.copyOf(acceptingStates);

        Map<Integer, List<Transition>> tmp = new LinkedHashMap<>();
        for (int i = 0; i < stateCount; i++) {
            tmp.put(i, List.copyOf(transitions.getOrDefault(i, List.of())));
        }
        this.transitions = Map.copyOf(tmp);
    }

    public List<Transition> outgoing(int state) {
        return transitions.getOrDefault(state, List.of());
    }

    public record Transition(int from, int to, BooleanFormula label) {
    }
}