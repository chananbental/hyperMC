package org.openu.hyperltl.mc;

import org.openu.hyperltl.automaton.BoolConst;
import org.openu.hyperltl.automaton.NBA;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class AutomatonLassoExtractorTest {

    @Test
    void extractsAcceptingLasso() {
        NBA nba = new NBA(
                List.of("a"),
                Set.of(0),
                Map.of(
                        0, List.of(new NBA.Transition(0, 1, new BoolConst(true))),
                        1, List.of(new NBA.Transition(1, 1, new BoolConst(true)))
                ),
                Set.of(1),
                2
        );

        AutomatonLassoExtractor extractor = new AutomatonLassoExtractor();
        var lassoOpt = extractor.extract(nba);

        assertTrue(lassoOpt.isPresent());
        AutomatonLasso lasso = lassoOpt.get();

        assertFalse(lasso.prefixStates().isEmpty());
        assertFalse(lasso.loopStates().isEmpty());
        assertEquals(1, lasso.loopStates().getFirst());
        assertEquals(1, lasso.loopStates().getLast());
    }

    @Test
    void returnsEmptyWhenNoAcceptingCycleExists() {
        NBA nba = new NBA(
                List.of("a"),
                Set.of(0),
                Map.of(
                        0, List.of(new NBA.Transition(0, 1, new BoolConst(true))),
                        1, List.of()
                ),
                Set.of(1),
                2
        );

        AutomatonLassoExtractor extractor = new AutomatonLassoExtractor();
        assertTrue(extractor.extract(nba).isEmpty());
    }
}