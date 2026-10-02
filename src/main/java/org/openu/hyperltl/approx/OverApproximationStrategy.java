package org.openu.hyperltl.approx;

import org.openu.hyperltl.model.SystemModel;
import org.openu.hyperltl.trace.TraceWitness;

import java.util.List;

public interface OverApproximationStrategy {
    SystemModel overApproximate(SystemModel original, int refinementRound);
    OverApproximationStrategy refine(List<TraceWitness> fakeCounterexampleTuple);
    String name();
}