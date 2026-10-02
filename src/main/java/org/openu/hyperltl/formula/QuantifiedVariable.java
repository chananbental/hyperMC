package org.openu.hyperltl.formula;

public record QuantifiedVariable(Quantifier quantifier, String variable) {

    public String prettyPrint() {
        return (quantifier.equals(Quantifier.FORALL) ? "∀" : "∃") + variable + ".";
    }

    public String formalPrint() {
        return quantifier.toString().toLowerCase() + " " + variable + ".";
    }
}