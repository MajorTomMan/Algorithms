package com.majortom.algorithms.core.visualization;

import com.majortom.algorithms.core.base.BaseAlgorithms;
import com.majortom.algorithms.core.base.BaseStructure;
import com.majortom.algorithms.core.maze.BaseMazeAlgorithms;
import com.majortom.algorithms.core.visualization.impl.controller.BaseModuleController;
import com.majortom.algorithms.core.visualization.manager.AlgorithmThreadManager;
import com.majortom.algorithms.core.visualization.stat.AlgorithmStats;

import javafx.application.Platform;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;

import java.net.URL;
import java.util.ResourceBundle;

/**
 * 算法可视化控制器基类
 * 职责：定义标准化的统计接口、日志接口、线程同步生命周期，并接管全局按钮逻辑。
 */
public abstract class BaseController<S extends BaseStructure<?>> implements Initializable {

    protected final DoubleProperty delayMs = new SimpleDoubleProperty(50.0);
    protected AlgorithmStats stats = new AlgorithmStats();

    // --- 核心 UI 引用 (由 MainController 注入) ---
    protected final BaseVisualizer<S> visualizer;
    protected Label statsLabel;
    protected TextArea logArea;
    protected Slider delaySlider;
    protected HBox customControlBox;

    // --- 全局控制按钮 (对齐 Main.fxml) ---
    protected Button startBtn;
    protected Button pauseBtn;
    protected Button resetBtn;

    public BaseController(BaseVisualizer<S> visualizer) {
        this.visualizer = visualizer;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupI18n();
    }

    // --- 核心 API 接口 ---

    /**
     * 核心同步钩子：算法线程调用。
     */
    protected final void onSync(S structure, Object a, Object b, int compareCount, int actionCount) {
        this.stats.compareCount = compareCount;
        this.stats.actionCount = actionCount;

        AlgorithmThreadManager.syncAndWait(() -> {
            if (visualizer != null) {
                visualizer.render(structure, a, b);
            }
            refreshStatsDisplay();
        });
    }

    /**
     * 步进检查：用于响应暂停和停止信号。
     */
    protected final void onStep() {
        AlgorithmThreadManager.checkStepStatus();
    }

    /**
     * 启动算法模板方法。
     */
    public final void startAlgorithm(BaseAlgorithms<S> algorithm, S data) {
        stopAlgorithm();
        resetStats();

        algorithm.setEnvironment(this::onSync, this::onStep);

        stats.startTime = System.currentTimeMillis();
        appendLog("Algorithm started: " + algorithm.getClass().getSimpleName());

        AlgorithmThreadManager.run(() -> {
            try {
                executeAlgorithm(algorithm, data);
            } catch (Exception e) {
                handleAlgorithmError(e);
            } finally {
                stats.endTime = System.currentTimeMillis();
                Platform.runLater(this::onAlgorithmFinished);
            }
        });
    }

    public void stopAlgorithm() {
        AlgorithmThreadManager.stopAll();
    }

    public void togglePause() {
        if (AlgorithmThreadManager.isPaused()) {
            AlgorithmThreadManager.resume();
        } else {
            AlgorithmThreadManager.pause();
        }
    }

    // --- 内部逻辑与辅助方法 ---

    private void resetStats() {
        this.stats = new AlgorithmStats();
    }

    protected final void appendLog(String message) {
        if (logArea != null) {
            Platform.runLater(
                    () -> logArea.appendText(String.format("[%tT] %s%n", System.currentTimeMillis(), message)));
        }
    }

    protected void refreshStatsDisplay() {
        if (statsLabel != null) {
            statsLabel.setText(formatStatsMessage());
        }
    }

    /**
     * 初始化全局控制按钮的行为
     */
    private void setupGlobalButtonActions() {
        if (startBtn != null) {
            startBtn.setOnAction(e -> handleAlgorithmStart());
        }
        if (pauseBtn != null) {
            pauseBtn.setOnAction(e -> togglePause());
        }
        if (resetBtn != null) {
            resetBtn.setOnAction(e -> {
                stopAlgorithm();
                ((BaseModuleController<S>) this).resetModule();
            });
        }
    }

    // --- 抽象钩子方法 ---

    protected abstract String formatStatsMessage();

    public abstract void setupCustomControls(HBox container);

    protected abstract void setupI18n();

    protected abstract void executeAlgorithm(BaseAlgorithms<S> algorithm, S data);

    public abstract void handleAlgorithmStart();

    protected void onAlgorithmFinished() {
        appendLog(String.format("Finished. Duration: %dms", stats.getDuration()));
    }

    protected void handleAlgorithmError(Exception e) {
        appendLog("Runtime Error: " + e.getMessage());
        e.printStackTrace();
    }

    // --- 生命周期管理与引用注入 ---

    /**
     * 由 MainController 调用，完成 UI 体系的整体依赖注入。
     */
    public final void setUIReferences(
            Label statsLabel,
            TextArea logArea,
            Slider delaySlider,
            HBox customBox,
            Button startBtn,
            Button pauseBtn,
            Button resetBtn) {
        this.statsLabel = statsLabel;
        this.logArea = logArea;
        this.delaySlider = delaySlider;
        this.customControlBox = customBox;
        this.startBtn = startBtn;
        this.pauseBtn = pauseBtn;
        this.resetBtn = resetBtn;

        // 1. 绑定滑块
        if (this.delaySlider != null) {
            this.delayMs.bind(this.delaySlider.valueProperty());
            this.delayMs.addListener((obs, oldVal, newVal) -> AlgorithmThreadManager.setDelay(newVal.longValue()));
            AlgorithmThreadManager.setDelay(delayMs.longValue());
        }

        // 2. 绑定按钮行为
        setupGlobalButtonActions();

        // 3. 触发初始化逻辑
        this.initialize(null, null);
    }

    public final BaseVisualizer<S> getVisualizer() {
        return visualizer;
    }

    /**
     * 安全地获取可视化视图根节点，用于 MainController 布局插入。
     */
    public final Region getVisualizerView() {
        if (visualizer instanceof Region) {
            return (Region) visualizer;
        }
        throw new IllegalStateException("Visualizer must extend Region for layout integration.");
    }
}