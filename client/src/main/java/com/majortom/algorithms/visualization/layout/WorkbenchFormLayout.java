package com.majortom.algorithms.visualization.layout;

import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBoxBase;
import javafx.scene.control.Label;
import javafx.scene.control.Labeled;
import javafx.scene.control.TextInputControl;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.Text;

import java.util.ArrayList;
import java.util.List;

/**
 * Central layout policy for every Structure/Algorithm control panel.
 *
 * <p>FXML describes semantic controls only. This class owns responsive rows,
 * localized text metrics and operation-control sizing so individual modules do
 * not need per-language, per-font-size or narrow-window fixes.</p>
 */
public final class WorkbenchFormLayout {

    private static final String INSTALLED_PROPERTY =
            WorkbenchFormLayout.class.getName() + ".installed";
    private static final String SECTION_METRICS_PROPERTY =
            WorkbenchFormLayout.class.getName() + ".sectionMetrics";
    private static final String REFRESH_SCHEDULED_PROPERTY =
            WorkbenchFormLayout.class.getName() + ".refreshScheduled";
    private static final double SECTION_SPACING = 8.0d;
    private static final double ROW_GAP = 6.0d;
    private static final double MIN_OPERATION_HEIGHT = 31.0d;
    private static final double METRIC_SAFETY_PADDING = 2.0d;
    private static final int AUTO_GROUP_COLUMNS = 2;

    private WorkbenchFormLayout() {
    }

    public static void install(Node root) {
        if (root == null || Boolean.TRUE.equals(root.getProperties().putIfAbsent(INSTALLED_PROPERTY, Boolean.TRUE))) {
            return;
        }
        replaceOperationRows(root);
        composeLooseOperationControls(root);
        normalize(root);
        installSectionMetrics(root);
    }

    private static void normalize(Node node) {
        if (node instanceof VBox box && isSection(box)) {
            box.setSpacing(SECTION_SPACING);
        }
        if (node instanceof TextInputControl input) {
            input.setMinWidth(0.0d);
            input.setPrefWidth(Region.USE_COMPUTED_SIZE);
            input.setMaxWidth(Double.MAX_VALUE);
            input.setMinHeight(Region.USE_PREF_SIZE);
            input.setPrefHeight(Region.USE_COMPUTED_SIZE);
        } else if (node instanceof ComboBoxBase<?> comboBox) {
            comboBox.setMinWidth(0.0d);
            comboBox.setPrefWidth(Region.USE_COMPUTED_SIZE);
            comboBox.setMaxWidth(Double.MAX_VALUE);
            comboBox.setMinHeight(Region.USE_PREF_SIZE);
            comboBox.setPrefHeight(Region.USE_COMPUTED_SIZE);
        } else if (node instanceof Button button) {
            button.setMinWidth(0.0d);
            button.setPrefWidth(Region.USE_COMPUTED_SIZE);
            button.setMaxWidth(Double.MAX_VALUE);
            button.setMinHeight(Region.USE_PREF_SIZE);
            button.setPrefHeight(Region.USE_COMPUTED_SIZE);
            // The row wraps whole controls. Keep button labels on one line so
            // CJK and Latin text follow the same geometry policy.
            button.setWrapText(false);
        } else if (node instanceof Label label && shouldWrap(label)) {
            label.setMinWidth(0.0d);
            label.setWrapText(true);
        } else if (node instanceof Labeled labeled) {
            labeled.setMinWidth(0.0d);
        }

        if (node instanceof Parent parent) {
            for (Node child : List.copyOf(parent.getChildrenUnmodifiable())) {
                normalize(child);
            }
        }
    }

    private static void replaceOperationRows(Node root) {
        if (!(root instanceof Parent parent)) {
            return;
        }
        List<Node> snapshot = new ArrayList<>(parent.getChildrenUnmodifiable());
        for (Node child : snapshot) {
            if (child instanceof HBox row && row.getStyleClass().contains("operation-row")) {
                replace(row);
            } else {
                replaceOperationRows(child);
            }
        }
    }

    private static void replace(HBox source) {
        if (!(source.getParent() instanceof Pane parent)) {
            return;
        }
        int index = parent.getChildren().indexOf(source);
        if (index < 0) {
            return;
        }

        AdaptiveFormRow replacement = operationRow(
                source.getSpacing() > 0.0d ? source.getSpacing() : ROW_GAP);
        replacement.setId(source.getId());
        replacement.getStyleClass().addAll(source.getStyleClass());
        replacement.setManaged(source.isManaged());
        replacement.setVisible(source.isVisible());
        replacement.setDisable(source.isDisable());
        replacement.setOpacity(source.getOpacity());

        List<Node> children = new ArrayList<>(source.getChildren());
        source.getChildren().clear();
        replacement.getChildren().setAll(children);
        parent.getChildren().set(index, replacement);
    }

    /**
     * Older module FXMLs contain direct operation-input / operation-button
     * children. Compose them into the same two-column adaptive grid used by
     * explicit operation-row declarations. This is intentionally centralized:
     * new and old modules get the same layout without FXML-specific patches.
     */
    private static void composeLooseOperationControls(Node root) {
        if (!(root instanceof Parent parent)) {
            return;
        }
        if (root instanceof VBox section && isOperationSection(section)) {
            composeSection(section);
        }
        for (Node child : List.copyOf(parent.getChildrenUnmodifiable())) {
            composeLooseOperationControls(child);
        }
    }

    private static void composeSection(VBox section) {
        List<Node> original = new ArrayList<>(section.getChildren());
        if (original.stream().noneMatch(WorkbenchFormLayout::isLooseOperationControl)) {
            return;
        }

        section.getChildren().clear();
        for (int index = 0; index < original.size();) {
            Node child = original.get(index);
            OperationRole role = operationRole(child);
            if (role == OperationRole.NONE) {
                section.getChildren().add(child);
                index++;
                continue;
            }

            AdaptiveFormRow row = operationRow(ROW_GAP);
            row.getStyleClass().add("auto-operation-row");
            int columns = 0;
            while (index < original.size() && columns < AUTO_GROUP_COLUMNS
                    && operationRole(original.get(index)) == role) {
                row.getChildren().add(original.get(index));
                index++;
                columns++;
            }
            syncManagedState(row);
            section.getChildren().add(row);
        }
    }

    private static AdaptiveFormRow operationRow(double gap) {
        AdaptiveFormRow row = new AdaptiveFormRow(gap);
        if (!row.getStyleClass().contains("operation-row")) {
            row.getStyleClass().add("operation-row");
        }
        row.setMaxWidth(Double.MAX_VALUE);
        return row;
    }

    private static void syncManagedState(AdaptiveFormRow row) {
        Runnable refresh = () -> {
            row.setManaged(row.getChildren().stream().anyMatch(Node::isManaged));
            row.setVisible(row.getChildren().stream().anyMatch(Node::isVisible));
        };
        for (Node child : row.getChildren()) {
            child.managedProperty().addListener((observable, oldValue, newValue) -> refresh.run());
            child.visibleProperty().addListener((observable, oldValue, newValue) -> refresh.run());
        }
        refresh.run();
    }

    /**
     * A module's outer controlPanel is later dismantled by MainController and
     * its sections are moved into structure/algorithm hosts. Therefore metric
     * listeners live on each section, not on the disposable outer panel.
     */
    private static void installSectionMetrics(Node root) {
        if (root instanceof VBox section && isOperationSection(section)) {
            installSectionMetricPolicy(section);
        }
        if (root instanceof Parent parent) {
            for (Node child : parent.getChildrenUnmodifiable()) {
                installSectionMetrics(child);
            }
        }
    }

    private static void installSectionMetricPolicy(VBox section) {
        if (Boolean.TRUE.equals(section.getProperties().putIfAbsent(SECTION_METRICS_PROPERTY, Boolean.TRUE))) {
            return;
        }
        section.sceneProperty().addListener((observable, oldScene, newScene) -> scheduleMetricRefresh(section));
        installFontListeners(section, section);
        scheduleMetricRefresh(section);
    }

    private static void installFontListeners(VBox section, Node node) {
        if (node instanceof TextInputControl input && isOperationControl(input)) {
            input.fontProperty().addListener((observable, oldValue, newValue) -> scheduleMetricRefresh(section));
        } else if (node instanceof Labeled labeled && isOperationControl(labeled)) {
            labeled.fontProperty().addListener((observable, oldValue, newValue) -> scheduleMetricRefresh(section));
        }
        if (node instanceof Parent parent) {
            for (Node child : parent.getChildrenUnmodifiable()) {
                installFontListeners(section, child);
            }
        }
    }

    private static void scheduleMetricRefresh(VBox section) {
        if (Boolean.TRUE.equals(section.getProperties().putIfAbsent(REFRESH_SCHEDULED_PROPERTY, Boolean.TRUE))) {
            return;
        }
        Platform.runLater(() -> {
            section.getProperties().remove(REFRESH_SCHEDULED_PROPERTY);
            if (section.getScene() == null) {
                return;
            }
            section.applyCss();
            refreshOperationMetrics(section);
            section.requestLayout();
        });
    }

    private static void refreshOperationMetrics(VBox section) {
        List<Region> controls = new ArrayList<>();
        collectOperationControls(section, controls);
        if (controls.isEmpty()) {
            return;
        }

        double commonHeight = MIN_OPERATION_HEIGHT;
        for (Region control : controls) {
            commonHeight = Math.max(commonHeight, metricHeight(control));
        }
        commonHeight = Math.ceil(commonHeight);
        for (Region control : controls) {
            control.setMinHeight(commonHeight);
            control.setPrefHeight(commonHeight);
        }
    }

    private static void collectOperationControls(Node node, List<Region> controls) {
        if (node instanceof Region region && isOperationControl(node)) {
            controls.add(region);
        }
        if (node instanceof Parent parent) {
            for (Node child : parent.getChildrenUnmodifiable()) {
                collectOperationControls(child, controls);
            }
        }
    }

    /**
     * Measure a mixed CJK/Latin probe with the control's resolved JavaFX font.
     * This accounts for CJK fallback ascent/descent instead of assuming that a
     * 24px font always fits in a 30px control.
     */
    private static double metricHeight(Region control) {
        Font font = fontOf(control);
        if (font == null) {
            return MIN_OPERATION_HEIGHT;
        }
        Text probe = new Text("国Ag");
        probe.setFont(font);
        return probe.getLayoutBounds().getHeight()
                + control.getInsets().getTop()
                + control.getInsets().getBottom()
                + METRIC_SAFETY_PADDING;
    }

    private static Font fontOf(Node node) {
        if (node instanceof TextInputControl input) {
            return input.getFont();
        }
        if (node instanceof Labeled labeled) {
            return labeled.getFont();
        }
        return null;
    }

    private static boolean isOperationControl(Node node) {
        return node.getStyleClass().contains("operation-input")
                || node.getStyleClass().contains("operation-button");
    }

    private static boolean isLooseOperationControl(Node node) {
        return operationRole(node) != OperationRole.NONE;
    }

    private static OperationRole operationRole(Node node) {
        if (node.getStyleClass().contains("operation-input")) {
            return OperationRole.INPUT;
        }
        if (node.getStyleClass().contains("operation-button")) {
            return OperationRole.BUTTON;
        }
        return OperationRole.NONE;
    }

    private static boolean isSection(VBox box) {
        return box.getStyleClass().contains("control-section")
                || box.getStyleClass().contains("control-card")
                || box.getStyleClass().contains("operation-section")
                || box.getStyleClass().contains("execution-section");
    }

    private static boolean isOperationSection(VBox box) {
        return box.getStyleClass().contains("operation-section")
                || box.getStyleClass().contains("execution-section");
    }

    private static boolean shouldWrap(Label label) {
        return label.getStyleClass().contains("control-label")
                || label.getStyleClass().contains("control-section-title")
                || label.getStyleClass().contains("operation-label")
                || label.getStyleClass().contains("operation-hint");
    }

    private enum OperationRole {
        NONE,
        INPUT,
        BUTTON
    }
}
