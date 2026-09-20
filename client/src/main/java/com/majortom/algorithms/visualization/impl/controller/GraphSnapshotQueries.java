package com.majortom.algorithms.visualization.impl.controller;

import com.majortom.algorithms.core.snapshot.GraphSnapshot;
import com.majortom.algorithms.core.snapshot.GraphSnapshotState;
import com.majortom.algorithms.core.snapshot.WeightedGraphSnapshot;
import com.majortom.algorithms.structure.graph.GraphStructure;
import com.majortom.algorithms.structure.graph.WeightedGraph;
import java.util.ArrayList;
import java.util.List;

/** Read-only graph snapshot conversion and node/edge queries, independent of JavaFX selection. */
final class GraphSnapshotQueries {
    private GraphSnapshotQueries() {}

    record SnapshotEdge(long id, long fromId, long toId, Double weight) {
    }

    static GraphStructure<Object> graphFromSnapshot(GraphSnapshotState<Object> snapshot) {
        return WeightedGraph.fromSnapshot(asWeightedSnapshot(snapshot));
    }

    static WeightedGraphSnapshot<Object> asWeightedSnapshot(GraphSnapshotState<Object> snapshot) {
        if (snapshot instanceof WeightedGraphSnapshot<?> weighted) {
            @SuppressWarnings("unchecked")
            WeightedGraphSnapshot<Object> typed = (WeightedGraphSnapshot<Object>) weighted;
            return typed;
        }
        if (snapshot instanceof GraphSnapshot<?> basic) {
            @SuppressWarnings("unchecked")
            GraphSnapshot<Object> typed = (GraphSnapshot<Object>) basic;
            List<WeightedGraphSnapshot.Vertex<Object>> vertices = typed.vertices().stream()
                    .map(vertex -> new WeightedGraphSnapshot.Vertex<>(vertex.id(), vertex.value()))
                    .toList();
            List<WeightedGraphSnapshot.Edge> edges = typed.edges().stream()
                    .map(edge -> new WeightedGraphSnapshot.Edge(
                            edge.id(), edge.fromId(), edge.toId(), 1.0d))
                    .toList();
            return new WeightedGraphSnapshot<>(typed.directed(), vertices, edges);
        }
        throw new IllegalArgumentException("unsupported graph snapshot type: " + snapshot.getClass().getName());
    }

    static List<Long> snapshotVertexIds(GraphSnapshotState<Object> snapshot) {
        List<Long> ids = new ArrayList<>();
        if (snapshot instanceof GraphSnapshot<?> basic) {
            for (GraphSnapshot.Vertex<?> vertex : basic.vertices()) {
                ids.add(vertex.id());
            }
        } else if (snapshot instanceof WeightedGraphSnapshot<?> weighted) {
            for (WeightedGraphSnapshot.Vertex<?> vertex : weighted.vertices()) {
                ids.add(vertex.id());
            }
        }
        return List.copyOf(ids);
    }

    static List<Long> snapshotEdgeIds(GraphSnapshotState<Object> snapshot) {
        List<Long> ids = new ArrayList<>();
        if (snapshot instanceof GraphSnapshot<?> basic) {
            for (GraphSnapshot.Edge edge : basic.edges()) {
                ids.add(edge.id());
            }
        } else if (snapshot instanceof WeightedGraphSnapshot<?> weighted) {
            for (WeightedGraphSnapshot.Edge edge : weighted.edges()) {
                ids.add(edge.id());
            }
        }
        return List.copyOf(ids);
    }

    static Long snapshotEdgeIdBetween(GraphSnapshotState<Object> snapshot, long fromId, long toId) {
        if (snapshot instanceof GraphSnapshot<?> basic) {
            for (GraphSnapshot.Edge edge : basic.edges()) {
                if (edgeConnects(snapshot.directed(), edge.fromId(), edge.toId(), fromId, toId)) {
                    return edge.id();
                }
            }
        } else if (snapshot instanceof WeightedGraphSnapshot<?> weighted) {
            for (WeightedGraphSnapshot.Edge edge : weighted.edges()) {
                if (edgeConnects(snapshot.directed(), edge.fromId(), edge.toId(), fromId, toId)) {
                    return edge.id();
                }
            }
        }
        return null;
    }

    static boolean edgeConnects(boolean directed, long edgeFrom, long edgeTo, long fromId, long toId) {
        if (edgeFrom == fromId && edgeTo == toId) {
            return true;
        }
        if (!directed && edgeFrom == toId && edgeTo == fromId) {
            return true;
        }
        return false;
    }

    static Object snapshotVertexValue(GraphSnapshotState<Object> snapshot, long nodeId) {
        if (snapshot instanceof GraphSnapshot<?> basic) {
            for (GraphSnapshot.Vertex<?> vertex : basic.vertices()) {
                if (vertex.id() == nodeId) {
                    return vertex.value();
                }
            }
            return null;
        }
        if (snapshot instanceof WeightedGraphSnapshot<?> weighted) {
            for (WeightedGraphSnapshot.Vertex<?> vertex : weighted.vertices()) {
                if (vertex.id() == nodeId) {
                    return vertex.value();
                }
            }
            return null;
        }
        return null;
    }

    static int snapshotDegree(GraphSnapshotState<Object> snapshot, long nodeId) {
        int degree = 0;
        if (snapshot instanceof GraphSnapshot<?> basic) {
            for (GraphSnapshot.Edge edge : basic.edges()) {
                if (edge.fromId() == nodeId || edge.toId() == nodeId) {
                    degree++;
                }
            }
            return degree;
        }
        if (snapshot instanceof WeightedGraphSnapshot<?> weighted) {
            for (WeightedGraphSnapshot.Edge edge : weighted.edges()) {
                if (edge.fromId() == nodeId || edge.toId() == nodeId) {
                    degree++;
                }
            }
        }
        return degree;
    }

    static SnapshotEdge snapshotEdge(GraphSnapshotState<Object> snapshot, long edgeId) {
        if (snapshot instanceof GraphSnapshot<?> basic) {
            for (GraphSnapshot.Edge edge : basic.edges()) {
                if (edge.id() == edgeId) {
                    return new SnapshotEdge(edge.id(), edge.fromId(), edge.toId(), null);
                }
            }
            return null;
        }
        if (snapshot instanceof WeightedGraphSnapshot<?> weighted) {
            for (WeightedGraphSnapshot.Edge edge : weighted.edges()) {
                if (edge.id() == edgeId) {
                    return new SnapshotEdge(edge.id(), edge.fromId(), edge.toId(), edge.weight());
                }
            }
        }
        return null;
    }

}
