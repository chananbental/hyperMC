package org.openu.hyperltl.formula;

import java.util.Set;

public sealed interface LTLFormula permits Atomic, UnaryFormula, BinaryFormula {

    String toSpotString();

    Set<String> atomicPropositions();

    String formalPrint();
}