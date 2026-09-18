package com.majortom.algorithms.telemetry.memory.analysis;

import com.majortom.algorithms.telemetry.analysis.TelemetryAnalysisResult;
import com.majortom.algorithms.telemetry.api.TelemetrySessionId;
import java.io.Serial;
import java.io.Serializable;
import java.util.List;
import java.util.Objects;

/** Optional JFR allocation breakdown. Values are sampled estimates, never exact allocation totals. */
public record MemoryAllocationAnalysis(
    TelemetrySessionId sessionId,
    long estimatedBytes,
    long sampleCount,
    List<MemoryAllocationTypeStat> topTypes,
    List<MemoryAllocationSiteStat> topSites)
    implements TelemetryAnalysisResult, Serializable {
  @Serial private static final long serialVersionUID = 1L;
  public static final String ANALYZER_ID = "memory.jfr-allocation";

  public MemoryAllocationAnalysis {
    sessionId = Objects.requireNonNull(sessionId, "sessionId");
    estimatedBytes = Math.max(0L, estimatedBytes);
    sampleCount = Math.max(0L, sampleCount);
    topTypes = topTypes == null ? List.of() : List.copyOf(topTypes);
    topSites = topSites == null ? List.of() : List.copyOf(topSites);
  }

  @Override
  public String analyzerId() {
    return ANALYZER_ID;
  }

  public boolean hasSamples() {
    return sampleCount > 0L;
  }
}
