package com.majortom.algorithms.visualization.render.api;

import java.util.Objects;

/**
 * Render-owned position while presenting one authoritative execution event.
 *
 * <p>{@code eventSequence} identifies the target event whose visual state is being presented.
 * {@code intraEventProgress} is the transition progress from the preceding event state to that target
 * event: {@code 0.0} is the start of the visual transition and {@code 1.0} is the exact target event.
 * This target-event convention lets animation, replay, step and scrub share one cursor while keeping
 * execution-time projection independent from animation duration.</p>
 *
 * <p>This model deliberately contains no telemetry concepts.</p>
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
    if (!Double.isFinite(intraEventProgress)
        || intraEventProgress < 0.0d
        || intraEventProgress > 1.0d) {
      throw new IllegalArgumentException("intraEventProgress must be in [0,1]");
    }
    mode = Objects.requireNonNull(mode, "mode");
  }

  public static PresentationCursor beforeStart(String runId, Mode mode) {
    return new PresentationCursor(runId, -1L, 0.0d, mode);
  }

  /** Start of the transition whose target is {@code eventSequence}. */
  public static PresentationCursor transitionStart(String runId, long eventSequence, Mode mode) {
    return new PresentationCursor(runId, eventSequence, 0.0d, mode);
  }

  /** Exact stable presentation of {@code eventSequence}. */
  public static PresentationCursor atEvent(String runId, long eventSequence, Mode mode) {
    return new PresentationCursor(runId, eventSequence, 1.0d, mode);
  }

  /** Compatibility spelling for an event whose transition has completed. */
  public static PresentationCursor completedEvent(String runId, long eventSequence, Mode mode) {
    return atEvent(runId, eventSequence, mode);
  }

  public enum Mode {
    LIVE,
    REPLAY
  }
}
