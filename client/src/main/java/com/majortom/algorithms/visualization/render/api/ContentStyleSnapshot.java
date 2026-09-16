package com.majortom.algorithms.visualization.render.api;

/**
 * Geometry-relevant content style only. UI chrome color and decoration are intentionally excluded.
 */
public record ContentStyleSnapshot(double fontSize, String chineseFamily, String englishFamily) {
    public ContentStyleSnapshot {
        if (!Double.isFinite(fontSize) || fontSize <= 0.0d)
            throw new IllegalArgumentException("fontSize must be positive");
        chineseFamily = chineseFamily == null ? "" : chineseFamily;
        englishFamily = englishFamily == null ? "" : englishFamily;
    }
}
