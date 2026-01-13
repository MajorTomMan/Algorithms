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
 * 职责：连接 UI 线程与算法执行线程，统筹动画节奏与数据同步。
 * * @param <S> 结构类型，必须继承自 BaseStructure，确保与 BaseAlgorithms 的泛型约束对齐
 */
public abstract class BaseController<S extends BaseStructure<?>> implements Initializable {

    // --- 动画状态控制 ---
    // 采用 DoubleProperty 方便与 JavaFX Slider 进行双向绑定
    protected final DoubleProperty delayMs = new SimpleDoubleProperty(50.0);

    // --- 视觉组件引用 (由子类通过 @FXML 或 setUIReferences 注入) ---
    protected BaseVisualizer<S> visualizer;
    protected Label statsLabel;
    protected TextArea logArea;
    protected Slider delaySlider;

    /**
     * 默认构造函数
     */
    public BaseController() {
    }

    /**
     * 带 Visualizer 的构造函数
     */
    public BaseController(BaseVisualizer<S> visualizer) {
        this.visualizer = visualizer;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // 初始化滑块绑定逻辑
        if (delaySlider != null) {
            // 建立双向绑定：Slider 的滑动会实时改变 delayMs 属性
            this.delayMs.bind(delaySlider.valueProperty());

            // 监听 delayMs 属性变化，实时更新算法线程的休眠时间
            delayMs.addListener((obs, oldVal, newVal) -> AlgorithmThreadManager.setDelay(newVal.longValue()));

            // 执行初始同步，确保管理器拿到滑块的初始值
            AlgorithmThreadManager.setDelay(delayMs.longValue());
        }
    }

    /**
     * 实现 SyncListener 接口逻辑：处理数据快照同步
     * 职责：在算法线程调用此方法时，阻塞算法线程，并在 UI 线程完成渲染后释放。
     * * @param structure 数据结构实体
     * 
     * @param a            正在操作的元素 A (如索引或节点)
     * @param b            正在操作的元素 B (如对比对象)
     * @param compareCount 实时比较计数
     * @param actionCount  实时操作计数
     */
    protected void onSync(S structure, Object a, Object b, int compareCount, int actionCount) {
        // 核心阻塞机制：确保渲染完成前算法不会继续跑
        AlgorithmThreadManager.syncAndWait(() -> {
            // 以下逻辑运行在 JavaFX Application Thread
            if (visualizer != null) {
                // 调用具体的 Canvas 或 SVG 绘图引擎
                visualizer.render(structure, a, b);
            }
            // 更新状态栏文字
            updateUIComponents(compareCount, actionCount);
        });
    }

    /**
     * 实现 StepListener 接口逻辑：处理算法步进节奏
     * 职责：检查当前算法是否处于暂停状态或是否已被中断。
     */
    protected void onStep() {
        // 由管理器统一检查中断标志位和暂停锁
        AlgorithmThreadManager.checkStepStatus();
    }

    /**
     * 启动/重启算法执行
     * * @param algorithm 具体的算法实例
     * 
     * @param data 待操作的数据实体
     */
    public void startAlgorithm(BaseAlgorithms<S> algorithm, S data) {
        // 1. 若当前有任务正在运行，强制停止并清理现场
        stopAlgorithm();

        // 2. 将控制器的回调注入算法环境
        // 这里的 this::onSync 能够完美匹配是因为泛型约束 S 已经对齐
        algorithm.setEnvironment(this::onSync, this::onStep);

        // 3. 提交任务到独立算法线程池
        AlgorithmThreadManager.run(() -> {
            try {
                // 调用具体的执行钩子（如 alg.run 或 alg.sort）
                executeAlgorithm(algorithm, data);
            } catch (Exception e) {
                // 捕获异常并反馈到 UI
                handleAlgorithmError(e);
            } finally {
                // 无论成功失败，在结束时重置状态并通知子类
                Platform.runLater(this::onAlgorithmFinished);
            }
        });
    }

    /**
     * 强制停止当前运行的算法线程
     */
    public void stopAlgorithm() {
        AlgorithmThreadManager.stopAll();
    }

    /**
     * 切换暂停/恢复状态
     */
    public void togglePause() {
        if (AlgorithmThreadManager.isPaused()) {
            AlgorithmThreadManager.resume();
        } else {
            AlgorithmThreadManager.pause();
        }
    }

    // --- 属性访问与注入 ---

    public Region getView() {
        return (Region) visualizer;
    }

    public BaseVisualizer<S> getVisualizer() {
        return visualizer;
    }

    /**
     * 由具体子类 Controller 调用，注入 FXML 控件引用
     */
    public void setUIReferences(Label statsLabel, TextArea logArea, Slider delaySlider) {
        this.statsLabel = statsLabel;
        this.logArea = logArea;
        this.delaySlider = delaySlider;
        // 注入后重新触发一遍初始化绑定
        initialize(null, null);
    }

    public boolean isRunning() {
        return AlgorithmThreadManager.isRunning();
    }

    public boolean isPaused() {
        return AlgorithmThreadManager.isPaused();
    }

    // --- 子类具体实现钩子 ---

    /** 具体算法的启动逻辑，通常调用 algorithm.run(data) 或 sort(data) */
    protected abstract void executeAlgorithm(BaseAlgorithms<S> algorithm, S data);

    /** 更新 UI 面板上的比较次数、操作次数、时间等信息 */
    protected abstract void updateUIComponents(int compareCount, int actionCount);

    /** 获取每个算法特有的 UI 控件列表（如“随机生成”、“重置”按钮） */
    public abstract List<Node> getCustomControls();

    /** 按钮触发器：处理启动逻辑，通常调用 startAlgorithm */
    public abstract void handleAlgorithmStart();

    // --- 可选重写钩子 ---

    protected void handleAlgorithmError(Exception e) {
        e.printStackTrace();
        Platform.runLater(() -> {
            if (logArea != null) {
                logArea.appendText("❌ Algorithm Error: " + e.getLocalizedMessage() + "\n");
            }
        });
    }

    protected void onAlgorithmFinished() {
        if (logArea != null) {
            logArea.appendText("✅ System: Algorithm execution completed.\n");
        }
    }
}