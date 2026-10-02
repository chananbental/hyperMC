package org.openu.hyperltl.model;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class SystemModelLoaderTest {

    @Test
    void loadsValidSystemModel() throws Exception {
        String json = """
                {
                  "atomicPropositions": ["a", "b"],
                  "initialState": "s0",
                  "states": {
                    "s0": {
                      "labels": ["a"],
                      "transitions": ["s1"]
                    },
                    "s1": {
                      "labels": ["b"],
                      "transitions": ["s1"]
                    }
                  }
                }
                """;

        Path temp = Files.createTempFile("system-", ".json");
        Files.writeString(temp, json);

        SystemModel model = SystemModelLoader.load(temp);

        assertEquals(Set.of("a", "b"), model.atomicPropositions());
        assertEquals("s0", model.initialState());
        assertEquals(Set.of("a"), model.states().get("s0").labels());
        assertEquals(Set.of("s1"), model.states().get("s0").transitions());
        assertEquals(Set.of("b"), model.states().get("s1").labels());
        assertEquals(Set.of("s1"), model.states().get("s1").transitions());
    }

    @Test
    void rejectsUnknownTransitionTarget() {
        assertThrows(IllegalArgumentException.class, () -> new SystemModel(
                Set.of("a"),
                "s0",
                java.util.Map.of(
                        "s0", new SystemModel.State(Set.of("a"), Set.of("s1"))
                )
        ));
    }

    @Test
    void rejectsUnknownAtomicProposition() {
        assertThrows(IllegalArgumentException.class, () -> new SystemModel(
                Set.of("a"),
                "s0",
                java.util.Map.of(
                        "s0", new SystemModel.State(Set.of("b"), Set.of("s0"))
                )
        ));
    }
}