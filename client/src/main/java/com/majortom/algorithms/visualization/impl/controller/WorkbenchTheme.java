package com.majortom.algorithms.visualization.impl.controller;

import atlantafx.base.theme.Styles;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.ButtonBase;
import javafx.scene.control.ComboBoxBase;
import javafx.scene.control.TextInputControl;

/** Applies AtlantaFX control semantics while preserving project-specific layout classes. */
final class WorkbenchTheme {

    private WorkbenchTheme() {
    }

    static void apply(Node root) {
        if (root == null) {
            return;
        }
        applyControl(root);
        if (root instanceof Parent parent) {
            for (Node child : parent.getChildrenUnmodifiable()) {
                apply(child);
            }
        }
    }

    static <T extends Node> T applyControl(T node) {
        if (node instanceof ButtonBase) {
            add(node, Styles.DENSE);
            applyButtonSemantic(node);
        } else if (node instanceof TextInputControl || node instanceof ComboBoxBase<?>) {
            add(node, Styles.DENSE);
        }
        if (node.getStyleClass().contains("operation-dialog-pane")) {
            add(node, Styles.ELEVATED_2);
        }
        return node;
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
