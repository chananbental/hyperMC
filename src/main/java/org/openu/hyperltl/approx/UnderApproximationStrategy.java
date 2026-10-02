package org.openu.hyperltl.approx;

import org.openu.hyperltl.model.SystemModel;
import org.openu.hyperltl.trace.TraceWitness;

import java.util.List;

public interface UnderApproximationStrategy {

    SystemModel underApproximate(SystemModel original, int refinementRound);
    UnderApproximationStrategy refine(List<TraceWitness> fakeCounterexampleTuple);
    String name();
}