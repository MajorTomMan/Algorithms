package com.majortom.algorithms.visualization;

import com.majortom.algorithms.core.domain.execution.ExecutionLifecycleEvent;
import com.majortom.algorithms.core.logging.LogEvent;
import com.majortom.algorithms.core.logging.LogLevel;
import com.majortom.algorithms.core.runtime.EventEnvelope;
import com.majortom.algorithms.visualization.logging.LogChannel;
import com.majortom.algorithms.visualization.logging.LogChannelId;
import com.majortom.algorithms.visualization.logging.LogChannelStore;
import com.majortom.algorithms.visualization.logging.LogEntry;
import com.majortom.algorithms.visualization.logging.LogView;
import com.majortom.algorithms.visualization.render.fx.FxDispatch;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Per-module log ownership, channel binding and structure-event projection. */
final class ControllerLogChannels {
    private LogChannelStore store;
    private LogView algorithmView;
    private LogView structureView;
    private String structureScopeId;
    private String algorithmId;

    void bind(LogChannelStore store, LogView algorithmView, LogView structureView, String moduleId) {
        this.store = Objects.requireNonNull(store, "logChannelStore");
        this.algorithmView = algorithmView;
        this.structureView = structureView;
        if (structureScopeId == null) structureScopeId = moduleId;
        bindStructureView();
        bindAlgorithmView();
    }

    String structureScope(String moduleId) {
        return structureScopeId == null ? moduleId : structureScopeId;
    }

    String activeAlgorithmId() { return algorithmId; }

    void activateAlgorithm(String id) {
        algorithmId = normalizeOwnerId(id);
        bindAlgorithmView();
    }

    void setStructureScopeId(String id) {
        String normalized = Objects.requireNonNull(normalizeOwnerId(id), "structureId");
        if (Objects.equals(structureScopeId, normalized)) return;
        structureScopeId = normalized;
        bindStructureView();
        bindAlgorithmView();
    }

    void appendAlgorithm(String message) { appendSystem(algorithmChannel(), message); }
    void appendStructure(String message) { appendSystem(structureChannel(), message); }

    void appendAlgorithmEvent(LogEvent event, Instant timestamp) {
        LogChannel channel = algorithmChannel();
        if (channel != null) channel.append(event, timestamp);
    }

    /** Project only the current operation's events, then update JavaFX once. */
    void appendStructureEvents(List<EventEnvelope> events) {
        LogChannel target = structureChannel();
        if (target == null || events.isEmpty()) return;
        List<LogEntry> batch = new ArrayList<>();
        for (EventEnvelope envelope : events) {
            if (envelope.event() instanceof ExecutionLifecycleEvent) continue;
            if (envelope.event() instanceof LogEvent logEvent) {
                batch.add(new LogEntry(envelope.timestamp(), logEvent.level(),
                        logEvent.tag(), logEvent.message()));
                continue;
            }
            String operation = envelope.operationId();
            int lastDot = operation.lastIndexOf('.');
            if (lastDot >= 0 && lastDot + 1 < operation.length()) operation = operation.substring(lastDot + 1);
            String tag = operation.isBlank() ? "STRUCTURE" : operation;
            batch.add(new LogEntry(envelope.timestamp(), LogLevel.INFO,
                    tag, envelope.event().toString()));
        }
        if (!batch.isEmpty()) dispatch(() -> target.appendAll(batch));
    }

    private LogChannel structureChannel() {
        return store == null ? null : store.channel(LogChannelId.structure(structureScopeId));
    }

    private LogChannel algorithmChannel() {
        return store == null || algorithmId == null ? null
                : store.channel(LogChannelId.algorithm(structureScopeId, algorithmId));
    }

    private void bindStructureView() {
        if (structureView != null) structureView.showChannel(structureChannel());
    }

    private void bindAlgorithmView() {
        if (algorithmView != null) algorithmView.showChannel(algorithmChannel());
    }

    private static String normalizeOwnerId(String id) {
        if (id == null) return null;
        String normalized = id.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private static void appendSystem(LogChannel channel, String message) {
        if (channel != null) dispatch(() -> channel.appendSystem(message));
    }

    private static void dispatch(Runnable task) {
        if (FxDispatch.isFxThread()) task.run();
        else FxDispatch.defer(task);
    }
}
