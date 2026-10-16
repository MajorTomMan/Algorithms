package com.majortom.algorithms.core.metadata;

import com.majortom.algorithms.core.annotation.ValueDisplay;
import com.majortom.algorithms.core.annotation.ValueField;
import com.majortom.algorithms.core.annotation.ValueId;
import com.majortom.algorithms.core.annotation.ValueType;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * @ValueType、@ValueField、@ValueId 的唯一元数据解释层。
 *
 * <p>使用 ClassValue 缓存每个实体的不可变描述符，避免每次渲染或映射重复反射。
 * Descriptor 同时提供工作台显示名称、适用结构、不可变承诺、业务 ID 和字段视图。
 * UI 与 GraphObjectMapper 共享相同的属性含义，但 core 不依赖它们。
 *
 * <p>@ValueId 是“类 + 业务键”关联，不等于 Graph 顶点 long ID。
 * @ValueField 的 SUMMARY 和 DETAIL 分开读取，避免节点每次刷新执行昂贵 getter。
 * @ValueType.immutable 是开发者对完整可达对象图的承诺，不做深度冻结。
 */
public final class ValueMetadata {
  private ValueMetadata() {}

  private static final ClassValue<Descriptor> TYPES = new ClassValue<>() {
    @Override protected Descriptor computeValue(Class<?> type) { return scan(type); }
  };

  public static Descriptor describe(Class<?> type) {
    return TYPES.get(Objects.requireNonNull(type, "type"));
  }

  public record FieldDescriptor(String key, String label, int order,
      boolean summary, boolean detail, AnnotatedMembers.Accessor accessor) {
    public Object read(Object value) { return accessor.read(value); }
  }

  public record Descriptor(Class<?> type, String id, String name, boolean declared,
      boolean immutable, List<Class<?>> structures,
      AnnotatedMembers.Accessor businessId, List<FieldDescriptor> fields) {

    public boolean supports(Class<?> structureContract) {
      Objects.requireNonNull(structureContract, "structureContract");
      if (structures.isEmpty()) return true;
      return structures.stream().anyMatch(candidate -> candidate.isAssignableFrom(structureContract));
    }

    /** Null means no business ID was declared, not a missing topology vertex. */
    public Object businessIdOf(Object value) {
      if (!type.isInstance(value)) throw new IllegalArgumentException("Mismatched value type");
      if (businessId == null) return null;
      Object result = businessId.read(value);
      if (result == null) throw new IllegalArgumentException("Null @ValueId on " + type.getName());
      return result;
    }
  }

  private static Descriptor scan(Class<?> type) {
    ValueType annotation = type.getAnnotation(ValueType.class);
    String id = annotation == null || annotation.id().isBlank()
        ? type.getName() : annotation.id().trim();
    String name = annotation == null || annotation.name().isBlank()
        ? type.getSimpleName() : annotation.name().trim();
    boolean immutable = annotation != null && annotation.immutable();
    List<Class<?>> structures = annotation == null ? List.of() : List.of(annotation.value());
    AnnotatedMembers.Accessor businessId = AnnotatedMembers.one(type, ValueId.class, false);
    List<FieldDescriptor> fields = AnnotatedMembers.marked(type, ValueField.class).stream()
        .map(property -> {
          ValueField field = property.annotation(ValueField.class);
          boolean summary = Arrays.asList(field.value()).contains(ValueDisplay.SUMMARY);
          boolean detail = Arrays.asList(field.value()).contains(ValueDisplay.DETAIL);
          if (!summary && !detail)
            throw new IllegalArgumentException("Empty @ValueField display mode on " + property.name());
          return new FieldDescriptor(property.name(),
              field.label().isBlank() ? property.name() : field.label(),
              field.order(), summary, detail, property);
        })
        .sorted(Comparator.comparingInt(FieldDescriptor::order).thenComparing(FieldDescriptor::key))
        .toList();
    return new Descriptor(type, id, name, annotation != null, immutable,
        structures, businessId, fields);
  }
}
