package com.majortom.algorithms.visualization.render.api;

import java.util.Objects;

public record RenderSessionId(String value) {
  public RenderSessionId {
    value = Objects.requireNonNull(value, "value").trim();
    if (value.isEmpty())
      throw new IllegalArgumentException("session id must not be blank");
  }
  public static RenderSessionId of(String value) {
    return new RenderSessionId(value);
  }
  @Override
  public String toString() {
    return value;
  }
}
