package com.majortom.algorithms.core.runtime;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;
import java.util.Objects;

/** Immutable event-sequence to original monotonic execution-time index. */
public record ExecutionAnchorTimeline(String runId, List<ExecutionAnchor> anchors)
    implements Serializable {
  @Serial private static final long serialVersionUID = 1L;

  public ExecutionAnchorTimeline {
    runId = requireText(runId, "runId");
    anchors = List.copyOf(Objects.requireNonNull(anchors, "anchors"));
    long previousSequence = -1L;
    long previousElapsed = -1L;
    for (ExecutionAnchor anchor : anchors) {
      Objects.requireNonNull(anchor, "anchor");
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

  public long durationNanos() {
    return anchors.isEmpty() ? 0L : anchors.getLast().elapsedNanos();
  }

  public boolean isEmpty() {
    return anchors.isEmpty();
  }

  private static String requireText(String value, String name) {
    Objects.requireNonNull(value, name);
    String normalized = value.trim();
    if (normalized.isEmpty()) {
      throw new IllegalArgumentException(name + " must not be blank");
    }
    return normalized;
  }
}
