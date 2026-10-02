package org.openu.hyperltl.mc;

import java.util.List;

public record AutomatonLasso(
        List<Integer> prefixStates,
        List<Integer> loopStates
) {
}