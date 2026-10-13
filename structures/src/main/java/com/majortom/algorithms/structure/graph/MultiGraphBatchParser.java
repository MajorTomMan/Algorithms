package com.majortom.algorithms.structure.graph;

import com.majortom.algorithms.core.annotation.*;
import com.majortom.algorithms.core.metadata.AnnotatedMembers;
import com.majortom.algorithms.core.metadata.GraphDirection;
import com.majortom.algorithms.core.metadata.ValueMetadata;
import com.majortom.algorithms.core.snapshot.MultiGraphSnapshot;
import java.lang.reflect.Array;
import java.util.*;

/**
 * Builds a multigraph exclusively from explicit node and edge annotations.
 * Reflection metadata is shared with the presentation layer via core.
 */
final class MultiGraphBatchParser {
  private MultiGraphBatchParser() {}

  static <T> MultiGraph<T> parse(GraphDirection direction, Class<T> nodeType, Object batch) {
    Objects.requireNonNull(direction, "direction");
    Objects.requireNonNull(nodeType, "nodeType");
    Objects.requireNonNull(batch, "batch");
    var nodeSources = AnnotatedMembers.marked(batch.getClass(), GraphNodes.class);
    var edgeSources = AnnotatedMembers.marked(batch.getClass(), GraphEdges.class);
    if (nodeSources.isEmpty()) throw new IllegalArgumentException(
        "Batch class has no @GraphNodes member: " + batch.getClass().getName());

    List<MultiGraphSnapshot.Vertex<T>> vertices = new ArrayList<>();
    List<MultiGraphSnapshot.Edge> edges = new ArrayList<>();
    IdentityHashMap<Object, Vertex<T>> byReference = new IdentityHashMap<>();
    Map<NodeIdentity, Vertex<T>> byBusinessKey = new HashMap<>();

    // Phase 1: collect explicitly declared vertices including isolated and forward-referenced nodes.
    for (var source : nodeSources) {
      for (Object item : elements(source.read(batch))) {
        if (item == null) throw new IllegalArgumentException("null node in " + source.name());
        if (byReference.containsKey(item)) continue;
        var metadata = ValueMetadata.describe(item.getClass());
        if (!metadata.supports(MultiGraphStructure.class))
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

    // Phase 2: connect exactly the edges declared by the author; no recursive relationship inference.
    for (var source : edgeSources) {
      for (Object item : elements(source.read(batch))) {
        if (item == null) throw new IllegalArgumentException("null edge in " + source.name());
        var fromAccessor = AnnotatedMembers.one(item.getClass(), GraphFrom.class, true);
        var toAccessor = AnnotatedMembers.one(item.getClass(), GraphTo.class, true);
        var bidirectional = AnnotatedMembers.one(item.getClass(), GraphBidirectional.class, false);
        var weightAccessor = AnnotatedMembers.one(item.getClass(), GraphWeight.class, false);
        Object fromValue = Objects.requireNonNull(fromAccessor.read(item), "edge source is null");
        Object toValue = Objects.requireNonNull(toAccessor.read(item), "edge target is null");
        Vertex<T> from = resolve(fromValue, byReference, byBusinessKey);
        Vertex<T> to = resolve(toValue, byReference, byBusinessKey);
        if (from == null || to == null) throw new IllegalArgumentException(
            "edge endpoint is not declared in @GraphNodes: " + item.getClass().getName());
        double weight = 1.0;
        if (weightAccessor != null) {
          Object raw = weightAccessor.read(item);
          if (!(raw instanceof Number number))
            throw new IllegalArgumentException("@GraphWeight must be numeric: " + weightAccessor.name());
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
        edges.add(new MultiGraphSnapshot.Edge(forwardId, from.id(), to.id(), item, weight));
        if (twoWay && direction.isDirected()) {
          if (from == to) throw new IllegalArgumentException("bidirectional self-loop is ambiguous");
          long reverseId = new MultiEdge<>(to, from, item, weight).id();
          edges.add(new MultiGraphSnapshot.Edge(reverseId, to.id(), from.id(), item, weight));
        }
      }
    }
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
    var descriptor = ValueMetadata.describe(value.getClass());
    Object id = descriptor.businessIdOf(value);
    return id == null ? null : new NodeIdentity(value.getClass(), id);
  }

  /** Accept collections, Java arrays, a scalar object and empty/null collections. */
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
}
