package com.majortom.algorithms.core.snapshot;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** UI-neutral saved Structure state with optional runtime ValueType metadata. */
public record StructureSnapshot<S>(
        String id,
        String moduleId,
        Instant createdAt,
        String valueTypeName,
        S state) {

    public StructureSnapshot {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("snapshot id must not be blank");
        }
        if (moduleId == null || moduleId.isBlank()) {
            throw new IllegalArgumentException("snapshot moduleId must not be blank");
        }
        createdAt = Objects.requireNonNull(createdAt, "createdAt");
        if (valueTypeName != null && valueTypeName.isBlank()) {
            throw new IllegalArgumentException("snapshot valueTypeName must not be blank");
        }
        state = Objects.requireNonNull(state, "state");
    }

    /** Compatibility constructor for domain snapshots, such as Maze, that intentionally have no ValueType. */
    public StructureSnapshot(String id, String moduleId, Instant createdAt, S state) {
        this(id, moduleId, createdAt, null, state);
    }

    public static <S> StructureSnapshot<S> create(String moduleId, S state) {
        return new StructureSnapshot<>(UUID.randomUUID().toString(), moduleId, Instant.now(), null, state);
    }

    public static <S> StructureSnapshot<S> create(String moduleId, Class<?> valueType, S state) {
        Objects.requireNonNull(valueType, "valueType");
        return new StructureSnapshot<>(
                UUID.randomUUID().toString(), moduleId, Instant.now(), valueType.getName(), state);
    }

    public boolean hasValueType() {
        return valueTypeName != null;
    }

    public boolean matchesValueType(Class<?> valueType) {
        Objects.requireNonNull(valueType, "valueType");
        return valueType.getName().equals(valueTypeName);
    }

    public void requireValueType(Class<?> valueType) {
        Objects.requireNonNull(valueType, "valueType");
        if (!matchesValueType(valueType)) {
            throw new IllegalArgumentException("snapshot value type mismatch: expected " + valueType.getName()
                    + ", actual " + (valueTypeName == null ? "<none>" : valueTypeName));
        }
    }
}
