package com.majortom.algorithms.structure.graph;

import com.majortom.algorithms.core.annotation.GraphEdges;
import com.majortom.algorithms.core.annotation.GraphNodes;
import com.majortom.algorithms.core.metadata.AnnotatedMembers;
import com.majortom.algorithms.core.metadata.GraphDirection;
import com.majortom.algorithms.core.metadata.ValueMetadata;
import com.majortom.algorithms.core.snapshot.GraphSnapshot;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;


/** Interprets annotated Java objects as graph topology; unrelated to input format or files. */
final class GraphObjectMapper {
  private GraphObjectMapper() {}

  static <T> Graph<T> read(GraphDirection direction, Class<T> nodeType, Object data) {
    Objects.requireNonNull(direction, "direction");
    Objects.requireNonNull(nodeType, "nodeType");
    Objects.requireNonNull(data, "data");
    Collector<T> collector = new Collector<>(nodeType);
    collector.collect(data);
    return collector.build(direction);
  }

  private record NodeIdentity(Class<?> type, Object id) {}
  private record EdgeSource(Object edge, GraphEdges mapping) {}

  private static final class Collector<T> {
    private final Class<T> nodeType;
    private final IdentityHashMap<Object, Vertex<T>> byReference = new IdentityHashMap<>();
    private final Map<NodeIdentity, Vertex<T>> byKey = new HashMap<>();
    private final Set<Object> walked = Collections.newSetFromMap(new IdentityHashMap<>());
    private final List<GraphSnapshot.Vertex<T>> nodes = new ArrayList<>();
    private final List<EdgeSource> pendingEdges = new ArrayList<>();

    Collector(Class<T> nodeType) { this.nodeType = nodeType; }

    void collect(Object root) {
      // A typed root can itself be the first vertex of a recursive tree.
      if (nodeType != Object.class && nodeType.isInstance(root)) {
        visit(root, true);
      } else if (root instanceof Iterable<?> || root.getClass().isArray()) {
        for (Object child : elements(root)) visit(child, true);
      } else {
        visit(root, false);
        if (nodes.isEmpty())
          throw new IllegalArgumentException("No @GraphNodes reachable from " + root.getClass().getName());
      }
    }

    private void visit(Object value, boolean isNode) {
      if (value == null) throw new IllegalArgumentException("Null graph node");
      if (isNode) {
        if (!nodeType.isInstance(value)) {
          // Explicitly nested wrapper object: it may expose further annotated nodes.
          if (AnnotatedMembers.marked(value.getClass(), GraphNodes.class).isEmpty())
            throw new IllegalArgumentException("Node type " + value.getClass().getName()
                + " is incompatible with " + nodeType.getName());
        } else if (!byReference.containsKey(value)) {
          var metadata = ValueMetadata.describe(value.getClass());
          if (!metadata.supports(GraphStructure.class))
            throw new IllegalArgumentException("Node type not enabled for Graph: "
                + value.getClass().getName());
          NodeIdentity key = identity(value);
          if (key != null && byKey.containsKey(key))
            throw new IllegalArgumentException("Duplicate graph node identity: " + key);
          Vertex<T> vertex = new Vertex<>(nodeType.cast(value));
          byReference.put(value, vertex);
          if (key != null) byKey.put(key, vertex);
          nodes.add(new GraphSnapshot.Vertex<>(vertex.id(), vertex.value()));
        }
      }
      if (!walked.add(value)) return; // Identity-based recursion and cycle protection.
      for (var member : AnnotatedMembers.marked(value.getClass(), GraphEdges.class)) {
        GraphEdges annotation = member.annotation(GraphEdges.class);
        for (Object edge : elements(member.read(value))) {
          if (edge == null) throw new IllegalArgumentException("Null graph edge at " + member.name());
          pendingEdges.add(new EdgeSource(edge, annotation));
        }
      }
      for (var member : AnnotatedMembers.marked(value.getClass(), GraphNodes.class)) {
        for (Object child : elements(member.read(value))) visit(child, true);
      }
    }

    Graph<T> build(GraphDirection direction) {
      List<GraphSnapshot.Edge> links = new ArrayList<>();
      for (EdgeSource source : pendingEdges) {
        Object edge = source.edge();
        GraphEdges spec = source.mapping();
        Object fromValue = Objects.requireNonNull(
            AnnotatedMembers.named(edge.getClass(), spec.from()).read(edge), "Null edge source");
        Object toValue = Objects.requireNonNull(
            AnnotatedMembers.named(edge.getClass(), spec.to()).read(edge), "Null edge target");
        Vertex<T> from = resolve(fromValue);
        Vertex<T> to = resolve(toValue);
        if (from == null || to == null)
          throw new IllegalArgumentException("Undeclared graph edge endpoint in " + edge.getClass().getName());
        double weight = 1.0;
        if (!spec.weight().isBlank()) {
          Object raw = AnnotatedMembers.named(edge.getClass(), spec.weight()).read(edge);
          if (!(raw instanceof Number number))
            throw new IllegalArgumentException("Graph edge weight must be numeric: " + spec.weight());
          weight = number.doubleValue();
          if (!Double.isFinite(weight)) throw new IllegalArgumentException("Non-finite graph edge weight");
        }
        boolean twoWay = false;
        if (!spec.bidirectional().isBlank()) {
          Object flag = AnnotatedMembers.named(edge.getClass(), spec.bidirectional()).read(edge);
          if (!(flag instanceof Boolean booleanFlag))
            throw new IllegalArgumentException("Graph edge bidirectional flag must be boolean");
          twoWay = booleanFlag;
        }
        long id = new Edge<>(from, to, edge, weight).id();
        links.add(new GraphSnapshot.Edge(id, from.id(), to.id(), edge, weight));
        if (twoWay && direction.isDirected()) {
          if (from == to) throw new IllegalArgumentException("Bidirectional self-loop is ambiguous");
          long reverseId = new Edge<>(to, from, edge, weight).id();
          links.add(new GraphSnapshot.Edge(reverseId, to.id(), from.id(), edge, weight));
        }
      }
      return Graph.fromSnapshot(new GraphSnapshot<>(direction, nodes, links));
    }

    private Vertex<T> resolve(Object value) {
      Vertex<T> exact = byReference.get(value);
      if (exact != null) return exact;
      NodeIdentity key = identity(value);
      return key == null ? null : byKey.get(key);
    }
  }

  private static NodeIdentity identity(Object value) {
    Object id = ValueMetadata.describe(value.getClass()).businessIdOf(value);
    return id == null ? null : new NodeIdentity(value.getClass(), id);
  }

  /** Traverse explicitly marked collections, arrays, Optional and Map values. */
  private static Iterable<?> elements(Object value) {
    if (value == null) return List.of();
    if (value instanceof Optional<?> optional) return optional.map(List::of).orElseGet(List::of);
    if (value instanceof Map<?, ?> map) return map.values();
    if (value instanceof Iterable<?> iterable) return iterable;
    if (value.getClass().isArray()) {
      return () -> new Iterator<>() {
        final int length = Array.getLength(value);
        int offset;
        @Override public boolean hasNext() { return offset < length; }
        @Override public Object next() {
          if (!hasNext()) throw new NoSuchElementException();
          return Array.get(value, offset++);
        }
      };
    }
    return List.of(value);
  }
}