package org.openu.hyperltl.trace;

import java.util.Set;

public record TraceStep(String state, Set<String> valuation) {
}