package com.majortom.algorithms.telemetry.timeline;

import com.majortom.algorithms.core.runtime.EventEnvelope;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Immutable event-sequence to original-execution-time index. */
public record ExecutionAnchorTimeline(String runId, List<ExecutionAnchor> anchors) {
  public ExecutionAnchorTimeline {
    runId = Objects.requireNonNull(runId, "runId").trim();
    if (runId.isEmpty()) {
      throw new IllegalArgumentException("runId must not be blank");
    }
    anchors = List.copyOf(Objects.requireNonNull(anchors, "anchors"));
    long previousSequence = -1L;
    long previousElapsed = -1L;
    for (ExecutionAnchor anchor : anchors) {
      if (anchor.eventSequence() <= previousSequence) {
        throw new IllegalArgumentException("event sequences must be strictly increasing");
      }
      if (anchor.elapsedNanos() < previousElapsed) {
        throw new IllegalArgumentException("anchor time must be monotonic");
      }
      previousSequence = anchor.eventSequence();
      previousElapsed = anchor.elapsedNanos();
    }
  }

  public static ExecutionAnchorTimeline fromEvents(List<EventEnvelope> events) {
    Objects.requireNonNull(events, "events");
    if (events.isEmpty()) {
      throw new IllegalArgumentException("events must not be empty");
    }
    String runId = events.getFirst().runId();
    Instant origin = events.getFirst().timestamp();
    List<ExecutionAnchor> result = new ArrayList<>(events.size());
    long previousElapsed = 0L;
    for (EventEnvelope event : events) {
      if (!runId.equals(event.runId())) {
        throw new IllegalArgumentException("Cannot build one anchor timeline from multiple runs");
      }
      long elapsed;
      try {
        elapsed = Math.max(0L, Duration.between(origin, event.timestamp()).toNanos());
      } catch (ArithmeticException exception) {
        elapsed = Long.MAX_VALUE;
      }
      elapsed = Math.max(previousElapsed, elapsed);
      result.add(new ExecutionAnchor(event.sequence(), elapsed));
      previousElapsed = elapsed;
    }
    return new ExecutionAnchorTimeline(runId, result);
  }

  public long durationNanos() {
    return anchors.isEmpty() ? 0L : anchors.getLast().elapsedNanos();
  }
}
