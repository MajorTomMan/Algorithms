package com.majortom.algorithms.core.runtime;

import java.io.Serial;
import java.io.Serializable;

/** Maps one authoritative execution event sequence to monotonic elapsed execution time. */
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
