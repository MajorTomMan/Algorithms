package com.majortom.algorithms.library.maze;

import com.majortom.algorithms.core.api.Algorithm;
import com.majortom.algorithms.core.api.AlgorithmContext;
import com.majortom.algorithms.library.graph.IntEdge;
import com.majortom.algorithms.library.graph.IntGraph;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/** Randomized BFS spanning-tree generator retaining the stable graph-generator-bfs ID. */
public final class GraphMazeBfsGenerator
        implements Algorithm<GraphMazeGenerationInput, GraphMazeGenerationOutput> {

    private static final int[][] DIRECTIONS = {{-1, 0}, {0, 1}, {1, 0}, {0, -1}};

    @Override
    public GraphMazeGenerationOutput run(GraphMazeGenerationInput input, AlgorithmContext context)
            throws InterruptedException {
        context.emit(new GraphMazeGenerationEvent.Initialized(input.rows(), input.columns()));
        List<Integer> nodes = nodes(input);
        List<IntEdge> edges = new ArrayList<>();
        Set<Integer> discovered = new HashSet<>();
        ArrayDeque<Integer> queue = new ArrayDeque<>();
        Random random = new Random(input.seed());
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
                context.checkpoint();
                IntEdge forward = new IntEdge(current, neighbor);
                IntEdge reverse = new IntEdge(neighbor, current);
                edges.add(forward);
                edges.add(reverse);
                context.emit(new GraphMazeGenerationEvent.EdgeAdded(forward));
                context.emit(new GraphMazeGenerationEvent.EdgeAdded(reverse));
                queue.addLast(neighbor);
            }
        }
        IntGraph graph = new IntGraph(nodes, edges);
        context.emit(new GraphMazeGenerationEvent.Completed(graph));
        return new GraphMazeGenerationOutput(input.rows(), input.columns(), graph);
    }

    private List<Integer> nodes(GraphMazeGenerationInput input) {
        int cellCount = MazeDimensions.checkedCellCount(input.rows(), input.columns());
        List<Integer> nodes = new ArrayList<>(cellCount);
        for (int node = 0; node < cellCount; node++) {
            nodes.add(node);
        }
        return nodes;
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
