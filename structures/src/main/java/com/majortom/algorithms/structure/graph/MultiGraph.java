package com.majortom.algorithms.structure.graph;
import com.majortom.algorithms.core.event.structure.MultiGraphStructureEvent;
import com.majortom.algorithms.core.metadata.GraphDirection;
import com.majortom.algorithms.core.runtime.ExecutionEvents;
import com.majortom.algorithms.core.snapshot.MultiGraphSnapshot;
import java.util.*;
/**
 * ID-based directed/undirected multigraph. No value-equality de-duplication:
 * two vertices may hold the same Java object or equal values.
 */
public final class MultiGraph<T> implements MultiGraphStructure<T> {
  private final GraphDirection direction;
  private final LinkedHashMap<Long, Vertex<T>> vertices = new LinkedHashMap<>();
  private final LinkedHashMap<Long, MultiEdge<T>> edges = new LinkedHashMap<>();
  private final LinkedHashMap<Long, LinkedHashSet<Long>> outgoing = new LinkedHashMap<>();
  private final LinkedHashMap<Long, LinkedHashSet<Long>> incoming = new LinkedHashMap<>();

  public MultiGraph() { this(GraphDirection.DIRECTED); }
  public MultiGraph(GraphDirection direction) { this.direction = Objects.requireNonNull(direction); }
  public record BiEdges<T>(MultiEdge<T> forward, MultiEdge<T> reverse) {}

  /** Batch import with mixed node types (MultiGraph<Object>). */
  public static MultiGraph<Object> from(GraphDirection direction, Object data) {
    return MultiGraphBatchParser.parse(direction, Object.class, data);
  }
  /** Batch import with an enforced common node type. */
  public static <T> MultiGraph<T> from(GraphDirection direction, Class<T> nodeType, Object data) {
    return MultiGraphBatchParser.parse(direction, nodeType, data);
  }
  public static <T> MultiGraph<T> fromSnapshot(MultiGraphSnapshot<T> snapshot) {
    MultiGraph<T> result = new MultiGraph<>(Objects.requireNonNull(snapshot).direction());
    result.initialize(snapshot);
    return result;
  }
  @Override public GraphDirection direction() { return direction; }
  @Override public int vertexCount() { return vertices.size(); }
  @Override public int edgeCount() { return edges.size(); }
  @Override public Vertex<T> vertex(long id) { return vertices.get(id); }
  @Override public MultiEdge<T> edge(long id) { return edges.get(id); }
  @Override public Iterable<Vertex<T>> vertices() { return List.copyOf(vertices.values()); }
  @Override public Iterable<MultiEdge<T>> edges() { return List.copyOf(edges.values()); }

  @Override public Vertex<T> addVertex(T value) {
    Vertex<T> vertex = new Vertex<>(Objects.requireNonNull(value, "value"));
    addVertexInternal(vertex);
    ExecutionEvents.structure(new MultiGraphStructureEvent.VertexAdded(vertex.id(), value));
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
  private MultiEdge<T> requireEdge(long edgeId) {
    MultiEdge<T> edge = edges.get(edgeId);
    if (edge == null) throw new IllegalArgumentException("unknown edge ID: " + edgeId);
    return edge;
  }
  private void attach(MultiEdge<T> edge) {
    if (edges.putIfAbsent(edge.id(), edge) != null)
      throw new IllegalArgumentException("duplicate edge ID: " + edge.id());
    outgoing.get(edge.from().id()).add(edge.id());
    incoming.get(edge.to().id()).add(edge.id());
    if (!direction.isDirected()) {
      outgoing.get(edge.to().id()).add(edge.id());
      incoming.get(edge.from().id()).add(edge.id());
    }
  }
  private void detach(MultiEdge<T> edge) {
    edges.remove(edge.id());
    outgoing.get(edge.from().id()).remove(edge.id());
    incoming.get(edge.to().id()).remove(edge.id());
    if (!direction.isDirected()) {
      outgoing.get(edge.to().id()).remove(edge.id());
      incoming.get(edge.from().id()).remove(edge.id());
    }
  }
  private static <T> MultiGraphStructureEvent.EdgeAdded added(MultiEdge<T> edge) {
    return new MultiGraphStructureEvent.EdgeAdded(
        edge.id(), edge.from().id(), edge.to().id(), edge.value(), edge.weight());
  }
  @Override public MultiEdge<T> addEdge(Vertex<T> from, Vertex<T> to, Object value, double weight) {
    requireVertex(from);
    requireVertex(to);
    MultiEdge<T> edge = new MultiEdge<>(from, to, value, weight);
    attach(edge);
    ExecutionEvents.structure(added(edge));
    return edge;
  }
  @Override public BiEdges<T> addBiEdge(Vertex<T> from, Vertex<T> to, Object forwardValue,
       Object reverseValue, double forwardWeight, double reverseWeight) {
    if (!direction.isDirected()) throw new IllegalStateException("use addEdge for an undirected graph");
    requireVertex(from);
    requireVertex(to);
    if (from == to) throw new IllegalArgumentException("bidirectional self-loop is ambiguous");
    // Fully validate before mutating either adjacency index.
    MultiEdge<T> forward = new MultiEdge<>(from, to, forwardValue, forwardWeight);
    MultiEdge<T> reverse = new MultiEdge<>(to, from, reverseValue, reverseWeight);
    attach(forward);
    attach(reverse);
    ExecutionEvents.structure(new MultiGraphStructureEvent.EdgesAdded(added(forward), added(reverse)));
    return new BiEdges<>(forward, reverse);
  }
  @Override public boolean removeEdge(long id) {
    MultiEdge<T> edge = edges.get(id);
    if (edge == null) return false;
    detach(edge);
    ExecutionEvents.structure(new MultiGraphStructureEvent.EdgeRemoved(id));
    return true;
  }
  @Override public boolean removeVertex(long id) {
    Vertex<T> vertex = vertices.get(id);
    if (vertex == null) return false;
    LinkedHashSet<Long> incident = new LinkedHashSet<>(outgoing.get(id));
    incident.addAll(incoming.get(id));
    for (long edgeId : incident) detach(edges.get(edgeId));
    vertices.remove(id);
    outgoing.remove(id);
    incoming.remove(id);
    ExecutionEvents.structure(new MultiGraphStructureEvent.VertexRemoved(
        id, vertex.value(), List.copyOf(incident)));
    return true;
  }
  @Override public double setWeight(long id, double weight) {
    if (!Double.isFinite(weight)) throw new IllegalArgumentException("non-finite edge weight");
    MultiEdge<T> edge = requireEdge(id);
    double old = edge.weight();
    if (Double.compare(old, weight) != 0) {
      edge.updateWeight(weight);
      ExecutionEvents.structure(new MultiGraphStructureEvent.EdgeWeightChanged(id, old, weight));
    }
    return old;
  }
  @Override public List<MultiEdge<T>> outgoingEdges(Vertex<T> vertex) {
    requireVertex(vertex);
    return outgoing.get(vertex.id()).stream().map(edges::get).toList();
  }
  @Override public List<MultiEdge<T>> incomingEdges(Vertex<T> vertex) {
    requireVertex(vertex);
    return incoming.get(vertex.id()).stream().map(edges::get).toList();
  }
  @Override public List<MultiEdge<T>> edges(Vertex<T> from, Vertex<T> to) {
    requireVertex(from);
    requireVertex(to);
    return outgoing.get(from.id()).stream().map(edges::get)
        .filter(edge -> direction.isDirected()
            ? edge.to() == to : (edge.from() == to || edge.to() == to))
        .toList();
  }
  @Override public MultiGraphSnapshot<T> snapshot() {
    List<MultiGraphSnapshot.Vertex<T>> nodes = vertices.values().stream()
        .map(v -> new MultiGraphSnapshot.Vertex<>(v.id(), v.value())).toList();
    List<MultiGraphSnapshot.Edge> links = edges.values().stream()
        .map(e -> new MultiGraphSnapshot.Edge(e.id(), e.from().id(), e.to().id(),
                                             e.value(), e.weight())).toList();
    return new MultiGraphSnapshot<>(direction, nodes, links);
  }
  @Override public void initialize(MultiGraphSnapshot<T> snapshot) {
    Objects.requireNonNull(snapshot, "snapshot");
    if (snapshot.direction() != direction) throw new IllegalArgumentException("direction mismatch");
    // Prepare and validate in a detached graph. Neither failures nor construction emit per-edge events.
    MultiGraph<T> restored = new MultiGraph<>(direction);
    for (MultiGraphSnapshot.Vertex<T> node : snapshot.vertices())
      restored.addVertexInternal(new Vertex<>(node.id(), node.value()));
    for (MultiGraphSnapshot.Edge link : snapshot.edges()) {
      Vertex<T> from = restored.vertex(link.fromId());
      Vertex<T> to = restored.vertex(link.toId());
      if (from == null || to == null) throw new IllegalArgumentException("edge references unknown vertex");
      restored.attach(new MultiEdge<>(link.id(), from, to, link.value(), link.weight()));
    }
    vertices.clear(); vertices.putAll(restored.vertices);
    edges.clear(); edges.putAll(restored.edges);
    outgoing.clear(); outgoing.putAll(restored.outgoing);
    incoming.clear(); incoming.putAll(restored.incoming);
    ExecutionEvents.structure(new MultiGraphStructureEvent.Reset(snapshot()));
  }
}
