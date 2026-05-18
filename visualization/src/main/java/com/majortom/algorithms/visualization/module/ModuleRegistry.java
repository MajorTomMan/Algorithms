package com.majortom.algorithms.visualization.module;

import com.majortom.algorithms.core.graph.algorithms.BFSAlgorithms;
import com.majortom.algorithms.core.graph.impl.DirectedGraph;
import com.majortom.algorithms.core.maze.algorithms.array.generate.BFSArrayMazeGenerator;
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
import com.majortom.algorithms.visualization.impl.visualizer.MazeModuleVisualizer;

import java.util.List;

/**
 * 可视化模块注册表。
 *
 * <p>
 * 主界面通过这里拿到默认模块列表，再根据 {@link AlgorithmModuleDefinition#controllerFactory()}
 * 创建对应控制器。它把“模块 ID / 国际化文案 key / 控制器工厂”集中在一个地方，
 * 避免主控制器知道每个模块的具体构造细节。
 * </p>
 *
 * <p>
 * 新增主程序模块时，通常在 core 中准备结构和算法，在 visualization 中准备 Controller
 * 与 Visualizer，最后在 {@link #defaults()} 里注册一条定义。
 * </p>
 */
public final class ModuleRegistry {

    /**
     * 工具类不允许实例化。
     */
    private ModuleRegistry() {
    }

    /**
     * 构建当前程序默认展示的算法模块列表。
     *
     * <p>
     * 这里创建的是工厂而不是直接创建控制器，因此主界面可以在用户切换模块时按需构造，
     * 每次构造都能拿到新的结构实例和算法实例，避免不同运行之间共享旧状态。
     * </p>
     *
     * @return 默认模块定义列表
     */
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
                            BFSArrayMazeGenerator generator = new BFSArrayMazeGenerator();
                            generator.setMazeEntity(maze);
                            return new MazeController(maze, generator, new MazeModuleVisualizer());
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
