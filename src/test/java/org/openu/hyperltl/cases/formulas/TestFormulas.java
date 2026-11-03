package org.openu.hyperltl.cases.formulas;

import org.openu.hyperltl.formula.HyperLTLFormula;
import org.openu.hyperltl.formula.HyperLTLParser;

import java.util.List;

public class TestFormulas extends MetaFormulaSuit {

    // AutoHyper preprocessing requires each quantified trace to appear in the body.
    // G(a_pi | !a_pi) clauses expose otherwise unused traces without changing truth.

    public static TestFormulas formulas = new TestFormulas();
    private static final HyperLTLParser parser = new HyperLTLParser();
    List<FormulaSuit> formulaSuits = List.of(new NoAlternation(), new OneAlternation(), new TwoAlternation(), new ThreeAlternation());

    @Override
    public List<FormulaSuit> getFormulaSuits() {
        return formulaSuits;
    }

    public static class NoAlternation extends MetaFormulaSuit {

        @Override
        public List<FormulaSuit> getFormulaSuits() {
            return List.of(new AA(), new EE());
        }

        public static class AA implements FormulaSuit {
            public final HyperLTLFormula firstFormula = parser.parse("forall pi1. forall pi2. G(a_pi1 -> F a_pi2)");
            public final HyperLTLFormula secondFormula = parser.parse("forall pi1. forall pi2. G((a_pi1 & b_pi1) -> F c_pi2)");
            public final HyperLTLFormula thirdFormula = parser.parse("forall pi1. forall pi2. G(a_pi1 -> X(a_pi2 | !a_pi2))");

            @Override
            public List<HyperLTLFormula> getFormulas() {
                return List.of(firstFormula, secondFormula, thirdFormula);
            }
        }

        public static class EE implements FormulaSuit {
            public final HyperLTLFormula firstFormula = parser.parse("exists pi1. exists pi2. F(a_pi1 & b_pi2)");
            public final HyperLTLFormula secondFormula = parser.parse("exists pi1. exists pi2. G(a_pi1 -> F c_pi2)");
            public final HyperLTLFormula thirdFormula = parser.parse("exists pi1. exists pi2. F a_pi1 & F b_pi2");

            @Override
            public List<HyperLTLFormula> getFormulas() {
                return List.of(firstFormula, secondFormula, thirdFormula);
            }
        }
    }

    public static class OneAlternation extends MetaFormulaSuit {

        @Override
        public List<FormulaSuit> getFormulaSuits() {
            return List.of(new AE(), new EA());
        }

        public static class AE implements FormulaSuit {
            public final HyperLTLFormula firstFormula = parser.parse("forall pi1. forall pi2. exists pi3. (G(a_pi1 -> F a_pi3)) & G(a_pi2 | !a_pi2)");
            public final HyperLTLFormula secondFormula = parser.parse("forall pi1. exists pi2. G(b_pi1 -> F c_pi2)");
            public final HyperLTLFormula thirdFormula = parser.parse("forall pi1. exists pi2. exists pi3. G(a_pi1 -> (F b_pi2 | F c_pi3))");

            @Override
            public List<HyperLTLFormula> getFormulas() {
                return List.of(firstFormula, secondFormula, thirdFormula);
            }
        }

        public static class EA implements FormulaSuit {
            public final HyperLTLFormula firstFormula = parser.parse("exists pi1. forall pi2. G(a_pi2 -> F a_pi1)");
            public final HyperLTLFormula secondFormula = parser.parse("exists pi1. exists pi2. forall pi3. (G(b_pi3 -> F c_pi1)) & G(a_pi2 | !a_pi2)");
            public final HyperLTLFormula thirdFormula = parser.parse("exists pi1. forall pi2. forall pi3. G((a_pi2 & b_pi3) -> F c_pi1)");

            @Override
            public List<HyperLTLFormula> getFormulas() {
                return List.of(firstFormula, secondFormula, thirdFormula);
            }
        }
    }

    public static class TwoAlternation extends MetaFormulaSuit {

        @Override
        public List<FormulaSuit> getFormulaSuits() {
            return List.of(new AEA(), new EAE());
        }

        public static class AEA implements FormulaSuit {
            public final HyperLTLFormula firstFormula = parser.parse("forall pi1. exists pi2. forall pi3. (G(a_pi1 -> F a_pi2)) & G(a_pi3 | !a_pi3)");
            public final HyperLTLFormula secondFormula = parser.parse("forall pi1. exists pi2. forall pi3. (G(b_pi3 -> F c_pi2)) & G(a_pi1 | !a_pi1)");
            public final HyperLTLFormula thirdFormula = parser.parse("forall pi1. exists pi2. forall pi3. G((a_pi1 & b_pi3) -> F(a_pi2 | c_pi2))");

            @Override
            public List<HyperLTLFormula> getFormulas() {
                return List.of(firstFormula, secondFormula, thirdFormula);
            }
        }

        public static class EAE implements FormulaSuit {
            public final HyperLTLFormula firstFormula = parser.parse("exists pi1. forall pi2. exists pi3. (G(a_pi2 -> F a_pi3)) & G(a_pi1 | !a_pi1)");
            public final HyperLTLFormula secondFormula = parser.parse("exists pi1. forall pi2. exists pi3. (G(b_pi2 -> F c_pi3)) & G(a_pi1 | !a_pi1)");
            public final HyperLTLFormula thirdFormula = parser.parse("exists pi1. forall pi2. exists pi3. G((a_pi2 & b_pi2) -> F(a_pi1 | c_pi3))");

            @Override
            public List<HyperLTLFormula> getFormulas() {
                return List.of(firstFormula, secondFormula, thirdFormula);
            }
        }
    }

    public static class ThreeAlternation extends MetaFormulaSuit {

        @Override
        public List<FormulaSuit> getFormulaSuits() {
            return List.of(new AEAE(), new EAEA());
        }

        public static class AEAE implements FormulaSuit {
            public final HyperLTLFormula firstFormula = parser.parse("forall pi1. exists pi2. forall pi3. exists pi4. ((G(a_pi1 -> F a_pi4)) & G(a_pi2 | !a_pi2)) & G(a_pi3 | !a_pi3)");
            public final HyperLTLFormula secondFormula = parser.parse("forall pi1. exists pi2. forall pi3. exists pi4. ((G(b_pi3 -> F c_pi4)) & G(a_pi1 | !a_pi1)) & G(a_pi2 | !a_pi2)");
            public final HyperLTLFormula thirdFormula = parser.parse("forall pi1. exists pi2. forall pi3. exists pi4. G((a_pi1 & b_pi3) -> F(a_pi2 | c_pi4))");

            @Override
            public List<HyperLTLFormula> getFormulas() {
                return List.of(firstFormula, secondFormula, thirdFormula);
            }
        }

        public static class EAEA implements FormulaSuit {
            public final HyperLTLFormula firstFormula = parser.parse("exists pi1. forall pi2. exists pi3. forall pi4. ((G(a_pi4 -> F a_pi3)) & G(a_pi1 | !a_pi1)) & G(a_pi2 | !a_pi2)");
            public final HyperLTLFormula secondFormula = parser.parse("exists pi1. forall pi2. exists pi3. forall pi4. ((G(b_pi2 -> F c_pi1)) & G(a_pi3 | !a_pi3)) & G(a_pi4 | !a_pi4)");
            public final HyperLTLFormula thirdFormula = parser.parse("exists pi1. forall pi2. exists pi3. forall pi4. G((a_pi2 & b_pi4) -> F(a_pi1 | c_pi3))");

            @Override
            public List<HyperLTLFormula> getFormulas() {
                return List.of(firstFormula, secondFormula, thirdFormula);
            }
        }
    }
}
