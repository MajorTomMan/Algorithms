package com.majortom.algorithms.visualization.impl.controller;

import com.majortom.algorithms.core.metadata.GraphDirection;
import com.majortom.algorithms.core.snapshot.GraphSnapshot;
import com.majortom.algorithms.structure.graph.Graph;
import java.util.ArrayList;
import java.util.List;

/** Read-only graph snapshot queries, independent of JavaFX selection. */
final class GraphSnapshotQueries {
    private GraphSnapshotQueries() {}

    record SnapshotEdge(long id, long fromId, long toId, double weight) {}

    static Graph<Object> graphFromSnapshot(GraphSnapshot<Object> snapshot) {
        return Graph.fromSnapshot(snapshot);
    }

    static List<Long> snapshotVertexIds(GraphSnapshot<Object> snapshot) {
        List<Long> ids = new ArrayList<>();
        for (GraphSnapshot.Vertex<Object> vertex : snapshot.vertices()) {
            ids.add(vertex.id());
        }
        return List.copyOf(ids);
    }

    static List<Long> snapshotEdgeIds(GraphSnapshot<Object> snapshot) {
        List<Long> ids = new ArrayList<>();
        for (GraphSnapshot.Edge edge : snapshot.edges()) {
            ids.add(edge.id());
        }
        return List.copyOf(ids);
    }

    static Long snapshotEdgeIdBetween(GraphSnapshot<Object> snapshot, long fromId, long toId) {
        for (GraphSnapshot.Edge edge : snapshot.edges()) {
            if (edgeConnects(snapshot.direction(), edge.fromId(), edge.toId(), fromId, toId)) {
                return edge.id();
            }
        }
        return null;
    }

    static boolean edgeConnects(GraphDirection direction, long edgeFrom, long edgeTo,
                                long fromId, long toId) {
        if (edgeFrom == fromId && edgeTo == toId) {
            return true;
        }
        if (direction == GraphDirection.UNDIRECTED && edgeFrom == toId && edgeTo == fromId) {
            return true;
        }
        return false;
    }

    static Object snapshotVertexValue(GraphSnapshot<Object> snapshot, long nodeId) {
        for (GraphSnapshot.Vertex<Object> vertex : snapshot.vertices()) {
            if (vertex.id() == nodeId) {
                return vertex.value();
            }
        }
        return null;
    }

    static int snapshotDegree(GraphSnapshot<Object> snapshot, long nodeId) {
        int degree = 0;
        for (GraphSnapshot.Edge edge : snapshot.edges()) {
            if (edge.fromId() == nodeId || edge.toId() == nodeId) {
                degree++;
            }
        }
        return degree;
    }

    static SnapshotEdge snapshotEdge(GraphSnapshot<Object> snapshot, long edgeId) {
        for (GraphSnapshot.Edge edge : snapshot.edges()) {
            if (edge.id() == edgeId) {
                return new SnapshotEdge(edge.id(), edge.fromId(), edge.toId(), edge.weight());
            }
        }
        return null;
    }
}
