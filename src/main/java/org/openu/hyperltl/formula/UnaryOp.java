package org.openu.hyperltl.formula;

public enum UnaryOp {
    NOT("!"),
    X("X"),
    F("F"),
    G("G");

    private final String text;

    UnaryOp(String text) {
        this.text = text;
    }

    public String text() {
        return text;
    }
}