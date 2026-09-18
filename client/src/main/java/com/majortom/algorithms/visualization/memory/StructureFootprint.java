package com.majortom.algorithms.visualization.memory;

import java.util.Objects;

/** On-demand deep object-graph footprint for the current editable structure. */
public record StructureFootprint(
        boolean available,
        String rootType,
        long totalBytes,
        long objectCount,
        String provider,
        String detail) {

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
