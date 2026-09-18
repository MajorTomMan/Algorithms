package com.majortom.algorithms.telemetry.memory.analysis;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

/** On-demand object-graph footprint for a caller-supplied domain root. */
public record StructureFootprint(
    boolean available,
    String rootType,
    long totalBytes,
    long objectCount,
    String provider,
    String detail) implements Serializable {
  @Serial private static final long serialVersionUID = 1L;

  public StructureFootprint {
    rootType = Objects.requireNonNullElse(rootType, "");
    totalBytes = Math.max(0L, totalBytes);
    objectCount = Math.max(0L, objectCount);
    provider = Objects.requireNonNullElse(provider, "");
    detail = Objects.requireNonNullElse(detail, "");
  }

  public static StructureFootprint unavailable(String detail) {
    return new StructureFootprint(false, "", 0L, 0L, "JOL", detail);
  }
}
