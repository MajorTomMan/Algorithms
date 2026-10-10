package com.majortom.algorithms.structure.graph;

import com.majortom.algorithms.core.metadata.GraphDirection;
import com.majortom.algorithms.core.runtime.StructureEvents;
import com.majortom.algorithms.core.snapshot.GraphSnapshot;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** A directed or undirected weighted graph with indexed adjacency and stable edge IDs. */
public final class Graph<T> implements GraphStructure<T> {
  private final GraphDirection direction;
  private final LinkedHashMap<T, Vertex<T>> verticesByValue = new LinkedHashMap<>();
  private final LinkedHashMap<Vertex<T>, LinkedHashMap<Vertex<T>, Edge<T>>> adjacency =
      new LinkedHashMap<>();
  private final LinkedHashMap<Long, Edge<T>> edgesById = new LinkedHashMap<>();

  public Graph() {
    this(GraphDirection.UNDIRECTED);
  }

  public Graph(GraphDirection direction) {
    this.direction = Objects.requireNonNull(direction, "direction");
  }

  public static <T> Graph<T> fromSnapshot(GraphSnapshot<T> snapshot) {
    Objects.requireNonNull(snapshot, "snapshot");
    Graph<T> graph = new Graph<>(snapshot.direction());
    Map<Long, Vertex<T>> verticesById = new LinkedHashMap<>();
    Set<T> values = new HashSet<>();

    for (GraphSnapshot.Vertex<T> source : snapshot.vertices()) {
      if (verticesById.containsKey(source.id())) {
        throw new IllegalArgumentException("duplicate vertex id: " + source.id());
      }
      if (!values.add(source.value())) {
        throw new IllegalArgumentException("duplicate vertex value: " + source.value());
      }
      Vertex<T> vertex = new Vertex<>(source.id(), source.value());
      graph.verticesByValue.put(vertex.value(), vertex);
      graph.adjacency.put(vertex, new LinkedHashMap<>());
      verticesById.put(vertex.id(), vertex);
    }

    Set<EdgeKey> edgeKeys = new HashSet<>();
    for (GraphSnapshot.Edge source : snapshot.edges()) {
      Vertex<T> from = verticesById.get(source.fromId());
      Vertex<T> to = verticesById.get(source.toId());
      if (from == null || to == null) {
        throw new IllegalArgumentException("edge references an unknown vertex");
      }
      if (graph.edgesById.containsKey(source.id())) {
        throw new IllegalArgumentException("duplicate edge id: " + source.id());
      }
      EdgeKey key = EdgeKey.of(source.fromId(), source.toId(), graph.direction);
      if (!edgeKeys.add(key)) {
        throw new IllegalArgumentException("duplicate edge: " + source.fromId() + " -> " + source.toId());
      }
      Edge<T> edge = new Edge<>(source.id(), from, to, source.weight());
      graph.attachEdge(edge);
    }
    return graph;
  }

  public GraphSnapshot<T> snapshot() {
    List<GraphSnapshot.Vertex<T>> vertices = new ArrayList<>();
    for (Vertex<T> vertex : verticesByValue.values()) {
      vertices.add(new GraphSnapshot.Vertex<>(vertex.id(), vertex.value()));
    }
    List<GraphSnapshot.Edge> edges = new ArrayList<>();
    for (Edge<T> edge : edgesById.values()) {
      edges.add(new GraphSnapshot.Edge(
          edge.id(), edge.from().id(), edge.to().id(), edge.weight()));
    }
    return new GraphSnapshot<>(direction, vertices, edges);
  }

  @Override
  public GraphDirection direction() {
    return direction;
  }

  @Override
  public int vertexCount() {
    return verticesByValue.size();
  }

  @Override
  public int edgeCount() {
    return edgesById.size();
  }

  @Override
  public void initialize(Map<T, ? extends Map<T, Double>> source) {
    Objects.requireNonNull(source, "adjacency");
    LinkedHashMap<T, Vertex<T>> newVertices = new LinkedHashMap<>();

    for (Map.Entry<T, ? extends Map<T, Double>> entry : source.entrySet()) {
      T fromValue = Objects.requireNonNull(entry.getKey(), "vertex value");
      newVertices.computeIfAbsent(fromValue, Vertex::new);
      Map<T, Double> neighbors = Objects.requireNonNull(entry.getValue(), "neighbors");
      for (Map.Entry<T, Double> neighbor : neighbors.entrySet()) {
        T toValue = Objects.requireNonNull(neighbor.getKey(), "neighbor value");
        requireFinite(Objects.requireNonNull(neighbor.getValue(), "edge weight"));
        newVertices.computeIfAbsent(toValue, Vertex::new);
      }
    }

    LinkedHashMap<Vertex<T>, LinkedHashMap<Vertex<T>, Edge<T>>> newAdjacency =
        new LinkedHashMap<>();
    LinkedHashMap<Long, Edge<T>> newEdges = new LinkedHashMap<>();
    for (Vertex<T> vertex : newVertices.values()) {
      newAdjacency.put(vertex, new LinkedHashMap<>());
    }

    for (Map.Entry<T, ? extends Map<T, Double>> entry : source.entrySet()) {
      Vertex<T> from = newVertices.get(entry.getKey());
      for (Map.Entry<T, Double> neighbor : entry.getValue().entrySet()) {
        Vertex<T> to = newVertices.get(neighbor.getKey());
        double weight = neighbor.getValue();
        Edge<T> previous = newAdjacency.get(from).get(to);
        if (previous != null) {
          if (Double.compare(previous.weight(), weight) != 0) {
            throw new IllegalArgumentException("conflicting weights for undirected edge");
          }
          continue;
        }
        Edge<T> edge = new Edge<>(from, to, weight);
        newEdges.put(edge.id(), edge);
        newAdjacency.get(from).put(to, edge);
        if (!isDirected()) {
          newAdjacency.get(to).put(from, edge);
        }
      }
    }

    verticesByValue.clear();
    verticesByValue.putAll(newVertices);
    adjacency.clear();
    adjacency.putAll(newAdjacency);
    edgesById.clear();
    edgesById.putAll(newEdges);
  }

  @Override
  public Vertex<T> vertex(T value) {
    return verticesByValue.get(value);
  }

  @Override
  public Vertex<T> addVertex(T value) {
    Objects.requireNonNull(value, "value");
    Vertex<T> existing = verticesByValue.get(value);
    if (existing != null) {
      return existing;
    }
    Vertex<T> vertex = new Vertex<>(value);
    verticesByValue.put(value, vertex);
    adjacency.put(vertex, new LinkedHashMap<>());
    StructureEvents.graphVertexAdded(vertex.id(), vertex.value());
    return vertex;
  }

  @Override
  public boolean removeVertex(Vertex<T> vertex) {
    if (!containsVertex(vertex)) {
      return false;
    }
    List<Edge<T>> incident = new ArrayList<>();
    for (Edge<T> edge : edgesById.values()) {
      if (edge.from() == vertex || edge.to() == vertex) {
        incident.add(edge);
      }
    }
    for (Edge<T> edge : incident) {
      removeEdge(edge.from(), edge.to());
    }
    adjacency.remove(vertex);
    verticesByValue.remove(vertex.value());
    StructureEvents.graphVertexRemoved(vertex.id(), vertex.value());
    return true;
  }

  @Override
  public Edge<T> edge(Vertex<T> from, Vertex<T> to) {
    Map<Vertex<T>, Edge<T>> outgoing = adjacency.get(from);
    if (outgoing == null) {
      return null;
    }
    return outgoing.get(to);
  }

  @Override
  public Edge<T> addEdge(Vertex<T> from, Vertex<T> to, double weight) {
    requireVertex(from);
    requireVertex(to);
    requireFinite(weight);
    Edge<T> existing = edge(from, to);
    if (existing != null) {
      setWeight(existing, weight);
      return existing;
    }
    Edge<T> added = new Edge<>(from, to, weight);
    attachEdge(added);
    StructureEvents.graphEdgeAdded(added.id(), from.id(), to.id(), weight);
    return added;
  }

  @Override
  public boolean removeEdge(Vertex<T> from, Vertex<T> to) {
    Edge<T> found = edge(from, to);
    if (found == null) {
      return false;
    }
    adjacency.get(found.from()).remove(found.to());
    if (!isDirected()) {
      adjacency.get(found.to()).remove(found.from());
    }
    edgesById.remove(found.id());
    StructureEvents.graphEdgeRemoved(found.id(), found.from().id(), found.to().id());
    return true;
  }

  @Override
  public boolean containsVertex(Vertex<T> vertex) {
    return vertex != null && adjacency.containsKey(vertex);
  }

  @Override
  public boolean containsEdge(Vertex<T> from, Vertex<T> to) {
    return edge(from, to) != null;
  }

  @Override
  public double weight(Edge<T> edge) {
    requireEdge(edge);
    return edge.weight();
  }

  @Override
  public double setWeight(Edge<T> edge, double weight) {
    requireEdge(edge);
    requireFinite(weight);
    double previous = edge.weight();
    if (Double.compare(previous, weight) != 0) {
      edge.updateWeight(weight);
      StructureEvents.graphEdgeWeightChanged(edge.id(), previous, weight);
    }
    return previous;
  }

  @Override
  public Iterable<Vertex<T>> vertices() {
    return List.copyOf(verticesByValue.values());
  }

  @Override
  public Iterable<Edge<T>> edges() {
    return List.copyOf(edgesById.values());
  }

  @Override
  public Iterable<Vertex<T>> neighbors(Vertex<T> vertex) {
    Map<Vertex<T>, Edge<T>> outgoing = adjacency.get(vertex);
    if (outgoing == null) {
      return List.of();
    }
    return List.copyOf(outgoing.keySet());
  }

  @Override
  public Map<Vertex<T>, Edge<T>> adjacentEdges(Vertex<T> vertex) {
    Map<Vertex<T>, Edge<T>> outgoing = adjacency.get(vertex);
    if (outgoing == null) {
      return Map.of();
    }
    return Collections.unmodifiableMap(new LinkedHashMap<>(outgoing));
  }

  private void attachEdge(Edge<T> edge) {
    edgesById.put(edge.id(), edge);
    adjacency.get(edge.from()).put(edge.to(), edge);
    if (!isDirected()) {
      adjacency.get(edge.to()).put(edge.from(), edge);
    }
  }

  private void requireVertex(Vertex<T> vertex) {
    if (!containsVertex(vertex)) {
      throw new IllegalArgumentException("vertex must belong to this graph");
    }
  }

  private void requireEdge(Edge<T> edge) {
    if (edge == null || edgesById.get(edge.id()) != edge) {
      throw new IllegalArgumentException("edge must belong to this graph");
    }
  }

  private static void requireFinite(double weight) {
    if (!Double.isFinite(weight)) {
      throw new IllegalArgumentException("edge weight must be finite");
    }
  }

  private record EdgeKey(long fromId, long toId) {
    private static EdgeKey of(long fromId, long toId, GraphDirection direction) {
      if (direction.isDirected() || fromId <= toId) {
        return new EdgeKey(fromId, toId);
      }
      return new EdgeKey(toId, fromId);
    }
  }
}
