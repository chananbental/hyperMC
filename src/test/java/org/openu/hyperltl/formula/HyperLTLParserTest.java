package org.openu.hyperltl.formula;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class HyperLTLParserTest {

    private final HyperLTLParser parser = new HyperLTLParser();

    @Test
    void parsesQuantifiersAndBody() {
        HyperLTLFormula formula = parser.parse("forall pi1. exists pi2. G(a_pi1 -> F(b_pi2))");

        assertEquals(2, formula.quantifiers().size());

        assertEquals(Quantifier.FORALL, formula.quantifiers().get(0).quantifier());
        assertEquals("pi1", formula.quantifiers().get(0).variable());

        assertEquals(Quantifier.EXISTS, formula.quantifiers().get(1).quantifier());
        assertEquals("pi2", formula.quantifiers().get(1).variable());

        assertEquals("G((a_pi1 -> F(b_pi2)))", formula.body().toSpotString());
    }

    @Test
    void parsesShortQuantifierNotation() {
        HyperLTLFormula formula = parser.parse("A pi1. E pi2. (a_pi1 U b_pi2)");

        assertEquals(2, formula.quantifiers().size());
        assertEquals(Quantifier.FORALL, formula.quantifiers().get(0).quantifier());
        assertEquals(Quantifier.EXISTS, formula.quantifiers().get(1).quantifier());
        assertEquals("(a_pi1 U b_pi2)", formula.body().toSpotString());
    }

    @Test
    void rejectsInvalidFormula() {
        assertThrows(IllegalArgumentException.class, () ->
                parser.parse("forall pi1 exists pi2. G(a_pi1)")
        );
    }
}