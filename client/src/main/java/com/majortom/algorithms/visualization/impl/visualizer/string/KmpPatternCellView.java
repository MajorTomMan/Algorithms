package com.majortom.algorithms.visualization.impl.visualizer.string;

import com.majortom.algorithms.visualization.common.VisualDensity;
import javafx.css.PseudoClass;
import javafx.geometry.Pos;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;

import java.util.Objects;

/** Presentation-only KMP pattern slot aligned beneath the immutable String base track. */
public final class KmpPatternCellView extends StackPane {
    private static final PseudoClass OBSERVED = PseudoClass.getPseudoClass("observed");
    private static final double BODY_HEIGHT = 36.0d;

    private final Text valueText = new Text();
    private final Text indexText = new Text();
    private final StackPane body = new StackPane();

    public KmpPatternCellView(int index, char value) {
        getStyleClass().add("kmp-pattern-cell");
        valueText.getStyleClass().add("kmp-pattern-value");
        indexText.getStyleClass().add("kmp-pattern-index");
        body.getStyleClass().add("kmp-pattern-body");
        body.setAlignment(Pos.CENTER);
        body.setMinHeight(BODY_HEIGHT);
        body.setPrefHeight(BODY_HEIGHT);
        body.setMaxHeight(BODY_HEIGHT);
        body.getChildren().setAll(valueText);
        VBox content = new VBox(2.0d, indexText, body);
        content.setAlignment(Pos.CENTER);
        getChildren().setAll(content);
        setIndex(index);
        setValue(value);
        setDensity(VisualDensity.DETAIL);
        setMouseTransparent(true);
    }

    public void setIndex(int index) {
        indexText.setText(Integer.toString(index));
    }

    public void setValue(char value) {
        valueText.setText(Character.toString(value));
    }

    public void setObserved(boolean observed) {
        pseudoClassStateChanged(OBSERVED, observed);
    }

    public void setDensity(VisualDensity density) {
        Objects.requireNonNull(density, "density");
        getStyleClass().removeAll("kmp-pattern-detail", "kmp-pattern-compact", "kmp-pattern-dense");
        switch (density) {
            case DETAIL -> configure("kmp-pattern-detail", 52.0d, true);
            case COMPACT -> configure("kmp-pattern-compact", 40.0d, true);
            case DENSE -> configure("kmp-pattern-dense", 28.0d, false);
        }
    }

    private void configure(String styleClass, double width, boolean showIndex) {
        getStyleClass().add(styleClass);
        body.setMinWidth(width);
        body.setPrefWidth(width);
        body.setMaxWidth(width);
        setMinWidth(width);
        setPrefWidth(width);
        setMaxWidth(width);
        indexText.setVisible(showIndex);
        indexText.setManaged(showIndex);
    }
}
