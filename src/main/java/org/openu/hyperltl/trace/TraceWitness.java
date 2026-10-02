package org.openu.hyperltl.trace;

import java.util.List;

public record TraceWitness(String variable,
                           List<TraceStep> prefix,
                           List<TraceStep> loop) {
}