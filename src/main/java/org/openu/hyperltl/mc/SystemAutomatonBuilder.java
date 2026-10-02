package org.openu.hyperltl.mc;

import org.openu.hyperltl.automaton.*;
import org.openu.hyperltl.model.SystemModel;

import java.util.*;

public final class SystemAutomatonBuilder {

    public BuiltSystemAutomaton buildForTraceVariable(SystemModel model, String traceVariable) {
        List<String> aps = model.atomicPropositions().stream()
                .sorted()
                .map(ap -> rename(ap, traceVariable))
                .toList();

        Map<String, Integer> systemStateToAutomatonState = new LinkedHashMap<>();
        Map<Integer, String> automatonStateToSystemState = new LinkedHashMap<>();

        int i = 0;
        for (String s : model.states().keySet().stream().sorted().toList()) {
            systemStateToAutomatonState.put(s, i);
            automatonStateToSystemState.put(i, s);
            i++;
        }

        Map<Integer, List<NBA.Transition>> transitions = new LinkedHashMap<>();
        for (var entry : model.states().entrySet()) {
            String fromState = entry.getKey();
            SystemModel.State state = entry.getValue();
            int from = systemStateToAutomatonState.get(fromState);

            BooleanFormula exactValuation = exactValuationLabel(model.atomicPropositions(), state.labels(), traceVariable);

            for (String tgt : state.transitions()) {
                int to = systemStateToAutomatonState.get(tgt);
                transitions.computeIfAbsent(from, __ -> new ArrayList<>())
                        .add(new NBA.Transition(from, to, exactValuation));
            }
        }

        Set<Integer> initial = Set.of(systemStateToAutomatonState.get(model.initialState()));
        Set<Integer> accepting = new LinkedHashSet<>(systemStateToAutomatonState.values());

        NBA nba = new NBA(aps, initial, transitions, accepting, systemStateToAutomatonState.size());
        return new BuiltSystemAutomaton(nba, automatonStateToSystemState);
    }

    private static BooleanFormula exactValuationLabel(Set<String> allAps, Set<String> trueAps, String traceVariable) {
        BooleanFormula current = new BoolConst(true);
        for (String ap : allAps.stream().sorted().toList()) {
            String renamed = rename(ap, traceVariable);
            BooleanFormula lit = trueAps.contains(ap) ? new BoolAtom(renamed) : new BoolNot(new BoolAtom(renamed));
            current = new BoolAnd(current, lit);
        }
        return current;
    }

    private static String rename(String ap, String traceVariable) {
        return ap + "_" + traceVariable;
    }
}