package org.openu.hyperltl.model;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

public final class SystemModelLoader {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    public static final String ATOMIC_PROPOSITIONS_FIELD_NAME = "atomicPropositions";
    public static final String INITIAL_STATE_FIELD_NAME = "initialState";
    public static final String STATES_FIELD_NAME = "states";
    public static final String LABELS_FIELD_NAME = "labels";
    public static final String TRANSITIONS_FIELD_NAME = "transitions";

    public static SystemModel load(Path path) throws IOException {
        JsonNode root = MAPPER.readTree(path.toFile());

        Set<String> aps = new LinkedHashSet<>();
        for (JsonNode ap : root.required(ATOMIC_PROPOSITIONS_FIELD_NAME)) {
            aps.add(ap.asText());
        }

        String initialState = root.required(INITIAL_STATE_FIELD_NAME).asText();

        Map<String, SystemModel.State> states = new LinkedHashMap<>();
        JsonNode statesNode = root.required(STATES_FIELD_NAME);
        Iterator<String> names = statesNode.fieldNames();
        while (names.hasNext()) {
            String name = names.next();
            JsonNode s = statesNode.required(name);

            Set<String> labels = new LinkedHashSet<>();
            for (JsonNode label : s.required(LABELS_FIELD_NAME)) {
                labels.add(label.asText());
            }

            Set<String> transitions = new LinkedHashSet<>();
            for (JsonNode t : s.required(TRANSITIONS_FIELD_NAME)) {
                transitions.add(t.asText());
            }

            states.put(name, new SystemModel.State(labels, transitions));
        }

        return new SystemModel(aps, initialState, states);
    }
}