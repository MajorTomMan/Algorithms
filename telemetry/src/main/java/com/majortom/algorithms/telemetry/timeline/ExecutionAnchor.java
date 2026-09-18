package com.majortom.algorithms.telemetry.timeline;

import java.io.Serial;
import java.io.Serializable;

/** Maps an authoritative execution event sequence to its original execution time. */
public record ExecutionAnchor(long eventSequence, long elapsedNanos) implements Serializable {
  @Serial private static final long serialVersionUID = 1L;

  public ExecutionAnchor {
    if (eventSequence < 0L) {
      throw new IllegalArgumentException("eventSequence must not be negative");
    }
    if (elapsedNanos < 0L) {
      throw new IllegalArgumentException("elapsedNanos must not be negative");
    }
  }
}
