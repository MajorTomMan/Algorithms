package com.majortom.algorithms.visualization.impl.controller;

import javafx.scene.Node;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;

import java.net.URL;

/** Shared client-side styling bridge for dynamically built operation dialogs. */
final class OperationDialogTheme {

    private static final String THEME_PATH = "/style/theme.css";

    private OperationDialogTheme() {
    }

    static void apply(Dialog<?> dialog, double preferredWidth) {
        DialogPane pane = dialog.getDialogPane();
        URL theme = OperationDialogTheme.class.getResource(THEME_PATH);
        if (theme != null && !pane.getStylesheets().contains(theme.toExternalForm())) {
            pane.getStylesheets().add(theme.toExternalForm());
        }
        addClasses(pane, "operation-dialog-pane");
        if (pane.getContent() != null) {
            addClasses(pane.getContent(), "operation-dialog-content");
        }
        Node closeButton = pane.lookupButton(ButtonType.CLOSE);
        if (closeButton != null) {
            addClasses(closeButton, "btn-ran-gold", "compact-button");
        }
        pane.setMinWidth(preferredWidth);
        pane.setPrefWidth(preferredWidth);
        dialog.setResizable(true);
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
