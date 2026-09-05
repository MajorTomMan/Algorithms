package com.majortom.algorithms.visualization.impl.visualizer.array;

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

/** Presentation-only cell of the Array logical memory strip. */
public final class ArrayCellView extends StackPane {
    private static final PseudoClass SELECTED = PseudoClass.getPseudoClass("selected");
    private static final PseudoClass CURRENT = PseudoClass.getPseudoClass("current");
    private static final PseudoClass OBSERVED = PseudoClass.getPseudoClass("observed");
    private static final PseudoClass VISITED = PseudoClass.getPseudoClass("visited");
    private static final PseudoClass COMPLETED = PseudoClass.getPseudoClass("completed");
    private static final double BODY_HEIGHT = 58.0d;

    private final Text valueText = new Text();
    private final Text indexText = new Text();
    private final StackPane body = new StackPane();
    private final Region selectionOutline = new Region();

    private int index;
    private VisualDensity density = VisualDensity.DETAIL;
    private IntConsumer selectionHandler = ignored -> { };

    public ArrayCellView(int index, int value) {
        getStyleClass().addAll("array-cell", "visual-entity");
        valueText.getStyleClass().add("array-cell-value");
        indexText.getStyleClass().add("array-cell-index");
        selectionOutline.getStyleClass().add("array-cell-selection-outline");
        selectionOutline.setMouseTransparent(true);
        selectionOutline.setManaged(true);

        body.getStyleClass().add("array-cell-body");
        body.setAlignment(Pos.CENTER);
        body.setMinHeight(BODY_HEIGHT);
        body.setPrefHeight(BODY_HEIGHT);
        body.setMaxHeight(BODY_HEIGHT);
        body.setPadding(new Insets(8.0d, 10.0d, 8.0d, 10.0d));
        body.getChildren().setAll(valueText, selectionOutline);

        VBox content = new VBox(5.0d, indexText, body);
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

    public void setValue(int value) {
        valueText.setText(Integer.toString(value));
    }


    public void setStripPosition(int index, int size) {
        getStyleClass().removeAll("array-cell-first", "array-cell-middle", "array-cell-last", "array-cell-only");
        if (size <= 1) {
            getStyleClass().add("array-cell-only");
        } else if (index == 0) {
            getStyleClass().add("array-cell-first");
        } else if (index == size - 1) {
            getStyleClass().add("array-cell-last");
        } else {
            getStyleClass().add("array-cell-middle");
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

    /** Compatibility alias for existing reducers that expose one highlighted mutation index. */
    public void setHighlighted(boolean highlighted) {
        setObserved(highlighted);
    }

    public void setDensity(VisualDensity density, boolean importantIndex) {
        this.density = Objects.requireNonNull(density, "density");
        getStyleClass().removeAll("array-cell-detail", "array-cell-compact", "array-cell-dense");
        switch (density) {
            case DETAIL -> {
                getStyleClass().add("array-cell-detail");
                configureWidth(64.0d);
                indexText.setVisible(true);
                indexText.setManaged(true);
            }
            case COMPACT -> {
                getStyleClass().add("array-cell-compact");
                configureWidth(50.0d);
                boolean showIndex = importantIndex || index % 2 == 0;
                indexText.setVisible(showIndex);
                indexText.setManaged(true);
            }
            case DENSE -> {
                getStyleClass().add("array-cell-dense");
                configureWidth(38.0d);
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
