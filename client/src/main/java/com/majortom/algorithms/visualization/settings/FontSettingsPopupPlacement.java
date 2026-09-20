package com.majortom.algorithms.visualization.settings;

import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Popup;

/** Keeps a font-settings popover inside the owner Workbench; presentation only. */
public final class FontSettingsPopupPlacement {
    private FontSettingsPopupPlacement() {}

    public static void show(Popup popup, Node owner, Bounds anchor, Bounds window) {
        VBox shell = (VBox) popup.getContent().getFirst();
        HBox arrowRow = (HBox) shell.getChildren().getFirst();
        ScrollPane scroll = (ScrollPane) shell.getChildren().get(1);
        VBox form = (VBox) scroll.getContent();
        shell.applyCss();

        // Popup autoFix is screen-relative; constrain this popup to Workbench bounds.
        double margin = 12.0d;
        double maxWidth = Math.max(1.0d, window.getWidth() - 2.0d * margin);
        double popupWidth = Math.min(shell.prefWidth(-1.0d), maxWidth);
        shell.setMinWidth(0.0d);
        shell.setPrefWidth(popupWidth);
        shell.setMaxWidth(maxWidth);
        double maxHeight = Math.max(1.0d, window.getHeight() - 2.0d * margin);
        double arrowHeight = arrowRow.prefHeight(-1.0d);
        double formHeight = form.prefHeight(popupWidth);
        scroll.setMinHeight(0.0d);
        scroll.setPrefHeight(Math.min(Math.max(1.0d, maxHeight - arrowHeight), formHeight + 2.0d));
        double popupHeight = arrowHeight + scroll.getPrefHeight();
        double x = Math.max(window.getMinX() + margin + popupWidth,
                Math.min(anchor.getMaxX(), window.getMaxX() - margin));
        double y = Math.max(window.getMinY() + margin,
                Math.min(anchor.getMaxY(), window.getMaxY() - margin - popupHeight));
        if (y < anchor.getMaxY() - arrowHeight - 1.0d) {
            // Hide the arrow if the owner window forces the popup away from its trigger.
            arrowRow.setManaged(false);
            arrowRow.setVisible(false);
            scroll.setPrefHeight(Math.min(maxHeight, formHeight + 2.0d));
            popupHeight = scroll.getPrefHeight();
            y = Math.max(window.getMinY() + margin,
                    Math.min(anchor.getMaxY(), window.getMaxY() - margin - popupHeight));
        }
        popup.show(owner, x, y);
    }
}
