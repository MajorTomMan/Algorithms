package com.majortom.algorithms.core.runtime;

import com.majortom.algorithms.core.api.StatisticsContribution;
import com.majortom.algorithms.core.domain.execution.ExecutionLifecycleEvent;
import com.majortom.algorithms.core.domain.execution.RunCancelledEvent;
import com.majortom.algorithms.core.domain.execution.RunCompletedEvent;
import com.majortom.algorithms.core.domain.execution.RunFailedEvent;
import com.majortom.algorithms.core.domain.execution.RunStartedEvent;

import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Stateless accumulator for base execution statistics and domain metric contributions. */
public final class StatisticsReducer {

    public ExecutionStatistics initialState() {
        return ExecutionStatistics.empty();
    }

    /** Reduces an event that does not create a visual frame. */
    public ExecutionStatistics reduce(ExecutionStatistics previous, ExecutionEvent event) {
        return reduce(previous, event, false);
    }

    /** Reduces an event and records the visual-frame decision made by a view reducer. */
    public ExecutionStatistics reduce(
            ExecutionStatistics previous,
            ExecutionEvent event,
            boolean visualFrame) {
        Objects.requireNonNull(previous, "previous");
        Objects.requireNonNull(event, "event");
        if (previous.endedAt().isPresent()) {
            throw new IllegalArgumentException("No event may follow a terminal execution event");
        }

        boolean lifecycle = event.payload() instanceof ExecutionLifecycleEvent;
        long algorithmEventCount = previous.algorithmEventCount();
        long lifecycleEventCount = previous.lifecycleEventCount();
        if (lifecycle) {
            lifecycleEventCount = Math.addExact(lifecycleEventCount, 1L);
        } else {
            algorithmEventCount = Math.addExact(algorithmEventCount, 1L);
        }

        long visualFrameCount = previous.visualFrameCount();
        if (visualFrame) {
            visualFrameCount = Math.addExact(visualFrameCount, 1L);
        }

        Optional<Instant> startedAt = previous.startedAt();
        Optional<Instant> endedAt = previous.endedAt();
        if (event.payload() instanceof RunStartedEvent) {
            if (startedAt.isPresent()) {
                throw new IllegalArgumentException("A run may only start once");
            }
            startedAt = Optional.of(event.occurredAt());
        }
        if (isTerminal(event)) {
            if (endedAt.isPresent()) {
                throw new IllegalArgumentException("A run may only end once");
            }
            endedAt = Optional.of(event.occurredAt());
        }

        Duration duration = duration(startedAt, endedAt, event.occurredAt());
        if (duration.compareTo(previous.duration()) < 0) {
            throw new IllegalArgumentException("Execution event timestamps must be monotonic");
        }
        Map<String, Long> metrics = mergeMetrics(previous.metrics(), event);
        return new ExecutionStatistics(
                Math.addExact(previous.totalEventCount(), 1L),
                algorithmEventCount,
                lifecycleEventCount,
                visualFrameCount,
                startedAt,
                endedAt,
                duration,
                metrics);
    }

    private static Duration duration(
            Optional<Instant> startedAt,
            Optional<Instant> endedAt,
            Instant currentEventAt) {
        if (startedAt.isEmpty()) {
            return Duration.ZERO;
        }
        Instant durationEnd = currentEventAt;
        if (endedAt.isPresent()) {
            durationEnd = endedAt.orElseThrow();
        }
        Duration duration = Duration.between(startedAt.orElseThrow(), durationEnd);
        if (duration.isNegative()) {
            throw new IllegalArgumentException("Execution event timestamps must not move before the run start");
        }
        return duration;
    }

    private static boolean isTerminal(ExecutionEvent event) {
        return event.payload() instanceof RunCompletedEvent
                || event.payload() instanceof RunCancelledEvent
                || event.payload() instanceof RunFailedEvent;
    }

    private static Map<String, Long> mergeMetrics(
            Map<String, Long> previous,
            ExecutionEvent event) {
        if (!(event.payload() instanceof StatisticsContribution contribution)) {
            return previous;
        }
        Map<String, Long> deltas = Objects.requireNonNull(
                contribution.metricDeltas(),
                "statistics contribution metricDeltas");
        if (deltas.isEmpty()) {
            return previous;
        }
        Map<String, Long> merged = new LinkedHashMap<>(previous);
        deltas.forEach((name, delta) -> {
            Objects.requireNonNull(name, "metric name");
            Objects.requireNonNull(delta, "metric delta");
            if (name.isBlank()) {
                throw new IllegalArgumentException("Metric names must not be blank");
            }
            if (delta < 0L) {
                throw new IllegalArgumentException("Metric deltas must not be negative");
            }
            merged.merge(name, delta, Math::addExact);
        });
        return merged;
    }
}
