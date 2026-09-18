package com.majortom.algorithms.telemetry.timeline;

import java.util.Objects;

/**
 * Presentation-only cursor shared by live execution, replay, step, and scrub.
 *
 * <p>The cursor contains no telemetry concepts. TimelineProjector maps it back to factual execution
 * time, allowing every telemetry view to follow animation/replay without Animation knowing about
 * Memory, CPU, GC, or any other probe.</p>
 */
public record PresentationCursor(
    String runId,
    long eventSequence,
    double intraEventProgress,
    Mode mode) {

  public PresentationCursor {
    runId = Objects.requireNonNull(runId, "runId").trim();
    if (runId.isEmpty()) {
      throw new IllegalArgumentException("runId must not be blank");
    }
    if (eventSequence < -1L) {
      throw new IllegalArgumentException("eventSequence must be -1 or greater");
    }
    if (!Double.isFinite(intraEventProgress) || intraEventProgress < 0.0d || intraEventProgress > 1.0d) {
      throw new IllegalArgumentException("intraEventProgress must be in [0,1]");
    }
    mode = Objects.requireNonNull(mode, "mode");
  }

  public static PresentationCursor beforeStart(String runId, Mode mode) {
    return new PresentationCursor(runId, -1L, 0.0d, mode);
  }

  public static PresentationCursor atEvent(String runId, long eventSequence, Mode mode) {
    return new PresentationCursor(runId, eventSequence, 0.0d, mode);
  }

  public enum Mode {
    LIVE,
    REPLAY
  }
}
