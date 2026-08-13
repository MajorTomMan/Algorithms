package com.majortom.algorithms.core.runtime;

import java.util.Objects;
import java.util.Optional;

/**
 * Stateful replay cursor around a stateless reducer.
 * It is the common boundary for validating run, algorithm, and contiguous sequence envelopes.
 */
public final class ReductionCursor<S> {

    private final EventReducer<S> reducer;
    private S state;
    private String runId;
    private String algorithmId;
    private long nextSequence;
    private long visualFrameCount;
    private final StatisticsReducer statisticsReducer = new StatisticsReducer();
    private ExecutionStatistics statistics;

    public ReductionCursor(EventReducer<S> reducer) {
        this.reducer = Objects.requireNonNull(reducer, "reducer");
        reset();
    }

    /** Validates and reduces the next event in the execution stream. */
    public Reduction<S> accept(ExecutionEvent event) {
        Objects.requireNonNull(event, "event");
        validateEnvelope(event);
        Reduction<S> reduction = Objects.requireNonNull(
                reducer.reduce(state, event),
                "reducer result");
        ExecutionStatistics nextStatistics = statisticsReducer.reduce(
                statistics,
                event,
                reduction.visualFrame());
        state = reduction.state();
        if (nextSequence == 0L) {
            runId = event.runId();
            algorithmId = event.algorithmId();
        }
        nextSequence++;
        if (reduction.visualFrame()) {
            visualFrameCount++;
        }
        statistics = nextStatistics;
        return reduction;
    }

    /** Restores this cursor to its reducer-defined state before sequence zero. */
    public void reset() {
        state = Objects.requireNonNull(reducer.initialState(), "reducer initial state");
        runId = null;
        algorithmId = null;
        nextSequence = 0L;
        visualFrameCount = 0L;
        statistics = statisticsReducer.initialState();
    }

    public S state() {
        return state;
    }

    public Optional<String> runId() {
        return Optional.ofNullable(runId);
    }

    public Optional<String> algorithmId() {
        return Optional.ofNullable(algorithmId);
    }

    public long eventCount() {
        return nextSequence;
    }

    public long nextSequence() {
        return nextSequence;
    }

    public long visualFrameCount() {
        return visualFrameCount;
    }

    public ExecutionStatistics statistics() {
        return statistics;
    }

    private void validateEnvelope(ExecutionEvent event) {
        if (event.sequence() != nextSequence) {
            throw new IllegalArgumentException(
                    "Expected execution event sequence " + nextSequence + " but received " + event.sequence());
        }
        if (nextSequence == 0L) {
            return;
        }
        if (!runId.equals(event.runId()) || !algorithmId.equals(event.algorithmId())) {
            throw new IllegalArgumentException("Cannot mix events from different executions");
        }
    }
}
