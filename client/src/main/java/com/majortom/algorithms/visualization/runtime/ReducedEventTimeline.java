package com.majortom.algorithms.visualization.runtime;

import com.majortom.algorithms.visualization.runtime.EventImportance;
import com.majortom.algorithms.visualization.runtime.EventReducer;
import com.majortom.algorithms.core.runtime.EventEnvelope;
import com.majortom.algorithms.core.runtime.ExecutionStatistics;
import com.majortom.algorithms.visualization.runtime.Reduction;
import com.majortom.algorithms.visualization.runtime.ReductionCursor;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Visible-frame index over an authoritative execution event stream.
 *
 * <p>The complete stream remains available for replay, while lifecycle and other non-visual
 * reductions do not create blank frames. Sparse immutable-state checkpoints bound the amount of
 * reduction work required by a backward or random seek.</p>
 */
public final class ReducedEventTimeline<S> {

    static final int DEFAULT_CHECKPOINT_INTERVAL = 256;

    private final List<EventEnvelope> events;
    private final EventReducer<S> reducer;
    private final List<Integer> frameEventIndexes;
    private final List<Checkpoint<S>> checkpoints;
    private final ExecutionStatistics statistics;

    private int currentIndex = -1;
    private int currentEventIndex = -1;
    private S currentState;

    public ReducedEventTimeline(List<EventEnvelope> events, EventReducer<S> reducer) {
        this(events, reducer, DEFAULT_CHECKPOINT_INTERVAL);
    }

    ReducedEventTimeline(
            List<EventEnvelope> events,
            EventReducer<S> reducer,
            int checkpointInterval) {
        this.events = List.copyOf(Objects.requireNonNull(events, "events"));
        this.reducer = Objects.requireNonNull(reducer, "reducer");
        if (checkpointInterval <= 0) {
            throw new IllegalArgumentException("checkpointInterval must be positive");
        }

        ReductionCursor<S> cursor = new ReductionCursor<>(reducer);
        List<Integer> discoveredFrames = new ArrayList<>();
        List<Checkpoint<S>> discoveredCheckpoints = new ArrayList<>();
        discoveredCheckpoints.add(new Checkpoint<>(-1, cursor.state()));
        for (int eventIndex = 0; eventIndex < this.events.size(); eventIndex++) {
            Reduction<S> reduction = cursor.accept(this.events.get(eventIndex));
            if (reduction.visualFrame()) {
                discoveredFrames.add(eventIndex);
            }
            if (shouldCheckpoint(eventIndex, reduction, checkpointInterval)) {
                discoveredCheckpoints.add(new Checkpoint<>(eventIndex, reduction.state()));
            }
        }
        frameEventIndexes = List.copyOf(discoveredFrames);
        checkpoints = List.copyOf(discoveredCheckpoints);
        statistics = cursor.statistics();
        currentState = Objects.requireNonNull(reducer.initialState(), "reducer initial state");
    }

    public int size() {
        return frameEventIndexes.size();
    }

    public boolean isEmpty() {
        return frameEventIndexes.isEmpty();
    }

    public int currentIndex() {
        return currentIndex;
    }

    public ExecutionStatistics statistics() {
        return statistics;
    }

    /** Read-only authoritative event stream retained for presentation inspection. */
    public List<EventEnvelope> events() {
        return events;
    }

    public S initialState() {
        return Objects.requireNonNull(reducer.initialState(), "reducer initial state");
    }

    public EventEnvelope event(int frameIndex) {
        return events.get(eventIndex(frameIndex));
    }

    public int eventIndex(int frameIndex) {
        requireFrameIndex(frameIndex);
        return frameEventIndexes.get(frameIndex);
    }

    public S seek(int frameIndex) {
        requireFrameIndex(frameIndex);
        int targetEventIndex = frameEventIndexes.get(frameIndex);
        if (targetEventIndex < currentEventIndex) {
            restoreNearestCheckpoint(targetEventIndex);
        }
        while (currentEventIndex < targetEventIndex) {
            currentEventIndex++;
            currentState = requireReduction(currentState, events.get(currentEventIndex)).state();
        }
        currentIndex = frameIndex;
        return currentState;
    }

    public void restart() {
        currentIndex = -1;
        currentEventIndex = -1;
        currentState = initialState();
    }

    private boolean shouldCheckpoint(
            int eventIndex,
            Reduction<S> reduction,
            int checkpointInterval) {
        if ((eventIndex + 1) % checkpointInterval == 0) {
            return true;
        }
        EventImportance importance = reduction.importance();
        return importance == EventImportance.CHECKPOINT || importance == EventImportance.TERMINAL;
    }

    private void restoreNearestCheckpoint(int targetEventIndex) {
        int low = 0;
        int high = checkpoints.size() - 1;
        Checkpoint<S> nearest = checkpoints.getFirst();
        while (low <= high) {
            int middle = low + (high - low) / 2;
            Checkpoint<S> candidate = checkpoints.get(middle);
            if (candidate.eventIndex() <= targetEventIndex) {
                nearest = candidate;
                low = middle + 1;
            } else {
                high = middle - 1;
            }
        }
        currentEventIndex = nearest.eventIndex();
        currentState = nearest.state();
    }

    private Reduction<S> requireReduction(S state, EventEnvelope event) {
        return Objects.requireNonNull(reducer.reduce(state, event), "reducer result");
    }

    private void requireFrameIndex(int frameIndex) {
        if (frameIndex < 0 || frameIndex >= frameEventIndexes.size()) {
            throw new IndexOutOfBoundsException(
                    "frameIndex: " + frameIndex + ", size: " + frameEventIndexes.size());
        }
    }

    private record Checkpoint<S>(int eventIndex, S state) {

        private Checkpoint {
            Objects.requireNonNull(state, "state");
        }
    }
}
