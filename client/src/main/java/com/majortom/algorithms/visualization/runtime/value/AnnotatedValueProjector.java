package com.majortom.algorithms.visualization.runtime.value;

import com.majortom.algorithms.core.annotation.ValueDisplay;
import com.majortom.algorithms.core.annotation.ValueField;
import com.majortom.algorithms.core.annotation.ValueType;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Lazy annotation-based presentation without requiring a text input adapter.
 * Intentionally never follows references to nested objects or discovers graph edges.
 */
final class AnnotatedValueProjector {
  private static final Map<Class<?>, List<Accessor>> CACHE = new ConcurrentHashMap<>();
  private AnnotatedValueProjector() {}

  static ValueProjection project(Object value) {
    Class<?> type = value.getClass();
    List<Accessor> accessors = CACHE.computeIfAbsent(type, AnnotatedValueProjector::scan);
    if (accessors.isEmpty()) {
      return new ValueProjection(typeLabel(type), List.of(), false);
    }
    List<FieldView> details = new ArrayList<>();
    List<String> summary = new ArrayList<>();
    for (Accessor accessor : accessors) {
      String label = accessor.label();
      String formatted;
      boolean error = false;
      try { formatted = displayValue(accessor.read(value)); }
      catch (RuntimeException failure) { formatted = "<error>"; error = true; }
      boolean outside = accessor.summary();
      if (outside) summary.add(formatted);
      if (accessor.detail()) {
        details.add(new FieldView(accessor.key(), label, formatted, outside, error));
      }
    }
    String title = summary.isEmpty() ? typeLabel(type) : String.join(" · ", summary);
    if (title.length() > 72) title = title.substring(0, 71) + "…";
    return new ValueProjection(title, details, true);
  }

  private static String typeLabel(Class<?> type) {
    ValueType annotation = type.getAnnotation(ValueType.class);
    return annotation != null && !annotation.name().isBlank()
        ? annotation.name() : type.getSimpleName();
  }

  private static String displayValue(Object value) {
    if (value == null) return "null";
    Class<?> type = value.getClass();
    if (value instanceof CharSequence || value instanceof Number || value instanceof Boolean
        || value instanceof Character || value instanceof Enum<?>)
      return String.valueOf(value);
    // Avoid invoking arbitrary recursive toString() implementations for object graphs.
    return type.getSimpleName();
  }

  private record Accessor(String key, String label, int order, boolean summary, boolean detail,
                          Field field, Method method) {
    Object read(Object value) {
      try {
        if (field != null) {
          if (!field.trySetAccessible()) throw new IllegalArgumentException("inaccessible field: " + field);
          return field.get(value);
        }
        if (!method.trySetAccessible()) throw new IllegalArgumentException("inaccessible method: " + method);
        return method.invoke(value);
      } catch (ReflectiveOperationException exception) {
        throw new IllegalArgumentException("Cannot read annotated value field " + key, exception);
      }
    }
  }

  private static List<Accessor> scan(Class<?> type) {
    Map<String, Accessor> properties = new LinkedHashMap<>();
    for (Class<?> cursor = type; cursor != null && cursor != Object.class; cursor = cursor.getSuperclass()) {
      for (Field field : cursor.getDeclaredFields()) {
        ValueField annotation = field.getAnnotation(ValueField.class);
        if (annotation == null) continue;
        if (Modifier.isStatic(field.getModifiers()))
          throw new IllegalArgumentException("@ValueField on static field: " + field);
        String key = field.getName();
        properties.putIfAbsent(key, accessor(key, annotation, field, null));
      }
      for (Method method : cursor.getDeclaredMethods()) {
        ValueField annotation = method.getAnnotation(ValueField.class);
        if (annotation == null) continue;
        if (Modifier.isStatic(method.getModifiers()) || method.getParameterCount() != 0
            || method.getReturnType() == void.class)
          throw new IllegalArgumentException("@ValueField must mark a no-arg accessor: " + method);
        String key = propertyName(method.getName());
        properties.putIfAbsent(key, accessor(key, annotation, null, method));
      }
    }
    return properties.values().stream()
        .sorted(Comparator.comparingInt(Accessor::order).thenComparing(Accessor::key))
        .toList();
  }

  private static Accessor accessor(String key, ValueField annotation, Field field, Method method) {
    boolean summary = Arrays.asList(annotation.value()).contains(ValueDisplay.SUMMARY);
    boolean detail = Arrays.asList(annotation.value()).contains(ValueDisplay.DETAIL);
    if (!summary && !detail)
      throw new IllegalArgumentException("@ValueField needs SUMMARY or DETAIL: " + key);
    return new Accessor(key, annotation.label().isBlank() ? key : annotation.label(),
        annotation.order(), summary, detail, field, method);
  }

  private static String propertyName(String name) {
    if (name.startsWith("get") && name.length() > 3)
      return Character.toLowerCase(name.charAt(3)) + name.substring(4);
    if (name.startsWith("is") && name.length() > 2)
      return Character.toLowerCase(name.charAt(2)) + name.substring(3);
    return name;
  }
}
