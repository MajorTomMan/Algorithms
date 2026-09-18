package com.majortom.algorithms.telemetry.analysis;

import com.majortom.algorithms.telemetry.api.TelemetrySessionId;

/** Marker for asynchronous deep-analysis results associated with one telemetry session. */
public interface TelemetryAnalysisResult {
  String analyzerId();

  TelemetrySessionId sessionId();
}
