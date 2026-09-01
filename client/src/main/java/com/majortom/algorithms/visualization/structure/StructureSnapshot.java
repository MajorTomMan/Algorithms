package com.majortom.algorithms.visualization.structure;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * An immutable, client-local copy of a structure that can be restored later in
 * the same desktop session.
 *
 * <p>The state is deliberately the module's immutable view state.  It keeps
 * the snapshot independent from a controller's mutable editing fields while
 * avoiding a second serialization format for the JavaFX client.</p>
 *
 * @param id stable identity within the in-memory snapshot store
 * @param moduleId module that owns the state
 * @param createdAt creation time used by the snapshot card
 * @param state immutable structure state
 * @param <S> module view-state type
 */
public record StructureSnapshot<S>(
        String id,
        String moduleId,
        Instant createdAt,
        S state) {

    public StructureSnapshot {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("snapshot id must not be blank");
        }
        if (moduleId == null || moduleId.isBlank()) {
            throw new IllegalArgumentException("snapshot moduleId must not be blank");
        }
        createdAt = Objects.requireNonNull(createdAt, "createdAt");
        state = Objects.requireNonNull(state, "state");
    }

    /** Creates a new session-local snapshot with a unique identity. */
    public static <S> StructureSnapshot<S> create(String moduleId, S state) {
        return new StructureSnapshot<>(UUID.randomUUID().toString(), moduleId, Instant.now(), state);
    }
}
