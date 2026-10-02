package org.openu.hyperltl.approx;

import org.openu.hyperltl.model.SystemModel;
import org.openu.hyperltl.trace.TraceWitness;

import java.util.*;

public final class SccApproximationStrategy implements ApproximationStrategy {
    private int retainedFrontierDepth = 0;

    @Override
    public SystemModel approximate(SystemModel original, ApproximationType approximationType, int refinementRound) {
        SccDecomposition decomposition = decompose(original);

        SystemModel approximation;

        if (approximationType.equals(ApproximationType.OVER)) {
            // Over-approximation:
            // complete each SCC internally and complete transitions across SCC boundaries according
            // to the SCC condensation edges. This preserves all original behaviors and may add more.
            approximation = buildOverApproximation(original, decomposition);
        } else {
            int allowedDepth = retainedFrontierDepth + refinementRound;

            Set<Integer> retainedSccs = new LinkedHashSet<>();
            for (Integer sccId : decomposition.reachableSccOrderFromInitial()) {
                int depth = decomposition.distanceFromInitialScc().getOrDefault(sccId, Integer.MAX_VALUE);
                if (depth <= allowedDepth) {
                    retainedSccs.add(sccId);
                }
            }

            // Under-approximation:
            // keep exactly the SCCs up to the current frontier depth in the condensation DAG.
            // This is a true subgraph of the original system, so it is language-sound as an under-approximation.
            approximation = buildUnderApproximation(original, decomposition, retainedSccs);
        }

        return approximation;
    }

    @Override
    public ApproximationStrategy refine(List<TraceWitness> fakeWitnesses) {
        retainedFrontierDepth += 1;

        return this;
    }

    @Override
    public String name() {
        return "scc-based-approximator";
    }

    private static SystemModel buildUnderApproximation(SystemModel original,
                                                       SccDecomposition decomposition,
                                                       Set<Integer> retainedSccs) {
        Set<String> keptStates = new LinkedHashSet<>();
        for (String state : original.states().keySet()) {
            int sccId = decomposition.stateToScc().get(state);
            if (retainedSccs.contains(sccId)) {
                keptStates.add(state);
            }
        }

        // Ensure the initial state always remains present.
        keptStates.add(original.initialState());

        Map<String, SystemModel.State> states = new LinkedHashMap<>();
        for (String stateName : keptStates) {
            SystemModel.State originalState = original.states().get(stateName);

            Set<String> keptTargets = new LinkedHashSet<>();
            for (String target : originalState.transitions()) {
                if (keptStates.contains(target)) {
                    keptTargets.add(target);
                }
            }

            states.put(stateName, new SystemModel.State(originalState.labels(), keptTargets));
        }

        return new SystemModel(original.atomicPropositions(), original.initialState(), states);
    }

    private static SystemModel buildOverApproximation(SystemModel original,
                                                      SccDecomposition decomposition) {
        Map<Integer, Set<String>> sccToStates = decomposition.sccToStates();
        Map<Integer, Set<Integer>> sccDag = decomposition.sccDag();

        Map<String, SystemModel.State> states = new LinkedHashMap<>();

        for (String stateName : original.states().keySet()) {
            SystemModel.State originalState = original.states().get(stateName);
            int ownScc = decomposition.stateToScc().get(stateName);

            // Complete transitions inside the SCC.
            Set<String> targets = new LinkedHashSet<>(sccToStates.get(ownScc));

            // Complete transitions to every state in each immediate successor SCC in the condensation graph.
            for (Integer succScc : sccDag.getOrDefault(ownScc, Set.of())) {
                targets.addAll(sccToStates.get(succScc));
            }

            states.put(stateName, new SystemModel.State(originalState.labels(), targets));
        }

        return new SystemModel(original.atomicPropositions(), original.initialState(), states);
    }

    private static SccDecomposition decompose(SystemModel model) {
        Map<String, Integer> index = new LinkedHashMap<>();
        Map<String, Integer> lowlink = new LinkedHashMap<>();
        Deque<String> stack = new ArrayDeque<>();
        Set<String> onStack = new HashSet<>();
        List<Set<String>> sccList = new ArrayList<>();
        int[] counter = {0};

        for (String state : model.states().keySet()) {
            if (!index.containsKey(state)) {
                strongConnect(state, model, index, lowlink, stack, onStack, sccList, counter);
            }
        }

        Map<String, Integer> stateToScc = new LinkedHashMap<>();
        Map<Integer, Set<String>> sccToStates = new LinkedHashMap<>();
        for (int i = 0; i < sccList.size(); i++) {
            Set<String> sccStates = sccList.get(i);
            sccToStates.put(i, new LinkedHashSet<>(sccStates));
            for (String s : sccStates) {
                stateToScc.put(s, i);
            }
        }

        Map<Integer, Set<Integer>> sccDag = new LinkedHashMap<>();
        for (int i = 0; i < sccList.size(); i++) {
            sccDag.put(i, new LinkedHashSet<>());
        }

        for (var entry : model.states().entrySet()) {
            String from = entry.getKey();
            int fromScc = stateToScc.get(from);
            for (String to : entry.getValue().transitions()) {
                int toScc = stateToScc.get(to);
                if (fromScc != toScc) {
                    sccDag.get(fromScc).add(toScc);
                }
            }
        }

        int initialScc = stateToScc.get(model.initialState());
        Map<Integer, Integer> distanceFromInitial = bfsDistances(initialScc, sccDag);
        List<Integer> reachableOrder = new ArrayList<>(distanceFromInitial.keySet());
        reachableOrder.sort(Comparator
                .comparingInt((Integer scc) -> distanceFromInitial.getOrDefault(scc, Integer.MAX_VALUE))
                .thenComparingInt(scc -> -sccToStates.get(scc).size())
                .thenComparingInt(Integer::intValue));

        return new SccDecomposition(stateToScc, sccToStates, sccDag, initialScc, distanceFromInitial, reachableOrder);
    }

    private static Map<Integer, Integer> bfsDistances(int start, Map<Integer, Set<Integer>> graph) {
        Map<Integer, Integer> distance = new LinkedHashMap<>();
        Deque<Integer> queue = new ArrayDeque<>();

        distance.put(start, 0);
        queue.add(start);

        while (!queue.isEmpty()) {
            int current = queue.removeFirst();
            int nextDistance = distance.get(current) + 1;

            for (Integer next : graph.getOrDefault(current, Set.of())) {
                if (!distance.containsKey(next)) {
                    distance.put(next, nextDistance);
                    queue.add(next);
                }
            }
        }

        return distance;
    }

    private static void strongConnect(String v,
                                      SystemModel model,
                                      Map<String, Integer> index,
                                      Map<String, Integer> lowlink,
                                      Deque<String> stack,
                                      Set<String> onStack,
                                      List<Set<String>> sccList,
                                      int[] counter) {
        index.put(v, counter[0]);
        lowlink.put(v, counter[0]);
        counter[0]++;

        stack.push(v);
        onStack.add(v);

        for (String w : model.states().get(v).transitions()) {
            if (!index.containsKey(w)) {
                strongConnect(w, model, index, lowlink, stack, onStack, sccList, counter);
                lowlink.put(v, Math.min(lowlink.get(v), lowlink.get(w)));
            } else if (onStack.contains(w)) {
                lowlink.put(v, Math.min(lowlink.get(v), index.get(w)));
            }
        }

        if (Objects.equals(lowlink.get(v), index.get(v))) {
            Set<String> scc = new LinkedHashSet<>();
            String w;
            do {
                w = stack.pop();
                onStack.remove(w);
                scc.add(w);
            } while (!w.equals(v));
            sccList.add(scc);
        }
    }

    private record SccDecomposition(Map<String, Integer> stateToScc,
                                    Map<Integer, Set<String>> sccToStates,
                                    Map<Integer, Set<Integer>> sccDag,
                                    int initialScc,
                                    Map<Integer, Integer> distanceFromInitialScc,
                                    List<Integer> reachableSccOrderFromInitial) {
    }
}