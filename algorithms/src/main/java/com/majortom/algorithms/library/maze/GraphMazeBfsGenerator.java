package com.majortom.algorithms.library.maze;

import com.majortom.algorithms.core.runtime.ExecutionEvents;
import com.majortom.algorithms.core.snapshot.GraphSnapshot;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/** Randomized BFS spanning-tree generator retaining the stable graph-generator-bfs ID. */
public final class GraphMazeBfsGenerator implements GraphMazeGenerator<Integer> {

    private static final int[][] DIRECTIONS = {{-1, 0}, {0, 1}, {1, 0}, {0, -1}};

    @Override
    public GraphMazeGenerationOutput generate(GraphMazeGenerationInput input) {
        List<GraphSnapshot.Vertex<Integer>> vertices = vertices(input);
        List<GraphSnapshot.Edge> edges = new ArrayList<>();
        Set<Integer> discovered = new HashSet<>();
        ArrayDeque<Integer> queue = new ArrayDeque<>();
        Random random = new Random(input.seed());
        long nextEdgeId = 1L;
        queue.add(0);
        discovered.add(0);
        while (!queue.isEmpty()) {
            int current = queue.removeFirst();
            List<Integer> neighbors = neighbors(input, current);
            Collections.shuffle(neighbors, random);
            for (int neighbor : neighbors) {
                if (!discovered.add(neighbor)) {
                    continue;
                }
                ExecutionEvents.checkpoint();
                edges.add(new GraphSnapshot.Edge(nextEdgeId++, current + 1L, neighbor + 1L));
                edges.add(new GraphSnapshot.Edge(nextEdgeId++, neighbor + 1L, current + 1L));
                queue.addLast(neighbor);
            }
        }
        GraphSnapshot<Integer> graph = new GraphSnapshot<>(true, vertices, edges);
        return new GraphMazeGenerationOutput(input.rows(), input.columns(), graph);
    }

    private List<GraphSnapshot.Vertex<Integer>> vertices(GraphMazeGenerationInput input) {
        int cellCount = MazeDimensions.checkedCellCount(input.rows(), input.columns());
        List<GraphSnapshot.Vertex<Integer>> vertices = new ArrayList<>(cellCount);
        for (int node = 0; node < cellCount; node++) {
            vertices.add(new GraphSnapshot.Vertex<>(node + 1L, node));
        }
        return vertices;
    }

    private List<Integer> neighbors(GraphMazeGenerationInput input, int node) {
        int row = node / input.columns();
        int column = node % input.columns();
        List<Integer> neighbors = new ArrayList<>(4);
        for (int[] direction : DIRECTIONS) {
            int nextRow = row + direction[0];
            int nextColumn = column + direction[1];
            if (nextRow >= 0 && nextColumn >= 0
                    && nextRow < input.rows() && nextColumn < input.columns()) {
                neighbors.add(nextRow * input.columns() + nextColumn);
            }
        }
        return neighbors;
    }
}
