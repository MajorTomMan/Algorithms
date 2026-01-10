package com.majortom.algorithms.utils;

import com.majortom.algorithms.core.graph.BaseGraph;
import com.majortom.algorithms.core.graph.BaseGraphAlgorithms;
import com.majortom.algorithms.core.maze.BaseMaze;
import com.majortom.algorithms.core.maze.strategies.MazeGeneratorStrategy;
import com.majortom.algorithms.core.maze.strategies.PathfindingStrategy;
import com.majortom.algorithms.core.sort.BaseSort;
import com.majortom.algorithms.core.tree.BaseTree;
import com.majortom.algorithms.core.visualization.impl.window.GraphWindow;
import com.majortom.algorithms.core.visualization.impl.window.MazeWindow;
import com.majortom.algorithms.core.visualization.impl.window.SortWindow;
import com.majortom.algorithms.core.visualization.impl.window.TreeWindow;
import javafx.application.Platform;

import java.util.concurrent.CountDownLatch;

/**
 * 算法实验室启动引擎
 */
public class AlgorithmLab {

    private static volatile boolean isRuntimeStarted = false;

    /**
     * 内部通用启动包装
     * 确保在执行任何 UI 相关逻辑前，Toolkit 已初始化
     */
    private static void safeLaunch(Runnable launchTask) {
        ensureRuntimeStarted();
        // 此时 Toolkit 已经初始化，可以安全调用 Platform.runLater
        Platform.runLater(() -> {
            try {
                launchTask.run();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private static void ensureRuntimeStarted() {
        if (isRuntimeStarted)
            return;

        synchronized (AlgorithmLab.class) {
            if (!isRuntimeStarted) {
                try {
                    CountDownLatch latch = new CountDownLatch(1);
                    // 启动 JavaFX 运行时
                    Platform.startup(latch::countDown);
                    latch.await();
                    isRuntimeStarted = true;
                } catch (IllegalStateException e) {
                    // 如果已经由其他逻辑启动，捕获异常
                    isRuntimeStarted = true;
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    // --- 静态 API 封装 ---

    /** 迷宫实验室 */
    @SuppressWarnings("unchecked")
    public static void showMaze(Object maze, Object generator, Object pathfinder) {
        safeLaunch(() -> MazeWindow.launch(
                (BaseMaze<int[][]>) maze,
                (MazeGeneratorStrategy<int[][]>) generator,
                (PathfindingStrategy<int[][]>) pathfinder));
    }

    /** 排序实验室 */
    public static void showSort(int[] data, Object algorithm) {
        safeLaunch(() -> SortWindow.launch((BaseSort) algorithm, data));
    }

    /** 树实验室 */
    @SuppressWarnings("unchecked")
    public static void showTree(Object tree, Integer[] initialData) {
        safeLaunch(() -> TreeWindow.launch((BaseTree<Integer>) tree, initialData));
    }

    /** 图实验室 */
    @SuppressWarnings("unchecked")
    public static <V> void showGraph(Object graph, Object executor, V startNode) {
        safeLaunch(() -> GraphWindow.launch(
                (BaseGraph<V>) graph,
                (BaseGraphAlgorithms<V>) executor,
                startNode));
    }
}