package com.majortom.algorithms.core.snapshot;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record StructureSnapshot<S>(String id, String moduleId, Instant createdAt, S state) {

    public StructureSnapshot {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("snapshot id must not be blank");
        }
        if (moduleId == null || moduleId.isBlank()) {
            throw new IllegalArgumentException("snapshot moduleId must not be blank");
        }
        createdAt = Objects.requireNonNull(createdAt);
        state = Objects.requireNonNull(state);
    }

    public static <S> StructureSnapshot<S> create(String moduleId, S state) {
        return new StructureSnapshot<>(UUID.randomUUID().toString(), moduleId, Instant.now(), state);
    }
}
