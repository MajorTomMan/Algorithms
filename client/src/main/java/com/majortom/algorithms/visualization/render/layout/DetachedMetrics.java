package com.majortom.algorithms.visualization.render.layout;

import com.majortom.algorithms.visualization.render.api.ContentStyleSnapshot;
import java.util.Objects;

/**
 * JavaFX-neutral geometry estimates used by capture. The values intentionally describe only
 * factual content geometry; CSS decoration is applied later during the authoritative FX commit.
 */
public final class DetachedMetrics {
  private DetachedMetrics() {}

  public static double textWidth(String text, ContentStyleSnapshot style) {
    Objects.requireNonNull(style, "style");
    if (text == null || text.isEmpty())
      return style.fontSize() * 0.6d;
    double units = 0.0d;
    for (int offset = 0; offset < text.length();) {
      int codePoint = text.codePointAt(offset);
      offset += Character.charCount(codePoint);
      if (Character.isWhitespace(codePoint))
        units += 0.35d;
      else if (isWide(codePoint))
        units += 1.0d;
      else if (Character.isUpperCase(codePoint))
        units += 0.68d;
      else if (Character.isDigit(codePoint))
        units += 0.62d;
      else
        units += 0.58d;
    }
    return Math.ceil(Math.max(1.0d, units * style.fontSize()));
  }

  public static double boxWidth(
      String text, ContentStyleSnapshot style, double minimum, double horizontalPadding) {
    return Math.max(minimum, Math.ceil(textWidth(text, style) + horizontalPadding));
  }

  private static boolean isWide(int codePoint) {
    Character.UnicodeScript script = Character.UnicodeScript.of(codePoint);
    return script == Character.UnicodeScript.HAN || script == Character.UnicodeScript.HIRAGANA
        || script == Character.UnicodeScript.KATAKANA || script == Character.UnicodeScript.HANGUL;
  }
}
