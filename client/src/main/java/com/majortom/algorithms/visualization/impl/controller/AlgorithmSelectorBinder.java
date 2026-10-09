package com.majortom.algorithms.visualization.impl.controller;

import com.majortom.algorithms.visualization.algorithm.AlgorithmCatalog;
import com.majortom.algorithms.visualization.international.I18N;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;
import javafx.beans.binding.Bindings;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListCell;

/**
 * Shared JavaFX projection of algorithm identifiers. ComboBox items are stable ids, never
 * translated names or positional indexes. Modules still own their algorithm/variant policies.
 */
final class AlgorithmSelectorBinder {
    private final ComboBox<String> selector;
    private final Supplier<List<String>> availableIds;
    private final Consumer<String> onSelectionChanged;
    private boolean refreshing;

    AlgorithmSelectorBinder(ComboBox<String> selector, Supplier<List<String>> availableIds,
            Consumer<String> onSelectionChanged) {
        this.selector = Objects.requireNonNull(selector, "selector");
        this.availableIds = Objects.requireNonNull(availableIds, "availableIds");
        this.onSelectionChanged = Objects.requireNonNull(onSelectionChanged, "onSelectionChanged");

        selector.setCellFactory(ignored -> localizedCell());
        selector.setButtonCell(localizedCell());
        selector.getSelectionModel().selectedItemProperty().addListener((observable, previous, current) -> {
            if (!refreshing && !Objects.equals(previous, current)) {
                onSelectionChanged.accept(current);
            }
        });
    }

    /** Rebuilds the choices after a type or structure-variant change; retains valid ids. */
    void refresh() {
        List<String> ids = List.copyOf(Objects.requireNonNull(availableIds.get(), "algorithm ids"));
        String previous = selectedId();
        String next = preferredId(ids, previous);
        refreshing = true;
        try {
            selector.getItems().setAll(ids);
            if (next == null) {
                selector.getSelectionModel().clearSelection();
            } else {
                selector.getSelectionModel().select(next);
            }
        } finally {
            refreshing = false;
        }
        if (!Objects.equals(previous, next)) {
            onSelectionChanged.accept(next);
        }
    }

    boolean select(String id) {
        if (id == null || !selector.getItems().contains(id)) return false;
        selector.getSelectionModel().select(id);
        return true;
    }

    String selectedId() {
        return selector.getSelectionModel().getSelectedItem();
    }

    static String preferredId(List<String> ids, String previous) {
        if (ids.contains(previous)) return previous;
        if (ids.isEmpty()) return null;
        return ids.getFirst();
    }

    private static ListCell<String> localizedCell() {
        return new ListCell<>() {
            @Override
            protected void updateItem(String id, boolean empty) {
                super.updateItem(id, empty);
                textProperty().unbind();
                if (empty || id == null) {
                    setText(null);
                    return;
                }
                textProperty().bind(Bindings.createStringBinding(
                        () -> AlgorithmCatalog.name(id), I18N.localeProperty()));
            }
        };
    }
}
