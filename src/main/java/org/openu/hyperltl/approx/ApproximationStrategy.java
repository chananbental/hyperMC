package org.openu.hyperltl.approx;

import org.openu.hyperltl.model.SystemModel;
import org.openu.hyperltl.trace.TraceWitness;

import java.util.List;

public interface ApproximationStrategy {

    SystemModel approximate(SystemModel original, ApproximationType approximationType, int refinementRound);
    ApproximationStrategy refine(List<TraceWitness> fakeCounterexampleTuple);
    String name();
}