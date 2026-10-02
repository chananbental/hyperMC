package org.openu.hyperltl.formula;

public enum BinaryOp {
    AND("&"),
    OR("|"),
    IMPLIES("->"),
    IFF("<->"),
    U("U"),
    R("R"),
    W("W");

    private final String text;

    BinaryOp(String text) {
        this.text = text;
    }

    public String text() {
        return text;
    }
}