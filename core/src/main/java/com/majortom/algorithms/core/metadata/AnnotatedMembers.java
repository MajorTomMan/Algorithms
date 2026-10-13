package com.majortom.algorithms.core.metadata;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Cached reflection metadata for explicitly annotated instance properties.
 * Record component annotations may be propagated to the backing field and accessor;
 * all such declarations describe one logical property.
 */
public final class AnnotatedMembers {
  private AnnotatedMembers() {}

  private static final ClassValue<Map<Class<? extends Annotation>, List<Accessor>>> CACHE =
      new ClassValue<>() {
        @Override protected Map<Class<? extends Annotation>, List<Accessor>> computeValue(Class<?> type) {
          return new ConcurrentHashMap<>();
        }
      };

  public static List<Accessor> marked(Class<?> type, Class<? extends Annotation> marker) {
    Objects.requireNonNull(type, "type");
    Objects.requireNonNull(marker, "marker");
    return CACHE.get(type).computeIfAbsent(marker, ignored -> scan(type, marker));
  }

  public static Accessor one(Class<?> type, Class<? extends Annotation> marker, boolean required) {
    List<Accessor> found = marked(type, marker);
    if (found.size() > 1 || (required && found.isEmpty()))
      throw new IllegalArgumentException("Expected " + (required ? "exactly one" : "at most one")
          + " @" + marker.getSimpleName() + " on " + type.getName());
    return found.isEmpty() ? null : found.getFirst();
  }

  public record Accessor(String name, Field field, Method method) {
    public Accessor {
      Objects.requireNonNull(name, "name");
      if ((field == null) == (method == null))
        throw new IllegalArgumentException("Accessor must contain one reflected member");
    }
    public <A extends Annotation> A annotation(Class<A> marker) {
      A result = field != null ? field.getAnnotation(marker) : method.getAnnotation(marker);
      if (result == null) throw new IllegalArgumentException("Missing @" + marker.getSimpleName());
      return result;
    }
    public Object read(Object receiver) {
      Objects.requireNonNull(receiver, "receiver");
      try {
        if (field != null) {
          if (!field.trySetAccessible())
            throw new IllegalArgumentException("Inaccessible annotated field: " + field);
          return field.get(receiver);
        }
        if (!method.trySetAccessible())
          throw new IllegalArgumentException("Inaccessible annotated method: " + method);
        return method.invoke(receiver);
      } catch (InvocationTargetException exception) {
        throw new IllegalArgumentException("Annotated accessor failed: " + name, exception.getCause());
      } catch (ReflectiveOperationException | SecurityException exception) {
        throw new IllegalArgumentException("Cannot access annotated member: " + name, exception);
      }
    }
  }

  private static List<Accessor> scan(Class<?> type, Class<? extends Annotation> marker) {
    Map<String, Accessor> found = new LinkedHashMap<>();
    for (Class<?> current = type; current != null && current != Object.class;
        current = current.getSuperclass()) {
      for (Field field : current.getDeclaredFields()) {
        if (!field.isAnnotationPresent(marker)) continue;
        if (field.isSynthetic() || Modifier.isStatic(field.getModifiers()))
          throw new IllegalArgumentException("Annotated field must be an instance field: " + field);
        found.putIfAbsent(field.getName(), new Accessor(field.getName(), field, null));
      }
      for (Method method : current.getDeclaredMethods()) {
        if (!method.isAnnotationPresent(marker)) continue;
        if (method.isSynthetic() || method.isBridge() || Modifier.isStatic(method.getModifiers())
            || method.getParameterCount() != 0 || method.getReturnType() == void.class)
          throw new IllegalArgumentException("Annotated method must be a non-static zero-argument accessor: "
              + method);
        String name = propertyName(method.getName());
        found.putIfAbsent(name, new Accessor(name, null, method));
      }
    }
    return List.copyOf(found.values());
  }

  private static String propertyName(String name) {
    if (name.startsWith("get") && name.length() > 3)
      return Character.toLowerCase(name.charAt(3)) + name.substring(4);
    if (name.startsWith("is") && name.length() > 2)
      return Character.toLowerCase(name.charAt(2)) + name.substring(3);
    return name;
  }
}
