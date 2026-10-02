package org.openu.hyperltl.automaton;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

public final class HoaWriter {

    public String write(NBA nba) {
        StringBuilder sb = new StringBuilder();
        sb.append("HOA: v1\n");
        sb.append("States: ").append(nba.stateCount()).append('\n');
        sb.append("Start: ").append(nba.initialStates().stream().map(String::valueOf).collect(Collectors.joining("&"))).append('\n');
        sb.append("AP: ").append(nba.atomicPropositions().size());
        for (String ap : nba.atomicPropositions()) {
            sb.append(" \"").append(ap).append("\"");
        }
        sb.append('\n');
        sb.append("acc-name: Buchi\n");
        sb.append("Acceptance: 1 Inf(0)\n");
        sb.append("properties: trans-labels explicit-labels state-acc\n");
        sb.append("--BODY--\n");

        Map<String, Integer> apIndex = new LinkedHashMap<>();
        for (int i = 0; i < nba.atomicPropositions().size(); i++) {
            apIndex.put(nba.atomicPropositions().get(i), i);
        }

        for (int s = 0; s < nba.stateCount(); s++) {
            sb.append("State: ").append(s);
            if (nba.acceptingStates().contains(s)) {
                sb.append(" {0}");
            }
            sb.append('\n');
            for (NBA.Transition t : nba.outgoing(s)) {
                sb.append("[")
                        .append(t.label().toHoa(apIndex))
                        .append("] ")
                        .append(t.to())
                        .append('\n');
            }
        }
        sb.append("--END--\n");
        return sb.toString();
    }
}