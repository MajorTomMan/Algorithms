package com.majortom.algorithms.telemetry.runtime;

import com.majortom.algorithms.telemetry.api.TelemetryCapabilities;
import com.majortom.algorithms.telemetry.api.TelemetryScopeId;

/** Creates isolated telemetry sessions for Structure, Algorithm, and Practice execution scopes. */
public interface TelemetryFramework {
  TelemetryCapabilities capabilities();

  TelemetrySession begin(TelemetryScopeId scope);
}
