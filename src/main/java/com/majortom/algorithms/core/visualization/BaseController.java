package com.majortom.algorithms.core.visualization;

import com.majortom.algorithms.core.base.BaseAlgorithms;
import com.majortom.algorithms.core.graph.BaseGraphAlgorithms;
import com.majortom.algorithms.core.visualization.manager.AlgorithmThreadManager;
import javafx.application.Platform;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.TextArea;
import javafx.scene.layout.Region;
import javafx.scene.Node;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * 算法可视化控制器基类
 * 职责：连接算法逻辑与 UI 呈现，管理执行节奏与线程同步
 * * @param <T> 数据结构类型（如 int[], int[][], BaseTree 等）
 */
public abstract class BaseController<T> implements Initializable {

    // --- 核心同步锁 ---
    // 用于确保“算法步进”必须等待“UI渲染”完成后才能继续
    private final Semaphore renderSemaphore = new Semaphore(0);

    // --- 动画状态控制 (响应式属性) ---
    protected final DoubleProperty delayMs = new SimpleDoubleProperty(50.0);
    protected volatile boolean isPaused = false;
    protected volatile boolean isRunning = false;

    // --- 视觉组件引用 ---
    protected BaseVisualizer<T> visualizer;

    protected Label statsLabel;
    protected TextArea logArea;
    protected Slider delaySlider;

    /**
     * 添加此构造函数以支持依赖注入
     */
    public BaseController(BaseAlgorithms<T> algorithm, BaseVisualizer<T> visualizer) {
        // 将注入的组件保存到基类成员中
        this.visualizer = visualizer;
        // 注意：算法逻辑可以保存在基类，也可以由子类持有
    }

    public BaseController() {

    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        if (delaySlider != null) {
            this.delayMs.bind(delaySlider.valueProperty());
        }
    }

    /**
     * 提供给外部获取该控制器的 UI 根节点
     */
    public Region getView() {
        return visualizer;
    }

    /**
     * 实现 SyncListener 接口逻辑：处理数据快照同步
     * 运行在算法线程中
     */
    protected void onSync(T data, Object a, Object b, int compareCount, int actionCount) {
        // 定义渲染完成后的回调
        Runnable renderCompleteCallback = () -> renderSemaphore.release();

        // 将渲染任务提交至 JavaFX Application Thread
        Platform.runLater(() -> {
            try {
                if (visualizer != null) {
                    visualizer.render(data, a, b);
                }
                updateUIComponents(compareCount, actionCount);
            } finally {
                // 无论渲染成功与否，都释放信号量，避免算法线程永久阻塞
                renderCompleteCallback.run();
            }
        });
    }

    /**
     * 实现 StepListener 接口逻辑：控制算法执行节奏
     * 运行在算法线程中
     */
    protected void onStep() {
        try {
            // 1. 处理暂停逻辑
            while (isPaused && isRunning) {
                Thread.sleep(100);
            }

            // 2. 核心握手：等待 UI 渲染完成的信号
            // 设置 5 秒超时，防止 UI 异常导致后台线程死锁
            boolean rendered = renderSemaphore.tryAcquire(5, TimeUnit.SECONDS);

            // 3. 节流延迟：根据 delayMs 调整速度
            long sleepTime = (long) delayMs.get();
            if (sleepTime > 0) {
                Thread.sleep(sleepTime);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * 启动/重启算法执行
     */
    public void startAlgorithm(BaseAlgorithms<T> algorithm, T data) {
        if (isRunning) {
            stopAlgorithm();
        }

        isRunning = true;
        isPaused = false;
        renderSemaphore.drainPermits();
        renderSemaphore.release();
        // 注入环境监听器
        algorithm.setEnvironment(this::onSync, this::onStep);

        // 提交至后台线程池执行，避免卡死界面
        AlgorithmThreadManager.run(() -> {
            try {
                executeAlgorithm(algorithm, data);
            } catch (Exception e) {
                handleAlgorithmError(e);
            } finally {
                isRunning = false;
                Platform.runLater(this::onAlgorithmFinished);
            }
        });
    }

    /**
     * 强制停止当前运行的算法
     */
    public void stopAlgorithm() {
        isRunning = false;
        isPaused = false;
        AlgorithmThreadManager.stopAll();
        renderSemaphore.release(); // 强行解开阻塞
    }

    // --- 子类必须实现的业务逻辑 ---
    /**
     * 子类重写：提供自己特有的控制按钮
     * 这些按钮会被 MainFrame 放入 customControlBox
     */
    public abstract List<Node> getCustomControls();

    /**
     * 算法执行的统一入口
     * 由 MainController 的 START 按钮触发
     */
    public abstract void handleAlgorithmStart();

    /** 具体算法的启动逻辑 */
    protected abstract void executeAlgorithm(BaseAlgorithms<T> algorithm, T data);

    /** UI 统计数据（如比较次数）的更新逻辑 */
    protected abstract void updateUIComponents(int compareCount, int actionCount);

    // --- 可选重写的钩子 ---

    protected void handleAlgorithmError(Exception e) {
        e.printStackTrace();
    }

    protected void onAlgorithmFinished() {
        // 算法正常结束后的 UI 处理
    }

    // --- 通用属性访问 ---
    public DoubleProperty delayMsProperty() {
        return delayMs;
    }

    public void setPaused(boolean paused) {
        this.isPaused = paused;
    }

    public boolean isPaused() {
        return isPaused;
    }

    /**
     * 注入全局 UI 引用，供子控制器更新日志和状态
     */
    public void setUIReferences(Label statsLabel, TextArea logArea) {
        this.statsLabel = statsLabel;
        this.logArea = logArea;
    }

    /**
     * 获取当前控制器的视觉呈现组件
     * 注意：由于你在基类中定义的是 protected BaseVisualizer<T> visualizer;
     * 所以需要一个 getter 暴露给 MainController。
     */
    public BaseVisualizer<T> getVisualizer() {
        return visualizer;
    }
}