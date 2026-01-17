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
import javafx.scene.layout.Region;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

/**
 * 算法可视化控制器基类
 * 职责：统筹 UI 线程渲染与算法执行线程的同步。
 * 
 * @param <S> 结构类型，继承自 BaseStructure
 */
public abstract class BaseController<S extends BaseStructure<?>> implements Initializable {

    // --- 动画状态 ---
    protected final DoubleProperty delayMs = new SimpleDoubleProperty(50.0);

    // --- 核心引用 ---
    protected BaseVisualizer<S> visualizer;
    protected Label statsLabel;
    protected TextArea logArea;
    protected Slider delaySlider;

    public BaseController() {
    }

    public BaseController(BaseVisualizer<S> visualizer) {
        this.visualizer = visualizer;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // 绑定延迟滑块逻辑
        if (delaySlider != null) {
            this.delayMs.bind(delaySlider.valueProperty());
            delayMs.addListener((obs, oldVal, newVal) -> AlgorithmThreadManager.setDelay(newVal.longValue()));

            AlgorithmThreadManager.setDelay(delayMs.longValue());
        }
    }

    /**
     * 核心同步钩子：阻塞算法线程直到 UI 渲染完成
     */
    protected void onSync(S structure, Object a, Object b, int compareCount, int actionCount) {
        AlgorithmThreadManager.syncAndWait(() -> {
            if (visualizer != null) {
                visualizer.render(structure, a, b);
            }
            updateUIComponents(compareCount, actionCount);
        });
    }

    /**
     * 步进检查：处理暂停与中断
     */
    protected void onStep() {
        AlgorithmThreadManager.checkStepStatus();
    }

    /**
     * 提交算法任务到线程池
     */
    public void startAlgorithm(BaseAlgorithms<S> algorithm, S data) {
        stopAlgorithm();

        // 注入回调接口
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

    public void togglePause() {
        if (AlgorithmThreadManager.isPaused()) {
            AlgorithmThreadManager.resume();
        } else {
            AlgorithmThreadManager.pause();
        }
    }

    // --- UI 注入与状态查询 ---

    public Region getView() {
        return (Region) visualizer;
    }

    public void setUIReferences(Label statsLabel, TextArea logArea, Slider delaySlider) {
        this.statsLabel = statsLabel;
        this.logArea = logArea;
        this.delaySlider = delaySlider;
        // 注入后立即执行初始化逻辑
        this.initialize(null, null);
    }

    public boolean isRunning() {
        return AlgorithmThreadManager.isRunning();
    }

    public boolean isPaused() {
        return AlgorithmThreadManager.isPaused();
    }

    // --- 抽象钩子 (由 MazeController 等子类实现) ---
    protected abstract void setupI18n();

    protected abstract void executeAlgorithm(BaseAlgorithms<S> algorithm, S data);

    protected abstract void updateUIComponents(int compareCount, int actionCount);

    public abstract List<Node> getCustomControls();

    public abstract void handleAlgorithmStart();

    // --- 事件回调 ---

    protected void handleAlgorithmError(Exception e) {
        Platform.runLater(() -> {
            if (logArea != null) {
                logArea.appendText("Error: " + e.getMessage() + "\n");
            }
        });
    }

    protected void onAlgorithmFinished() {
        Platform.runLater(() -> {
            if (logArea != null) {
                logArea.appendText("Execution completed.\n");
            }
        });
    }

    public BaseVisualizer<S> getVisualizer() {
        return visualizer;
    }
}