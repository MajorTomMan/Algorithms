package com.majortom.algorithms.library.graph;

import com.majortom.algorithms.core.runtime.ExecutionEvents;
import com.majortom.algorithms.library.structure.GraphStructure;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** Breadth-first traversal over the graph-domain contract. */
public final class GraphBfs {

    public GraphBfsOutput traverse(GraphStructure<Integer> graph, int startNode) {
        Objects.requireNonNull(graph, "graph");
        if (!graph.containsVertex(startNode)) {
            throw new IllegalArgumentException("startNode must exist in graph");
        }
        IntGraph snapshot = snapshot(graph);
        ExecutionEvents.emit(new GraphBfsEvent.Initialized(snapshot, startNode));
        ArrayDeque<Integer> queue = new ArrayDeque<>();
        Set<Integer> discovered = new HashSet<>();
        Map<Integer, Integer> parents = new HashMap<>();
        List<Integer> order = new ArrayList<>();
        List<IntEdge> discoveryEdges = new ArrayList<>();
        queue.add(startNode);
        discovered.add(startNode);
        ExecutionEvents.emit(new GraphBfsEvent.Discovered(startNode, null));
        while (!queue.isEmpty()) {
            ExecutionEvents.checkpoint();
            int node = queue.removeFirst();
            order.add(node);
            ExecutionEvents.emit(new GraphBfsEvent.Entered(node, parents.get(node)));
            for (int neighbor : graph.neighbors(node)) {
                ExecutionEvents.checkpoint();
                ExecutionEvents.emit(new GraphBfsEvent.EdgeExamined(node, neighbor));
                if (discovered.add(neighbor)) {
                    IntEdge edge = new IntEdge(node, neighbor);
                    discoveryEdges.add(edge);
                    parents.put(neighbor, node);
                    ExecutionEvents.emit(new GraphBfsEvent.Discovered(neighbor, node));
                    queue.addLast(neighbor);
                }
            }
            ExecutionEvents.emit(new GraphBfsEvent.Visited(node));
        }
        ExecutionEvents.emit(new GraphBfsEvent.Completed(order));
        return new GraphBfsOutput(order, discoveryEdges);
    }

    public static IntGraph snapshot(GraphStructure<Integer> graph) {
        Objects.requireNonNull(graph, "graph");
        List<Integer> nodes = new ArrayList<>(graph.raw().keySet());
        List<IntEdge> edges = new ArrayList<>();
        for (Integer node : nodes) {
            for (Integer neighbor : graph.neighbors(node)) {
                edges.add(new IntEdge(node, neighbor));
            }
        }
        return new IntGraph(nodes, edges);
    }
}
