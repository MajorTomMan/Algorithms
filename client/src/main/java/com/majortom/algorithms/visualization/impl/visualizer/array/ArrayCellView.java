package com.majortom.algorithms.visualization.impl.visualizer.array;

import javafx.css.PseudoClass;
import javafx.geometry.Pos;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;

/** Array-specific cell view. It represents one visible index, not a generic structure node. */
public final class ArrayCellView extends StackPane {
    private static final PseudoClass HIGHLIGHTED = PseudoClass.getPseudoClass("highlighted");
    private static final PseudoClass COMPLETED = PseudoClass.getPseudoClass("completed");
    private static final double WIDTH = 62.0d;
    private static final double HEIGHT = 54.0d;

    private final Text valueText = new Text();
    private final Text indexText = new Text();

    public ArrayCellView(int index, int value) {
        getStyleClass().add("array-cell");
        Rectangle body = new Rectangle(WIDTH, HEIGHT);
        body.getStyleClass().add("array-cell-body");
        valueText.getStyleClass().add("array-cell-value");
        indexText.getStyleClass().add("array-cell-index");
        StackPane valueLayer = new StackPane(body, valueText);
        valueLayer.setAlignment(Pos.CENTER);
        indexText.setTranslateY(HEIGHT / 2.0d + 13.0d);
        getChildren().addAll(valueLayer, indexText);
        setMinSize(WIDTH, HEIGHT + 20.0d);
        setPrefSize(WIDTH, HEIGHT + 20.0d);
        setMaxSize(WIDTH, HEIGHT + 20.0d);
        setIndex(index);
        setValue(value);
    }

    public void setIndex(int index) {
        indexText.setText(Integer.toString(index));
    }

    public void setValue(int value) {
        valueText.setText(Integer.toString(value));
    }

    public void setHighlighted(boolean highlighted) {
        pseudoClassStateChanged(HIGHLIGHTED, highlighted);
    }

    public void setCompleted(boolean completed) {
        pseudoClassStateChanged(COMPLETED, completed);
    }
}
