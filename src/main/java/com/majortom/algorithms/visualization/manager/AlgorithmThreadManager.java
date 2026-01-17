package com.majortom.algorithms.visualization.manager;

import javafx.application.Platform;
import java.util.concurrent.*;

/**
 * 算法线程管理器
 * 职责：管控算法执行线程的生命周期，实现“计算-渲染”步调一致，并处理暂停与降频。
 */
public class AlgorithmThreadManager {

    // 核心执行器：单线程池确保算法按顺序执行，不会出现多个算法抢夺同一个 Canvas
    private static final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "Algorithms-Worker-Thread");
        t.setDaemon(true); // 守护线程，主程序关闭时自动退出
        return t;
    });

    private static Future<?> currentTask;

    // 信号量：renderLock 初始为 0，代表“画完了吗？”
    private static final Semaphore renderLock = new Semaphore(0);
    // 信号量：pauseLock 初始为 1，代表“通行证”
    private static final Semaphore pauseLock = new Semaphore(1);

    private static volatile long currentDelay = 50;
    private static volatile boolean isPaused = false;

    public static void setDelay(long delay) {
        currentDelay = delay;
    }

    /**
     * 暂停逻辑：消耗掉通行证
     */
    public static void pause() {
        if (!isPaused) {
            pauseLock.tryAcquire();
            isPaused = true;
        }
    }

    /**
     * 恢复逻辑：归还通行证
     */
    public static void resume() {
        if (isPaused) {
            pauseLock.release();
            isPaused = false;
        }
    }

    public static boolean isPaused() {
        return isPaused;
    }

    /**
     * 核心同步逻辑：计算线程在这里等待 UI 线程绘制完成
     */
    public static void syncAndWait(Runnable renderTask) {
        if (Thread.currentThread().isInterrupted())
            return;
        checkStepStatus();

        Platform.runLater(() -> {
            try {
                renderTask.run();
            } finally {
                renderLock.release();
            }
        });

        try {
            if (renderLock.tryAcquire(5, TimeUnit.SECONDS)) {
                long sleepTime = Math.max(1, currentDelay);
                Thread.sleep(sleepTime);
            }
        } catch (InterruptedException e) {
            renderLock.release();
            Thread.currentThread().interrupt();
        }
    }

    /**
     * 步进状态检查
     * 职责：被算法逻辑或控制器调用，用于响应暂停信号
     */
    public static void checkStepStatus() {
        try {
            pauseLock.acquire();
            pauseLock.release(); 
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public static void run(Runnable task) {
        run(task, null);
    }

    public static void run(Runnable task, Runnable onFinished) {
        stopAll();
        renderLock.drainPermits(); 
        resume(); 

        currentTask = executor.submit(() -> {
            try {
                task.run();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (onFinished != null) {
                    Platform.runLater(onFinished);
                }
            }
        });
    }

    /**
     * 检查当前是否有算法在跑
     */
    public static boolean isRunning() {
        return currentTask != null && !currentTask.isDone();
    }

    /**
     * 强制停止并清理现场
     */
    public static void stopAll() {
        if (currentTask != null) {
            currentTask.cancel(true);
        }
        renderLock.release();
        resume();
    }

    /**
     * 在 UI 线程安全地更新状态信息
     * 
     * @param action 具体的 UI 更新逻辑
     */
    public static void postStatus(Runnable action) {
        if (Platform.isFxApplicationThread()) {
            action.run();
        } else {
            Platform.runLater(action);
        }
    }
}