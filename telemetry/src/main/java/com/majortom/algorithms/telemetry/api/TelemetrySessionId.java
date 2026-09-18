package com.majortom.algorithms.telemetry.api;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

/** Unique identity for one concrete run of a stable telemetry scope. */
public record TelemetrySessionId(long sequence, TelemetryScopeId scope) implements Serializable {
  @Serial private static final long serialVersionUID = 1L;

  public TelemetrySessionId {
    if (sequence < 0L) {
      throw new IllegalArgumentException("sequence must not be negative");
    }
    scope = Objects.requireNonNull(scope, "scope");
  }
}
