package com.majortom.algorithms.visualization.settings;

/** Application-level text display preference. Empty family/color means project default. */
public record FontSettings(String family, double size, String color) {

    public FontSettings {
        if (family == null) {
            family = "";
        }
        if (color == null) {
            color = "";
        }
    }
}
