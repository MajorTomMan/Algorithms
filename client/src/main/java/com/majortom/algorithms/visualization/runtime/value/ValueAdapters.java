package com.majortom.algorithms.visualization.runtime.value;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.function.Function;

/** Supported scalar input types at the Workbench boundary. Annotation discovery stays in the core registry. */
public final class ValueAdapters {
  private static final Map<Class<?>, ValueAdapter<?>> BY_TYPE = adapters();

  private ValueAdapters() {}

  public static List<Class<?>> supportedTypes() {
    return List.copyOf(BY_TYPE.keySet());
  }

  public static List<String> supportedTypeNames() {
    return BY_TYPE.keySet().stream().map(Class::getSimpleName).toList();
  }

  public static boolean supports(Class<?> type) {
    return BY_TYPE.containsKey(type);
  }

  public static Class<?> requireType(String name) {
    Objects.requireNonNull(name, "name");
    return BY_TYPE.keySet().stream()
        .filter(type -> type.getSimpleName().equals(name) || type.getName().equals(name))
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException("Unsupported Workbench value type: " + name));
  }

  @SuppressWarnings("unchecked")
  public static <T> ValueAdapter<T> require(Class<T> type) {
    Objects.requireNonNull(type, "type");
    ValueAdapter<?> adapter = BY_TYPE.get(type);
    if (adapter == null) {
      throw new IllegalArgumentException("Unsupported Workbench value type: " + type.getName());
    }
    return (ValueAdapter<T>) adapter;
  }

  @SuppressWarnings("unchecked")
  public static ValueAdapter<Object> requireObjectAdapter(Class<?> type) {
    return (ValueAdapter<Object>) require((Class<Object>) type);
  }

  /** One place for demo/random input creation; controllers must not guess a value's runtime type. */
  public static Object randomValue(Class<?> type, Random random) {
    Objects.requireNonNull(random, "random");
    if (type == Boolean.class) return random.nextBoolean();
    if (type == Character.class) return (char) ('A' + random.nextInt(26));
    if (type == String.class) return "V" + (random.nextInt(900) + 100);
    int number = random.nextInt(100) + 1;
    if (type == Byte.class) return (byte) number;
    if (type == Short.class) return (short) number;
    if (type == Integer.class) return number;
    if (type == Long.class) return (long) number;
    if (type == Float.class) return (float) number;
    if (type == Double.class) return (double) number;
    throw new IllegalArgumentException("No sample generator for " + type.getName());
  }

  /** Deterministic distinct values for structures that use values as vertex/node identities. */
  public static Object distinctValue(Class<?> type, int index) {
    if (index < 0 || index >= maxDistinctSamples(type)) {
      throw new IllegalArgumentException("Too many distinct values for " + type.getName());
    }
    if (type == Boolean.class) return index == 1;
    if (type == Character.class) return (char) ('A' + index);
    if (type == String.class) return "N" + index;
    if (type == Byte.class) return (byte) index;
    if (type == Short.class) return (short) index;
    if (type == Integer.class) return index;
    if (type == Long.class) return (long) index;
    if (type == Float.class) return (float) index;
    if (type == Double.class) return (double) index;
    throw new IllegalArgumentException("No distinct value generator for " + type.getName());
  }

  public static int maxDistinctSamples(Class<?> type) {
    if (type == Boolean.class) return 2;
    if (type == Character.class) return 26;
    if (type == Byte.class) return 128;
    requireObjectAdapter(type);
    return Integer.MAX_VALUE;
  }

  private static Map<Class<?>, ValueAdapter<?>> adapters() {
    LinkedHashMap<Class<?>, ValueAdapter<?>> adapters = new LinkedHashMap<>();
    register(adapters, scalar(Integer.class, Integer::valueOf));
    register(adapters, scalar(String.class, Function.identity()));
    register(adapters, scalar(Byte.class, Byte::valueOf));
    register(adapters, scalar(Short.class, Short::valueOf));
    register(adapters, scalar(Long.class, Long::valueOf));
    register(adapters, scalar(Float.class, text -> {
      float value = Float.parseFloat(text);
      if (!Float.isFinite(value)) throw new NumberFormatException("Non-finite float: " + text);
      return value;
    }));
    register(adapters, scalar(Double.class, text -> {
      double value = Double.parseDouble(text);
      if (!Double.isFinite(value)) throw new NumberFormatException("Non-finite double: " + text);
      return value;
    }));
    register(adapters, scalar(Boolean.class, text -> {
      if ("true".equalsIgnoreCase(text)) return true;
      if ("false".equalsIgnoreCase(text)) return false;
      throw new IllegalArgumentException("Expected true or false: " + text);
    }));
    register(adapters, scalar(Character.class, text -> {
      if (text.length() != 1) throw new IllegalArgumentException("Expected one character: " + text);
      return text.charAt(0);
    }));
    return Collections.unmodifiableMap(adapters);
  }

  private static <T> ValueAdapter<T> scalar(Class<T> type, Function<String, T> parser) {
    return new ValueAdapter<>() {
      @Override public Class<T> type() { return type; }
      @Override public T parse(String text) { return parser.apply(Objects.requireNonNull(text, "text").trim()); }
      @Override public String format(T value) { return Objects.requireNonNull(value, "value").toString(); }
    };
  }

  private static <T> void register(Map<Class<?>, ValueAdapter<?>> adapters, ValueAdapter<T> adapter) {
    adapters.put(adapter.type(), adapter);
  }
}
