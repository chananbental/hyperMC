package org.openu.hyperltl.formula;

import java.util.List;
import java.util.stream.Collectors;

public record HyperLTLFormula(List<QuantifiedVariable> quantifiers, LTLFormula body) {

    public String prettyPrint() {
        return quantifiers.stream()
                .map(QuantifiedVariable::prettyPrint)
                .collect(Collectors.joining()) + " " + body.toSpotString();
    }

    public String formalPrint() {
        return quantifiers.stream()
                .map(QuantifiedVariable::formalPrint)
                .collect(Collectors.joining(" ")) + " " + body.formalPrint();
    }

    public HyperLTLFormula {
        quantifiers = List.copyOf(quantifiers);
    }
}