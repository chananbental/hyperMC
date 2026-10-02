package org.openu.hyperltl.cases.formulas;

import org.openu.hyperltl.formula.HyperLTLFormula;

import java.util.List;

public abstract class MetaFormulaSuit implements FormulaSuit{

    @Override
    public List<HyperLTLFormula> getFormulas() {
        return getFormulaSuits().stream().flatMap(suit -> suit.getFormulas().stream()).toList();
    }

    public abstract List<FormulaSuit> getFormulaSuits();
}
