package com.majortom.algorithms.structure.graph;

import com.majortom.algorithms.core.annotation.*;
import com.majortom.algorithms.core.metadata.GraphDirection;
import com.majortom.algorithms.core.snapshot.MultiGraphSnapshot;
import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.*;

/**
 * Converts an explicitly annotated batch object to graph topology, without inferring any
 * business relationship. Nodes are declared by @GraphNodes and edges by @GraphEdges;
 * endpoint references must resolve to declared nodes (same instance or @ValueId).
 */
final class MultiGraphBatchParser {
  private MultiGraphBatchParser() {}

  static <T> MultiGraph<T> parse(GraphDirection direction, Class<T> nodeType, Object batch) {
    Objects.requireNonNull(direction, "direction");
    Objects.requireNonNull(nodeType, "nodeType");
    Objects.requireNonNull(batch, "batch");
    List<Member> nodeSources = members(batch.getClass(), GraphNodes.class);
    List<Member> edgeSources = members(batch.getClass(), GraphEdges.class);
    if (nodeSources.isEmpty()) throw new IllegalArgumentException(
        "Batch class has no @GraphNodes member: " + batch.getClass().getName());

    List<MultiGraphSnapshot.Vertex<T>> vertices = new ArrayList<>();
    List<MultiGraphSnapshot.Edge> edges = new ArrayList<>();
    IdentityHashMap<Object, Vertex<T>> byReference = new IdentityHashMap<>();
    Map<NodeIdentity, Vertex<T>> byBusinessKey = new HashMap<>();

    // Phase 1: collect *all* declared nodes, including isolated ones and forward-referenced nodes.
    for (Member source : nodeSources) {
      for (Object item : elements(source.read(batch))) {
        if (item == null) throw new IllegalArgumentException("null node in " + source.name());
        if (byReference.containsKey(item)) continue;
        ValueType type = item.getClass().getAnnotation(ValueType.class);
        if (type != null && type.value().length > 0 && Arrays.stream(type.value())
            .noneMatch(contract -> contract.isAssignableFrom(MultiGraphStructure.class)))
          throw new IllegalArgumentException("value type is not enabled for MultiGraph: "
              + item.getClass().getName());
        T value = nodeType.cast(item);
        NodeIdentity key = identity(item);
        if (key != null && byBusinessKey.containsKey(key))
          throw new IllegalArgumentException("duplicate node business identity: " + key);
        Vertex<T> vertex = new Vertex<>(value);
        byReference.put(item, vertex);
        if (key != null) byBusinessKey.put(key, vertex);
        vertices.add(new MultiGraphSnapshot.Vertex<>(vertex.id(), value));
      }
    }

    // Phase 2: read only the relations explicitly declared by the algorithm author.
    for (Member source : edgeSources) {
      for (Object item : elements(source.read(batch))) {
        if (item == null) throw new IllegalArgumentException("null edge in " + source.name());
        Member fromMember = exactlyOne(item.getClass(), GraphFrom.class, true);
        Member toMember = exactlyOne(item.getClass(), GraphTo.class, true);
        Member bidirectional = exactlyOne(item.getClass(), GraphBidirectional.class, false);
        Member weightMember = exactlyOne(item.getClass(), GraphWeight.class, false);
        Object fromValue = Objects.requireNonNull(fromMember.read(item), "edge source is null");
        Object toValue = Objects.requireNonNull(toMember.read(item), "edge target is null");
        Vertex<T> from = resolve(fromValue, byReference, byBusinessKey);
        Vertex<T> to = resolve(toValue, byReference, byBusinessKey);
        if (from == null || to == null) throw new IllegalArgumentException(
            "edge endpoint is not declared in @GraphNodes: " + item.getClass().getName());
        double weight = 1.0;
        if (weightMember != null) {
          Object raw = weightMember.read(item);
          if (!(raw instanceof Number number))
            throw new IllegalArgumentException("@GraphWeight must be numeric: " + weightMember.name());
          weight = number.doubleValue();
          if (!Double.isFinite(weight)) throw new IllegalArgumentException("edge weight must be finite");
        }
        boolean twoWay = false;
        if (bidirectional != null) {
          Object raw = bidirectional.read(item);
          if (!(raw instanceof Boolean flag))
            throw new IllegalArgumentException("@GraphBidirectional must be boolean: "
                + bidirectional.name());
          twoWay = flag;
        }
        long forwardId = new MultiEdge<>(from, to, item, weight).id();
        edges.add(new MultiGraphSnapshot.Edge(
            forwardId, from.id(), to.id(), item, weight));
        if (twoWay && direction.isDirected()) {
          if (from == to) throw new IllegalArgumentException("bidirectional self-loop is ambiguous");
          long reverseId = new MultiEdge<>(to, from, item, weight).id();
          edges.add(new MultiGraphSnapshot.Edge(
              reverseId, to.id(), from.id(), item, weight));
        }
      }
    }

    // Snapshot restore validates all IDs/endpoints and initializes indexes without per-edge events.
    return MultiGraph.fromSnapshot(new MultiGraphSnapshot<>(direction, vertices, edges));
  }

  private static <T> Vertex<T> resolve(Object value, IdentityHashMap<Object, Vertex<T>> references,
      Map<NodeIdentity, Vertex<T>> businessKeys) {
    Vertex<T> direct = references.get(value);
    if (direct != null) return direct;
    NodeIdentity key = identity(value);
    return key == null ? null : businessKeys.get(key);
  }

  private record NodeIdentity(Class<?> type, Object id) {}

  private static NodeIdentity identity(Object value) {
    List<Member> ids = members(value.getClass(), ValueId.class);
    if (ids.isEmpty()) return null;
    if (ids.size() != 1) throw new IllegalArgumentException(
        "Only one @ValueId is allowed: " + value.getClass().getName());
    Object key = ids.getFirst().read(value);
    if (key == null) throw new IllegalArgumentException("@ValueId returned null");
    return new NodeIdentity(value.getClass(), key);
  }

  /** Iterable, Java array, single object, and null values are supported. */
  private static Iterable<?> elements(Object value) {
    if (value == null) return List.of();
    if (value instanceof Iterable<?> iterable) return iterable;
    if (value.getClass().isArray()) {
      return () -> new Iterator<>() {
        final int length = Array.getLength(value);
        int index;
        @Override public boolean hasNext() { return index < length; }
        @Override public Object next() {
          if (!hasNext()) throw new NoSuchElementException();
          return Array.get(value, index++);
        }
      };
    }
    return List.of(value);
  }

  private static Member exactlyOne(Class<?> type, Class<? extends Annotation> marker, boolean required) {
    List<Member> found = members(type, marker);
    if (found.size() > 1 || required && found.isEmpty()) throw new IllegalArgumentException(
        "Expected " + (required ? "exactly one" : "at most one") + " @"
            + marker.getSimpleName() + " on " + type.getName());
    return found.isEmpty() ? null : found.getFirst();
  }

  private record Member(String name, Field field, Method method) {
    Object read(Object receiver) {
      try {
        if (field != null) {
          if (!field.trySetAccessible()) throw new IllegalArgumentException("inaccessible field: " + field);
          return field.get(receiver);
        }
        if (!method.trySetAccessible()) throw new IllegalArgumentException("inaccessible method: " + method);
        return method.invoke(receiver);
      } catch (ReflectiveOperationException exception) {
        throw new IllegalArgumentException("Cannot read @" + name + " on "
            + receiver.getClass().getName(), exception);
      }
    }
  }

  private static List<Member> members(Class<?> type, Class<? extends Annotation> marker) {
    LinkedHashMap<String, Member> members = new LinkedHashMap<>();
    for (Class<?> cursor = type; cursor != null && cursor != Object.class; cursor = cursor.getSuperclass()) {
      for (Field field : cursor.getDeclaredFields()) {
        if (!field.isAnnotationPresent(marker)) continue;
        if (Modifier.isStatic(field.getModifiers())) throw new IllegalArgumentException(
            "Annotated member cannot be static: " + field);
        members.putIfAbsent(field.getName(), new Member(field.getName(), field, null));
      }
      for (Method method : cursor.getDeclaredMethods()) {
        if (!method.isAnnotationPresent(marker)) continue;
        if (method.isSynthetic() || Modifier.isStatic(method.getModifiers())
            || method.getParameterCount() != 0 || method.getReturnType() == void.class)
          throw new IllegalArgumentException("Annotated method must be a non-static no-arg accessor: " + method);
        String property = propertyName(method.getName());
        // A record component may propagate its annotation to both field and accessor.
        members.putIfAbsent(property, new Member(property, null, method));
      }
    }
    return List.copyOf(members.values());
  }

  private static String propertyName(String method) {
    if (method.startsWith("get") && method.length() > 3)
      return Character.toLowerCase(method.charAt(3)) + method.substring(4);
    if (method.startsWith("is") && method.length() > 2)
      return Character.toLowerCase(method.charAt(2)) + method.substring(3);
    return method;
  }
}
