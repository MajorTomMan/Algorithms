package com.majortom.algorithms.visualization.settings;

/**
 * Application-level text display preference.
 *
 * <p>Chinese and Latin text keep independent font families while sharing the
 * same size/color policy. Empty family means the JavaFX/system default; empty color
 * keeps the project default.</p>
 */
public record FontSettings(
        String chineseFamily,
        String englishFamily,
        double size,
        String color) {

    public FontSettings {
        if (chineseFamily == null) {
            chineseFamily = "";
        }
        if (englishFamily == null) {
            englishFamily = "";
        }
        if (color == null) {
            color = "";
        }
    }

}
