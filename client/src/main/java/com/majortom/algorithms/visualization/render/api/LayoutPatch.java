package com.majortom.algorithms.visualization.render.api;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public record LayoutPatch(
        long modelRevision,
        Map<String, ElementGeometry> elements,
        List<EdgeGeometry> edges,
        BoundsSnapshot primaryContentBounds) {
    public LayoutPatch {
        elements = Map.copyOf(Objects.requireNonNull(elements, "elements"));
        edges = List.copyOf(Objects.requireNonNull(edges, "edges"));
        Objects.requireNonNull(primaryContentBounds, "primaryContentBounds");
    }

    public static LayoutPatch from(LayoutResult result) {
        return new LayoutPatch(
                result.modelRevision(), result.elements(), result.edges(), result.bounds());
    }
}
