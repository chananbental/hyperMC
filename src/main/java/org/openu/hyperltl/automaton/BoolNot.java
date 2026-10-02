package org.openu.hyperltl.automaton;

import java.util.Map;
import java.util.Set;

public record BoolNot(BooleanFormula child) implements BooleanFormula {

    @Override
    public boolean evaluate(Set<String> valuation) {
        return !child.evaluate(valuation);
    }

    @Override
    public String toHoa(Map<String, Integer> apIndex) {
        return "!" + parenthesize(child, apIndex);
    }

    private String parenthesize(BooleanFormula formula, Map<String, Integer> apIndex) {
        return switch (formula) {
            case BoolAtom a -> a.toHoa(apIndex);
            case BoolConst c -> c.toHoa(apIndex);
            default -> "(" + formula.toHoa(apIndex) + ")";
        };
    }
}