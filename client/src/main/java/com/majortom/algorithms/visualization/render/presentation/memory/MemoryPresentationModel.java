package com.majortom.algorithms.visualization.render.presentation.memory;

import com.majortom.algorithms.telemetry.api.TelemetryDomain;
import com.majortom.algorithms.telemetry.memory.analysis.MemoryAllocationAnalysis;
import com.majortom.algorithms.telemetry.memory.analysis.StructureFootprint;
import com.majortom.algorithms.telemetry.memory.api.MemoryAllocationSample;
import com.majortom.algorithms.telemetry.memory.api.MemoryCapabilities;
import com.majortom.algorithms.telemetry.memory.api.MemoryFacts;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalLong;

/** Immutable, renderer-ready Memory presentation state. */
public record MemoryPresentationModel(
    TelemetryDomain expectedDomain,
    MemoryCapabilities capabilities,
    Optional<MemoryFacts> facts,
    List<MemoryAllocationSample> timelineSamples,
    long visibleElapsedNanos,
    OptionalLong currentAllocatedBytes,
    boolean cursorProjected,
    Optional<MemoryAllocationAnalysis> deepAnalysis,
    Optional<StructureFootprint> footprint,
    boolean deepAnalysisEnabled,
    boolean footprintSupported,
    boolean footprintBusy) {

  public MemoryPresentationModel {
    expectedDomain = Objects.requireNonNull(expectedDomain, "expectedDomain");
    capabilities = Objects.requireNonNull(capabilities, "capabilities");
    facts = Objects.requireNonNull(facts, "facts");
    timelineSamples = List.copyOf(Objects.requireNonNull(timelineSamples, "timelineSamples"));
    visibleElapsedNanos = Math.max(0L, visibleElapsedNanos);
    currentAllocatedBytes = Objects.requireNonNull(currentAllocatedBytes, "currentAllocatedBytes");
    deepAnalysis = Objects.requireNonNull(deepAnalysis, "deepAnalysis");
    footprint = Objects.requireNonNull(footprint, "footprint");
  }
}
