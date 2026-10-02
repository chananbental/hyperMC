package org.openu.hyperltl.approx;

import org.openu.hyperltl.model.SystemModel;
import org.openu.hyperltl.trace.TraceWitness;

import java.util.List;

public final class ExactApproximationStrategy implements ApproximationStrategy {
    @Override
    public SystemModel approximate(SystemModel original, ApproximationType approximationType, int refinementRound) {
        return original;
    }

    @Override
    public ApproximationStrategy refine(List<TraceWitness> fakeCounterexampleTuple) {
        return this;
    }

    @Override
    public String name() {
        return "exact";
    }
}