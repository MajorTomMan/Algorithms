package com.majortom.algorithms.visualization.render.api;

import java.util.Objects;

/** JavaFX-neutral topology edge used by layout engines. */
public record LayoutLink(String id, String sourceId, String targetId, String relation, int order) {
    public LayoutLink {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(sourceId, "sourceId");
        Objects.requireNonNull(targetId, "targetId");
        relation = relation == null ? "" : relation;
    }

    public LayoutLink(String id, String sourceId, String targetId) {
        this(id, sourceId, targetId, "", 0);
    }
}
