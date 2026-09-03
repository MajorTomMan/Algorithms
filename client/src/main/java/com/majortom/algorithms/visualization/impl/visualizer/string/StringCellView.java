package com.majortom.algorithms.visualization.impl.visualizer.string;

import javafx.css.PseudoClass;
import javafx.geometry.Pos;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;

/** String-specific character cell with its immutable visual index label. */
public final class StringCellView extends StackPane {
    private static final PseudoClass HIGHLIGHTED = PseudoClass.getPseudoClass("highlighted");
    private static final PseudoClass COMPLETED = PseudoClass.getPseudoClass("completed");
    private static final double WIDTH = 48.0d;
    private static final double HEIGHT = 48.0d;

    private final Text characterText = new Text();
    private final Text indexText = new Text();

    public StringCellView(int index, char value) {
        getStyleClass().add("string-cell");
        Rectangle body = new Rectangle(WIDTH, HEIGHT);
        body.getStyleClass().add("string-cell-body");
        characterText.getStyleClass().add("string-cell-value");
        indexText.getStyleClass().add("string-cell-index");
        StackPane valueLayer = new StackPane(body, characterText);
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

    public void setValue(char value) {
        characterText.setText(Character.toString(value));
    }

    public void setHighlighted(boolean highlighted) {
        pseudoClassStateChanged(HIGHLIGHTED, highlighted);
    }

    public void setCompleted(boolean completed) {
        pseudoClassStateChanged(COMPLETED, completed);
    }
}
