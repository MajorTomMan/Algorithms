package com.majortom.algorithms.visualization.manager;

import javafx.application.Platform;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 算法线程管理器。
 *
 * <p>它是 visualization 层连接算法线程和 JavaFX UI 线程的核心工具。
 * 算法本身运行在单独的 worker 线程中，渲染任务通过 {@link Platform#runLater(Runnable)}
 * 投递到 JavaFX Application Thread。两个线程之间通过 {@link #renderLock} 保证
 * “算法推进一步 -> UI 画完这一帧 -> 算法继续下一步”的节奏。</p>
 *
 * <p>暂停/恢复由 {@link #pauseLock} 控制，停止和旧任务回调隔离由 {@link #taskVersion}
 * 控制。这样即使用户连续点击运行、停止、重新运行，旧任务的完成回调也不会覆盖新任务 UI。</p>
 */
public class AlgorithmThreadManager {

    /**
     * 核心执行器。
     * <p>单线程池确保同一时间只有一个算法执行，不会出现多个算法同时抢夺同一个画布。</p>
     */
    private static final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "Algorithms-Worker-Thread");
        t.setDaemon(true); // 守护线程，主程序关闭时自动退出
        return t;
    });

    /**
     * 当前正在运行的算法任务。
     */
    private static Future<?> currentTask;

    /**
     * 任务版本号。
     * <p>每次开始或停止都会递增，完成回调只有在版本仍然匹配时才允许更新 UI。</p>
     */
    private static final AtomicLong taskVersion = new AtomicLong();

    /**
     * 渲染同步信号量。
     * <p>初始为 0。算法线程提交渲染任务后等待，UI 线程绘制完成后 release。</p>
     */
    private static final Semaphore renderLock = new Semaphore(0);

    /**
     * 暂停信号量。
     * <p>初始为 1，相当于一张通行证。暂停时消耗通行证，算法线程会阻塞在
     * {@link #checkStepStatus()}；恢复时归还通行证。</p>
     */
    private static final Semaphore pauseLock = new Semaphore(1);

    /**
     * 当前帧间延迟，单位毫秒。
     */
    private static volatile long currentDelay = 50;

    /**
     * 当前暂停状态的快速标记。
     */
    private static volatile boolean isPaused = false;

    /**
     * 暴露给控制器绑定的只读暂停属性。
     */
    private static final ReadOnlyBooleanWrapper pausedProperty = new ReadOnlyBooleanWrapper(false);

    /**
     * 设置算法播放速度。
     *
     * @param delay 每一帧渲染完成后的等待毫秒数
     */
    public static void setDelay(long delay) {
        currentDelay = delay;
    }

    /**
     * 获取当前播放延迟。
     *
     * @return 每帧延迟毫秒数
     */
    public static long getDelay() {
        return currentDelay;
    }

    /**
     * 暂停逻辑：消耗掉通行证
     */
    public static void pause() {
        if (!isPaused) {
            pauseLock.tryAcquire();
            isPaused = true;
            updatePausedProperty(true);
        }
    }

    /**
     * 恢复逻辑：归还通行证
     */
    public static void resume() {
        if (isPaused) {
            pauseLock.release();
            isPaused = false;
            updatePausedProperty(false);
        }
    }

    /**
     * 判断当前是否处于暂停状态。
     *
     * @return 如果算法线程会在下一次检查时阻塞，返回 true
     */
    public static boolean isPaused() {
        return isPaused;
    }

    /**
     * 获取可绑定到 UI 控件的暂停状态。
     *
     * @return 只读暂停属性
     */
    public static ReadOnlyBooleanProperty pausedProperty() {
        return pausedProperty.getReadOnlyProperty();
    }

    /**
     * 同步执行一次渲染任务，并让算法线程等待 UI 绘制完成。
     *
     * <p>控制器通常在 {@code ExecutionControl.acceptFrame(...)} 中调用它。
     * 传入的 {@code renderTask} 会被投递到 JavaFX UI 线程；UI 绘制完成后释放
     * {@link #renderLock}，算法线程再按照 {@link #currentDelay} 休眠一小段时间。</p>
     *
     * @param renderTask 需要在 JavaFX UI 线程执行的渲染逻辑
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
     * 检查暂停状态。
     *
     * <p>算法线程在每个同步点会调用它。未暂停时 acquire/release 会立即通过；
     * 暂停时 acquire 会阻塞，直到 {@link #resume()} 归还通行证。</p>
     */
    public static void checkStepStatus() {
        try {
            pauseLock.acquire();
            pauseLock.release(); 
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * 在算法 worker 线程中运行任务。
     *
     * @param task 算法任务
     */
    public static void run(Runnable task) {
        run(task, null);
    }

    /**
     * 停止旧任务并启动一个新的算法任务。
     *
     * <p>启动前会清理渲染锁、恢复暂停状态并递增版本号。任务完成后只有在版本号仍然匹配、
     * 且线程未被中断时，才会把 {@code onFinished} 投递回 UI 线程。</p>
     *
     * @param task 在 worker 线程中执行的算法任务
     * @param onFinished 算法自然结束后的 UI 回调，可为空
     */
    public static void run(Runnable task, Runnable onFinished) {
        stopAll();
        renderLock.drainPermits();
        resume();
        long runVersion = taskVersion.incrementAndGet();

        currentTask = executor.submit(() -> {
            try {
                task.run();
            } finally {
                if (onFinished != null
                        && runVersion == taskVersion.get()
                        && !Thread.currentThread().isInterrupted()) {
                    Platform.runLater(onFinished);
                }
            }
        });
    }

    /**
     * 检查当前是否有算法在运行。
     *
     * @return 当前任务存在且未完成时返回 true
     */
    public static boolean isRunning() {
        return currentTask != null && !currentTask.isDone();
    }

    /**
     * 强制停止当前算法并清理同步状态。
     *
     * <p>这个方法会取消 worker 线程任务、释放可能阻塞中的渲染等待、恢复暂停状态，
     * 并通过递增版本号阻止旧任务完成回调影响新 UI。</p>
     */
    public static void stopAll() {
        taskVersion.incrementAndGet();
        if (currentTask != null) {
            currentTask.cancel(true);
            currentTask = null;
        }
        renderLock.drainPermits();
        renderLock.release();
        resume();
    }

    /**
     * 在 UI 线程安全地执行状态更新。
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

    /**
     * 更新暂停属性。
     *
     * @param paused 最新暂停状态
     */
    private static void updatePausedProperty(boolean paused) {
        postStatus(() -> pausedProperty.set(paused));
    }
}
