package com.majortom.algorithms.core.visualization;

import atlantafx.base.theme.PrimerDark;
import com.majortom.algorithms.core.base.listener.SyncListener;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * 通用可视化窗体基座 - FXML 驱动版
 * 集成了 AtlantaFX 主题、FXML 自动绑定及算法线程管理
 */
public abstract class BaseWindow<T> extends Application implements SyncListener<T> {

    // --- FXML 注入组件 (名称必须与 MainView.fxml 中的 fx:id 严格一致) ---
    @FXML
    protected HBox controlPanel;
    @FXML
    protected BorderPane rootPane;
    @FXML
    protected StackPane canvasContainer; // 用于挂载 Canvas 的中心容器
    @FXML
    protected Button startBtn, resetBtn;
    @FXML
    protected Slider speedSlider;
    @FXML
    protected TextArea logArea;
    @FXML
    protected Label timeLabel, compareLabel, actionLabel;

    // --- 运行状态变量 ---
    protected long startTime = 0;
    protected Runnable pendingTask;
    private long lastRefreshTime = 0;
    private volatile Thread algorithmThread = null;

    @Override
    public void start(Stage stage) throws IOException {
        setUserAgentStylesheet(new PrimerDark().getUserAgentStylesheet());

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/MainView.fxml"));
        loader.setController(this);
        BorderPane fxmlRoot = loader.load();

        // 此时 controlPanel 已经被 loader 注入实例了
        Region visualizer = createCenterComponent();
        if (visualizer != null && canvasContainer != null) {
            canvasContainer.getChildren().add(visualizer);
        }

        setupActions();

        Scene scene = new Scene(fxmlRoot);
        stage.setScene(scene);
        stage.setTitle("MajorTom Algorithm Lab 2.0");
        stage.setOnCloseRequest(e -> stopAlgorithmThread());
        stage.show();
    }

    /**
     * FXML 映射方法：开始按钮点击
     */
    @FXML
    protected void handleStartAction() {
        if (pendingTask != null) {
            executeTask();
        }
    }

    /**
     * FXML 映射方法：重置按钮点击
     */
    @FXML
    protected void handleResetAction() {
        stopAlgorithmThread();
        this.startTime = 0;
        Platform.runLater(() -> {
            timeLabel.setText("耗时: 0.000s");
            compareLabel.setText("比较: 0");
            actionLabel.setText("操作: 0");
            logArea.setText("系统已重置...\n");
            startBtn.setDisable(false);
            handleDataReset();
        });
    }

    /**
     * 算法同步回调：由算法线程调用此方法更新 UI
     */
    @Override
    public void onSync(T data, Object a, Object b, int compareCount, int actionCount) {
        if (Thread.currentThread().isInterrupted()) {
            throw new RuntimeException("Algorithm Interrupted");
        }

        long now = System.currentTimeMillis();
        double duration = (startTime == 0) ? 0 : (now - startTime) / 1000.0;
        int delay = (int) speedSlider.getValue();

        // 节流刷新：若无延迟则限制刷新率，避免 UI 线程阻塞
        if (delay > 0 || (now - lastRefreshTime) > 16) {
            lastRefreshTime = now;
            Platform.runLater(() -> {
                timeLabel.setText(String.format("%.3f s", duration));
                compareLabel.setText("比较: " + compareCount);
                actionLabel.setText("操作: " + actionCount);
                refresh(data, a, b);
            });
        }

        try {
            if (delay > 0) {
                Thread.sleep(delay);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * 开启独立的算法运行线程
     */
    protected void executeTask() {
        stopAlgorithmThread();
        algorithmThread = new Thread(() -> {
            try {
                Platform.runLater(() -> startBtn.setDisable(true));
                this.startTime = System.currentTimeMillis();
                pendingTask.run();
            } catch (Exception e) {
                System.err.println("Task execution failed: " + e.getMessage());
            } finally {
                Platform.runLater(() -> startBtn.setDisable(false));
            }
        }, "Algorithm-Executor");
        algorithmThread.start();
    }

    public void stopAlgorithmThread() {
        if (algorithmThread != null && algorithmThread.isAlive()) {
            algorithmThread.interrupt();
            algorithmThread = null;
        }
    }

    /** 设置待运行的算法 */
    public void setTask(Runnable task) {
        this.pendingTask = task;
    }

    // --- 抽象接口 ---
    protected abstract Region createCenterComponent();

    protected abstract void refresh(T data, Object a, Object b);

    protected abstract void handleDataReset();

    protected abstract void setupActions();
}