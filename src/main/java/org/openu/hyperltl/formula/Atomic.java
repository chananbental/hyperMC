package org.openu.hyperltl.formula;

import java.util.Set;

public record Atomic(String name) implements LTLFormula {

    @Override
    public String toSpotString() {
        return name;
    }

    @Override
    public Set<String> atomicPropositions() {
        return Set.of(name);
    }

    public String formalPrint() {
        String[] nameParts = name.split("_");

        return nameParts.length == 1 ?
                name :
                "\"" + nameParts[0] + "\"_" + nameParts[1];
    }
}