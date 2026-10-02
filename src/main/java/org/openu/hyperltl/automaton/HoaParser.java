package org.openu.hyperltl.automaton;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class HoaParser {

    private static final Pattern STATES = Pattern.compile("^States:\\s+(\\d+)$");
    private static final Pattern START = Pattern.compile("^Start:\\s+(.+)$");
    private static final Pattern AP = Pattern.compile("^AP:\\s+(\\d+)\\s+(.+)$");
    private static final Pattern STATE = Pattern.compile("^State:\\s+(\\d+)(?:\\s+\\{([^}]*)})?.*$");
    private static final Pattern EDGE = Pattern.compile("^\\[(.+)]\\s+(\\d+).*$");

    public NBA parse(String hoa) {
        String[] lines = hoa.split("\\R");
        int stateCount = -1;
        Set<Integer> initialStates = new LinkedHashSet<>();
        List<String> aps = new ArrayList<>();
        Set<Integer> accepting = new LinkedHashSet<>();
        Map<Integer, List<NBA.Transition>> transitions = new LinkedHashMap<>();

        boolean body = false;
        Integer currentState = null;

        for (String raw : lines) {
            String line = raw.trim();
            if (line.isEmpty()) {
                continue;
            }
            if (line.equals("--BODY--")) {
                body = true;
                continue;
            }
            if (line.equals("--END--")) {
                break;
            }
            if (!body) {
                Matcher ms = STATES.matcher(line);
                if (ms.matches()) {
                    stateCount = Integer.parseInt(ms.group(1));
                    continue;
                }
                Matcher mst = START.matcher(line);
                if (mst.matches()) {
                    for (String part : mst.group(1).split("&| ")) {
                        String p = part.trim();
                        if (!p.isEmpty()) {
                            initialStates.add(Integer.parseInt(p));
                        }
                    }
                    continue;
                }
                Matcher map = AP.matcher(line);
                if (map.matches()) {
                    String rest = map.group(2).trim();
                    Matcher q = Pattern.compile("\"([^\"]+)\"").matcher(rest);
                    while (q.find()) {
                        aps.add(q.group(1));
                    }
                }
                continue;
            }

            Matcher stateMatcher = STATE.matcher(line);
            if (stateMatcher.matches()) {
                currentState = Integer.parseInt(stateMatcher.group(1));
                transitions.putIfAbsent(currentState, new ArrayList<>());
                String acc = stateMatcher.group(2);
                if (acc != null && !acc.isBlank()) {
                    accepting.add(currentState);
                }
                continue;
            }

            Matcher edge = EDGE.matcher(line);
            if (edge.matches() && currentState != null) {
                String labelText = edge.group(1).trim();
                int to = Integer.parseInt(edge.group(2));
                BooleanFormula label = parseBooleanFormula(labelText, aps);
                transitions.computeIfAbsent(currentState, __ -> new ArrayList<>())
                        .add(new NBA.Transition(currentState, to, label));
            }
        }

        if (stateCount < 0) {
            throw new IllegalArgumentException("Invalid HOA: missing state count");
        }

        return new NBA(aps, initialStates, transitions, accepting, stateCount);
    }

    private BooleanFormula parseBooleanFormula(String input, List<String> aps) {
        return new HoaLabelParser(input, aps).parse();
    }

    private static final class HoaLabelParser {
        private final String input;
        private final List<String> aps;
        private int i;

        HoaLabelParser(String input, List<String> aps) {
            this.input = input;
            this.aps = aps;
        }

        BooleanFormula parse() {
            BooleanFormula f = parseOr();
            skip();
            return f;
        }

        private BooleanFormula parseOr() {
            BooleanFormula left = parseAnd();
            while (true) {
                skip();
                if (peek('|')) {
                    i++;
                    left = new BoolOr(left, parseAnd());
                } else {
                    return left;
                }
            }
        }

        private BooleanFormula parseAnd() {
            BooleanFormula left = parseUnary();
            while (true) {
                skip();
                if (peek('&')) {
                    i++;
                    left = new BoolAnd(left, parseUnary());
                } else {
                    return left;
                }
            }
        }

        private BooleanFormula parseUnary() {
            skip();
            if (peek('!')) {
                i++;
                return new BoolNot(parseUnary());
            }
            if (peek('(')) {
                i++;
                BooleanFormula inner = parseOr();
                skip();
                expect(')');
                return inner;
            }
            if (peek('t')) {
                i++;
                return new BoolConst(true);
            }
            if (peek('f')) {
                i++;
                return new BoolConst(false);
            }
            return parseIndexAtom();
        }

        private BooleanFormula parseIndexAtom() {
            skip();
            int start = i;
            while (i < input.length() && Character.isDigit(input.charAt(i))) {
                i++;
            }
            if (start == i) {
                throw new IllegalArgumentException("Expected HOA AP index near: " + input.substring(Math.min(i, input.length())));
            }
            int idx = Integer.parseInt(input.substring(start, i));
            return new BoolAtom(aps.get(idx));
        }

        private void skip() {
            while (i < input.length() && Character.isWhitespace(input.charAt(i))) {
                i++;
            }
        }

        private boolean peek(char c) {
            return i < input.length() && input.charAt(i) == c;
        }

        private void expect(char c) {
            if (!peek(c)) {
                throw new IllegalArgumentException("Expected '" + c + "' in HOA label: " + input);
            }
            i++;
        }
    }
}