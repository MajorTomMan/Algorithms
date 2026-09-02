package com.majortom.algorithms.core.runtime;

import com.majortom.algorithms.core.domain.execution.ExecutionLifecycleEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Thread-safe event sink that builds immutable, replayable snapshots for one execution. */
public final class RecordingEventSink implements EventSink {

    private final List<EventEnvelope> events = new ArrayList<>();
    private final StatisticsReducer statisticsReducer = new StatisticsReducer();
    private String runId;
    private String operationId;
    private ExecutionRecordingState state = ExecutionRecordingState.NOT_STARTED;
    private ExecutionStatistics statistics = ExecutionStatistics.empty();

    @Override
    public synchronized void accept(EventEnvelope event) {
        Objects.requireNonNull(event, "event");
        validateEnvelope(event);

        ExecutionRecordingState nextState = state;
        ExecutionStatistics nextStatistics;

        if (event.event() instanceof ExecutionLifecycleEvent lifecycleEvent) {
            nextState = ExecutionRecording.transition(state, lifecycleEvent);
        } else {
            if (state != ExecutionRecordingState.RUNNING) {
                throw new IllegalArgumentException("Domain events are only valid while a run is active");
            }
        }
        nextStatistics = statisticsReducer.reduce(statistics, event);

        if (events.isEmpty()) {
            runId = event.runId();
            operationId = event.operationId();
        }
        events.add(event);
        state = nextState;
        statistics = nextStatistics;
    }

    /** Returns whether this sink has accepted at least one event. */
    public synchronized boolean hasEvents() {
        return !events.isEmpty();
    }

    /**
     * Creates an immutable point-in-time recording.
     *
     * @throws IllegalStateException when no event has been accepted yet
     */
    public synchronized ExecutionRecording snapshot() {
        if (events.isEmpty()) {
            throw new IllegalStateException("Cannot snapshot an empty execution recording");
        }
        return new ExecutionRecording(runId, operationId, state, statistics, events);
    }

    /** Creates a snapshot with host timing and resource data supplied by the caller. */
    public synchronized ExecutionRecording snapshot(ExecutionSummary summary) {
        if (events.isEmpty()) {
            throw new IllegalStateException("Cannot snapshot an empty execution recording");
        }
        Objects.requireNonNull(summary, "summary");
        return new ExecutionRecording(runId, operationId, state, statistics, summary, events);
    }

    private void validateEnvelope(EventEnvelope event) {
        long expectedSequence = events.size();
        if (event.sequence() != expectedSequence) {
            throw new IllegalArgumentException(
                    "Expected execution event sequence " + expectedSequence + " but received " + event.sequence());
        }
        if (!events.isEmpty()
                && (!runId.equals(event.runId()) || !operationId.equals(event.operationId()))) {
            throw new IllegalArgumentException("Cannot mix events from different executions");
        }
    }
}
