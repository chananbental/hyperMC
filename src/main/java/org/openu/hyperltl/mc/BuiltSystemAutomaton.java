package org.openu.hyperltl.mc;

import org.openu.hyperltl.automaton.NBA;

import java.util.Map;

public record BuiltSystemAutomaton(NBA nba,
                                   Map<Integer, String> automatonStateToSystemState) {
}