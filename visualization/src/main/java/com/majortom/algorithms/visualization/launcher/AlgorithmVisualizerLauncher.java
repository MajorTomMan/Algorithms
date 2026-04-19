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
 * 算法可视化实验室 - 启动入口
 * 职责：初始化主窗口，加载全局 CSS 样式，并配置 JavaFX 渲染环境。
 */
public class AlgorithmVisualizerLauncher extends Application {

    private static final double DESIGN_WIDTH = 1520;
    private static final double DESIGN_HEIGHT = 900;

    @Override
    public void start(Stage primaryStage) {
        Application.setUserAgentStylesheet(new PrimerDark().getUserAgentStylesheet());
        try {
            // 1. 加载主界面 FXML
            ResourceBundle bundle = ResourceBundle.getBundle("language.language", Locale.getDefault());
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/MainControls.fxml"));
            loader.setResources(bundle);
            // 2. 创建整界面等比缩放的场景
            Scene scene = ResponsiveStageScaler.createScene(loader.load(), DESIGN_WIDTH, DESIGN_HEIGHT);
            scene.setFill(Color.web("#0A0A0E"));

            // 3. 配置窗口属性
            primaryStage.setTitle("Algorithms");
            primaryStage.setScene(scene);
            primaryStage.setWidth(DESIGN_WIDTH);
            primaryStage.setHeight(DESIGN_HEIGHT);
            primaryStage.setMinWidth(DESIGN_WIDTH);
            primaryStage.setMinHeight(DESIGN_HEIGHT);
            primaryStage.setMaxWidth(DESIGN_WIDTH);
            primaryStage.setMaxHeight(DESIGN_HEIGHT);
            primaryStage.setResizable(false);
            primaryStage.centerOnScreen();

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
     * 程序运行主入口
     */
    public static void launch(String[] args) {
        // 部分操作系统（如 macOS）可能需要指定此属性以优化 GraphStream 性能
        System.setProperty("prism.lcdtext", "false");
        // 修复Ubuntu 显卡过来驱动失效导致界面卡死的问题
        System.setProperty("prism.order", "sw");
        Application.launch(AlgorithmVisualizerLauncher.class, args);
    }
}
