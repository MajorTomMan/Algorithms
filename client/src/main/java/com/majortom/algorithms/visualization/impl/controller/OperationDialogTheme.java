package com.majortom.algorithms.visualization.impl.controller;

import javafx.scene.Node;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;

import java.net.URL;

/** Shared client-side styling bridge for dynamically built operation dialogs. */
final class OperationDialogTheme {

    private static final String THEME_PATH = "/style/theme.css";
    private static final String LAYOUT_PATH = "/style/workbench-layout.css";

    private OperationDialogTheme() {
    }

    static void apply(Dialog<?> dialog) {
        DialogPane pane = dialog.getDialogPane();
        addStylesheet(pane, THEME_PATH);
        addStylesheet(pane, LAYOUT_PATH);
        addClasses(pane, "operation-dialog-pane");
        WorkbenchTheme.apply(pane);
        if (pane.getContent() != null) {
            addClasses(pane.getContent(), "operation-dialog-content");
        }
        Node closeButton = pane.lookupButton(ButtonType.CLOSE);
        if (closeButton != null) {
            addClasses(closeButton, "btn-ran-gold", "compact-button");
            WorkbenchTheme.warningOutlined(closeButton);
        }
        dialog.setResizable(true);
    }

    private static void addStylesheet(DialogPane pane, String path) {
        URL resource = OperationDialogTheme.class.getResource(path);
        if (resource != null && !pane.getStylesheets().contains(resource.toExternalForm())) {
            pane.getStylesheets().add(resource.toExternalForm());
        }
    }

    static <T extends Node> T addClasses(T node, String... styleClasses) {
        for (String styleClass : styleClasses) {
            if (!node.getStyleClass().contains(styleClass)) {
                node.getStyleClass().add(styleClass);
            }
        }
        return node;
    }
}
