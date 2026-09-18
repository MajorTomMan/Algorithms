package com.majortom.algorithms.visualization.render.presentation.memory;

import com.majortom.algorithms.core.runtime.ExecutionAnchorTimeline;
import com.majortom.algorithms.telemetry.api.TelemetryDomain;
import com.majortom.algorithms.telemetry.memory.analysis.MemoryAllocationAnalysis;
import com.majortom.algorithms.telemetry.memory.analysis.StructureFootprint;
import com.majortom.algorithms.telemetry.memory.api.MemoryCapabilities;
import com.majortom.algorithms.telemetry.memory.api.MemoryFacts;
import java.util.Objects;
import java.util.Optional;

/** Immutable factual input captured by the Memory presentation source. */
public record MemoryPresentationInput(
    TelemetryDomain expectedDomain,
    MemoryCapabilities capabilities,
    Optional<MemoryFacts> facts,
    Optional<ExecutionAnchorTimeline> executionAnchors,
    Optional<MemoryAllocationAnalysis> deepAnalysis,
    Optional<StructureFootprint> footprint,
    boolean deepAnalysisEnabled,
    boolean footprintSupported,
    boolean footprintBusy) {

  private static final MemoryCapabilities NO_CAPABILITIES =
      new MemoryCapabilities(false, false, false, false);

  public MemoryPresentationInput {
    expectedDomain = Objects.requireNonNull(expectedDomain, "expectedDomain");
    capabilities = Objects.requireNonNull(capabilities, "capabilities");
    facts = Objects.requireNonNull(facts, "facts");
    executionAnchors = Objects.requireNonNull(executionAnchors, "executionAnchors");
    deepAnalysis = Objects.requireNonNull(deepAnalysis, "deepAnalysis");
    footprint = Objects.requireNonNull(footprint, "footprint");
  }

  public static MemoryPresentationInput empty(TelemetryDomain domain) {
    return new MemoryPresentationInput(
        Objects.requireNonNull(domain, "domain"),
        NO_CAPABILITIES,
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        false,
        false,
        false);
  }
}
