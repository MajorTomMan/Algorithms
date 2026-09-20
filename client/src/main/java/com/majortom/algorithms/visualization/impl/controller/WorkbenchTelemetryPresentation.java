package com.majortom.algorithms.visualization.impl.controller;

import com.majortom.algorithms.telemetry.api.TelemetryDomain;
import com.majortom.algorithms.telemetry.memory.analysis.StructureFootprint;
import com.majortom.algorithms.visualization.BaseController;
import com.majortom.algorithms.visualization.memory.MemoryProfileView;
import com.majortom.algorithms.visualization.metrics.RuntimeOverviewModel;
import com.majortom.algorithms.visualization.render.api.PresentationSurfacePort;
import com.majortom.algorithms.visualization.render.api.PresentationSurface;
import com.majortom.algorithms.visualization.render.api.RenderSessionId;
import com.majortom.algorithms.visualization.render.presentation.MutablePresentationModelSource;
import com.majortom.algorithms.visualization.render.presentation.metrics.FxRuntimeOverviewRenderer;
import com.majortom.algorithms.visualization.render.presentation.memory.FxMemoryRenderer;
import com.majortom.algorithms.visualization.render.presentation.memory.MemoryPresentationInput;
import com.majortom.algorithms.visualization.render.presentation.memory.MemoryPresentationSource;
import com.majortom.algorithms.visualization.render.fx.FxDispatch;
import java.util.function.Supplier;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;

/** Owns runtime statistics, memory presentation surfaces, and their asynchronous context lifetime. */
final class WorkbenchTelemetryPresentation {
    private static final RenderSessionId RUNTIME_OVERVIEW_SURFACE_ID =
            RenderSessionId.of("workbench:runtime-overview");
    private static final RenderSessionId STRUCTURE_MEMORY_SURFACE_ID =
            RenderSessionId.of("workbench:memory:structure");
    private static final RenderSessionId ALGORITHM_MEMORY_SURFACE_ID =
            RenderSessionId.of("workbench:memory:algorithm");

    private final PresentationSurfacePort presentationSurfacePort;
    private final Supplier<BaseController<?>> controller;
    private MutablePresentationModelSource<RuntimeOverviewModel> runtimeOverviewPresentationSource;
    private MemoryPresentationSource structureMemoryPresentationSource;
    private MemoryPresentationSource algorithmMemoryPresentationSource;
    private FxMemoryRenderer structureMemoryRenderer;
    private FxMemoryRenderer algorithmMemoryRenderer;
    private StructureFootprint structureFootprint;
    private boolean structureFootprintBusy;
    private long memoryContextGeneration;

    WorkbenchTelemetryPresentation(PresentationSurfacePort port, Supplier<BaseController<?>> controller) {
        this.presentationSurfacePort = port;
        this.controller = controller;
    }

    void resetContext() {
        memoryContextGeneration++;
        structureFootprint = null;
        structureFootprintBusy = false;
    }

    void installRuntime(GridPane structureMetricsGrid, VBox algorithmMetricsSection,
            GridPane algorithmMetricsGrid, GridPane performanceMetricsGrid,
            FxRuntimeOverviewRenderer.RunSummaryBindings runMetrics) {
        runtimeOverviewPresentationSource =
                new MutablePresentationModelSource<>(RuntimeOverviewModel.empty());
        PresentationSurface<RuntimeOverviewModel> surface = PresentationSurface.standalone(
                RUNTIME_OVERVIEW_SURFACE_ID,
                runtimeOverviewPresentationSource,
                new FxRuntimeOverviewRenderer(
                        structureMetricsGrid,
                        algorithmMetricsSection,
                        algorithmMetricsGrid,
                        performanceMetricsGrid,
                        runMetrics));
        presentationSurfacePort.registerPresentationSurface(surface)
                .thenCompose(ignored ->
                        presentationSurfacePort.activatePresentationSurface(RUNTIME_OVERVIEW_SURFACE_ID))
                .exceptionally(failure -> {
                    System.err.println("Failed to initialize runtime overview presentation surface: "
                            + failure);
                    return null;
                });
    }

    void installMemory(MemoryProfileView structureMemoryView, MemoryProfileView algorithmMemoryView) {
        if (structureMemoryView == null || algorithmMemoryView == null) return;
        structureMemoryPresentationSource =
                new MemoryPresentationSource(MemoryPresentationInput.empty(TelemetryDomain.STRUCTURE));
        algorithmMemoryPresentationSource =
                new MemoryPresentationSource(MemoryPresentationInput.empty(TelemetryDomain.ALGORITHM));
        structureMemoryRenderer = new FxMemoryRenderer(structureMemoryView);
        algorithmMemoryRenderer = new FxMemoryRenderer(algorithmMemoryView);

        structureMemoryView.setDeepAnalysisAction(this::setDeepMemoryAnalysisEnabled);
        algorithmMemoryView.setDeepAnalysisAction(this::setDeepMemoryAnalysisEnabled);
        structureMemoryView.setFootprintAction(this::requestStructureFootprint);
        structureMemoryView.setChartInvalidationAction(
                () -> invalidateMemorySurface(STRUCTURE_MEMORY_SURFACE_ID));
        algorithmMemoryView.setChartInvalidationAction(
                () -> invalidateMemorySurface(ALGORITHM_MEMORY_SURFACE_ID));

        registerMemorySurface(PresentationSurface.standalone(
                STRUCTURE_MEMORY_SURFACE_ID, structureMemoryPresentationSource, structureMemoryRenderer));
        registerMemorySurface(PresentationSurface.standalone(
                ALGORITHM_MEMORY_SURFACE_ID, algorithmMemoryPresentationSource, algorithmMemoryRenderer));
    }

    private <M> void registerMemorySurface(PresentationSurface<M> surface) {
        presentationSurfacePort.registerPresentationSurface(surface)
                .thenCompose(ignored ->
                        presentationSurfacePort.activatePresentationSurface(surface.sessionId()))
                .exceptionally(failure -> {
                    System.err.println("Failed to initialize Memory presentation surface: " + failure);
                    return null;
                });
    }

    void bindMemoryPresentationCursor(RenderSessionId cursorSessionId) {
        if (cursorSessionId == null
                || structureMemoryPresentationSource == null
                || algorithmMemoryPresentationSource == null) {
            return;
        }
        presentationSurfacePort.registerPresentationSurface(PresentationSurface.followingCursor(
                STRUCTURE_MEMORY_SURFACE_ID,
                cursorSessionId,
                structureMemoryPresentationSource,
                structureMemoryRenderer));
        presentationSurfacePort.registerPresentationSurface(PresentationSurface.followingCursor(
                ALGORITHM_MEMORY_SURFACE_ID,
                cursorSessionId,
                algorithmMemoryPresentationSource,
                algorithmMemoryRenderer));
    }

    private void invalidateMemorySurface(RenderSessionId surfaceId) {
        presentationSurfacePort.invalidatePresentationSurface(surfaceId);
    }

    void publishRuntimeOverview(RuntimeOverviewModel overview) {
        if (runtimeOverviewPresentationSource == null) return;
        runtimeOverviewPresentationSource.publish(
                overview == null ? RuntimeOverviewModel.empty() : overview);
        presentationSurfacePort.invalidatePresentationSurface(RUNTIME_OVERVIEW_SURFACE_ID);
    }

    void publishMemoryPresentations() {
        BaseController<?> currentSubController = controller.get();
        if (structureMemoryPresentationSource == null || algorithmMemoryPresentationSource == null) {
            return;
        }
        if (currentSubController == null) {
            structureMemoryPresentationSource.publish(
                    MemoryPresentationInput.empty(TelemetryDomain.STRUCTURE));
            algorithmMemoryPresentationSource.publish(
                    MemoryPresentationInput.empty(TelemetryDomain.ALGORITHM));
        } else {
            var capabilities = currentSubController.memoryCapabilities();
            structureMemoryPresentationSource.publish(new MemoryPresentationInput(
                    TelemetryDomain.STRUCTURE,
                    capabilities,
                    currentSubController.structureMemoryProfile(),
                    currentSubController.latestStructureExecutionAnchors(),
                    currentSubController.structureDeepMemoryProfile(),
                    java.util.Optional.ofNullable(structureFootprint),
                    currentSubController.isDeepMemoryAnalysisEnabled(),
                    currentSubController.structureFootprintAvailable(),
                    structureFootprintBusy));
            algorithmMemoryPresentationSource.publish(new MemoryPresentationInput(
                    TelemetryDomain.ALGORITHM,
                    capabilities,
                    currentSubController.algorithmMemoryProfile(),
                    currentSubController.latestExecutionAnchors(),
                    currentSubController.algorithmDeepMemoryProfile(),
                    java.util.Optional.empty(),
                    currentSubController.isDeepMemoryAnalysisEnabled(),
                    false,
                    false));
        }
        invalidateMemorySurface(STRUCTURE_MEMORY_SURFACE_ID);
        invalidateMemorySurface(ALGORITHM_MEMORY_SURFACE_ID);
    }

    private void setDeepMemoryAnalysisEnabled(boolean enabled) {
        if (currentSubController == null) return;
        currentSubController.setDeepMemoryAnalysisEnabled(enabled);
        publishMemoryPresentations();
    }

    private void requestStructureFootprint() {
        BaseController<?> currentSubController = controller.get();
        BaseController<?> owner = currentSubController;
        if (owner == null || structureFootprintBusy || !owner.structureFootprintAvailable()) return;
        long generation = memoryContextGeneration;
        structureFootprintBusy = true;
        structureFootprint = null;
        publishMemoryPresentations();
        try {
            owner.analyzeStructureFootprint().whenComplete((result, failure) -> FxDispatch.defer(() -> {
                if (owner != controller.get() || generation != memoryContextGeneration) return;
                structureFootprintBusy = false;
                structureFootprint = failure == null
                        ? result
                        : StructureFootprint.unavailable(
                                failure.getMessage() == null
                                        ? failure.getClass().getSimpleName()
                                        : failure.getMessage());
                publishMemoryPresentations();
            }));
        } catch (RuntimeException failure) {
            structureFootprintBusy = false;
            structureFootprint = StructureFootprint.unavailable(
                    failure.getMessage() == null ? failure.getClass().getSimpleName() : failure.getMessage());
            publishMemoryPresentations();
        }
    }

}
