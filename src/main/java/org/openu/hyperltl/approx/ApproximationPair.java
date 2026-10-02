package org.openu.hyperltl.approx;

import org.openu.hyperltl.model.SystemModel;

import static java.util.Objects.nonNull;

public record ApproximationPair(SystemModel underApproximation,
                                SystemModel overApproximation) {
    @Override
    public boolean equals(Object obj) {
        return nonNull(obj) && obj instanceof ApproximationPair
                && underApproximation.equals(((ApproximationPair) obj).underApproximation)
                && overApproximation.equals(((ApproximationPair) obj).overApproximation);
    }
}