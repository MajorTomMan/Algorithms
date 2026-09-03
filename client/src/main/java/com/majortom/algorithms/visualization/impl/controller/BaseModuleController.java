package com.majortom.algorithms.visualization.impl.controller;

import com.majortom.algorithms.visualization.BaseController;
import com.majortom.algorithms.visualization.BaseVisualizer;
import com.majortom.algorithms.visualization.international.I18N;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;

import java.io.IOException;

/** Module controller with deliberately delayed FXML loading. */
public abstract class BaseModuleController<S> extends BaseController<S> {

    private static final String SECTION_EXPANDED_PROPERTY =
            BaseModuleController.class.getName() + ".sectionExpanded";

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
            WorkbenchTheme.apply(controlPanel);
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

    /** Toggles the controls that belong to the section whose header was clicked. */
    @FXML
    protected final void toggleSection(MouseEvent event) {
        toggleSection(event.getSource());
        event.consume();
    }

    /** Allows keyboard users to expand or collapse the focused section header. */
    @FXML
    protected final void handleSectionKey(KeyEvent event) {
        if (event.getCode() != KeyCode.ENTER && event.getCode() != KeyCode.SPACE) {
            return;
        }
        toggleSection(event.getSource());
        event.consume();
    }

    private void toggleSection(Object source) {
        if (!(source instanceof Node header)
                || !(header.getParent() instanceof VBox section)) {
            return;
        }

        boolean expanded = !Boolean.FALSE.equals(
                section.getProperties().get(SECTION_EXPANDED_PROPERTY));
        setSectionExpanded(section, header, !expanded);
    }

    private void setSectionExpanded(VBox section, Node header, boolean expanded) {
        section.getProperties().put(SECTION_EXPANDED_PROPERTY, expanded);
        for (Node child : section.getChildren()) {
            if (child != header) {
                child.setManaged(expanded);
                child.setVisible(expanded);
            }
        }
        updateSectionChevron(header, expanded);
    }

    private void updateSectionChevron(Node header, boolean expanded) {
        if (!(header instanceof Pane pane)) {
            return;
        }
        for (Node child : pane.getChildren()) {
            if (child instanceof Label label && label.getStyleClass().contains("section-chevron")) {
                label.setText(expanded ? "⌃" : "⌄");
                return;
            }
        }
    }

    @Override
    protected final void resetModuleState() {
        onResetData();
    }

    protected abstract void onResetData();
}
