package com.majortom.algorithms.telemetry.api;

import java.io.Serial;
import java.io.Serializable;

/** Numeric value preserving exact integer counters while also supporting fractional gauges. */
public sealed interface TelemetryValue extends Serializable
    permits TelemetryValue.LongValue, TelemetryValue.DoubleValue {

  double asDouble();

  record LongValue(long value) implements TelemetryValue {
    @Serial private static final long serialVersionUID = 1L;

    @Override
    public double asDouble() {
      return value;
    }
  }

  record DoubleValue(double value) implements TelemetryValue {
    @Serial private static final long serialVersionUID = 1L;

    public DoubleValue {
      if (!Double.isFinite(value)) {
        throw new IllegalArgumentException("value must be finite");
      }
    }

    @Override
    public double asDouble() {
      return value;
    }
  }

  static TelemetryValue of(long value) {
    return new LongValue(value);
  }

  static TelemetryValue of(double value) {
    return new DoubleValue(value);
  }
}
