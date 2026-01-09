package com.majortom.algorithms;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import com.majortom.algorithms.core.graph.BaseGraph;
import com.majortom.algorithms.core.graph.algorithms.BFSAlgorithms;
import com.majortom.algorithms.core.graph.impl.DirectedGraph;
import com.majortom.algorithms.core.graph.impl.UndirectedGraph;
import com.majortom.algorithms.core.maze.algorithms.generate.BFSMazeGenerator;
import com.majortom.algorithms.core.maze.algorithms.generate.DFSMazeGenerator;
import com.majortom.algorithms.core.maze.algorithms.pathfinding.BFSMazePathfinder;
import com.majortom.algorithms.core.maze.algorithms.pathfinding.DFSMazePathfinder;
import com.majortom.algorithms.core.maze.impl.ArrayMaze;
import com.majortom.algorithms.core.sort.impl.InsertionSort;
import com.majortom.algorithms.core.tree.impl.AVLTree;
import com.majortom.algorithms.core.visualization.impl.frame.GraphFrame;
import com.majortom.algorithms.core.visualization.impl.frame.MazeFrame;
import com.majortom.algorithms.core.visualization.impl.frame.SortFrame;
import com.majortom.algorithms.core.visualization.impl.frame.TreeFrame;
import com.majortom.algorithms.utils.AlgorithmsUtils;

public class AppTest {
    @Test
    public void testSort() {
        Integer[] rawData = AlgorithmsUtils.randomArray(100, 12);
        int[] dataForInsertion = AlgorithmsUtils.toPrimitive(rawData);

        int[] dataForShell = dataForInsertion.clone();
        SortFrame.launch(new InsertionSort(), dataForInsertion);
        // SortFrame.launch(new ShellSort(), dataForShell);
    }

    @Test
    public void testUndirectedGraph() {
        BaseGraph<String> graph = new UndirectedGraph<>();
        AlgorithmsUtils.buildRandomGraph(graph, 10, 15, true);

        BFSAlgorithms<String> bfs = new BFSAlgorithms<>();

        GraphFrame.launch(graph, bfs, "A");
    }

    @Test
    public void testDirectedGraph() {
        BaseGraph<String> graph = new DirectedGraph<>();
        AlgorithmsUtils.buildRandomGraph(graph, 10, 15, true);
        BFSAlgorithms<String> bfs = new BFSAlgorithms<>();
        GraphFrame.launch(graph, bfs, "A");
    }

    @Test
    public void testTree() {
        AVLTree<Integer> avl = new AVLTree<>();
        Integer[] data = AlgorithmsUtils.randomArray(10, 10);
        TreeFrame.launch(avl, data);
    }

    @Test
    public void testMaze() {
        ArrayMaze container = new ArrayMaze(31, 31);

        BFSMazeGenerator generator = new BFSMazeGenerator();
        DFSMazePathfinder pathfinder = new DFSMazePathfinder();
        //BFSMazePathfinder pathfinder = new BFSMazePathfinder();
        MazeFrame.launch(container, 20, generator, pathfinder);
    }

    @AfterEach
    public void tearDown() {
        System.out.println("Press Enter to continue...");

        try {
            System.in.read();
        } catch (Exception e) {
        }
    }
}