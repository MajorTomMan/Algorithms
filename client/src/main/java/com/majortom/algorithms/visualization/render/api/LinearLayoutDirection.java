package com.majortom.algorithms.visualization.render.api;

import java.util.Locale;

/** Stable direction values used by the linear-layout metadata protocol. */
public enum LinearLayoutDirection {
  RIGHT(true, false),
  DOWN(false, false),
  LEFT(true, true),
  UP(false, true);

  private final boolean horizontal;
  private final boolean reverse;

  LinearLayoutDirection(boolean horizontal, boolean reverse) {
    this.horizontal = horizontal;
    this.reverse = reverse;
  }

  public boolean horizontal() {
    return horizontal;
  }

  public boolean reverse() {
    return reverse;
  }

  public static LinearLayoutDirection parse(String raw) {
    if (raw == null) return RIGHT;
    try {
      return valueOf(raw.toUpperCase(Locale.ROOT));
    } catch (IllegalArgumentException ignored) {
      return RIGHT;
    }
  }
}
