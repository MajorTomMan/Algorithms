package com.majortom.algorithms.core.runtime;

import com.majortom.algorithms.core.base.BaseStructure;
import com.majortom.algorithms.core.tree.node.TreeNode;

import java.util.ArrayList;
import java.util.List;

public class ExecutionContext<S extends BaseStructure<?>> {

    private final String moduleId;
    private final String algorithmId;
    private final String inputSignature;
    private final ExecutionControl<S> control;
    private final ExecutionTimeline<S> timeline = new ExecutionTimeline<>();
    private final ExecutionStats stats = new ExecutionStats();
    private final List<ExecutionMessage> messages = new ArrayList<>();

    public ExecutionContext(
            String moduleId,
            String algorithmId,
            String inputSignature,
            ExecutionControl<S> control) {
        this.moduleId = moduleId;
        this.algorithmId = algorithmId;
        this.inputSignature = inputSignature;
        this.control = control;
        this.stats.markStarted();
    }

    public void sync(S structure, Object a, Object b, String label) {
        sync(structure, a, b, label, true);
    }

    @SuppressWarnings("unchecked")
    public void sync(S structure, Object a, Object b, String label, boolean awaitStep) {
        requireActive();
        stats.syncFrom(structure);

        S snapshot = (S) structure.copy();
        ExecutionFrame<S> frame = new ExecutionFrame<>(
                timeline.size(),
                stats.snapshot().durationMillis(),
                label,
                normalizeFocus(a),
                normalizeFocus(b),
                snapshot,
                stats.snapshot());

        timeline.add(frame);

        if (control != null) {
            control.acceptFrame(frame, awaitStep);
        }

        requireActive();
    }

    public void message(ExecutionMessageLevel level, String code, String text) {
        messages.add(new ExecutionMessage(System.currentTimeMillis(), level, code, text));
    }

    public boolean isCancelled() {
        return control != null && control.isCancelled();
    }

    public long delayMillis() {
        return control == null ? 0L : control.getDelayMillis();
    }

    public ExecutionStatsSnapshot currentStats() {
        return stats.snapshot();
    }

    public ExecutionTimeline<S> timeline() {
        return timeline;
    }

    public ExecutionRecord<S> finish() {
        stats.markEnded();
        return new ExecutionRecord<>(
                moduleId,
                algorithmId,
                inputSignature,
                System.currentTimeMillis(),
                timeline,
                stats.snapshot(),
                List.copyOf(messages));
    }

    public void requireActive() {
        if (isCancelled()) {
            throw new ExecutionException("execution.cancelled", "Execution cancelled");
        }
    }

    private Object normalizeFocus(Object focus) {
        if (focus instanceof TreeNode<?> treeNode) {
            return treeNode.data;
        }
        return focus;
    }
}
