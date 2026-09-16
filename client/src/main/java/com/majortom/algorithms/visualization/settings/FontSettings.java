package com.majortom.algorithms.visualization.settings;

/**
 * Application-level text display preference.
 *
 * <p>Chinese and Latin text use independent font families while sharing the
 * same size and color policy. Empty family/color means project default.</p>
 */
public record FontSettings(String chineseFamily, String englishFamily, double size, String color) {
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
