package org.openu.hyperltl.formula;

import java.util.ArrayList;
import java.util.List;

public final class HyperLTLParser {

    private List<Token> tokens;
    private int pos;

    public HyperLTLFormula parse(String text) {
        this.tokens = new Tokenizer(text).tokenize();
        this.pos = 0;

        List<QuantifiedVariable> quantifiers = new ArrayList<>();
        while (peekType(TokenType.FORALL) || peekType(TokenType.EXISTS)) {
            Quantifier q = consume().type() == TokenType.FORALL ? Quantifier.FORALL : Quantifier.EXISTS;
            String var = expect(TokenType.IDENT).text();
            expect(TokenType.DOT);
            quantifiers.add(new QuantifiedVariable(q, var));
        }

        LTLFormula body = parseIff();
        expect(TokenType.EOF);

        return new HyperLTLFormula(quantifiers, body);
    }

    private LTLFormula parseIff() {
        LTLFormula left = parseImplies();
        while (peekType(TokenType.IFF)) {
            consume();
            left = new BinaryFormula(BinaryOp.IFF, left, parseImplies());
        }
        return left;
    }

    private LTLFormula parseImplies() {
        LTLFormula left = parseOr();
        while (peekType(TokenType.IMPLIES)) {
            consume();
            left = new BinaryFormula(BinaryOp.IMPLIES, left, parseOr());
        }
        return left;
    }

    private LTLFormula parseOr() {
        LTLFormula left = parseAnd();
        while (peekType(TokenType.OR)) {
            consume();
            left = new BinaryFormula(BinaryOp.OR, left, parseAnd());
        }
        return left;
    }

    private LTLFormula parseAnd() {
        LTLFormula left = parseTemporal();
        while (peekType(TokenType.AND)) {
            consume();
            left = new BinaryFormula(BinaryOp.AND, left, parseTemporal());
        }
        return left;
    }

    private LTLFormula parseTemporal() {
        LTLFormula left = parseUnary();
        while (peekType(TokenType.U) || peekType(TokenType.R) || peekType(TokenType.W)) {
            Token op = consume();
            BinaryOp bop = switch (op.type()) {
                case U -> BinaryOp.U;
                case R -> BinaryOp.R;
                case W -> BinaryOp.W;
                default -> throw new IllegalStateException();
            };
            left = new BinaryFormula(bop, left, parseUnary());
        }
        return left;
    }

    private LTLFormula parseUnary() {
        if (peekType(TokenType.NOT)) {
            consume();
            return new UnaryFormula(UnaryOp.NOT, parseUnary());
        }
        if (peekType(TokenType.X)) {
            consume();
            return new UnaryFormula(UnaryOp.X, parseUnary());
        }
        if (peekType(TokenType.F)) {
            consume();
            return new UnaryFormula(UnaryOp.F, parseUnary());
        }
        if (peekType(TokenType.G)) {
            consume();
            return new UnaryFormula(UnaryOp.G, parseUnary());
        }
        if (peekType(TokenType.LPAREN)) {
            consume();
            LTLFormula inner = parseIff();
            expect(TokenType.RPAREN);
            return inner;
        }
        return new Atomic(expect(TokenType.IDENT).text());
    }

    private boolean peekType(TokenType type) {
        return tokens.get(pos).type() == type;
    }

    private Token consume() {
        return tokens.get(pos++);
    }

    private Token expect(TokenType type) {
        Token token = consume();
        if (token.type() != type) {
            throw new IllegalArgumentException("Expected " + type + " but got " + token.type() + " near token '" + token.text() + "'");
        }
        return token;
    }

    private enum TokenType {
        FORALL, EXISTS, DOT,
        LPAREN, RPAREN,
        NOT, AND, OR, IMPLIES, IFF,
        X, F, G, U, R, W,
        IDENT, EOF
    }

    private record Token(TokenType type, String text) {
    }

    private static final class Tokenizer {
        private final String input;
        private int i;

        private Tokenizer(String input) {
            this.input = input;
        }

        List<Token> tokenize() {
            List<Token> out = new ArrayList<>();
            while (i < input.length()) {
                char c = input.charAt(i);
                if (Character.isWhitespace(c)) {
                    i++;
                    continue;
                }
                if (input.startsWith("forall", i)) {
                    out.add(new Token(TokenType.FORALL, "forall"));
                    i += 6;
                    continue;
                }
                if (input.startsWith("exists", i)) {
                    out.add(new Token(TokenType.EXISTS, "exists"));
                    i += 6;
                    continue;
                }
                if (input.startsWith("A", i) && boundary(i + 1)) {
                    out.add(new Token(TokenType.FORALL, "A"));
                    i += 1;
                    continue;
                }
                if (input.startsWith("E", i) && boundary(i + 1)) {
                    out.add(new Token(TokenType.EXISTS, "E"));
                    i += 1;
                    continue;
                }
                if (input.startsWith("<->", i)) {
                    out.add(new Token(TokenType.IFF, "<->"));
                    i += 3;
                    continue;
                }
                if (input.startsWith("->", i)) {
                    out.add(new Token(TokenType.IMPLIES, "->"));
                    i += 2;
                    continue;
                }
                switch (c) {
                    case '.' -> out.add(new Token(TokenType.DOT, "."));
                    case '(' -> out.add(new Token(TokenType.LPAREN, "("));
                    case ')' -> out.add(new Token(TokenType.RPAREN, ")"));
                    case '!' -> out.add(new Token(TokenType.NOT, "!"));
                    case '&' -> out.add(new Token(TokenType.AND, "&"));
                    case '|' -> out.add(new Token(TokenType.OR, "|"));
                    default -> {
                        if (c == 'X') {
                            out.add(new Token(TokenType.X, "X"));
                            i++;
                            continue;
                        }
                        if (c == 'F') {
                            out.add(new Token(TokenType.F, "F"));
                            i++;
                            continue;
                        }
                        if (c == 'G') {
                            out.add(new Token(TokenType.G, "G"));
                            i++;
                            continue;
                        }
                        if (c == 'U') {
                            out.add(new Token(TokenType.U, "U"));
                            i++;
                            continue;
                        }
                        if (c == 'R') {
                            out.add(new Token(TokenType.R, "R"));
                            i++;
                            continue;
                        }
                        if (c == 'W') {
                            out.add(new Token(TokenType.W, "W"));
                            i++;
                            continue;
                        }
                        if (Character.isLetter(c) || c == '_') {
                            int start = i;
                            i++;
                            while (i < input.length()) {
                                char d = input.charAt(i);
                                if (Character.isLetterOrDigit(d) || d == '_' || d == '@') {
                                    i++;
                                } else {
                                    break;
                                }
                            }
                            out.add(new Token(TokenType.IDENT, input.substring(start, i)));
                            continue;
                        }
                        throw new IllegalArgumentException("Unexpected character: " + c + " at index " + i);
                    }
                }
                i++;
            }
            out.add(new Token(TokenType.EOF, "<eof>"));
            return out;
        }

        private boolean boundary(int idx) {
            return idx >= input.length() || !Character.isLetterOrDigit(input.charAt(idx)) && input.charAt(idx) != '_';
        }
    }
}