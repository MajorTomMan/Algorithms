package com.majortom.algorithms.visualization.module;

import com.majortom.algorithms.core.graph.algorithms.BFSAlgorithms;
import com.majortom.algorithms.core.graph.impl.DirectedGraph;
import com.majortom.algorithms.core.maze.algorithms.generate.BFSMazeGenerator;
import com.majortom.algorithms.core.maze.impl.ArrayMaze;
import com.majortom.algorithms.core.tree.BaseTree;
import com.majortom.algorithms.core.tree.alg.AVLTreeAlgorithms;
import com.majortom.algorithms.core.tree.impl.AVLTreeEntity;
import com.majortom.algorithms.utils.AlgorithmsUtils;
import com.majortom.algorithms.visualization.impl.controller.GraphController;
import com.majortom.algorithms.visualization.impl.controller.MazeController;
import com.majortom.algorithms.visualization.impl.controller.SortController;
import com.majortom.algorithms.visualization.impl.controller.TreeController;
import com.majortom.algorithms.visualization.impl.visualizer.HistogramSortVisualizer;
import com.majortom.algorithms.visualization.impl.visualizer.SquareMazeVisualizer;

import java.util.List;

public final class ModuleRegistry {

    private ModuleRegistry() {
    }

    public static List<AlgorithmModuleDefinition> defaults() {
        return List.of(
                new AlgorithmModuleDefinition(
                        "sort",
                        "module.sort",
                        () -> new SortController<Integer>(new HistogramSortVisualizer<>())),
                new AlgorithmModuleDefinition(
                        "maze",
                        "module.maze",
                        () -> {
                            ArrayMaze maze = new ArrayMaze(51, 51);
                            BFSMazeGenerator generator = new BFSMazeGenerator();
                            generator.setMazeEntity(maze);
                            return new MazeController<>(maze, generator, new SquareMazeVisualizer());
                        }),
                new AlgorithmModuleDefinition(
                        "tree",
                        "module.tree",
                        () -> {
                            BaseTree<Integer> tree = new AVLTreeEntity<>();
                            AVLTreeAlgorithms<Integer> algorithms = new AVLTreeAlgorithms<>();
                            Integer[] array = AlgorithmsUtils.randomArray(15, 100);
                            for (Integer item : array) {
                                algorithms.put(tree, item);
                            }
                            return new TreeController<>(tree, algorithms);
                        }),
                new AlgorithmModuleDefinition(
                        "graph",
                        "module.graph",
                        () -> {
                            DirectedGraph<Integer> graph = new DirectedGraph<>("A");
                            AlgorithmsUtils.buildRandomGraph(graph, 10, 15, true);
                            return new GraphController<>(new BFSAlgorithms<>(), graph);
                        }));
    }
}
