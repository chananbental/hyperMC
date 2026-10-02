package org.openu.hyperltl.automaton;

import java.util.Map;
import java.util.Set;

public record BoolConst(boolean value) implements BooleanFormula {

    @Override
    public boolean evaluate(Set<String> valuation) {
        return value;
    }

    @Override
    public String toHoa(Map<String, Integer> apIndex) {
        return value ? "t" : "f";
    }
}