package org.openu.hyperltl.formula;

import java.util.LinkedHashSet;
import java.util.Set;

public record BinaryFormula(BinaryOp op, LTLFormula left, LTLFormula right) implements LTLFormula {

    @Override
    public String toSpotString() {
        return "(" + left.toSpotString() + " " + op.text() + " " + right.toSpotString() + ")";
    }

    @Override
    public Set<String> atomicPropositions() {
        Set<String> result = new LinkedHashSet<>(left.atomicPropositions());
        result.addAll(right.atomicPropositions());

        return result;
    }

    @Override
    public String formalPrint() {
        boolean isLeftSideBinary = left instanceof BinaryFormula;
        boolean isRightSideBinary = right instanceof BinaryFormula;

        String leftSide = isLeftSideBinary ? "(" + left.formalPrint() + ")" : left.formalPrint();
        String rightSide = isRightSideBinary ? "(" + right.formalPrint() + ")" : right.formalPrint();

        return leftSide + " " + op.text() + " " + rightSide;
    }
}
