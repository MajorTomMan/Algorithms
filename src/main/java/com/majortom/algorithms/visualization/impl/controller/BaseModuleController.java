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
 * 模块控制器基类
 * 职责：
 * 1. 自动加载模块专属的 FXML 控制面板
 * 2. 统一管理线程安全的国际化日志 (logI18n)
 * 3. 规范化性能统计信息的展示格式
 */
public abstract class BaseModuleController<S extends BaseStructure<?>> extends BaseController<S> {

    protected Node controlPanel; // 对应的自定义控制面板组件

    /**
     * @param visualizer 对应的可视化器
     * @param fxmlPath   自定义控制面板的 FXML 路径 (如 "/fxml/GraphControls.fxml")
     */
    public BaseModuleController(BaseVisualizer<S> visualizer, String fxmlPath) {
        super(visualizer);
        loadControlPanel(fxmlPath);
    }

    /**
     * 加载 FXML 控制面板并设置 Controller 为当前类
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
     * 实现父类 API：将自定义控件注入主界面底栏
     */
    @Override
    public void setupCustomControls(HBox container) {
        if (container != null && controlPanel != null) {
            container.getChildren().clear();
            container.getChildren().add(controlPanel);
        }
    }

    /**
     * 线程安全的国际化日志输出
     * 
     * @param key  I18N 资源文件中的 Key
     * @param args 格式化参数
     */
    protected void logI18n(String key, Object... args) {
        AlgorithmThreadManager.postStatus(() -> {
            if (logArea != null) {
                try {
                    String pattern = I18N.getBundle().getString(key);
                    appendLog(String.format(pattern, args));
                } catch (Exception e) {
                    appendLog(key); // 找不到 Key 时降级输出原始字符串
                }
            }
        });
    }

    /**
     * 统一实现统计信息刷新逻辑
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
     * 默认的统计信息格式：操作数 + 比较数
     * 子类如需显示耗时或特定指标，可重写此方法
     */
    @Override
    protected String formatStatsMessage() {
        try {
            String sK = I18N.getBundle().getString("stats.action");
            String cK = I18N.getBundle().getString("stats.compare");
            return String.format("%s: %d | %s: %d", sK, stats.actionCount, cK, stats.compareCount);
        } catch (Exception e) {
            return "Actions: " + stats.actionCount + " | Compares: " + stats.compareCount;
        }
    }

    /**
     * 模块清理逻辑：停止算法并清空日志
     */
    public void resetModule() {
        stopAlgorithm();
        AlgorithmThreadManager.postStatus(() -> {
            if (logArea != null)
                logArea.clear();
            if (statsLabel != null)
                statsLabel.setText(I18N.getBundle().getString("side.ready"));
            onResetData();
        });
    }

    /**
     * 子类实现：重置数据结构本身的状态
     */
    protected abstract void onResetData();
}