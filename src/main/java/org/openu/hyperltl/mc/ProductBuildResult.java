package org.openu.hyperltl.mc;

import org.openu.hyperltl.automaton.NBA;

import java.util.Map;

public record ProductBuildResult(
        NBA nba,
        Map<Integer, ProductStateComponents> decomposition
) {
}