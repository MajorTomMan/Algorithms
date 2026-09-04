package com.majortom.algorithms.library.basic.graph;

import com.majortom.algorithms.core.event.structure.GraphStructureEvent;
import com.majortom.algorithms.core.runtime.ExecutionEvents;
import com.majortom.algorithms.core.snapshot.GraphSnapshot;
import com.majortom.algorithms.library.structure.GraphStructure;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class Graph<T> implements GraphStructure<T> {
    private final boolean directed;
    private final LinkedHashMap<T, Vertex<T>> verticesByValue = new LinkedHashMap<>();
    private final LinkedHashMap<Vertex<T>, LinkedHashSet<Vertex<T>>> adjacency = new LinkedHashMap<>();
    private final LinkedHashSet<Edge<T>> edges = new LinkedHashSet<>();

    public Graph() {
        this(false);
    }

    public Graph(boolean directed) {
        this.directed = directed;
    }

    public static <T> Graph<T> fromSnapshot(GraphSnapshot<T> snapshot) {
        Objects.requireNonNull(snapshot, "snapshot");
        Graph<T> graph = new Graph<>(snapshot.directed());
        Map<Long, Vertex<T>> verticesById = new LinkedHashMap<>();
        Set<T> values = new HashSet<>();
        for (GraphSnapshot.Vertex<T> source : snapshot.vertices()) {
            if (verticesById.containsKey(source.id())) {
                throw new IllegalArgumentException("snapshot contains duplicate vertex id: " + source.id());
            }
            if (!values.add(source.value())) {
                throw new IllegalArgumentException("snapshot contains duplicate vertex value: " + source.value());
            }
            Vertex<T> vertex = new Vertex<>(source.id(), source.value());
            graph.verticesByValue.put(vertex.value(), vertex);
            graph.adjacency.put(vertex, new LinkedHashSet<>());
            verticesById.put(vertex.id(), vertex);
        }

        Set<Long> edgeIds = new HashSet<>();
        Set<EdgeKey> edgeKeys = new HashSet<>();
        for (GraphSnapshot.Edge source : snapshot.edges()) {
            if (!edgeIds.add(source.id())) {
                throw new IllegalArgumentException("snapshot contains duplicate edge id: " + source.id());
            }
            Vertex<T> from = verticesById.get(source.fromId());
            Vertex<T> to = verticesById.get(source.toId());
            if (from == null || to == null) {
                throw new IllegalArgumentException("snapshot edge references an unknown vertex");
            }
            EdgeKey key = EdgeKey.of(source.fromId(), source.toId(), snapshot.directed());
            if (!edgeKeys.add(key)) {
                throw new IllegalArgumentException("snapshot contains parallel edge: "
                        + source.fromId() + " -> " + source.toId());
            }
            Edge<T> edge = new Edge<>(source.id(), from, to);
            graph.edges.add(edge);
            graph.adjacency.get(from).add(to);
            if (!graph.directed) {
                graph.adjacency.get(to).add(from);
            }
        }
        return graph;
    }

    private record EdgeKey(long fromId, long toId) {
        private static EdgeKey of(long fromId, long toId, boolean directed) {
            if (directed || fromId <= toId) {
                return new EdgeKey(fromId, toId);
            }
            return new EdgeKey(toId, fromId);
        }
    }

    @Override
    public int vertexCount() {
        return adjacency.size();
    }

    @Override
    public int edgeCount() {
        return edges.size();
    }

    @Override
    public boolean isDirected() {
        return directed;
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
        adjacency.put(vertex, new LinkedHashSet<>());
        ExecutionEvents.emit(new GraphStructureEvent.VertexAdded(vertex.id(), vertex.value()));
        return vertex;
    }

    @Override
    public boolean removeVertex(Vertex<T> vertex) {
        if (!containsVertex(vertex)) {
            return false;
        }
        List<Edge<T>> incidentEdges = new ArrayList<>();
        for (Edge<T> edge : edges) {
            if (edge.from() == vertex || edge.to() == vertex) {
                incidentEdges.add(edge);
            }
        }
        for (Edge<T> edge : incidentEdges) {
            removeEdge(edge.from(), edge.to());
        }
        adjacency.remove(vertex);
        verticesByValue.remove(vertex.value());
        for (Set<Vertex<T>> neighbors : adjacency.values()) {
            neighbors.remove(vertex);
        }
        ExecutionEvents.emit(new GraphStructureEvent.VertexRemoved(vertex.id(), vertex.value()));
        return true;
    }

    @Override
    public Edge<T> addEdge(Vertex<T> from, Vertex<T> to) {
        requireVertex(from);
        requireVertex(to);
        Edge<T> existing = findEdge(from, to);
        if (existing != null) {
            return existing;
        }
        Edge<T> edge = new Edge<>(from, to);
        edges.add(edge);
        adjacency.get(from).add(to);
        if (!directed) {
            adjacency.get(to).add(from);
        }
        ExecutionEvents.emit(new GraphStructureEvent.EdgeAdded(edge.id(), from.id(), to.id()));
        return edge;
    }

    @Override
    public boolean removeEdge(Vertex<T> from, Vertex<T> to) {
        Edge<T> edge = findEdge(from, to);
        if (edge == null) {
            return false;
        }
        edges.remove(edge);
        adjacency.get(from).remove(to);
        if (!directed) {
            adjacency.get(to).remove(from);
        }
        ExecutionEvents.emit(new GraphStructureEvent.EdgeRemoved(edge.id(), edge.from().id(), edge.to().id()));
        return true;
    }

    @Override
    public boolean containsVertex(Vertex<T> vertex) {
        return vertex != null && adjacency.containsKey(vertex);
    }

    @Override
    public boolean containsEdge(Vertex<T> from, Vertex<T> to) {
        return findEdge(from, to) != null;
    }

    @Override
    public Iterable<Vertex<T>> vertices() {
        return List.copyOf(adjacency.keySet());
    }

    @Override
    public Iterable<Edge<T>> edges() {
        return List.copyOf(edges);
    }

    @Override
    public Iterable<Vertex<T>> neighbors(Vertex<T> vertex) {
        Set<Vertex<T>> neighbors = adjacency.get(vertex);
        if (neighbors == null) {
            return List.of();
        }
        return Collections.unmodifiableList(new ArrayList<>(neighbors));
    }

    public Vertex<T> vertex(T value) {
        return verticesByValue.get(value);
    }

    private Edge<T> findEdge(Vertex<T> from, Vertex<T> to) {
        if (from == null || to == null) {
            return null;
        }
        for (Edge<T> edge : edges) {
            if (edge.from() == from && edge.to() == to) {
                return edge;
            }
            if (!directed && edge.from() == to && edge.to() == from) {
                return edge;
            }
        }
        return null;
    }

    private void requireVertex(Vertex<T> vertex) {
        if (!containsVertex(vertex)) {
            throw new IllegalArgumentException("vertex must belong to this graph");
        }
    }
}
