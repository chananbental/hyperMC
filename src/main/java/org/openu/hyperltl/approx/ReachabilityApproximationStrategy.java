package org.openu.hyperltl.approx;

import org.openu.hyperltl.model.SystemModel;
import org.openu.hyperltl.trace.TraceWitness;

import java.util.*;

public final class ReachabilityApproximationStrategy implements ApproximationStrategy {

    private int underDepth = 1;
    private int overDepthPadding = 1;

    private static SystemModel restrictToDepth(SystemModel original, int depth) {
        Set<String> keep = bfs(original, depth);
        return inducedSubgraph(original, keep);
    }

    private static SystemModel relaxWithReachableClosure(SystemModel original, int depth) {
        Set<String> keep = bfs(original, depth);
        return inducedSubgraph(original, keep);
    }

    private static Set<String> bfs(SystemModel original, int depth) {
        Set<String> visited = new LinkedHashSet<>();
        Deque<Node> q = new ArrayDeque<>();
        q.add(new Node(original.initialState(), 0));
        visited.add(original.initialState());
        while (!q.isEmpty()) {
            Node n = q.removeFirst();
            if (n.depth >= depth) {
                continue;
            }
            for (String nxt : original.states().get(n.state).transitions()) {
                if (visited.add(nxt)) {
                    q.add(new Node(nxt, n.depth + 1));
                }
            }
        }
        return visited;
    }

    private static SystemModel inducedSubgraph(SystemModel original, Set<String> keep) {
        Map<String, SystemModel.State> states = new LinkedHashMap<>();
        for (String s : keep) {
            var state = original.states().get(s);
            Set<String> targets = new LinkedHashSet<>();
            for (String t : state.transitions()) {
                if (keep.contains(t)) {
                    targets.add(t);
                }
            }
            if (targets.isEmpty()) {
                targets.add(s);
            }
            states.put(s, new SystemModel.State(state.labels(), targets));
        }
        return new SystemModel(original.atomicPropositions(), original.initialState(), states);
    }

    @Override
    public SystemModel approximate(SystemModel original, ApproximationType approximationType, int refinementRound) {
        return approximationType.equals(ApproximationType.UNDER) ?
                restrictToDepth(original, Math.max(1, underDepth + refinementRound)) :
                relaxWithReachableClosure(original, Math.max(1, underDepth + overDepthPadding + refinementRound));
    }

    @Override
    public ApproximationStrategy refine(List<TraceWitness> fakeWitnesses) {
        this.underDepth += 1;
        this.overDepthPadding += 1;
        return this;
    }

    @Override
    public String name() {
        return "reachable-under-over";
    }

    private record Node(String state, int depth) {
    }
}