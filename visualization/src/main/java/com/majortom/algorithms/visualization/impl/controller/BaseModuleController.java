package com.majortom.algorithms.visualization.impl.controller;

import com.majortom.algorithms.core.base.BaseStructure;
import com.majortom.algorithms.visualization.BaseController;
import com.majortom.algorithms.visualization.BaseVisualizer;
import com.majortom.algorithms.visualization.international.I18N;
import com.majortom.algorithms.visualization.manager.AlgorithmThreadManager;

import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.layout.HBox;

import java.io.IOException;

/**
 * 模块控制器基类。
 *
 * <p>它在 {@link BaseController} 的通用执行能力之上，增加模块专属 FXML 控制面板加载、
 * 国际化日志输出和默认统计文案。排序、树、图、迷宫控制器都可以继承它。</p>
 *
 * @param <S> 当前模块的数据结构类型
 */
public abstract class BaseModuleController<S extends BaseStructure<?>> extends BaseController<S> {

    /**
     * 模块自定义控制面板节点。
     */
    protected Node controlPanel; // 对应的自定义控制面板组件

    /**
     * 创建模块控制器并加载专属控制面板。
     *
     * @param visualizer 对应的可视化器
     * @param fxmlPath   自定义控制面板的 FXML 路径 (如 "/fxml/GraphControls.fxml")
     */
    public BaseModuleController(BaseVisualizer<S> visualizer, String fxmlPath) {
        super(visualizer);
        loadControlPanel(fxmlPath);
    }

    /**
     * 加载 FXML 控制面板并设置 Controller 为当前类。
     *
     * @param fxmlPath FXML 资源路径
     */
    private void loadControlPanel(String fxmlPath) {
        if (fxmlPath == null || fxmlPath.isEmpty())
            return;

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            loader.setResources(I18N.getBundle());
            loader.setController(this);
            this.controlPanel = loader.load();
        } catch (IOException e) {
            System.err.println("[Error] Module control panel load failed: " + fxmlPath);
            e.printStackTrace();
        }
    }

    /**
     * 将自定义控件注入主界面底栏。
     *
     * @param container 主界面底栏容器
     */
    @Override
    public void setupCustomControls(HBox container) {
        if (container != null && controlPanel != null) {
            container.getChildren().clear();
            container.getChildren().add(controlPanel);
        }
    }

    /**
     * 线程安全的国际化日志输出。
     * 
     * @param key  I18N 资源文件中的 Key
     * @param args 格式化参数
     */
    protected void logI18n(String key, Object... args) {
        AlgorithmThreadManager.postStatus(() -> {
            if (logArea != null) {
                appendLog(I18N.text(key, args));
            }
        });
    }

    /**
     * 线程安全地刷新统计信息。
     */
    @Override
    protected void refreshStatsDisplay() {
        AlgorithmThreadManager.postStatus(() -> {
            if (statsLabel != null) {
                statsLabel.setText(formatStatsMessage());
            }
        });
    }

    /**
     * 默认统计信息格式。
     *
     * @return 操作数和比较数文本
     */
    @Override
    protected String formatStatsMessage() {
        try {
            return formatMetric("stats.action", stats.actionCount())
                    + " | "
                    + formatMetric("stats.compare", stats.compareCount());
        } catch (Exception e) {
            return "Actions: " + stats.actionCount() + " | Compares: " + stats.compareCount();
        }
    }

    /**
     * 格式化单个国际化指标。
     *
     * @param key 国际化 key
     * @param value 指标值
     * @return 格式化后的文本
     */
    protected final String formatMetric(String key, int value) {
        return I18N.text(key, value);
    }

    /**
     * 重置模块运行状态和日志。
     */
    public void resetModule() {
        stopAlgorithm();
        AlgorithmThreadManager.postStatus(() -> {
            if (logArea != null)
                logArea.clear();
            if (statsLabel != null)
                statsLabel.setText(I18N.text("status.system.ready"));
            onResetData();
        });
    }

    /**
     * 子类实现：重置数据结构本身的状态。
     */
    protected abstract void onResetData();
}
