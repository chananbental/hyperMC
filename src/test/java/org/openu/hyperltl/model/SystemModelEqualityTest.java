package org.openu.hyperltl.model;

import org.junit.jupiter.api.Test;
import java.util.Map;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;

class SystemModelEqualityTest {
    @Test
    void equalityPreservesStateNamesAndTheirLabels() {
        SystemModel.State a = new SystemModel.State(Set.of("a"), Set.of("s0"));
        SystemModel.State b = new SystemModel.State(Set.of(), Set.of("s0"));
        SystemModel first = new SystemModel(Set.of("a"), "s0", Map.of("s0", a, "s1", b));
        SystemModel swapped = new SystemModel(Set.of("a"), "s0", Map.of("s0", b, "s1", a));
        assertNotEquals(first, swapped);
        SystemModel copy = new SystemModel(Set.of("a"), "s0", Map.of("s1", b, "s0", a));
        assertEquals(first, copy);
        assertEquals(first.hashCode(), copy.hashCode());
    }
}
