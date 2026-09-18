package com.majortom.algorithms.telemetry.probe;

import com.majortom.algorithms.telemetry.api.TelemetryCapabilities;
import com.majortom.algorithms.telemetry.api.TelemetryMetricDescriptor;
import com.majortom.algorithms.telemetry.api.TelemetrySessionId;
import java.util.List;

/** Pluggable source of execution telemetry facts. Implementations must never depend on JavaFX. */
public interface TelemetryProbe {
  String id();

  default TelemetryCapabilities capabilities() {
    return TelemetryCapabilities.EMPTY;
  }

  List<TelemetryMetricDescriptor> descriptors();

  TelemetryProbeSession open(TelemetrySessionId sessionId);
}
