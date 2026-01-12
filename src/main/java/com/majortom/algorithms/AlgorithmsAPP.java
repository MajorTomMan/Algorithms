package com.majortom.algorithms;

import atlantafx.base.theme.PrimerDark;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

/**
 * 算法实验室 - 统一启动入口 [2026-01-12]
 * 职责：整合 排序、树、图、地图 四大模块的可视化页面
 */
public class AlgorithmsAPP extends Application {

    @Override
    public void start(Stage primaryStage) {
        // 1. 激活文艺忧郁的深色主题 (AtlantaFX)
        Application.setUserAgentStylesheet(new PrimerDark().getUserAgentStylesheet());

        // 2. 创建主布局容器
        TabPane mainTabPane = new TabPane();
        mainTabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        // 3. 注入四大实验模块标签页
        mainTabPane.getTabs().addAll(
                createSortTab(),
                createTreeTab(),
                createGraphTab(),
                createMazeTab()
        );

        // 4. 配置主舞台
        Scene scene = new Scene(mainTabPane, 1280, 850);
        primaryStage.setTitle("MajorTom Algorithm Lab | 峻豪的算法实验室");
        primaryStage.setScene(scene);
        primaryStage.show();

        System.out.println("=== [2026-01-12] 实验室系统已就绪，欢迎进入算法的世界 ===");
    }

    // --- 各模块 UI 构造方法 (当前为占位布局) ---

    private Tab createSortTab() {
        Tab tab = new Tab("排序算法 (Sort)");
        tab.setContent(createPlaceholderLayout("排序可视化画布区域", "执行冒泡/快速排序"));
        return tab;
    }

    private Tab createTreeTab() {
        Tab tab = new Tab("平衡树 (AVL Tree)");
        // 这里后续放入你的 TreeCanvas
        tab.setContent(createPlaceholderLayout("AVL 树动态旋转画布", "插入节点进行自平衡"));
        return tab;
    }

    private Tab createGraphTab() {
        Tab tab = new Tab("图论展示 (Graph)");
        tab.setContent(createPlaceholderLayout("图论渲染区域 (GraphStream集成)", "执行 BFS / DFS 搜索"));
        return tab;
    }

    private Tab createMazeTab() {
        Tab tab = new Tab("迷宫寻路 (Maze/Map)");
        tab.setContent(createPlaceholderLayout("31x31 迷宫网格", "执行 A* 寻路算法"));
        return tab;
    }

    /**
     * 通用的占位符布局构造器
     */
    private BorderPane createPlaceholderLayout(String centerText, String btnText) {
        BorderPane pane = new BorderPane();
        pane.setPadding(new Insets(20));

        // 中间：黑色背景的预览区
        StackPane canvasArea = new StackPane(new Label(centerText));
        canvasArea.setStyle("-fx-background-color: #0F0F13; -fx-border-color: #333333; -fx-border-radius: 10;");
        pane.setCenter(canvasArea);

        // 底部：控制台
        HBox controls = new HBox(15);
        controls.setPadding(new Insets(15, 0, 0, 0));
        controls.setAlignment(Pos.CENTER_LEFT);
        
        Button actionBtn = new Button(btnText);
        actionBtn.getStyleClass().add("accent"); // 蓝色高亮样式
        
        controls.getChildren().addAll(new Label("控制面板:"), actionBtn);
        pane.setBottom(controls);

        return pane;
    }

    /**
     * 静态主入口
     */
    public static void main(String[] args) {
        // 启动 JavaFX 运行时环境
        launch(args);
    }
}