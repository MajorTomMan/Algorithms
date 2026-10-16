package com.majortom.algorithms.structure.graph;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;

import com.majortom.algorithms.core.event.structure.GraphStructureEvent;
import com.majortom.algorithms.core.metadata.GraphDirection;
import com.majortom.algorithms.core.runtime.ExecutionEvents;
import com.majortom.algorithms.core.snapshot.GraphSnapshot;

/**
 * ID 为核心的有向/无向多重图实现，是真实拓扑及其不变量的所有者。
 *
 * <p>顶点、边分别以独立 long ID 存储，adjacency 中存的是边 ID，
 * 所以两顶点之间允许多条平行边，具有相同 Java 值的顶点也不会合并。
 * 有向图保留出/入方向；无向图同一条边可以从两个端点访问。
 *
 * <p>每次结构突变向 ExecutionEvents 发布事实；若未进入 Runtime 上下文，
 * 结构仍能使用，但不会自动产生可回放记录。
 *
 * <p>{@link #from(GraphDirection, Class, Object)} 负责业务模型映射，
 * {@link #fromSnapshot(GraphSnapshot)} 负责创建新结构，
 * {@link #initialize(GraphSnapshot)} 在独立临时图验证后整体替换状态，
 * 成功后只发布 Reset，避免中间状态与事件序列不一致。
 * 图方向在实例构造时固定，初始化不能改变方向。
 *
 * <p>这里不做 ELK 布局、摄像机控制或动画：它们属于客户端 RenderFramework。
 */
public final class Graph<T> implements GraphStructure<T> {
  private final GraphDirection direction;
  private final LinkedHashMap<Long, Vertex<T>> vertices = new LinkedHashMap<>();
  private final LinkedHashMap<Long, Edge<T>> edges = new LinkedHashMap<>();
  private final LinkedHashMap<Long, LinkedHashSet<Long>> outgoing = new LinkedHashMap<>();
  private final LinkedHashMap<Long, LinkedHashSet<Long>> incoming = new LinkedHashMap<>();

  public Graph() {
    this(GraphDirection.UNDIRECTED);
  }

  public Graph(GraphDirection direction) {
    this.direction = Objects.requireNonNull(direction);
  }

  public record BiEdges<T>(Edge<T> forward, Edge<T> reverse) {
  }

  /** Read annotated objects as a graph with mixed node value types. */
  public static Graph<Object> from(GraphDirection direction, Object data) {
    return GraphObjectMapper.read(direction, Object.class, data);
  }

  /** Read annotated objects with a validated node type. */
  public static <T> Graph<T> from(GraphDirection direction, Class<T> nodeType, Object data) {
    return GraphObjectMapper.read(direction, nodeType, data);
  }

  public static <T> Graph<T> fromSnapshot(GraphSnapshot<T> snapshot) {
    Graph<T> result = new Graph<>(Objects.requireNonNull(snapshot).direction());
    result.restore(snapshot, false);
    return result;
  }

  @Override
  public GraphDirection direction() {
    return direction;
  }

  @Override
  public int vertexCount() {
    return vertices.size();
  }

  @Override
  public int edgeCount() {
    return edges.size();
  }

  @Override
  public Vertex<T> vertex(long id) {
    return vertices.get(id);
  }

  @Override
  public boolean containsVertex(Vertex<T> vertex) {
    return vertex != null && vertices.get(vertex.id()) == vertex;
  }

  @Override
  public boolean containsEdge(Vertex<T> from, Vertex<T> to) {
    requireVertex(from);
    requireVertex(to);
    // Existence requires no intermediate list of matching parallel edges.
    for (long id : outgoing.get(from.id())) {
      Edge<T> edge = edges.get(id);
      if (direction.isDirected()
          ? edge.to() == to
          : (edge.from() == to || edge.to() == to))
        return true;
    }
    return false;
  }

  @Override
  public boolean isDirected() {
    return direction.isDirected();
  }

  @Override
  public boolean isEmpty() {
    return vertices.isEmpty();
  }

  @Override
  public Edge<T> edge(long id) {
    return edges.get(id);
  }

  @Override
  public Iterable<Vertex<T>> vertices() {
    return List.copyOf(vertices.values());
  }

  @Override
  public Iterable<Edge<T>> edges() {
    return List.copyOf(edges.values());
  }

  @Override
  public Vertex<T> addVertex(T value) {
    Vertex<T> vertex = new Vertex<>(Objects.requireNonNull(value, "value"));
    addVertexInternal(vertex);
    ExecutionEvents.structure(new GraphStructureEvent.VertexAdded(vertex.id(), value));
    return vertex;
  }

  private void addVertexInternal(Vertex<T> vertex) {
    if (vertices.putIfAbsent(vertex.id(), vertex) != null)
      throw new IllegalArgumentException("duplicate vertex ID: " + vertex.id());
    outgoing.put(vertex.id(), new LinkedHashSet<>());
    incoming.put(vertex.id(), new LinkedHashSet<>());
  }

  private void requireVertex(Vertex<T> vertex) {
    if (vertex == null || vertices.get(vertex.id()) != vertex)
      throw new IllegalArgumentException("vertex does not belong to this graph");
  }

  private Edge<T> requireEdge(long edgeId) {
    Edge<T> edge = edges.get(edgeId);
    if (edge == null)
      throw new IllegalArgumentException("unknown edge ID: " + edgeId);
    return edge;
  }

  private void attach(Edge<T> edge) {
    if (edges.putIfAbsent(edge.id(), edge) != null)
      throw new IllegalArgumentException("duplicate edge ID: " + edge.id());
    outgoing.get(edge.from().id()).add(edge.id());
    incoming.get(edge.to().id()).add(edge.id());
    if (!direction.isDirected() && edge.from() != edge.to()) {
      outgoing.get(edge.to().id()).add(edge.id());
      incoming.get(edge.from().id()).add(edge.id());
    }
  }

  private void detach(Edge<T> edge) {
    edges.remove(edge.id());
    outgoing.get(edge.from().id()).remove(edge.id());
    incoming.get(edge.to().id()).remove(edge.id());
    if (!direction.isDirected() && edge.from() != edge.to()) {
      outgoing.get(edge.to().id()).remove(edge.id());
      incoming.get(edge.from().id()).remove(edge.id());
    }
  }

  private static <T> GraphStructureEvent.EdgeAdded added(Edge<T> edge) {
    return new GraphStructureEvent.EdgeAdded(
        edge.id(), edge.from().id(), edge.to().id(), edge.value(), edge.weight());
  }

  @Override
  public Edge<T> addEdge(Vertex<T> from, Vertex<T> to, Object value, double weight) {
    requireVertex(from);
    requireVertex(to);
    Edge<T> edge = new Edge<>(from, to, value, weight);
    attach(edge);
    ExecutionEvents.structure(added(edge));
    return edge;
  }

  @Override
  public BiEdges<T> addBiEdge(Vertex<T> from, Vertex<T> to, Object forwardValue,
      Object reverseValue, double forwardWeight, double reverseWeight) {
    if (!direction.isDirected())
      throw new IllegalStateException("use addEdge for an undirected graph");
    requireVertex(from);
    requireVertex(to);
    if (from == to)
      throw new IllegalArgumentException("bidirectional self-loop is ambiguous");
    // Fully validate before mutating either adjacency index.
    Edge<T> forward = new Edge<>(from, to, forwardValue, forwardWeight);
    Edge<T> reverse = new Edge<>(to, from, reverseValue, reverseWeight);
    attach(forward);
    attach(reverse);
    ExecutionEvents.structure(new GraphStructureEvent.EdgesAdded(added(forward), added(reverse)));
    return new BiEdges<>(forward, reverse);
  }

  @Override
  public boolean removeEdge(long id) {
    Edge<T> edge = edges.get(id);
    if (edge == null)
      return false;
    detach(edge);
    ExecutionEvents.structure(new GraphStructureEvent.EdgeRemoved(id));
    return true;
  }

  @Override
  public boolean removeVertex(long id) {
    Vertex<T> vertex = vertices.get(id);
    if (vertex == null)
      return false;
    LinkedHashSet<Long> incident = new LinkedHashSet<>(outgoing.get(id));
    incident.addAll(incoming.get(id));
    for (long edgeId : incident)
      detach(edges.get(edgeId));
    vertices.remove(id);
    outgoing.remove(id);
    incoming.remove(id);
    ExecutionEvents.structure(new GraphStructureEvent.VertexRemoved(
        id, vertex.value(), List.copyOf(incident)));
    return true;
  }

  @Override
  public double setWeight(long id, double weight) {
    if (!Double.isFinite(weight))
      throw new IllegalArgumentException("non-finite edge weight");
    Edge<T> edge = requireEdge(id);
    double old = edge.weight();
    if (Double.compare(old, weight) != 0) {
      edge.updateWeight(weight);
      ExecutionEvents.structure(new GraphStructureEvent.EdgeWeightChanged(id, old, weight));
    }
    return old;
  }

  @Override
  public List<Edge<T>> outgoingEdges(Vertex<T> vertex) {
    requireVertex(vertex);
    return outgoing.get(vertex.id()).stream().map(edges::get).toList();
  }

  @Override
  public List<Edge<T>> incomingEdges(Vertex<T> vertex) {
    requireVertex(vertex);
    return incoming.get(vertex.id()).stream().map(edges::get).toList();
  }

  @Override
  public List<Edge<T>> edges(Vertex<T> from, Vertex<T> to) {
    requireVertex(from);
    requireVertex(to);
    return outgoing.get(from.id()).stream().map(edges::get)
        .filter(edge -> direction.isDirected()
            ? edge.to() == to
            : (edge.from() == to || edge.to() == to))
        .toList();
  }

  @Override
  public Edge<T> addEdge(Vertex<T> from, Vertex<T> to, double weight) {
    return addEdge(from, to, null, weight);
  }

  @Override
  public boolean removeVertex(Vertex<T> vertex) {
    return containsVertex(vertex) && removeVertex(vertex.id());
  }

  @Override
  public double weight(Edge<T> edge) {
    Objects.requireNonNull(edge, "edge");
    requireEdge(edge.id());
    if (edges.get(edge.id()) != edge)
      throw new IllegalArgumentException("edge does not belong to this graph");
    return edge.weight();
  }

  @Override
  public double setWeight(Edge<T> edge, double weight) {
    if (edge == null || edges.get(edge.id()) != edge)
      throw new IllegalArgumentException("edge does not belong to this graph");
    return setWeight(edge.id(), weight);
  }

  @Override
  public Iterable<Vertex<T>> neighbors(Vertex<T> vertex) {
    requireVertex(vertex);
    LinkedHashSet<Vertex<T>> result = new LinkedHashSet<>();
    for (long id : outgoing.get(vertex.id())) {
      Edge<T> edge = edges.get(id);
      result.add(edge.from() == vertex ? edge.to() : edge.from());
    }
    return List.copyOf(result);
  }

  @Override
  public GraphSnapshot<T> snapshot() {
    List<GraphSnapshot.Vertex<T>> nodes = vertices.values().stream()
        .map(v -> new GraphSnapshot.Vertex<>(v.id(), v.value())).toList();
    List<GraphSnapshot.Edge> links = edges.values().stream()
        .map(e -> new GraphSnapshot.Edge(e.id(), e.from().id(), e.to().id(),
            e.value(), e.weight()))
        .toList();
    return new GraphSnapshot<>(direction, nodes, links);
  }

  @Override
  public void initialize(GraphSnapshot<T> snapshot) {
    restore(snapshot, true);
  }

  private void restore(GraphSnapshot<T> snapshot, boolean notifyReset) {
    Objects.requireNonNull(snapshot, "snapshot");
    if (snapshot.direction() != direction)
      throw new IllegalArgumentException("direction mismatch");
    // Prepare and validate in a detached graph. Neither failures nor construction
    // emit per-edge events.
    Graph<T> restored = new Graph<>(direction);
    for (GraphSnapshot.Vertex<T> node : snapshot.vertices())
      restored.addVertexInternal(new Vertex<>(node.id(), node.value()));
    for (GraphSnapshot.Edge link : snapshot.edges()) {
      Vertex<T> from = restored.vertex(link.fromId());
      Vertex<T> to = restored.vertex(link.toId());
      if (from == null || to == null)
        throw new IllegalArgumentException("edge references unknown vertex");
      restored.attach(new Edge<>(link.id(), from, to, link.value(), link.weight()));
    }
    vertices.clear();
    vertices.putAll(restored.vertices);
    edges.clear();
    edges.putAll(restored.edges);
    outgoing.clear();
    outgoing.putAll(restored.outgoing);
    incoming.clear();
    incoming.putAll(restored.incoming);
    if (notifyReset)
      ExecutionEvents.structure(new GraphStructureEvent.Reset(snapshot()));
  }
}