package com.majortom.algorithms.visualization.common.layout;

import java.util.Map;
import java.util.Objects;

/** Immutable presentation geometry; it contains no structure-family semantics. */
public record LayoutResult(Map<String, ElementBounds> elements, Map<String, EdgeRoute> edges) {
    public LayoutResult {
        elements = Map.copyOf(Objects.requireNonNull(elements, "elements"));
        edges = Map.copyOf(Objects.requireNonNull(edges, "edges"));
    }
}
