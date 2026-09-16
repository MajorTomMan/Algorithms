package com.majortom.algorithms.visualization.module;

import java.util.Objects;

/** Client-side navigation metadata. It deliberately stays out of the domain registry. */
public record FamilyNavigationMetadata(int order, String glyph) {
    public FamilyNavigationMetadata {
        Objects.requireNonNull(glyph, "glyph");
        if (glyph.isBlank()) {
            glyph = "·";
        }
    }
}
