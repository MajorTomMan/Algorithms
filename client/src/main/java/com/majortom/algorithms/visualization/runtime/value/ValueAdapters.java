package com.majortom.algorithms.visualization.runtime.value;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.function.Function;

/** Single Workbench registry for typed input, optional examples, and explicit field presentation. */
public final class ValueAdapters {
  private static final Map<Class<?>, Entry<?>> ENTRIES = new LinkedHashMap<>();
  // Explicit contract: the developer guarantees the entire reachable value graph is immutable.
  // A record containing a mutable List does NOT automatically meet this contract.
  private static final Set<Class<?>> DECLARED_IMMUTABLE = new HashSet<>();
  static { registerBuiltIns(); }
  private ValueAdapters() {}

  private record Entry<T>(ValueAdapter<T> adapter, SampleGenerator<T> generator,
      DistinctSampleProvider<T> distinct) {}

  /** Register text input and optional sample generation; display is declared on the value type. */
  public static synchronized <T> void register(ValueAdapter<T> adapter) {
    register(adapter, null, null);
  }

  public static synchronized <T> void register(ValueAdapter<T> adapter,
      SampleGenerator<T> generator, DistinctSampleProvider<T> distinct) {
    Objects.requireNonNull(adapter, "adapter");
    Class<T> type = Objects.requireNonNull(adapter.type(), "adapter.type");
    if (type.isPrimitive() || type == Void.class || type.isArray())
      throw new IllegalArgumentException("Unsupported element type: " + type.getName());
    if (ENTRIES.containsKey(type))
      throw new IllegalArgumentException("Duplicate type: " + type.getName());
    if (distinct != null && distinct.capacity() < 0)
      throw new IllegalArgumentException("Negative distinct capacity: " + type.getName());
    ENTRIES.put(type, new Entry<>(adapter, generator, distinct));
  }

  public static synchronized List<Class<?>> supportedTypes() { return List.copyOf(ENTRIES.keySet()); }

  /** Display identifiers are disambiguated; type identity in the registry remains Class<?>. */
  public static synchronized List<String> supportedTypeNames() {
    return ENTRIES.keySet().stream().map(ValueAdapters::typeName).toList();
  }

  public static synchronized String typeName(Class<?> type) {
    long matches = ENTRIES.keySet().stream().filter(t -> t.getSimpleName().equals(type.getSimpleName())).count();
    return matches <= 1 ? type.getSimpleName() : type.getName();
  }

  public static synchronized boolean supports(Class<?> type) { return ENTRIES.containsKey(type); }

  /** Stage-A replay safety. Arbitrary mutable objects need an explicit freezing strategy later. */
  public static synchronized void declareImmutable(Class<?> type) {
    Objects.requireNonNull(type, "type");
    if (!supports(type)) throw new IllegalArgumentException("Register the input adapter first: " + type.getName());
    DECLARED_IMMUTABLE.add(type);
  }

  public static synchronized boolean canReplay(Class<?> type) {
    return supports(type) && (isBuiltinScalar(type) || type.isEnum() || DECLARED_IMMUTABLE.contains(type));
  }

  private static boolean isBuiltinScalar(Class<?> type) {
    return type == String.class || type == Byte.class || type == Short.class
        || type == Integer.class || type == Long.class || type == Float.class
        || type == Double.class || type == Boolean.class || type == Character.class;
  }

  public static void requireReplayable(Class<?> type) {
    if (!canReplay(type)) throw new IllegalArgumentException(
        "Replay requires an explicitly immutable value type: " + type.getName());
  }
  public static synchronized boolean canGenerate(Class<?> type) {
    Entry<?> entry = ENTRIES.get(type);
    return entry != null && entry.generator() != null;
  }
  public static synchronized boolean canGenerateDistinct(Class<?> type, int count) {
    Entry<?> entry = ENTRIES.get(type);
    return count >= 0 && entry != null && entry.distinct() != null && count <= entry.distinct().capacity();
  }

  public static synchronized Class<?> requireType(String name) {
    Objects.requireNonNull(name, "name");
    for (Class<?> type : ENTRIES.keySet()) if (type.getName().equals(name)) return type;
    List<Class<?>> matches = ENTRIES.keySet().stream().filter(t -> t.getSimpleName().equals(name)).toList();
    if (matches.size() == 1) return matches.getFirst();
    throw new IllegalArgumentException((matches.isEmpty() ? "Unsupported" : "Ambiguous") + " Workbench value type: " + name);
  }

  @SuppressWarnings("unchecked")
  private static synchronized <T> Entry<T> entry(Class<T> type) {
    Entry<?> result = ENTRIES.get(Objects.requireNonNull(type, "type"));
    if (result == null) throw new IllegalArgumentException("Unsupported Workbench value type: " + type.getName());
    return (Entry<T>) result;
  }
  public static <T> ValueAdapter<T> require(Class<T> type) { return entry(type).adapter(); }
  @SuppressWarnings("unchecked")
  public static ValueAdapter<Object> requireObjectAdapter(Class<?> type) { return require((Class<Object>) type); }
  @SuppressWarnings("unchecked")
  private static Entry<Object> objectEntry(Class<?> type) { return entry((Class<Object>) type); }

  public static Object randomValue(Class<?> type, Random random) {
    SampleGenerator<Object> generator = objectEntry(type).generator();
    if (generator == null) throw new IllegalArgumentException("No sample generator for " + type.getName());
    return Objects.requireNonNull(generator.next(Objects.requireNonNull(random, "random")), "generated value");
  }

  public static Object distinctValue(Class<?> type, int index) {
    DistinctSampleProvider<Object> distinct = objectEntry(type).distinct();
    if (distinct == null) throw new IllegalArgumentException("No distinct sample generator for " + type.getName());
    if (index < 0 || index >= distinct.capacity()) throw new IllegalArgumentException("Too many distinct values for " + type.getName());
    return Objects.requireNonNull(distinct.valueAt(index), "distinct value");
  }
  public static int maxDistinctSamples(Class<?> type) {
    DistinctSampleProvider<Object> distinct = objectEntry(type).distinct();
    return distinct == null ? 0 : distinct.capacity();
  }

  /** Read annotated SUMMARY fields without invoking DETAIL-only accessors. */
  public static String summary(Object value) {
    return AnnotatedValueProjector.summary(value);
  }

  /** Evaluate explicitly declared DETAIL fields only when an inspector requests them. */
  public static ValueProjection project(Object value) {
    return AnnotatedValueProjector.project(value);
  }

  private static void registerBuiltIns() {
    addScalar(Integer.class, Integer::valueOf, random -> random.nextInt(100) + 1, Integer.MAX_VALUE, i -> i);
    addScalar(String.class, Function.identity(), random -> "V" + (random.nextInt(900) + 100), Integer.MAX_VALUE, i -> "N" + i);
    addScalar(Byte.class, Byte::valueOf, random -> (byte) (random.nextInt(100) + 1), 128, i -> (byte) (int) i);
    addScalar(Short.class, Short::valueOf, random -> (short) (random.nextInt(100) + 1), 32768, i -> (short) (int) i);
    addScalar(Long.class, Long::valueOf, random -> (long) (random.nextInt(100) + 1), Integer.MAX_VALUE, i -> (long) i);
    addScalar(Float.class, text -> {
      float result = Float.parseFloat(text);
      if (!Float.isFinite(result)) throw new NumberFormatException("Non-finite float: " + text);
      return result;
    }, random -> (float) (random.nextInt(100) + 1), 16777216, i -> (float) i);
    addScalar(Double.class, text -> {
      double result = Double.parseDouble(text);
      if (!Double.isFinite(result)) throw new NumberFormatException("Non-finite double: " + text);
      return result;
    }, random -> (double) (random.nextInt(100) + 1), Integer.MAX_VALUE, i -> (double) i);
    addScalar(Boolean.class, text -> {
      if ("true".equalsIgnoreCase(text)) return true;
      if ("false".equalsIgnoreCase(text)) return false;
      throw new IllegalArgumentException("Expected true or false: " + text);
    }, random -> random.nextBoolean(), 2, i -> i == 1);
    addScalar(Character.class, text -> {
      if (text.length() != 1) throw new IllegalArgumentException("Expected one character: " + text);
      return text.charAt(0);
    }, random -> (char) ('A' + random.nextInt(26)), 26, i -> (char) ('A' + i));
  }

  private static <T> void addScalar(Class<T> type, Function<String, T> parser,
      SampleGenerator<T> generator, int capacity, Function<Integer, T> distinct) {
    ValueAdapter<T> adapter = new ValueAdapter<>() {
      @Override public Class<T> type() { return type; }
      @Override public T parse(String text) { return parser.apply(Objects.requireNonNull(text, "text").trim()); }
      @Override public String format(T value) { return Objects.requireNonNull(value, "value").toString(); }
    };
    register(adapter, generator, new DistinctSampleProvider<>() {
      @Override public int capacity() { return capacity; }
      @Override public T valueAt(int index) { return distinct.apply(index); }
    });
  }
}
