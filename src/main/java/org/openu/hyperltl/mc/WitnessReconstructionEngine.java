package org.openu.hyperltl.mc;

import org.openu.hyperltl.model.SystemModel;
import org.openu.hyperltl.trace.TraceStep;
import org.openu.hyperltl.trace.TraceWitness;

import java.util.*;

public final class WitnessReconstructionEngine {
    public List<TraceWitness> reconstructWitnesses(ReconstructionContext context,
                                                   AutomatonLasso finalLasso,
                                                   SystemModel originalSystem) {
        List<QuantifierIntegrationStep> steps = context.steps();
        if (steps.isEmpty()) {
            return List.of();
        }

        List<TraceWitness> reconstructed = new ArrayList<>();
        AutomatonLasso currentLasso = finalLasso;

        for (int i = steps.size() - 1; i >= 0; i--) {
            QuantifierIntegrationStep step = steps.get(i);
            ProjectionResult projection = projectOneStep(step, currentLasso, originalSystem);
            reconstructed.add(projection.variableWitness());
            currentLasso = projection.previousAutomatonLasso();
            // Complementation changes states; earlier product runs cannot be recovered.
            if (step.complementedBeforeStep()) break;
        }

        reconstructed.sort(Comparator.comparing(TraceWitness::variable));
        return reconstructed;
    }

    private ProjectionResult projectOneStep(QuantifierIntegrationStep step,
                                            AutomatonLasso lassoInAfterAutomaton,
                                            SystemModel originalSystem) {
        List<Integer> prevPrefix = new ArrayList<>();
        List<Integer> prevLoop = new ArrayList<>();

        List<String> varPrefixStates = new ArrayList<>();
        List<String> varLoopStates = new ArrayList<>();

        // Prefix states are observed states.
        List<Integer> prefix = lassoInAfterAutomaton.prefixStates();
        // The extractor includes the loop entry in the prefix; emit it only in the loop.
        for (int prefixIndex = 0; prefixIndex < Math.max(0, prefix.size() - 1); prefixIndex++) {
            int state = prefix.get(prefixIndex);
            ProductStateComponents c = component(step, state);
            prevPrefix.add(c.leftState());
            varPrefixStates.add(systemState(step, c.rightState()));
        }

        // Loop states are also observed states.
        // The lasso loop usually ends by repeating the start state, e.g. [q5, q7, q5].
        // For the witness representation we keep one cycle without the duplicated closing state.
        List<Integer> rawLoop = lassoInAfterAutomaton.loopStates();
        int loopLimit = rawLoop.isEmpty() ? 0 : rawLoop.size() - 1;

        for (int i = 0; i < loopLimit; i++) {
            int state = rawLoop.get(i);
            ProductStateComponents c = component(step, state);
            prevLoop.add(c.leftState());
            varLoopStates.add(systemState(step, c.rightState()));
        }

        TraceWitness witness = new TraceWitness(
                step.variable(),
                toTraceSteps(varPrefixStates, originalSystem, step.variable()),
                toTraceSteps(varLoopStates, originalSystem, step.variable())
        );

        // The previous automaton lasso should keep the repeated closing state in the loop.
        List<Integer> previousLoopWithClosure = rebuildClosedLoop(prevLoop);

        AutomatonLasso previousLasso = new AutomatonLasso(
                appendLoopEntry(prevPrefix, previousLoopWithClosure),
                previousLoopWithClosure
        );

        return new ProjectionResult(previousLasso, witness);
    }

    private ProductStateComponents component(QuantifierIntegrationStep step, int afterState) {
        ProductStateComponents c = step.productStateDecomposition().get(afterState);
        if (c == null) {
            throw new IllegalStateException(
                    "Missing decomposition for step " + step.stepIndex() + ", state " + afterState);
        }
        return c;
    }

    private String systemState(QuantifierIntegrationStep step, int rightState) {
        String s = step.systemAutomaton().automatonStateToSystemState().get(rightState);
        if (s == null) {
            throw new IllegalStateException(
                    "Missing system-state mapping for step " + step.stepIndex() + ", right state " + rightState);
        }
        return s;
    }

    private List<TraceStep> toTraceSteps(List<String> stateNames,
                                         SystemModel originalSystem,
                                         String variable) {
        List<TraceStep> out = new ArrayList<>();
        for (String state : stateNames) {
            out.add(new TraceStep(state, renamedValuation(originalSystem, state, variable)));
        }
        return out;
    }

    private Set<String> renamedValuation(SystemModel originalSystem, String stateName, String variable) {
        Set<String> labels = originalSystem.states().get(stateName).labels();
        Set<String> renamed = new LinkedHashSet<>();
        for (String ap : labels) {
            renamed.add(ap + "_" + variable);
        }
        return renamed;
    }

    private List<Integer> rebuildClosedLoop(List<Integer> openLoopStates) {
        if (openLoopStates.isEmpty()) {
            return List.of();
        }
        List<Integer> closed = new ArrayList<>(openLoopStates);
        closed.add(openLoopStates.getFirst());
        return closed;
    }

    private List<Integer> appendLoopEntry(List<Integer> prefix, List<Integer> loop) {
        List<Integer> result = new ArrayList<>(prefix);
        if (!loop.isEmpty()) result.add(loop.getFirst());
        return result;
    }

    private record ProjectionResult(
            AutomatonLasso previousAutomatonLasso,
            TraceWitness variableWitness
    ) {
    }
}
