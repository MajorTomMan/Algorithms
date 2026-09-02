package com.majortom.algorithms.library.structure;

import com.majortom.algorithms.core.runtime.ExecutionEvents;
import com.majortom.algorithms.library.structure.event.GraphStructureEvent;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class MutableGraph<T> implements GraphStructure<T> {
    private final LinkedHashMap<T, LinkedHashSet<T>> adjacency = new LinkedHashMap<>();

    @Override public int size() { return adjacency.size(); }
    @Override public boolean containsVertex(T vertex) { return adjacency.containsKey(vertex); }

    @Override
    public void addVertex(T vertex) {
        Objects.requireNonNull(vertex, "vertex");
        if (adjacency.putIfAbsent(vertex, new LinkedHashSet<>()) == null) {
            ExecutionEvents.emit(new GraphStructureEvent.VertexAdded(vertex));
        }
    }

    @Override
    public boolean removeVertex(T vertex) {
        if (adjacency.remove(vertex) == null) return false;
        for (LinkedHashSet<T> neighbors : adjacency.values()) neighbors.remove(vertex);
        ExecutionEvents.emit(new GraphStructureEvent.VertexRemoved(vertex));
        return true;
    }

    @Override
    public void addEdge(T from, T to) {
        LinkedHashSet<T> neighbors = adjacency.get(from);
        if (neighbors == null || !adjacency.containsKey(to)) throw new IllegalArgumentException("both vertices must exist");
        if (neighbors.add(to)) ExecutionEvents.emit(new GraphStructureEvent.EdgeAdded(from, to));
    }

    @Override
    public boolean removeEdge(T from, T to) {
        LinkedHashSet<T> neighbors = adjacency.get(from);
        if (neighbors == null || !neighbors.remove(to)) return false;
        ExecutionEvents.emit(new GraphStructureEvent.EdgeRemoved(from, to));
        return true;
    }

    @Override
    public Set<T> neighbors(T vertex) {
        LinkedHashSet<T> neighbors = adjacency.get(vertex);
        if (neighbors == null) return Set.of();
        return Collections.unmodifiableSet(new LinkedHashSet<>(neighbors));
    }

    @Override
    public Map<T, ? extends Set<T>> raw() {
        return Collections.unmodifiableMap(adjacency);
    }
}
