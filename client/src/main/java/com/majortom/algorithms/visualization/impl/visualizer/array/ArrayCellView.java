package com.majortom.algorithms.visualization.impl.visualizer.array;

import javafx.css.PseudoClass;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;

/** Array-specific cell view. It represents one visible index, not a generic structure node. */
public final class ArrayCellView extends StackPane {
    private static final PseudoClass HIGHLIGHTED = PseudoClass.getPseudoClass("highlighted");
    private static final PseudoClass COMPLETED = PseudoClass.getPseudoClass("completed");
    private static final double MIN_WIDTH = 62.0d;
    private static final double MIN_BODY_HEIGHT = 54.0d;

    private final Text valueText = new Text();
    private final Text indexText = new Text();

    public ArrayCellView(int index, int value) {
        getStyleClass().add("array-cell");
        valueText.getStyleClass().add("array-cell-value");
        indexText.getStyleClass().add("array-cell-index");

        StackPane valueLayer = new StackPane(valueText);
        valueLayer.getStyleClass().add("array-cell-body");
        valueLayer.setAlignment(Pos.CENTER);
        valueLayer.setMinSize(MIN_WIDTH, MIN_BODY_HEIGHT);
        valueLayer.setPadding(new Insets(9.0d, 14.0d, 9.0d, 14.0d));

        VBox content = new VBox(4.0d, valueLayer, indexText);
        content.setAlignment(Pos.CENTER);
        getChildren().add(content);
        setMinWidth(MIN_WIDTH);
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
