package com.majortom.algorithms.structure.graph;

import com.majortom.algorithms.core.metadata.GraphMappingMetadata;
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


/**
 * 注解式业务对象到 Graph 的映射器，刻意与文件格式和 UI 分离。
 *
 * <p><b>两阶段建图：</b>先递归读取 @GraphNodes 收集顶点与业务键，
 * 同时暂存 @GraphEdges；收集结束后才解析边端点。
 * 这种顺序允许边在数据中先于其端点出现，并可检测不存在的端点。
 *
 * <p>同一对象引用优先使用 IdentityHashMap 匹配；
 * 不同对象按“运行时类型 + @ValueId”匹配，并拒绝重复业务键。
 * 因此边端点应是 City 实体或可匹配的对象，单纯字符串 "sz"
 * 并不会自动转换成 City：那是业务数据加载阶段的职责。
 *
 * <p>在有向图中 bidirectional 会展开两条独立边 ID；无向图只需一条。
 * 最终通过 GraphSnapshot → Graph.fromSnapshot 构造合法图，
 * 不在解析业务对象的途中修改工作台图。
 */
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
  private record EdgeSource(Object edge, GraphMappingMetadata.EdgeCollection mapping) {}

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
          if (GraphMappingMetadata.describe(value.getClass()).nodes().isEmpty())
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
      var metadata = GraphMappingMetadata.describe(value.getClass());
      for (var mapping : metadata.edges()) {
        for (Object edge : elements(mapping.member().read(value))) {
          if (edge == null)
            throw new IllegalArgumentException("Null graph edge at " + mapping.member().name());
          pendingEdges.add(new EdgeSource(edge, mapping));
        }
      }
      for (var member : metadata.nodes()) {
        for (Object child : elements(member.read(value))) visit(child, true);
      }
    }

    Graph<T> build(GraphDirection direction) {
      List<GraphSnapshot.Edge> links = new ArrayList<>();
      for (EdgeSource source : pendingEdges) {
        Object edge = source.edge();
        GraphMappingMetadata.EdgeValues mapped = source.mapping().read(edge);
        Vertex<T> from = resolve(mapped.from());
        Vertex<T> to = resolve(mapped.to());
        if (from == null || to == null)
          throw new IllegalArgumentException("Undeclared graph edge endpoint in " + edge.getClass().getName());
        double weight = mapped.weight();
        boolean twoWay = mapped.bidirectional();
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