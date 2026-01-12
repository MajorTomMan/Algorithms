package com.majortom.algorithms.core.visualization.manager;

import javafx.application.Platform;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class AlgorithmThreadManager {

    // 使用单线程池，确保同一时间只有一个算法在运行，避免多个算法竞争 CPU 导致可视化错乱
    private static final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r);
        t.setDaemon(true); // 设置为守护线程，主程序关闭时自动结束
        t.setName("Algorithms-Worker-Thread");
        return t;
    });

    private static Future<?> currentTask;

    /**
     * 运行算法任务
     */
    public static void run(Runnable task) {
        // 如果当前有任务在运行，可以根据需求选择取消或者等待
        if (currentTask != null && !currentTask.isDone()) {
            currentTask.cancel(true);
        }
        currentTask = executor.submit(task);
    }

    /**
     * 安全地将任务提交到 JavaFX UI 线程
     */
    public static void updateUI(Runnable uiTask) {
        if (Platform.isFxApplicationThread()) {
            uiTask.run();
        } else {
            Platform.runLater(uiTask);
        }
    }

    /**
     * 停止当前所有任务
     */
    public static void stopAll() {
        if (currentTask != null) {
            currentTask.cancel(true);
        }
    }
}