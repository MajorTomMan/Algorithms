package com.majortom.algorithms.visualization.impl.controller;

import com.majortom.algorithms.visualization.international.I18N;
import javafx.collections.FXCollections;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListCell;
import java.util.function.Consumer;

/** Typed selector shared by both graph implementations, without altering string selectors. */
final class GraphImplementationSelector {
    private GraphImplementationSelector() {}

    static void install(ComboBox<GraphImplementation> selector,
                        GraphImplementation selected,
                        Consumer<GraphImplementation> changeListener) {
        selector.setItems(FXCollections.observableArrayList(GraphImplementation.values()));
        selector.setCellFactory(ignored -> new Cell());
        selector.setButtonCell(new Cell());
        selector.getSelectionModel().select(selected);
        selector.getSelectionModel().selectedItemProperty().addListener(
                (observable, previous, next) -> {
                    if (next != null && next != selected) changeListener.accept(next);
                });
    }

    private static final class Cell extends ListCell<GraphImplementation> {
        @Override protected void updateItem(GraphImplementation value, boolean empty) {
            super.updateItem(value, empty);
            textProperty().unbind();
            if (empty || value == null) setText(null);
            else textProperty().bind(I18N.createStringBinding(value.labelKey()));
        }
    }
}
