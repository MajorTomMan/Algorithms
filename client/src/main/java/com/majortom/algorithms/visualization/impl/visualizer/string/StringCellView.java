package com.majortom.algorithms.visualization.impl.visualizer.string;

import com.majortom.algorithms.visualization.common.VisualDensity;
import javafx.css.PseudoClass;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;

import java.util.Objects;
import java.util.function.IntConsumer;

/** Presentation-only character slot for the String logical track. */
public final class StringCellView extends StackPane {
    private static final PseudoClass SELECTED = PseudoClass.getPseudoClass("selected");
    private static final PseudoClass CURRENT = PseudoClass.getPseudoClass("current");
    private static final PseudoClass OBSERVED = PseudoClass.getPseudoClass("observed");
    private static final PseudoClass VISITED = PseudoClass.getPseudoClass("visited");
    private static final PseudoClass COMPLETED = PseudoClass.getPseudoClass("completed");
    private static final double BODY_HEIGHT = 54.0d;

    private final Text characterText = new Text();
    private final Text indexText = new Text();
    private final StackPane body = new StackPane();
    private final Region selectionOutline = new Region();

    private int index;
    private VisualDensity density = VisualDensity.DETAIL;
    private IntConsumer selectionHandler = ignored -> { };

    public StringCellView(int index, char value) {
        getStyleClass().addAll("string-cell", "visual-entity");
        characterText.getStyleClass().add("string-cell-value");
        indexText.getStyleClass().add("string-cell-index");
        selectionOutline.getStyleClass().add("string-cell-selection-outline");
        selectionOutline.setMouseTransparent(true);

        body.getStyleClass().add("string-cell-body");
        body.setAlignment(Pos.CENTER);
        body.setMinHeight(BODY_HEIGHT);
        body.setPrefHeight(BODY_HEIGHT);
        body.setMaxHeight(BODY_HEIGHT);
        body.setPadding(new Insets(6.0d, 8.0d, 6.0d, 8.0d));
        body.getChildren().setAll(characterText, selectionOutline);

        VBox content = new VBox(4.0d, indexText, body);
        content.setAlignment(Pos.CENTER);
        getChildren().setAll(content);
        setCursor(Cursor.HAND);
        setPickOnBounds(true);
        setOnMouseClicked(event -> {
            if (event.getButton() == MouseButton.PRIMARY && event.getClickCount() == 1) {
                selectionHandler.accept(this.index);
                event.consume();
            }
        });
        setIndex(index);
        setValue(value);
        setDensity(VisualDensity.DETAIL, true);
    }

    public int index() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
        indexText.setText(Integer.toString(index));
    }

    public void setValue(char value) {
        characterText.setText(Character.toString(value));
    }

    public void setTrackPosition(int index, int size) {
        getStyleClass().removeAll("string-cell-first", "string-cell-middle", "string-cell-last", "string-cell-only");
        if (size <= 1) {
            getStyleClass().add("string-cell-only");
        } else if (index == 0) {
            getStyleClass().add("string-cell-first");
        } else if (index == size - 1) {
            getStyleClass().add("string-cell-last");
        } else {
            getStyleClass().add("string-cell-middle");
        }
    }

    public void setSelectionHandler(IntConsumer selectionHandler) {
        this.selectionHandler = Objects.requireNonNull(selectionHandler, "selectionHandler");
    }

    public void setSelected(boolean selected) {
        pseudoClassStateChanged(SELECTED, selected);
        selectionOutline.setVisible(selected);
    }

    public void setCurrent(boolean current) {
        pseudoClassStateChanged(CURRENT, current);
    }

    public void setObserved(boolean observed) {
        pseudoClassStateChanged(OBSERVED, observed);
    }

    public void setVisited(boolean visited) {
        pseudoClassStateChanged(VISITED, visited);
    }

    public void setCompleted(boolean completed) {
        pseudoClassStateChanged(COMPLETED, completed);
    }

    /** Compatibility alias retained for presentation callers using the old one-highlight API. */
    public void setHighlighted(boolean highlighted) {
        setObserved(highlighted);
    }

    public void setDensity(VisualDensity density, boolean importantIndex) {
        this.density = Objects.requireNonNull(density, "density");
        getStyleClass().removeAll("string-cell-detail", "string-cell-compact", "string-cell-dense");
        switch (density) {
            case DETAIL -> {
                getStyleClass().add("string-cell-detail");
                configureWidth(52.0d);
                indexText.setVisible(true);
                indexText.setManaged(true);
            }
            case COMPACT -> {
                getStyleClass().add("string-cell-compact");
                configureWidth(40.0d);
                boolean showIndex = importantIndex || index % 2 == 0;
                indexText.setVisible(showIndex);
                indexText.setManaged(true);
            }
            case DENSE -> {
                getStyleClass().add("string-cell-dense");
                configureWidth(28.0d);
                boolean showIndex = importantIndex || index % 5 == 0;
                indexText.setVisible(showIndex);
                indexText.setManaged(true);
            }
        }
    }

    public VisualDensity density() {
        return density;
    }

    private void configureWidth(double width) {
        body.setMinWidth(width);
        body.setPrefWidth(width);
        body.setMaxWidth(width);
        setMinWidth(width);
        setPrefWidth(width);
        setMaxWidth(width);
    }
}
