package org.openu.hyperltl;

public record ModelCheckingResult(boolean holds,
                                  String message,
                                  HyperCounterexample counterexample,
                                  int refinementRounds) {

    public String prettyPrint() {
        StringBuilder sb = new StringBuilder();
        sb.append(holds ? "RESULT: HOLDS\n" : "RESULT: DOES NOT HOLD\n");
        sb.append(message).append('\n');
        if (counterexample != null && !counterexample.traces().isEmpty()) {
            sb.append("Counterexample:\n");
            sb.append(counterexample).append('\n');
        }
        return sb.toString();
    }
}