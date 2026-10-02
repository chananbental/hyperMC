package org.openu.hyperltl.automaton;

import java.util.Map;
import java.util.Set;

public record BoolAnd(BooleanFormula left, BooleanFormula right) implements BooleanFormula {

    @Override
    public boolean evaluate(Set<String> valuation) {
        return left.evaluate(valuation) && right.evaluate(valuation);
    }

    @Override
    public String toHoa(Map<String, Integer> apIndex) {
        return "(" + left.toHoa(apIndex) + " & " + right.toHoa(apIndex) + ")";
    }
}