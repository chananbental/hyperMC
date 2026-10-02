package org.openu.hyperltl.mc;

import org.openu.hyperltl.automaton.BoolAnd;
import org.openu.hyperltl.automaton.BooleanFormula;
import org.openu.hyperltl.automaton.NBA;

import java.util.*;

import static java.util.Objects.isNull;
import static org.openu.hyperltl.utils.Timer.runAndTime;

public final class ProductConstructionWithDecomposition {

    public static ProductBuildResult intersect(NBA left, BuiltSystemAutomaton rightSystem) {
        return runAndTime(() -> {
            NBA right = rightSystem.nba();
            List<String> mergedAp = mergeAps(left.atomicPropositions(), right.atomicPropositions());

            record ProductState(int leftState, int rightState, int phase) {
            }

            Map<ProductState, Integer> index = new LinkedHashMap<>();
            List<ProductState> reverse = new ArrayList<>();
            Map<Integer, List<NBA.Transition>> out = new LinkedHashMap<>();
            Set<Integer> initial = new LinkedHashSet<>();
            Set<Integer> accepting = new LinkedHashSet<>();
            Map<Integer, ProductStateComponents> decomposition = new LinkedHashMap<>();

            Deque<ProductState> queue = new ArrayDeque<>();
            Set<ProductState> discovered = new LinkedHashSet<>();

            for (int ls : left.initialStates()) {
                for (int rs : right.initialStates()) {
                    ProductState ps = new ProductState(ls, rs, 0);
                    int id = getOrCreate(ps, index, reverse, out);
                    initial.add(id);

                    queue.add(ps);
                    discovered.add(ps);
                    decomposition.put(id, new ProductStateComponents(ls, rs, 0));
                }
            }

            while (!queue.isEmpty()) {
                ProductState cur = queue.removeFirst();
                int curId = index.get(cur);

                if (cur.phase == 1 && right.acceptingStates().contains(cur.rightState)) {
                    accepting.add(curId);
                }

                for (NBA.Transition lt : left.outgoing(cur.leftState)) {
                    for (NBA.Transition rt : right.outgoing(cur.rightState)) {
                        BooleanFormula guard = new BoolAnd(lt.label(), rt.label());

                        int nextPhase = cur.phase;
                        if (cur.phase == 0 && left.acceptingStates().contains(lt.to())) {
                            nextPhase = 1;
                        } else if (cur.phase == 1 && right.acceptingStates().contains(rt.to())) {
                            nextPhase = 0;
                        }

                        ProductState nxt = new ProductState(lt.to(), rt.to(), nextPhase);
                        int nxtId = getOrCreate(nxt, index, reverse, out);

                        out.computeIfAbsent(curId, __ -> new ArrayList<>())
                                .add(new NBA.Transition(curId, nxtId, guard));

                        if (discovered.add(nxt)) {
                            queue.add(nxt);
                            decomposition.put(nxtId, new ProductStateComponents(lt.to(), rt.to(), nextPhase));
                        }
                    }
                }
            }

            NBA product = new NBA(mergedAp, initial, out, accepting, reverse.size());
            return new ProductBuildResult(product, decomposition);
        }, elapsedTime -> IO.println(elapsedTime + " time to intersect " + elapsedTime));
    }

    private static <S> int getOrCreate(S state, Map<S, Integer> index,
            List<S> reverse, Map<Integer, List<NBA.Transition>> out) {
        Integer id = index.get(state);
        if (isNull(id)) {
            id = reverse.size();
            index.put(state, id);
            reverse.add(state);
            out.putIfAbsent(id, new ArrayList<>());
        }

        return id;
    }

    private static List<String> mergeAps(List<String> a, List<String> b) {
        LinkedHashSet<String> merged = new LinkedHashSet<>(a);
        merged.addAll(b);

        return List.copyOf(merged);
    }
}