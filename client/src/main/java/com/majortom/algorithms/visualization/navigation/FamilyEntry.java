package com.majortom.algorithms.visualization.navigation;

import javafx.beans.value.ObservableStringValue;

import java.util.Objects;

/**
 * Immutable navigation description. Visual geometry and numbering are owned by {@link
 * FamilyNavigator}.
 */
public record FamilyEntry(
        String id, String glyph, ObservableStringValue name, boolean disabled, Runnable action) {

    public FamilyEntry {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(glyph, "glyph");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(action, "action");
    }
}
