package com.majortom.algorithms.visualization;

import com.majortom.algorithms.core.runtime.ExecutionOperation;
import com.majortom.algorithms.telemetry.api.TelemetryDomain;
import com.majortom.algorithms.telemetry.api.TelemetryScopeId;
import com.majortom.algorithms.telemetry.memory.api.MemoryCapabilities;
import com.majortom.algorithms.telemetry.memory.api.MemoryFacts;
import com.majortom.algorithms.telemetry.memory.analysis.MemoryAllocationAnalysis;
import com.majortom.algorithms.telemetry.memory.analysis.StructureFootprint;
import com.majortom.algorithms.telemetry.memory.runtime.MemoryTelemetryRun;
import com.majortom.algorithms.telemetry.memory.runtime.MemoryTelemetryService;
import com.majortom.algorithms.telemetry.runtime.TelemetryStore;
import com.majortom.algorithms.visualization.render.fx.FxDispatch;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletionStage;

/** Per-controller memory sampling and profile lifetime; never owns an editable structure. */
final class ControllerMemoryProfile {
    private final MemoryTelemetryService service = MemoryTelemetryService.shared();
    private final Runnable refreshPresentation;
    private volatile MemoryTelemetryRun activeAlgorithmRun;
    private volatile boolean deepMemoryAnalysisEnabled;

    ControllerMemoryProfile(Runnable refreshPresentation) {
        this.refreshPresentation = Objects.requireNonNull(refreshPresentation, "refreshPresentation");
    }

    ExecutionOperation<?> profileOperation(TelemetryScopeId scope, ExecutionOperation<?> operation,
            boolean exposeAsActiveAlgorithm) {
        Objects.requireNonNull(scope, "scope");
        Objects.requireNonNull(operation, "operation");
        return () -> {
            MemoryTelemetryRun memoryRun = service.begin(scope, deepMemoryAnalysisEnabled);
            if (exposeAsActiveAlgorithm) activeAlgorithmRun = memoryRun;
            try {
                Object result = operation.execute();
                memoryRun.complete();
                return result;
            } catch (InterruptedException exception) {
                memoryRun.cancel();
                throw exception;
            } catch (RuntimeException | Error exception) {
                memoryRun.fail();
                throw exception;
            } finally {
                if (!memoryRun.finished()) memoryRun.cancel();
                if (exposeAsActiveAlgorithm && activeAlgorithmRun == memoryRun) activeAlgorithmRun = null;
                memoryRun.analysisCompletion().whenComplete((analysis, error) -> {
                    if (error == null && analysis != null) FxDispatch.defer(refreshPresentation);
                });
            }
        };
    }

    MemoryCapabilities capabilities() { return service.capabilities(); }

    Optional<MemoryFacts> algorithmMemoryProfile(String structureScope, String algorithmId) {
        MemoryTelemetryRun active = activeAlgorithmRun;
        if (active != null) return Optional.of(MemoryFacts.from(active.snapshot()));
        if (algorithmId == null) return Optional.empty();
        return service.latest(TelemetryScopeId.algorithm(structureScope, algorithmId)).map(MemoryFacts::from);
    }

    Optional<MemoryFacts> structureMemoryProfile(String structureScope) {
        return service.latestByComponent(TelemetryDomain.STRUCTURE, structureScope).map(MemoryFacts::from);
    }

    TelemetryStore telemetryProfiles() { return service.store(); }

    void setDeepMemoryAnalysisEnabled(boolean enabled) {
        deepMemoryAnalysisEnabled = enabled && capabilities().jfrAvailable();
    }

    boolean isDeepMemoryAnalysisEnabled() { return deepMemoryAnalysisEnabled; }

    Optional<MemoryAllocationAnalysis> algorithmDeepMemoryProfile(String structureScope, String algorithmId) {
        if (algorithmId == null) return Optional.empty();
        return service.latestAnalysis(TelemetryScopeId.algorithm(structureScope, algorithmId));
    }

    Optional<MemoryAllocationAnalysis> structureDeepMemoryProfile(String structureScope) {
        return service.latestAnalysisByComponent(TelemetryDomain.STRUCTURE, structureScope);
    }

    boolean structureFootprintAvailable() { return service.footprintAvailable(); }

    CompletionStage<StructureFootprint> analyzeStructureFootprint(Object root) {
        return service.analyzeFootprint(root);
    }
}
