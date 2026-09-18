package com.majortom.algorithms.telemetry.analysis;

import com.majortom.algorithms.telemetry.api.TelemetrySessionId;

/** Optional deep analyzer such as JFR allocation hotspot analysis. */
public interface TelemetryAnalyzer {
  String id();

  TelemetryAnalysisSession begin(TelemetrySessionId sessionId);
}
