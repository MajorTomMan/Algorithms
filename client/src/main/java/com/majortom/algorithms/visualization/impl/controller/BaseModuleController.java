package com.majortom.algorithms.visualization.impl.controller;

import com.majortom.algorithms.visualization.BaseController;
import com.majortom.algorithms.visualization.BaseVisualizer;
import com.majortom.algorithms.visualization.international.I18N;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
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
            installDataTools();
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

    /** Keeps a one-option selector translated when the application locale changes. */
    protected final void bindSingleLocalizedChoice(ComboBox<String> selector, String key) {
        if (selector == null) {
            return;
        }
        Runnable refresh = () -> {
            selector.getItems().setAll(I18N.text(key));
            selector.getSelectionModel().selectFirst();
        };
        refresh.run();
        I18N.localeProperty().addListener((observable, oldLocale, newLocale) -> refresh.run());
    }

    protected boolean supportsDataTools() {
        return false;
    }

    protected boolean showRandomDataTool() {
        return true;
    }

    protected String bulkInputPromptKey() {
        return "prompt.data.bulk.values";
    }

    protected void applyBulkData(String input) {
    }

    protected void randomizeData() {
    }

    protected final java.util.List<Integer> parseIntegerBatchInput(String input) {
        if (input == null || input.isBlank()) {
            logI18n("message.error.bulk_input_empty");
            return null;
        }
        String[] tokens = input.trim().split("[,;\\s]+");
        java.util.List<Integer> values = new java.util.ArrayList<>(tokens.length);
        try {
            for (String token : tokens) {
                if (!token.isBlank()) {
                    values.add(Integer.valueOf(token));
                }
            }
        } catch (NumberFormatException exception) {
            logI18n("message.error.bulk_input_invalid");
            return null;
        }
        if (values.isEmpty()) {
            logI18n("message.error.bulk_input_empty");
            return null;
        }
        return java.util.List.copyOf(values);
    }

    private void installDataTools() {
        if (!supportsDataTools() || !(controlPanel instanceof VBox root)) {
            return;
        }

        VBox section = new VBox(8);
        section.getStyleClass().addAll(
                "control-section", "control-card", "operation-section", "structure-section");

        HBox header = new HBox();
        header.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        header.setFocusTraversable(true);
        header.getStyleClass().add("control-section-header");
        header.setOnMouseClicked(this::toggleSection);
        header.setOnKeyPressed(this::handleSectionKey);

        Label title = new Label();
        title.textProperty().bind(I18N.createStringBinding("label.data.tools"));
        title.getStyleClass().add("control-label");
        Region spacer = new Region();
        HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);
        Label chevron = new Label("⌃");
        chevron.getStyleClass().add("section-chevron");
        header.getChildren().addAll(title, spacer, chevron);

        TextField bulkInput = new TextField();
        bulkInput.setMaxWidth(Double.MAX_VALUE);
        bulkInput.promptTextProperty().bind(I18N.createStringBinding(bulkInputPromptKey()));
        bulkInput.getStyleClass().addAll("dark-textfield", "operation-input");

        Button applyButton = new Button();
        applyButton.setMaxWidth(Double.MAX_VALUE);
        applyButton.textProperty().bind(I18N.createStringBinding("action.data.apply"));
        applyButton.getStyleClass().addAll("btn-ran-blue", "operation-button");
        applyButton.setOnAction(event -> applyBulkData(bulkInput.getText()));
        HBox.setHgrow(applyButton, javafx.scene.layout.Priority.ALWAYS);

        HBox actions = new HBox(6);
        actions.getStyleClass().add("operation-row");
        actions.getChildren().add(applyButton);
        if (showRandomDataTool()) {
            Button randomButton = new Button();
            randomButton.setMaxWidth(Double.MAX_VALUE);
            randomButton.textProperty().bind(I18N.createStringBinding("action.data.random"));
            randomButton.getStyleClass().addAll("btn-ran-gold", "operation-button");
            randomButton.setOnAction(event -> randomizeData());
            HBox.setHgrow(randomButton, javafx.scene.layout.Priority.ALWAYS);
            actions.getChildren().add(randomButton);
        }

        section.getChildren().addAll(header, bulkInput, actions);
        root.getChildren().add(section);
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
                if (expanded) {
                    label.setText("⌃");
                } else {
                    label.setText("⌄");
                }
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
