package com.majortom.algorithms.visualization.logging;

import java.util.Objects;

/** Stable routing identity for an independently retained workbench log. */
public record LogChannelId(Kind kind, String structureId, String algorithmId) {
  public enum Kind {
    STRUCTURE,
    ALGORITHM,
    SYSTEM
  }

  public LogChannelId {
    Objects.requireNonNull(kind, "kind");
    structureId = normalize(structureId);
    algorithmId = normalize(algorithmId);
    if (kind == Kind.STRUCTURE && structureId == null) {
      throw new IllegalArgumentException("structure log requires structureId");
    }
    if (kind == Kind.ALGORITHM && (structureId == null || algorithmId == null)) {
      throw new IllegalArgumentException("algorithm log requires structureId and algorithmId");
    }
  }

  public static LogChannelId structure(String structureId) {
    return new LogChannelId(Kind.STRUCTURE, structureId, null);
  }

  public static LogChannelId algorithm(String structureId, String algorithmId) {
    return new LogChannelId(Kind.ALGORITHM, structureId, algorithmId);
  }

  public static LogChannelId system() {
    return new LogChannelId(Kind.SYSTEM, null, null);
  }

  private static String normalize(String value) {
    if (value == null) return null;
    String normalized = value.trim();
    return normalized.isEmpty() ? null : normalized;
  }
}
