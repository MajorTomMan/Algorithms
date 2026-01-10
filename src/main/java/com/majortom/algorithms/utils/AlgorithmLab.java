package com.majortom.algorithms.utils;

import com.majortom.algorithms.core.visualization.BaseController;
import com.majortom.algorithms.core.visualization.impl.controller.GraphController;
import com.majortom.algorithms.core.visualization.impl.controller.MazeController;
import com.majortom.algorithms.core.visualization.impl.controller.SortController;
import com.majortom.algorithms.core.visualization.impl.controller.TreeController;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

import java.util.concurrent.CountDownLatch;

/**
 * 峻豪算法实验室 - 纯 JavaFX 启动引擎与 API 封装
 * 职责：环境初始化、FXML 加载、利用统一 init 接口进行数据注入
 */
public class AlgorithmLab {

    private static volatile boolean isRuntimeStarted = false;
    private static final String FXML_PATH = "/fxml/MainView.fxml";

    /**
     * 核心启动逻辑（极致通用化）
     * 
     * @param controller 具体的控制器实例
     * @param args       传递给控制器 init 方法的变长参数包
     */
    private static void launch(BaseController<?> controller, Object... args) {
        // 1. 确保 JavaFX 环境单例启动
        ensureRuntimeStarted();

        // 2. 在 FX 线程执行 UI 操作
        Platform.runLater(() -> {
            try {
                Stage stage = new Stage();

                // 3. 动态加载 FXML 并手动绑定控制器
                FXMLLoader loader = new FXMLLoader(AlgorithmLab.class.getResource(FXML_PATH));
                loader.setController(controller);

                BorderPane root = loader.load();

                // 4. 调用基类定义的统一入口 init [cite: 2026-01-10]
                // 这里的 args 会被原封不动地传给子类的 init(Object... args)
                controller.init(args);

                // 5. 舞台设置
                stage.setTitle("MajorTom Algorithm Lab 2.0 - " + controller.getClass().getSimpleName());
                Scene scene = new Scene(root, 1080, 750);

                // 建议：在此处注入全局 CSS 以维持“文艺忧郁”的暗色调 [cite: 2025-12-25]
                // scene.getStylesheets().add(AlgorithmLab.class.getResource("/styles/main.css").toExternalForm());

                stage.setScene(scene);
                stage.show();
            } catch (Exception e) {
                System.err.println("❌ 实验室启动失败: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    /**
     * 内部方法：环境初始化
     */
    private static void ensureRuntimeStarted() {
        if (!isRuntimeStarted) {
            synchronized (AlgorithmLab.class) {
                if (!isRuntimeStarted) {
                    CountDownLatch latch = new CountDownLatch(1);
                    try {
                        Platform.startup(latch::countDown);
                        latch.await();
                        isRuntimeStarted = true;
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } catch (IllegalStateException e) {
                        isRuntimeStarted = true;
                    }
                }
            }
        }
    }

    // --- 对外暴露的极简静态 API ---

    /** 迷宫实验室：传入 Maze, Generator, Pathfinder */
    public static void showMaze(Object maze, Object generator, Object pathfinder) {
        launch(new MazeController(), maze, generator, pathfinder);
    }

    /** 排序实验室：传入 int[] 数据, SortAlgorithm */
    public static void showSort(int[] data, Object algorithm) {
        launch(new SortController(), data, algorithm);
    }

    /** 树实验室：传入 Tree, Integer[] 初始数据 */
    public static void showTree(Object tree, Integer[] initialData) {
        launch(new TreeController(), tree, initialData);
    }

    /** 图实验室：传入 Graph, Algorithm, StartNodeName */
    public static void showGraph(Object graph, Object executor, String startNode) {
        launch(new GraphController<String>(), graph, executor, startNode);
    }
}