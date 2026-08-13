package com.majortom.algorithms.library.graph;

import com.majortom.algorithms.core.api.Algorithm;
import com.majortom.algorithms.core.api.AlgorithmContext;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Breadth-first traversal independent of GraphStream. */
public final class GraphBfs implements Algorithm<GraphBfsInput, GraphBfsOutput> {

    @Override
    public GraphBfsOutput run(GraphBfsInput input, AlgorithmContext context) throws InterruptedException {
        context.emit(new GraphBfsEvent.Initialized(input.graph(), input.startNode()));
        Map<Integer, List<Integer>> adjacency = adjacency(input.graph());
        ArrayDeque<Integer> queue = new ArrayDeque<>();
        Set<Integer> discovered = new HashSet<>();
        Map<Integer, Integer> parents = new HashMap<>();
        List<Integer> order = new ArrayList<>();
        List<IntEdge> discoveryEdges = new ArrayList<>();
        queue.add(input.startNode());
        discovered.add(input.startNode());
        context.emit(new GraphBfsEvent.Discovered(input.startNode(), null));
        while (!queue.isEmpty()) {
            context.checkpoint();
            int node = queue.removeFirst();
            order.add(node);
            context.emit(new GraphBfsEvent.Entered(node, parents.get(node)));
            for (int neighbor : adjacency.getOrDefault(node, List.of())) {
                context.checkpoint();
                context.emit(new GraphBfsEvent.EdgeExamined(node, neighbor));
                if (discovered.add(neighbor)) {
                    IntEdge edge = new IntEdge(node, neighbor);
                    discoveryEdges.add(edge);
                    parents.put(neighbor, node);
                    context.emit(new GraphBfsEvent.Discovered(neighbor, node));
                    queue.addLast(neighbor);
                }
            }
            context.emit(new GraphBfsEvent.Visited(node));
        }
        context.emit(new GraphBfsEvent.Completed(order));
        return new GraphBfsOutput(order, discoveryEdges);
    }

    private Map<Integer, List<Integer>> adjacency(IntGraph graph) {
        Map<Integer, List<Integer>> adjacency = new HashMap<>();
        for (int node : graph.nodes()) {
            adjacency.put(node, new ArrayList<>());
        }
        for (IntEdge edge : graph.edges()) {
            adjacency.get(edge.from()).add(edge.to());
        }
        return adjacency;
    }
}
