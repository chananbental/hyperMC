package org.openu.hyperltl;

import org.openu.hyperltl.trace.TraceWitness;

import java.util.List;

public record HyperCounterexample(List<TraceWitness> traces) {
    public HyperCounterexample {
        traces = List.copyOf(traces);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("HyperCounterexample{\n");
        for (TraceWitness t : traces) {
            sb.append("  ").append(t).append('\n');
        }
        sb.append('}');
        return sb.toString();
    }
}