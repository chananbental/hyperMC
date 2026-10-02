package org.openu.hyperltl.automaton;

import java.util.Map;
import java.util.Set;

public sealed interface BooleanFormula permits BoolConst, BoolAtom, BoolNot, BoolAnd, BoolOr {

    boolean evaluate(Set<String> valuation);

    String toHoa(Map<String, Integer> apIndex);
}