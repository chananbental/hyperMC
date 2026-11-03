package org.openu.hyperltl.mc;

import org.openu.hyperltl.automaton.NBA;

import java.util.*;
import java.util.function.IntPredicate;

public final class AutomatonLassoExtractor {

    public Optional<AutomatonLasso> extract(NBA nba) {
        if (nba.initialStates().isEmpty()) {
            return Optional.empty();
        }

        for (Integer init : nba.initialStates()) {
            for (Integer accept : nba.acceptingStates().stream().sorted().toList()) {
                List<Integer> prefix = bfs(nba, init, s -> s == accept, false);
                if (prefix == null) continue;
                List<Integer> loop = bfs(nba, accept, s -> s == accept, true);
                if (loop == null || loop.size() < 2) continue;
                return Optional.of(new AutomatonLasso(prefix, loop));
            }
        }

        return Optional.empty();
    }

    private List<Integer> bfs(NBA nba, int start, IntPredicate goal, boolean requireOneEdge) {
        Deque<Integer> queue = new ArrayDeque<>();
        Map<Integer, Integer> prev = new HashMap<>();

        queue.add(start);
        prev.put(start, -1);

        while (!queue.isEmpty()) {
            int cur = queue.removeFirst();

            if (!requireOneEdge && goal.test(cur)) {
                return restore(prev, cur);
            }

            for (NBA.Transition t : nba.outgoing(cur)) {
                int next = t.to();

                // Important: allow closing a loop back to start
                if (requireOneEdge && goal.test(next)) {
                    List<Integer> path = restore(prev, cur);
                    path.add(next);
                    return path;
                }

                if (!prev.containsKey(next)) {
                    prev.put(next, cur);
                    queue.add(next);
                }
            }
        }

        return null;
    }

    private List<Integer> restore(Map<Integer, Integer> prev, int end) {
        LinkedList<Integer> path = new LinkedList<>();
        int cur = end;
        while (cur != -1) {
            path.addFirst(cur);
            cur = prev.get(cur);
        }
        return path;
    }
}
