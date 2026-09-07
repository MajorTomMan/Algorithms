package com.majortom.algorithms.visualization.impl.controller;

import atlantafx.base.theme.Styles;
import javafx.application.Platform;
import javafx.css.PseudoClass;
import javafx.event.ActionEvent;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBase;
import javafx.scene.control.ComboBoxBase;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextInputControl;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.collections.ListChangeListener;

/** Applies AtlantaFX control semantics while preserving project-specific layout classes. */
final class WorkbenchTheme {

    private static final PseudoClass SELECTED = PseudoClass.getPseudoClass("selected");

    private WorkbenchTheme() {
    }

    static void apply(Node root) {
        if (root == null) {
            return;
        }
        installWorkbenchFixes(root);
        applyControl(root);
        if (root instanceof Parent parent) {
            for (Node child : parent.getChildrenUnmodifiable()) {
                apply(child);
            }
        }
    }

    private static void installWorkbenchFixes(Node root) {
        if (!(root instanceof Parent parent) || root.getParent() != null) {
            return;
        }
        java.net.URL resource = WorkbenchTheme.class.getResource("/style/workbench-fixes.css");
        if (resource == null) {
            return;
        }
        String stylesheet = resource.toExternalForm();
        if (!parent.getStylesheets().contains(stylesheet)) {
            parent.getStylesheets().add(stylesheet);
        }
    }

    static <T extends Node> T applyControl(T node) {
        if (node instanceof ButtonBase) {
            add(node, Styles.DENSE);
            applyButtonSemantic(node);
            if (node instanceof Button button && "savedInputBtn".equals(button.getId())) {
                installSavedSnapshotPicker(button);
            }
        } else if (node instanceof TextInputControl || node instanceof ComboBoxBase<?>) {
            add(node, Styles.DENSE);
        }
        if (node instanceof VBox host && "algorithmControlsHost".equals(node.getId())) {
            installMazeRunVisibility(host);
        }
        if (node.getStyleClass().contains("operation-dialog-pane")) {
            add(node, Styles.ELEVATED_2);
        }
        return node;
    }

    private static void installMazeRunVisibility(VBox host) {
        if (Boolean.TRUE.equals(host.getProperties().putIfAbsent("mazeRunVisibilityInstalled", Boolean.TRUE))) {
            return;
        }
        host.getChildren().addListener((ListChangeListener<Node>) change ->
                Platform.runLater(() -> refreshMazeRunVisibility(host)));
        Platform.runLater(() -> refreshMazeRunVisibility(host));
    }

    private static void refreshMazeRunVisibility(VBox host) {
        Parent root = rootOf(host);
        Node startNode;
        if (root == null) {
            startNode = null;
        } else {
            startNode = root.lookup("#startBtn");
        }
        if (!(startNode instanceof Button startButton)) {
            return;
        }
        boolean mazeControls = host.lookup("#buildBtn") != null && host.lookup("#solveBtn") != null;
        startButton.setManaged(!mazeControls);
        startButton.setVisible(!mazeControls);
    }

    /**
     * Upgrades the existing Saved Snapshot button into an arbitrary-snapshot picker. The menu
     * delegates to the action already attached to each snapshot card, preserving one input owner.
     */
    private static void installSavedSnapshotPicker(Button button) {
        if (Boolean.TRUE.equals(button.getProperties().putIfAbsent("savedSnapshotPickerInstalled", Boolean.TRUE))) {
            return;
        }
        button.addEventFilter(ActionEvent.ACTION, event -> {
            event.consume();
            showSavedSnapshotMenu(button);
        });
    }

    private static void showSavedSnapshotMenu(Button source) {
        Parent root = rootOf(source);
        Node cardsNode;
        if (root == null) {
            cardsNode = null;
        } else {
            cardsNode = root.lookup("#snapshotCards");
        }
        if (!(cardsNode instanceof VBox snapshotCards)) {
            return;
        }

        ContextMenu menu = new ContextMenu();
        menu.getStyleClass().add("algorithm-snapshot-menu");
        for (Node node : snapshotCards.getChildren()) {
            if (!(node instanceof VBox card) || !card.getStyleClass().contains("snapshot-card-saved")) {
                continue;
            }
            Button useInput = lastButton(card);
            if (useInput == null) {
                continue;
            }
            String label = snapshotCardLabel(card);
            MenuItem item = new MenuItem(label);
            item.setOnAction(event -> {
                useInput.fire();
                Platform.runLater(() -> showSelectedSnapshot(source, root, label));
            });
            menu.getItems().add(item);
        }
        if (!menu.getItems().isEmpty()) {
            menu.show(source, javafx.geometry.Side.BOTTOM, 0.0d, 4.0d);
        }
    }

    private static void showSelectedSnapshot(Button savedButton, Parent root, String label) {
        savedButton.pseudoClassStateChanged(SELECTED, true);
        Node current = root.lookup("#currentInputBtn");
        if (current != null) {
            current.pseudoClassStateChanged(SELECTED, false);
        }
        Node sourceLabel = root.lookup("#algorithmInputSourceLabel");
        if (sourceLabel instanceof Label algorithmInputSourceLabel) {
            algorithmInputSourceLabel.setText(label);
        }
    }

    private static Button lastButton(Parent parent) {
        Button result = null;
        for (Node child : parent.getChildrenUnmodifiable()) {
            if (child instanceof Button button) {
                result = button;
            }
            if (child instanceof Parent nested) {
                Button nestedResult = lastButton(nested);
                if (nestedResult != null) {
                    result = nestedResult;
                }
            }
        }
        return result;
    }

    private static String snapshotCardLabel(VBox card) {
        for (Node child : card.getChildren()) {
            if (child instanceof Label label && label.getText() != null && !label.getText().isBlank()) {
                return label.getText();
            }
        }
        return "Snapshot";
    }

    private static Parent rootOf(Node node) {
        Parent parent = node.getParent();
        while (parent != null && parent.getParent() != null) {
            parent = parent.getParent();
        }
        return parent;
    }

    static <T extends Node> T outlined(T node) {
        add(node, Styles.BUTTON_OUTLINED, Styles.DENSE);
        return node;
    }

    static <T extends Node> T warningOutlined(T node) {
        add(node, Styles.WARNING, Styles.BUTTON_OUTLINED, Styles.DENSE);
        return node;
    }

    static <T extends Node> T leftPill(T node) {
        add(node, Styles.LEFT_PILL);
        return node;
    }

    static <T extends Node> T rightPill(T node) {
        add(node, Styles.RIGHT_PILL);
        return node;
    }

    private static void applyButtonSemantic(Node node) {
        if (hasAny(node, "btn-primary", "btn-run-neon", "btn-ran-blue", "btn-ran-purple", "btn-neon-cyan")) {
            add(node, Styles.ACCENT);
        } else if (hasAny(node, "btn-ran-red", "btn-neon-pink")) {
            add(node, Styles.DANGER);
        } else if (hasAny(node, "btn-ran-gold", "btn-ran-yellow")) {
            add(node, Styles.WARNING);
        } else if (hasAny(node, "btn-ran-white", "shell-toggle-button", "workspace-mode-button",
                "module-button", "sidebar-catalog-button", "sidebar-algorithm-button", "snapshot-card-action")) {
            add(node, Styles.BUTTON_OUTLINED);
        }

        if (hasAny(node, "compact-button", "operation-button", "snapshot-card-action")) {
            add(node, Styles.SMALL);
        }
        if (node.getStyleClass().contains("operation-button") && node instanceof Region region) {
            region.setMaxWidth(Double.MAX_VALUE);
            if (node.getParent() instanceof HBox) {
                HBox.setHgrow(node, Priority.ALWAYS);
            }
        }
    }

    private static boolean hasAny(Node node, String... styleClasses) {
        for (String styleClass : styleClasses) {
            if (node.getStyleClass().contains(styleClass)) {
                return true;
            }
        }
        return false;
    }

    private static void add(Node node, String... styleClasses) {
        Styles.addStyleClass(node, styleClasses[0], tail(styleClasses));
    }

    private static String[] tail(String[] values) {
        if (values.length <= 1) {
            return new String[0];
        }
        String[] tail = new String[values.length - 1];
        System.arraycopy(values, 1, tail, 0, tail.length);
        return tail;
    }
}
