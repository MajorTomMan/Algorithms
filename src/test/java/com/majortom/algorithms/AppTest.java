package com.majortom.algorithms;

import com.majortom.algorithms.core.graph.BaseGraph;
import com.majortom.algorithms.core.graph.algorithms.BFSAlgorithms;
import com.majortom.algorithms.core.graph.impl.DirectedGraph;
import com.majortom.algorithms.core.graph.impl.UndirectedGraph;
import com.majortom.algorithms.core.maze.algorithms.generate.BFSMazeGenerator;
import com.majortom.algorithms.core.maze.algorithms.pathfinding.AStarMazePathfinder;
import com.majortom.algorithms.core.maze.impl.ArrayMaze;
import com.majortom.algorithms.core.sort.impl.InsertionSort;
import com.majortom.algorithms.core.tree.impl.AVLTree;
import com.majortom.algorithms.utils.AlgorithmLab;
import com.majortom.algorithms.utils.AlgorithmsUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * 算法实验室 - 集成可视化测试
 * 采用全新的 AlgorithmLab 静态引擎
 */
public class AppTest {

    @Test
    public void testSort() {
        Integer[] rawData = AlgorithmsUtils.randomArray(20, 100);
        int[] dataForInsertion = AlgorithmsUtils.toPrimitive(rawData);

        // 使用极简 API 启动排序实验室 [cite: 2026-01-10]
        AlgorithmLab.showSort(dataForInsertion, new InsertionSort());
    }

    @Test
    public void testUndirectedGraph() {
        BaseGraph<String> graph = new UndirectedGraph<>();
        AlgorithmsUtils.buildRandomGraph(graph, 10, 15, true);
        BFSAlgorithms<String> bfs = new BFSAlgorithms<>();

        // 启动图算法实验室
        AlgorithmLab.showGraph(graph, bfs, "A");
    }

    @Test
    public void testDirectedGraph() {
        BaseGraph<String> graph = new DirectedGraph<>();
        AlgorithmsUtils.buildRandomGraph(graph, 10, 15, true);
        BFSAlgorithms<String> bfs = new BFSAlgorithms<>();

        AlgorithmLab.showGraph(graph, bfs, "A");
    }

    @Test
    public void testTree() {
        AVLTree<Integer> avl = new AVLTree<>();
        Integer[] data = AlgorithmsUtils.randomArray(10, 100);

        // 启动树实验室，展示 AVL 平衡过程
        AlgorithmLab.showTree(avl, data);
    }

    @Test
    public void testMaze() {
        ArrayMaze container = new ArrayMaze(31, 31);
        BFSMazeGenerator generator = new BFSMazeGenerator();
        AStarMazePathfinder pathfinder = new AStarMazePathfinder();

        // 启动迷宫实验室：生成 + 寻路一体化
        AlgorithmLab.showMaze(container, generator, pathfinder);
    }

    @AfterEach
    public void tearDown() {
        // 由于 JavaFX 是异步渲染，如果不阻塞主线程，JUnit 会在窗口弹出瞬间结束进程
        System.out.println("\n------------------------------------------------");
        System.out.println("🧪 实验室窗口已弹出。输入 [Enter] 键关闭当前测试并继续...");
        System.out.println("------------------------------------------------");

        try {
            System.in.read();
        } catch (Exception ignored) {
        }
    }
}