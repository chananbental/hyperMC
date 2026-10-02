package org.openu.hyperltl.formula;

import java.util.Set;

public record UnaryFormula(UnaryOp op, LTLFormula child) implements LTLFormula {

    @Override
    public String toSpotString() {
        return op.text() + "(" + child.toSpotString() + ")";
    }

    @Override
    public Set<String> atomicPropositions() {
        return child.atomicPropositions();
    }

    @Override
    public String formalPrint() {
        return op.text() +
                (child instanceof Atomic ?
                        (op.equals(UnaryOp.NOT) ? "" : " ") + child.formalPrint() :
                        "(" + child.formalPrint() + ")"
                );
    }
}