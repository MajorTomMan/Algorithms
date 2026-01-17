package com.majortom.algorithms.core.visualization;

import com.majortom.algorithms.core.base.BaseAlgorithms;
import com.majortom.algorithms.core.base.BaseStructure;
import com.majortom.algorithms.core.visualization.manager.AlgorithmThreadManager;
import javafx.application.Platform;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.TextArea;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

/**
 * 算法可视化控制器基类
 * 职责：统筹 UI 线程渲染与算法执行线程的同步。
 * * @param <S> 结构类型，继承自 BaseStructure
 */
public abstract class BaseController<S extends BaseStructure<?>> implements Initializable {

    // --- 动画状态 ---
    protected final DoubleProperty delayMs = new SimpleDoubleProperty(50.0);

    // --- 核心引用 ---
    protected BaseVisualizer<S> visualizer;
    protected Label statsLabel;
    protected TextArea logArea;
    protected Slider delaySlider;
    protected S originalData;

    public BaseController(BaseVisualizer<S> visualizer) {
        this.visualizer = visualizer;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        if (delaySlider != null) {
            this.delayMs.bind(delaySlider.valueProperty());
            delayMs.addListener((obs, oldVal, newVal) -> AlgorithmThreadManager.setDelay(newVal.longValue()));
            AlgorithmThreadManager.setDelay(delayMs.longValue());
        }
    }

    // --- 核心动作接口：由子类实现具体业务逻辑 ---

    /**
     * 响应开始按钮 (startBtn)
     */
    public abstract void handleStartAction();

    /**
     * 响应暂停/恢复按钮 (pauseBtn)
     */
    public void handlePauseAction() {
        if (AlgorithmThreadManager.isPaused()) {
            AlgorithmThreadManager.resume();
        } else {
            AlgorithmThreadManager.pause();
        }
    }

    /**
     * 响应重置按钮 (resetBtn)
     * 子类需在此处理特定清理逻辑（如 GraphStream 样式、Maze 随机点等）
     */
    public abstract void handleResetAction();

    // --- 算法执行辅助方法 ---

    /**
     * 提交算法任务到线程池，并自动生成初始数据快照
     */
    @SuppressWarnings("unchecked")
    protected void startAlgorithm(BaseAlgorithms<S> algorithm, S data) {
        stopAlgorithm();

        // 执行前备份当前数据状态
        if (data != null) {
            this.originalData = (S) data.copy();
        }

        algorithm.setEnvironment(this::onSync, this::onStep);

        AlgorithmThreadManager.run(() -> {
            try {
                executeAlgorithm(algorithm, data);
            } catch (Exception e) {
                handleAlgorithmError(e);
            } finally {
                Platform.runLater(this::onAlgorithmFinished);
            }
        });
    }

    public void stopAlgorithm() {
        AlgorithmThreadManager.stopAll();
    }

    // --- 同步钩子 ---

    protected void onSync(S structure, Object a, Object b, int compareCount, int actionCount) {
        AlgorithmThreadManager.syncAndWait(() -> {
            if (visualizer != null) {
                visualizer.render(structure, a, b);
            }
            updateUIComponents(compareCount, actionCount);
        });
    }

    protected void onStep() {
        AlgorithmThreadManager.checkStepStatus();
    }

    // --- UI 注入与状态查询 ---

    public void setUIReferences(Label statsLabel, TextArea logArea, Slider delaySlider) {
        this.statsLabel = statsLabel;
        this.logArea = logArea;
        this.delaySlider = delaySlider;
        this.initialize(null, null);
    }

    public BaseVisualizer<S> getVisualizer() {
        return visualizer;
    }

    // --- 抽象钩子 ---

    protected abstract void setupI18n();

    protected abstract void executeAlgorithm(BaseAlgorithms<S> algorithm, S data);

    protected abstract void updateUIComponents(int compareCount, int actionCount);

    public abstract List<Node> getCustomControls();

    /**
     * 钩子方法：用于在数据回滚后更新子类内部的具体引用
     */
    protected abstract void updateCurrentDataReference(S restoredData);

    // --- 默认回调行为 ---

    /**
     * 算法发生错误时的处理回调
     */
    protected void handleAlgorithmError(Exception e) {
        AlgorithmThreadManager.postStatus(() -> {
            if (logArea != null) {
                logArea.appendText("Error: " + e.getMessage() + "\n");
            }
        });
    }

    /**
     * 算法正常结束时的处理回调
     */
    protected void onAlgorithmFinished() {
        AlgorithmThreadManager.postStatus(() -> {
            if (logArea != null) {
                logArea.appendText("Execution completed.\n");
            }
        });
    }
}