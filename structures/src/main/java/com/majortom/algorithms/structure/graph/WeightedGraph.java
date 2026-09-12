package com.majortom.algorithms.structure.graph;

import com.majortom.algorithms.core.annotation.Structure;
import com.majortom.algorithms.core.runtime.StructureEvents;
import com.majortom.algorithms.core.snapshot.GraphSnapshot;
import com.majortom.algorithms.core.snapshot.WeightedGraphSnapshot;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Weighted graph variant that reuses the canonical Graph topology implementation. */
@Structure(id = "weighted-graph", name = "Weighted Graph", contract = WeightedGraphStructure.class)
public final class WeightedGraph<T> implements WeightedGraphStructure<T> {
    private final Graph<T> graph;
    private final LinkedHashMap<Long, Double> weightsByEdgeId = new LinkedHashMap<>();

    public WeightedGraph() {
        this(false);
    }

    public WeightedGraph(boolean directed) {
        graph = new Graph<>(directed);
    }

    private WeightedGraph(Graph<T> graph, Map<Long, Double> weights) {
        this.graph = Objects.requireNonNull(graph, "graph");
        weightsByEdgeId.putAll(Objects.requireNonNull(weights, "weights"));
    }

    public static <T> WeightedGraph<T> fromSnapshot(WeightedGraphSnapshot<T> snapshot) {
        Objects.requireNonNull(snapshot, "snapshot");
        List<GraphSnapshot.Vertex<T>> vertices = snapshot.vertices().stream()
                .map(vertex -> new GraphSnapshot.Vertex<>(vertex.id(), vertex.value()))
                .toList();
        List<GraphSnapshot.Edge> edges = snapshot.edges().stream()
                .map(edge -> new GraphSnapshot.Edge(edge.id(), edge.fromId(), edge.toId()))
                .toList();
        Graph<T> topology = Graph.fromSnapshot(new GraphSnapshot<>(snapshot.directed(), vertices, edges));
        LinkedHashMap<Long, Double> weights = new LinkedHashMap<>();
        for (WeightedGraphSnapshot.Edge edge : snapshot.edges()) {
            weights.put(edge.id(), edge.weight());
        }
        return new WeightedGraph<>(topology, weights);
    }

    public WeightedGraphSnapshot<T> snapshot() {
        List<WeightedGraphSnapshot.Vertex<T>> vertices = new ArrayList<>();
        for (Vertex<T> vertex : graph.vertices()) {
            vertices.add(new WeightedGraphSnapshot.Vertex<>(vertex.id(), vertex.value()));
        }
        List<WeightedGraphSnapshot.Edge> edges = new ArrayList<>();
        for (Edge<T> edge : graph.edges()) {
            edges.add(new WeightedGraphSnapshot.Edge(
                    edge.id(), edge.from().id(), edge.to().id(), weight(edge)));
        }
        return new WeightedGraphSnapshot<>(isDirected(), vertices, edges);
    }

    @Override
    public int vertexCount() {
        return graph.vertexCount();
    }

    @Override
    public void initialize(Map<T, ? extends Collection<T>> adjacency) {
        graph.initialize(adjacency);
        weightsByEdgeId.clear();
        for (Edge<T> edge : graph.edges()) {
            weightsByEdgeId.put(edge.id(), 1.0d);
        }
    }

    @Override
    public void initializeWeighted(Map<T, ? extends Map<T, Double>> adjacency) {
        Objects.requireNonNull(adjacency, "adjacency");
        LinkedHashMap<T, Collection<T>> topology = new LinkedHashMap<>();
        for (Map.Entry<T, ? extends Map<T, Double>> entry : adjacency.entrySet()) {
            Map<T, Double> neighbors = Objects.requireNonNull(entry.getValue(), "neighbors");
            topology.put(entry.getKey(), List.copyOf(neighbors.keySet()));
        }
        graph.initialize(topology);
        weightsByEdgeId.clear();
        for (Edge<T> edge : graph.edges()) {
            Double weight = lookupWeight(adjacency, edge.from().value(), edge.to().value());
            if (weight == null && !graph.isDirected()) {
                weight = lookupWeight(adjacency, edge.to().value(), edge.from().value());
            }
            if (weight == null) {
                throw new IllegalArgumentException("weighted adjacency is missing edge weight for "
                        + edge.from().value() + " -> " + edge.to().value());
            }
            requireFinite(weight);
            weightsByEdgeId.put(edge.id(), weight);
        }
    }

    @Override
    public int edgeCount() {
        return graph.edgeCount();
    }

    @Override
    public boolean isDirected() {
        return graph.isDirected();
    }

    @Override
    public Vertex<T> vertex(T value) {
        return graph.vertex(value);
    }

    @Override
    public Vertex<T> addVertex(T value) {
        return graph.addVertex(value);
    }

    @Override
    public boolean removeVertex(Vertex<T> vertex) {
        List<Long> removedEdgeIds = new ArrayList<>();
        for (Edge<T> edge : graph.edges()) {
            if (edge.from() == vertex || edge.to() == vertex) {
                removedEdgeIds.add(edge.id());
            }
        }
        boolean removed = graph.removeVertex(vertex);
        if (removed) {
            for (Long edgeId : removedEdgeIds) {
                weightsByEdgeId.remove(edgeId);
            }
        }
        return removed;
    }

    @Override
    public Edge<T> addEdge(Vertex<T> from, Vertex<T> to, double weight) {
        requireFinite(weight);
        Edge<T> existing = edge(from, to);
        if (existing != null) {
            setWeight(existing, weight);
            return existing;
        }
        Edge<T> added = graph.addEdge(from, to);
        weightsByEdgeId.put(added.id(), weight);
        StructureEvents.graphEdgeWeightChanged(added.id(), null, weight);
        return added;
    }

    @Override
    public boolean removeEdge(Vertex<T> from, Vertex<T> to) {
        Edge<T> existing = edge(from, to);
        if (existing == null) {
            return false;
        }
        boolean removed = graph.removeEdge(from, to);
        if (removed) {
            weightsByEdgeId.remove(existing.id());
        }
        return removed;
    }

    @Override
    public boolean containsVertex(Vertex<T> vertex) {
        return graph.containsVertex(vertex);
    }

    @Override
    public boolean containsEdge(Vertex<T> from, Vertex<T> to) {
        return graph.containsEdge(from, to);
    }

    @Override
    public Iterable<Vertex<T>> vertices() {
        return graph.vertices();
    }

    @Override
    public Iterable<Edge<T>> edges() {
        return graph.edges();
    }

    @Override
    public Iterable<Vertex<T>> neighbors(Vertex<T> vertex) {
        return graph.neighbors(vertex);
    }

    @Override
    public double weight(Edge<T> edge) {
        Objects.requireNonNull(edge, "edge");
        Double weight = weightsByEdgeId.get(edge.id());
        if (weight == null || !containsEdgeInstance(edge)) {
            throw new IllegalArgumentException("edge must belong to this weighted graph");
        }
        return weight;
    }

    @Override
    public double setWeight(Edge<T> edge, double weight) {
        Objects.requireNonNull(edge, "edge");
        requireFinite(weight);
        if (!containsEdgeInstance(edge)) {
            throw new IllegalArgumentException("edge must belong to this weighted graph");
        }
        Double previous = weightsByEdgeId.put(edge.id(), weight);
        if (previous == null || Double.compare(previous, weight) != 0) {
            StructureEvents.graphEdgeWeightChanged(edge.id(), previous, weight);
        }
        if (previous == null) {
            return weight;
        }
        return previous;
    }

    public Edge<T> edge(Vertex<T> from, Vertex<T> to) {
        if (from == null || to == null) {
            return null;
        }
        for (Edge<T> edge : graph.edges()) {
            if (edge.from() == from && edge.to() == to) {
                return edge;
            }
            if (!graph.isDirected() && edge.from() == to && edge.to() == from) {
                return edge;
            }
        }
        return null;
    }

    private static <T> Double lookupWeight(
            Map<T, ? extends Map<T, Double>> adjacency, T from, T to) {
        Map<T, Double> neighbors = adjacency.get(from);
        if (neighbors == null) {
            return null;
        }
        return neighbors.get(to);
    }

    private boolean containsEdgeInstance(Edge<T> target) {
        for (Edge<T> edge : graph.edges()) {
            if (edge == target) {
                return true;
            }
        }
        return false;
    }

    private static void requireFinite(double weight) {
        if (!Double.isFinite(weight)) {
            throw new IllegalArgumentException("edge weight must be finite");
        }
    }
}
