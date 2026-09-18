package com.majortom.algorithms.telemetry.memory.runtime;

import com.majortom.algorithms.telemetry.analysis.TelemetryAnalysisSession;
import com.majortom.algorithms.telemetry.analysis.TelemetryAnalysisStore;
import com.majortom.algorithms.telemetry.api.TelemetryDomain;
import com.majortom.algorithms.telemetry.api.TelemetryProfile;
import com.majortom.algorithms.telemetry.api.TelemetryScopeId;
import com.majortom.algorithms.telemetry.memory.analysis.JfrAllocationTelemetryAnalyzer;
import com.majortom.algorithms.telemetry.memory.analysis.JolStructureFootprintAnalyzer;
import com.majortom.algorithms.telemetry.memory.analysis.MemoryAllocationAnalysis;
import com.majortom.algorithms.telemetry.memory.analysis.StructureFootprint;
import com.majortom.algorithms.telemetry.memory.api.MemoryCapabilities;
import com.majortom.algorithms.telemetry.memory.probe.GarbageCollectionProbe;
import com.majortom.algorithms.telemetry.memory.probe.HeapUsageProbe;
import com.majortom.algorithms.telemetry.memory.probe.ThreadAllocationProbe;
import com.majortom.algorithms.telemetry.runtime.DefaultTelemetryFramework;
import com.majortom.algorithms.telemetry.runtime.TelemetrySession;
import com.majortom.algorithms.telemetry.runtime.TelemetryStore;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletionStage;

/**
 * Memory-specific facade over the generic telemetry framework. It owns probes, stores and analyzers,
 * and exposes immutable facts only; it has no JavaFX, controller, animation, or Render dependency.
 */
public final class MemoryTelemetryService implements AutoCloseable {
  private static final MemoryTelemetryService SHARED = new MemoryTelemetryService();

  private final DefaultTelemetryFramework framework;
  private final TelemetryStore store = new TelemetryStore(32);
  private final TelemetryAnalysisStore analysisStore = new TelemetryAnalysisStore();
  private final JfrAllocationTelemetryAnalyzer jfrAnalyzer = new JfrAllocationTelemetryAnalyzer();
  private final JolStructureFootprintAnalyzer footprintAnalyzer = new JolStructureFootprintAnalyzer();
  private final MemoryCapabilities capabilities;

  public static MemoryTelemetryService shared() {
    return SHARED;
  }

  public MemoryTelemetryService() {
    this(DefaultTelemetryFramework.DEFAULT_SAMPLE_INTERVAL_MILLIS);
  }

  public MemoryTelemetryService(long sampleIntervalMillis) {
    this.framework = new DefaultTelemetryFramework(
        List.of(new ThreadAllocationProbe(), new HeapUsageProbe(), new GarbageCollectionProbe()),
        sampleIntervalMillis);
    this.capabilities = MemoryCapabilities.from(
        framework.capabilities(), jfrAnalyzer.isAvailable());
  }

  public MemoryCapabilities capabilities() {
    return capabilities;
  }

  public MemoryTelemetryRun begin(TelemetryScopeId scope, boolean deepAnalysis) {
    Objects.requireNonNull(scope, "scope");
    TelemetrySession session = framework.begin(scope);
    TelemetryAnalysisSession analysis =
        deepAnalysis && capabilities.jfrAvailable() ? jfrAnalyzer.begin(session.id()) : null;
    // JFR setup and all framework bootstrap allocations happen before this exact boundary.
    session.rebase();
    if (analysis != null) analysis.markExecutionStart();
    return new MemoryTelemetryRun(this, session, analysis);
  }

  public Optional<TelemetryProfile> latest(TelemetryScopeId scope) {
    return store.latest(scope);
  }

  public Optional<TelemetryProfile> latestByComponent(TelemetryDomain domain, String componentId) {
    return store.latestByComponent(domain, componentId);
  }

  public TelemetryStore store() {
    return store;
  }

  public Optional<MemoryAllocationAnalysis> latestAnalysis(TelemetryScopeId scope) {
    return analysisStore.latest(
        MemoryAllocationAnalysis.ANALYZER_ID, scope, MemoryAllocationAnalysis.class);
  }

  public Optional<MemoryAllocationAnalysis> latestAnalysisByComponent(
      TelemetryDomain domain, String componentId) {
    return analysisStore.latestByComponent(
        MemoryAllocationAnalysis.ANALYZER_ID, domain, componentId, MemoryAllocationAnalysis.class);
  }

  public boolean footprintAvailable() {
    return footprintAnalyzer.isAvailable();
  }

  public CompletionStage<StructureFootprint> analyzeFootprint(Object root) {
    return footprintAnalyzer.analyze(root);
  }

  void record(TelemetryProfile profile) {
    store.record(profile);
  }

  void recordAnalysis(MemoryAllocationAnalysis analysis) {
    analysisStore.record(analysis);
  }

  @Override
  public void close() {
    framework.close();
    jfrAnalyzer.close();
    footprintAnalyzer.close();
  }
}
