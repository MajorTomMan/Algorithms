package com.majortom.algorithms.visualization.common;

import javafx.scene.layout.StackPane;

/**
 * A full-size overlay host whose children never contribute to the viewport's
 * requested size. StackPane still lays out and anchors its managed cards.
 */
public final class OverlayPane extends StackPane {
    @Override protected double computeMinWidth(double height) { return 0; }
    @Override protected double computeMinHeight(double width) { return 0; }
    @Override protected double computePrefWidth(double height) { return 0; }
    @Override protected double computePrefHeight(double width) { return 0; }
}
