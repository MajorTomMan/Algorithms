package com.majortom.algorithms.visualization.impl.controller;

import com.majortom.algorithms.visualization.BaseController;
import com.majortom.algorithms.visualization.BaseVisualizer;
import com.majortom.algorithms.visualization.international.I18N;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.layout.HBox;

import java.io.IOException;

/** Module controller with deliberately delayed FXML loading. */
public abstract class BaseModuleController<S> extends BaseController<S> {

    private final String fxmlPath;
    protected Node controlPanel;

    protected BaseModuleController(BaseVisualizer<S> visualizer, String fxmlPath) {
        super(visualizer);
        this.fxmlPath = fxmlPath;
    }

    /** Called after the concrete controller constructor has completed. */
    public final void loadControlPanel() {
        if (controlPanel != null || fxmlPath == null || fxmlPath.isBlank()) {
            return;
        }
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            loader.setResources(I18N.getBundle());
            loader.setController(this);
            controlPanel = loader.load();
        } catch (IOException exception) {
            throw new IllegalStateException("Module control panel load failed: " + fxmlPath, exception);
        }
    }

    @Override
    public final void setupCustomControls(HBox container) {
        loadControlPanel();
        if (container != null && controlPanel != null) {
            container.getChildren().setAll(controlPanel);
        }
    }

    protected final void logI18n(String key, Object... arguments) {
        Runnable task = () -> appendLog(I18N.text(key, arguments));
        if (Platform.isFxApplicationThread()) {
            task.run();
        } else {
            Platform.runLater(task);
        }
    }

    protected final String formatMetric(String key, long value) {
        return I18N.text(key, value);
    }

    public final void resetModule() {
        stopAlgorithm();
        if (logArea != null) {
            logArea.clear();
        }
        onResetData();
        dispatchVisualizerReset();
        refreshStatsDisplay();
    }

    protected abstract void onResetData();
}
