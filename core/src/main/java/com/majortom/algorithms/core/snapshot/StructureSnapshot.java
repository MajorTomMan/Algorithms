package com.majortom.algorithms.core.snapshot;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * 跨工作台保存、预览与重放领域结构状态的通用快照信封。
 *
 * <p>封装模块 ID、创建时间、可选的元素类全限定名和真实领域 state。
 * {@link #requireValueType(Class)} 使用 Java 类型名而非 @ValueType.name
 * 来保护恢复时的类型安全，防止把 City 数据载入 Integer 图。
 *
 * <p>Record 自身不复制 state 内部任意可变对象；模块需要提供可安全重用的结构快照。
 * UI 通过对应 Controller 选择 live 或保存的输入，运行时并不持有 JavaFX 节点。
 */
public record StructureSnapshot<S>(
    String id, String moduleId, Instant createdAt, String valueTypeName, S state) {
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

  /**
   * Compatibility constructor for domain snapshots, such as Maze, that intentionally have no
   * ValueType.
   */
  public StructureSnapshot(String id, String moduleId, Instant createdAt, S state) {
    this(id, moduleId, createdAt, null, state);
  }

  public static <S> StructureSnapshot<S> create(String moduleId, S state) {
    return new StructureSnapshot<>(
        UUID.randomUUID().toString(), moduleId, Instant.now(), null, state);
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
      throw new IllegalArgumentException("snapshot value type mismatch: expected "
          + valueType.getName() + ", actual " + (valueTypeName == null ? "<none>" : valueTypeName));
    }
  }
}
