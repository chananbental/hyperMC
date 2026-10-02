package org.openu.hyperltl.automaton;

import java.util.Map;
import java.util.Set;

public record BoolAtom(String ap) implements BooleanFormula {

    @Override
    public boolean evaluate(Set<String> valuation) {
        return valuation.contains(ap);
    }

    @Override
    public String toHoa(Map<String, Integer> apIndex) {
        Integer idx = apIndex.get(ap);
        if (idx == null) {
            throw new IllegalArgumentException("Unknown AP in HOA emission: " + ap);
        }
        return idx.toString();
    }
}