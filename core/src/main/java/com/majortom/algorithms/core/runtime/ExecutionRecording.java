package com.majortom.algorithms.core.runtime;

import com.majortom.algorithms.core.domain.execution.ExecutionLifecycleEvent;
import com.majortom.algorithms.core.domain.execution.RunCancelledEvent;
import com.majortom.algorithms.core.domain.execution.RunCompletedEvent;
import com.majortom.algorithms.core.domain.execution.RunFailedEvent;
import com.majortom.algorithms.core.domain.execution.RunStartedEvent;

import java.util.List;
import java.util.Objects;

/** Immutable snapshot of the authoritative event history for one runtime-managed operation. */
public record ExecutionRecording(
        String runId,
        String operationId,
        ExecutionRecordingState state,
        ExecutionStatistics statistics,
        ExecutionSummary summary,
        List<EventEnvelope> events) {

    /** Creates a recording with event-derived timing and no host measurements. */
    public ExecutionRecording(
            String runId,
            String operationId,
            ExecutionRecordingState state,
            ExecutionStatistics statistics,
            List<EventEnvelope> events) {
        this(runId, operationId, state, statistics, ExecutionSummary.from(statistics), events);
    }

    public ExecutionRecording {
        runId = requireText(runId, "runId");
        operationId = requireText(operationId, "operationId");
        state = Objects.requireNonNull(state, "state");
        statistics = Objects.requireNonNull(statistics, "statistics");
        summary = Objects.requireNonNull(summary, "summary");
        if (!statistics.equals(summary.statistics())) {
            throw new IllegalArgumentException("Summary statistics must match recording statistics");
        }
        events = List.copyOf(Objects.requireNonNull(events, "events"));
        if (events.isEmpty()) {
            throw new IllegalArgumentException("An execution recording requires at least one event");
        }
        validate(runId, operationId, state, statistics, events);
    }

    /** Replays the immutable event sequence into another runtime-neutral sink. */
    public void replay(EventSink eventSink) {
        Objects.requireNonNull(eventSink, "eventSink");
        events.forEach(eventSink::accept);
    }

    /** Returns a copy with host timing and resource measurements attached. */
    public ExecutionRecording withSummary(ExecutionSummary value) {
        Objects.requireNonNull(value, "summary");
        return new ExecutionRecording(runId, operationId, state, statistics, value, events);
    }

    private static void validate(
            String runId,
            String operationId,
            ExecutionRecordingState expectedState,
            ExecutionStatistics statistics,
            List<EventEnvelope> events) {
        ExecutionRecordingState derivedState = ExecutionRecordingState.NOT_STARTED;
        StatisticsReducer statisticsReducer = new StatisticsReducer();
        ExecutionStatistics derivedStatistics = statisticsReducer.initialState();

        for (int index = 0; index < events.size(); index++) {
            EventEnvelope event = Objects.requireNonNull(events.get(index), "events[" + index + "]");
            if (!runId.equals(event.runId()) || !operationId.equals(event.operationId())) {
                throw new IllegalArgumentException("All events must belong to the recorded execution");
            }
            if (event.sequence() != index) {
                throw new IllegalArgumentException("Execution event sequence must start at zero and be contiguous");
            }

            if (event.event() instanceof ExecutionLifecycleEvent lifecycleEvent) {
                derivedState = transition(derivedState, lifecycleEvent);
            } else {
                if (derivedState != ExecutionRecordingState.RUNNING) {
                    throw new IllegalArgumentException("Domain events are only valid while a run is active");
                }
            }
            derivedStatistics = statisticsReducer.reduce(derivedStatistics, event);
        }

        if (derivedState != expectedState) {
            throw new IllegalArgumentException("Recording state does not match its lifecycle events");
        }
        if (!statistics.equals(derivedStatistics)) {
            throw new IllegalArgumentException("Execution statistics do not match the recorded events");
        }
    }

    static ExecutionRecordingState transition(
            ExecutionRecordingState currentState,
            ExecutionLifecycleEvent lifecycleEvent) {
        Objects.requireNonNull(currentState, "currentState");
        Objects.requireNonNull(lifecycleEvent, "lifecycleEvent");
        if (currentState.isTerminal()) {
            throw new IllegalArgumentException("No event may follow a terminal lifecycle event");
        }
        if (lifecycleEvent instanceof RunStartedEvent) {
            if (currentState != ExecutionRecordingState.NOT_STARTED) {
                throw new IllegalArgumentException("A run may only start once");
            }
            return ExecutionRecordingState.RUNNING;
        }
        if (lifecycleEvent instanceof RunCompletedEvent) {
            if (currentState != ExecutionRecordingState.RUNNING) {
                throw new IllegalArgumentException("A run must start before it completes");
            }
            return ExecutionRecordingState.COMPLETED;
        }
        if (lifecycleEvent instanceof RunCancelledEvent) {
            return ExecutionRecordingState.CANCELLED;
        }
        if (lifecycleEvent instanceof RunFailedEvent) {
            return ExecutionRecordingState.FAILED;
        }
        throw new IllegalArgumentException("Unsupported execution lifecycle event: " + lifecycleEvent.getClass());
    }

    private static String requireText(String value, String name) {
        Objects.requireNonNull(value, name);
        if (value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value;
    }
}
