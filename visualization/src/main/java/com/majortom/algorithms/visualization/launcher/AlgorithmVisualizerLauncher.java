package com.majortom.algorithms.visualization.launcher;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Locale;
import java.util.ResourceBundle;

import com.majortom.algorithms.visualization.manager.AlgorithmThreadManager;

import atlantafx.base.theme.PrimerDark;

/**
 * 算法可视化实验室 JavaFX 启动入口。
 *
 * <p>它负责初始化主题、加载主 FXML、创建响应式 Scene，并在窗口关闭时停止后台算法线程。</p>
 */
public class AlgorithmVisualizerLauncher extends Application {

    /**
     * JavaFX 应用启动回调。
     *
     * @param primaryStage 主窗口
     */
    @Override
    public void start(Stage primaryStage) {
        Application.setUserAgentStylesheet(new PrimerDark().getUserAgentStylesheet());
        try {
            // 1. 加载主界面 FXML
            ResourceBundle bundle = ResourceBundle.getBundle("language.language", Locale.getDefault());
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/MainControls.fxml"));
            loader.setResources(bundle);
            // 2. 创建整界面等比缩放的场景。
            // 1800px 是按底栏最宽的迷宫模块反推的默认宽度：
            // 全局 6 个按钮 + 两个滑块 + 结构/生成/求解/操作控件约需要 1760px。
            Scene scene = ResponsiveStageScaler.createScene(loader.load(), 1800, 860);
            scene.setFill(Color.web("#0A0A0E"));

            // 3. 配置窗口属性
            primaryStage.setTitle("Algorithms");
            primaryStage.setScene(scene);
            primaryStage.setMinWidth(1280);
            primaryStage.setMinHeight(640);

            // 4. 优雅退出：确保程序关闭时停止所有后台算法线程
            primaryStage.setOnCloseRequest(event -> {
                AlgorithmThreadManager.stopAll();
                System.exit(0);
            });
            primaryStage.show();

        } catch (IOException e) {
            System.err.println("[Critical Error] Failed to launch the main frame:");
            e.printStackTrace();
        }
    }

    /**
     * 程序运行主入口。
     *
     * @param args 命令行参数
     */
    public static void launch(String[] args) {
        // 部分操作系统（如 macOS）可能需要指定此属性以优化 GraphStream 性能
        System.setProperty("prism.lcdtext", "false");
        // 修复Ubuntu 显卡过来驱动失效导致界面卡死的问题
        System.setProperty("prism.order", "sw");
        Application.launch(AlgorithmVisualizerLauncher.class, args);
    }
}
