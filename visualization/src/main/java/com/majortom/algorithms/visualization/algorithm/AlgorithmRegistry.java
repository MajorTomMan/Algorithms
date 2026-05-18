package com.majortom.algorithms.visualization.algorithm;

import com.majortom.algorithms.core.base.BaseAlgorithms;
import com.majortom.algorithms.core.graph.algorithms.BFSAlgorithms;
import com.majortom.algorithms.core.maze.algorithms.array.generate.BFSArrayMazeGenerator;
import com.majortom.algorithms.core.maze.algorithms.array.generate.DFSArrayMazeGenerator;
import com.majortom.algorithms.core.maze.algorithms.array.generate.UnionFindArrayMazeGenerator;
import com.majortom.algorithms.core.maze.algorithms.array.pathfinding.AStarArrayMazePathfinder;
import com.majortom.algorithms.core.maze.algorithms.array.pathfinding.DFSArrayMazePathfinder;
import com.majortom.algorithms.core.maze.algorithms.graph.generate.DFSGraphMazeGenerator;
import com.majortom.algorithms.core.sort.alg.HeapSort;
import com.majortom.algorithms.core.sort.alg.InsertionSort;
import com.majortom.algorithms.core.sort.alg.QuickSort;
import com.majortom.algorithms.core.sort.alg.SelectionSort;
import com.majortom.algorithms.core.tree.alg.AVLTreeAlgorithms;

import java.util.List;
import java.util.function.Supplier;

/**
 * 算法注册表。
 *
 * <p>
 * 控制器不再自己维护算法下拉框列表，而是按模块、用途和结构类型向这里查询。
 * 这里使用显式注册，保持 JavaFX 桌面应用的启动行为稳定，不做 classpath 扫描。
 * </p>
 *
 * <p>
 * 这个注册表是“算法实现”和“界面选择器”的中间层。新增算法时，通常只需要：
 * 实现算法类、补充国际化文案、在这里注册 descriptor。控制器会按当前模块和结构类型自动拿到候选算法。
 * </p>
 */
public final class AlgorithmRegistry {

        /**
         * 默认算法注册项。
         */
        private static final List<AlgorithmDescriptor> DEFAULTS = List.of(
                        descriptor("insertion-sort", "algorithm.sort.insertion", "sort",
                                        AlgorithmFamily.SORT, AlgorithmStructure.ARRAY_SORT, InsertionSort::new),
                        descriptor("selection-sort", "algorithm.sort.selection", "sort",
                                        AlgorithmFamily.SORT, AlgorithmStructure.ARRAY_SORT, SelectionSort::new),
                        descriptor("quick-sort", "algorithm.sort.quick", "sort",
                                        AlgorithmFamily.SORT, AlgorithmStructure.ARRAY_SORT, QuickSort::new),
                        descriptor("heap-sort", "algorithm.sort.heap", "sort",
                                        AlgorithmFamily.SORT, AlgorithmStructure.ARRAY_SORT, HeapSort::new),
                        descriptor("maze-generator-bfs", "algorithm.maze.generate.bfs", "maze",
                                        AlgorithmFamily.MAZE_GENERATOR, AlgorithmStructure.ARRAY_MAZE,
                                        BFSArrayMazeGenerator::new),
                        descriptor("maze-generator-dfs", "algorithm.maze.generate.dfs", "maze",
                                        AlgorithmFamily.MAZE_GENERATOR, AlgorithmStructure.ARRAY_MAZE,
                                        DFSArrayMazeGenerator::new),
                        descriptor("maze-generator-union-find", "algorithm.maze.generate.uf", "maze",
                                        AlgorithmFamily.MAZE_GENERATOR, AlgorithmStructure.ARRAY_MAZE,
                                        UnionFindArrayMazeGenerator::new),
                        descriptor("graph-generator-bfs", "algorithm.graph.generate.bfs", "maze",
                                        AlgorithmFamily.MAZE_GENERATOR, AlgorithmStructure.GRAPH_MAZE,
                                        DFSGraphMazeGenerator::new),
                        descriptor("maze-pathfinder-astar", "algorithm.maze.solve.astar", "maze",
                                        AlgorithmFamily.MAZE_PATHFINDER, AlgorithmStructure.ARRAY_MAZE,
                                        AStarArrayMazePathfinder::new),
                        descriptor("maze-pathfinder-dfs", "algorithm.maze.solve.dfs", "maze",
                                        AlgorithmFamily.MAZE_PATHFINDER, AlgorithmStructure.ARRAY_MAZE,
                                        DFSArrayMazePathfinder::new),
                        descriptor("tree-avl", "algorithm.tree.avl", "tree",
                                        AlgorithmFamily.TREE_OPERATION, AlgorithmStructure.AVL_TREE,
                                        AVLTreeAlgorithms::new),
                        descriptor("graph-bfs", "algorithm.graph.bfs", "graph",
                                        AlgorithmFamily.GRAPH_TRAVERSAL, AlgorithmStructure.DIRECTED_GRAPH,
                                        BFSAlgorithms::new));

        /**
         * 工具类不允许实例化。
         */
        private AlgorithmRegistry() {
        }

        /**
         * 查询匹配模块、用途和结构类型的算法。
         *
         * <p>
         * 这三个条件共同保证 UI 不会展示不兼容的算法。例如迷宫模块切到图结构时，
         * 二维数组迷宫生成器不会被误放到图迷宫下拉框中。
         * </p>
         *
         * @param moduleId  模块 ID
         * @param family    算法用途
         * @param structure 数据结构类型
         * @return 匹配的算法列表；没有图迷宫算法时会返回空列表
         */
        public static List<AlgorithmDescriptor> find(
                        String moduleId,
                        AlgorithmFamily family,
                        AlgorithmStructure structure) {
                return DEFAULTS.stream()
                                .filter(descriptor -> descriptor.moduleId().equals(moduleId))
                                .filter(descriptor -> descriptor.family() == family)
                                .filter(descriptor -> descriptor.structure() == structure)
                                .toList();
        }

        /**
         * 创建算法注册项。
         *
         * <p>
         * 这是内部小工厂，统一处理泛型擦除带来的类型噪声，让默认注册列表保持可读。
         * </p>
         */
        @SuppressWarnings({ "unchecked", "rawtypes" })
        private static AlgorithmDescriptor descriptor(
                        String id,
                        String labelKey,
                        String moduleId,
                        AlgorithmFamily family,
                        AlgorithmStructure structure,
                        Supplier<? extends BaseAlgorithms> factory) {
                return new AlgorithmDescriptor(id, labelKey, moduleId, family, structure, factory);
        }
}
