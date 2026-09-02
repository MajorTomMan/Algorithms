package com.majortom.algorithms.library.maze;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/** Randomized Kruskal/union-find perfect-maze generator. */
public final class UnionFindArrayMazeGenerator implements ArrayMazeGenerator {
    @Override
    public ArrayMazeGenerationOutput generate(ArrayMazeGenerationInput input) {
        Random random = new Random(input.seed());
        ArrayMazeSupport.GenerationState state = ArrayMazeSupport.initialize(input);
        List<GridPoint> cells = logicalCells(input);
        for (GridPoint cell : cells) ArrayMazeSupport.open(input, state.open(), cell);
        List<CellEdge> edges = new ArrayList<>();
        for (GridPoint cell : cells) {
            addEdgeIfInside(input, edges, cell, 0, 2);
            addEdgeIfInside(input, edges, cell, 2, 0);
        }
        Collections.shuffle(edges, random);
        DisjointSet sets = new DisjointSet(input.rows() * input.columns());
        for (CellEdge edge : edges) {
            int left = ArrayMazeSupport.index(input.columns(), edge.left());
            int right = ArrayMazeSupport.index(input.columns(), edge.right());
            if (!sets.union(left, right)) continue;
            GridPoint corridor = new GridPoint(
                    (edge.left().row() + edge.right().row()) / 2,
                    (edge.left().column() + edge.right().column()) / 2);
            ArrayMazeSupport.open(input, state.open(), corridor);
        }
        return new ArrayMazeGenerationOutput(ArrayMazeSupport.complete(input, state));
    }

    private List<GridPoint> logicalCells(ArrayMazeGenerationInput input) {
        List<GridPoint> cells = new ArrayList<>();
        for (int row = 1; row < input.rows(); row += 2) {
            for (int column = 1; column < input.columns(); column += 2) cells.add(new GridPoint(row, column));
        }
        return cells;
    }

    private void addEdgeIfInside(ArrayMazeGenerationInput input, List<CellEdge> edges, GridPoint left, int rowDelta, int columnDelta) {
        int rightRow = left.row() + rowDelta;
        int rightColumn = left.column() + columnDelta;
        if (ArrayMazeSupport.isInner(input, rightRow, rightColumn)) {
            edges.add(new CellEdge(left, new GridPoint(rightRow, rightColumn)));
        }
    }

    private record CellEdge(GridPoint left, GridPoint right) {}

    private static final class DisjointSet {
        private final int[] parent;
        private final byte[] rank;

        private DisjointSet(int size) {
            parent = new int[size];
            rank = new byte[size];
            for (int index = 0; index < size; index++) parent[index] = index;
        }

        private boolean union(int left, int right) {
            int leftRoot = find(left);
            int rightRoot = find(right);
            if (leftRoot == rightRoot) return false;
            if (rank[leftRoot] < rank[rightRoot]) parent[leftRoot] = rightRoot;
            else if (rank[leftRoot] > rank[rightRoot]) parent[rightRoot] = leftRoot;
            else { parent[rightRoot] = leftRoot; rank[leftRoot]++; }
            return true;
        }

        private int find(int value) {
            int root = value;
            while (parent[root] != root) root = parent[root];
            int current = value;
            while (parent[current] != current) {
                int next = parent[current];
                parent[current] = root;
                current = next;
            }
            return root;
        }
    }
}
