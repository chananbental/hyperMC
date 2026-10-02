package org.openu.hyperltl.approx;

import org.openu.hyperltl.model.SystemModel;
import org.openu.hyperltl.trace.TraceWitness;

import java.util.List;

import static org.openu.hyperltl.utils.Timer.runAndTime;

public final class ApproximationCoordinator {

    private ApproximationStrategy underStrategy;
    private ApproximationStrategy overStrategy;

    public ApproximationCoordinator(ApproximationStrategy underStrategy,
                                    ApproximationStrategy overStrategy) {
        this.underStrategy = underStrategy;
        this.overStrategy = overStrategy;
    }

    public ApproximationPair approximate(SystemModel original, int refinementRound) {
        return new ApproximationPair(
                approximate(original, ApproximationType.UNDER, refinementRound),
                approximate(original, ApproximationType.OVER, refinementRound)
        );
    }

    private SystemModel approximate(SystemModel original, ApproximationType approximationType, int refinementRound) {
        return runAndTime(
                () -> (approximationType.equals(ApproximationType.UNDER) ? underStrategy : overStrategy).approximate(original, approximationType, refinementRound),
                elapsedTime -> IO.println(elapsedTime + " time to " + approximationType.toString().toLowerCase() + " approximate in refinementRound #" + refinementRound + ": " + elapsedTime)
        );
    }

    public void refine(List<TraceWitness> fakeCounterexampleTuple) {
        underStrategy = underStrategy.refine(fakeCounterexampleTuple);
        overStrategy = overStrategy.refine(fakeCounterexampleTuple);
    }

    public String name() {
        return "under=" + underStrategy.name() + ", over=" + overStrategy.name();
    }
}