package com.majortom.algorithms.core.visualization.impl.window;

import atlantafx.base.theme.Styles;
import com.majortom.algorithms.core.maze.BaseMaze;
import com.majortom.algorithms.core.maze.strategies.MazeGeneratorStrategy;
import com.majortom.algorithms.core.maze.strategies.PathfindingStrategy;
import com.majortom.algorithms.core.visualization.BaseWindow;
import com.majortom.algorithms.core.visualization.impl.visualizer.MazeVisualizer;
import javafx.application.Platform;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.stage.Stage;

/**
 * 迷宫算法可视化窗口
 * 职责：适配 FXML 架构，支持迷宫生成与寻路双阶段交互
 */
public class MazeWindow<T> extends BaseWindow<int[][]> {

    private static final String BTN_GENERATE = "生成迷宫";
    private static final String BTN_PATHFIND = "开始寻路";
    private static final String LOG_RESET = "状态:\n迷宫已重置。";
    private static final String LOG_FORMAT = "实时坐标:\nRow: %s\nCol: %s\n操作: %d";

    private final BaseMaze<int[][]> maze;
    private MazeVisualizer mazeVisualizer;

    private MazeGeneratorStrategy<int[][]> generator;
    private PathfindingStrategy<int[][]> pathfinder;
    private Button pathfindBtn;

    public MazeWindow(BaseMaze<int[][]> maze) {
        this.maze = maze;
    }

    @Override
    protected Region createCenterComponent() {
        // 初始化霓虹版画布
        this.mazeVisualizer = new MazeVisualizer(maze.getData());

        // 包装在 ScrollPane 中以支持大型迷宫滚动
        ScrollPane scrollPane = new ScrollPane(mazeVisualizer);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        // 保持深色背景，移除边框
        scrollPane.setStyle("-fx-background: #1A1A1A; -fx-background-color: transparent; -fx-border-color: transparent;"); 

        return scrollPane;
    }

    @Override
    protected void setupActions() {
        // 1. 初始化 FXML 提供的开始按钮
        startBtn.setText(BTN_GENERATE);
        startBtn.getStyleClass().add(Styles.ACCENT);

        // 2. 动态创建寻路按钮（FXML 中未定义此特有按钮）
        pathfindBtn = new Button(BTN_PATHFIND);
        pathfindBtn.getStyleClass().addAll(Styles.BUTTON_OUTLINED, Styles.SUCCESS);
        pathfindBtn.setDisable(true);
        pathfindBtn.setPrefHeight(35); // 匹配 FXML 按钮高度

        // 将寻路按钮插入到控制栏
        HBox footer = (HBox) rootPane.getBottom();
        footer.getChildren().add(1, pathfindBtn); // 插入在 startBtn 之后

        // 3. 绑定算法监听器到基类的 onSync [cite: 2026-01-10]
        this.maze.setListener((data, a, b, comp, act) -> {
            this.onSync(data, a, b, comp, act);
        });

        // 4. 生成按钮逻辑
        startBtn.setOnAction(e -> {
            if (generator != null) {
                this.setTask(() -> {
                    maze.initial();
                    maze.resetStatistics();
                    generator.generate(maze);
                    maze.pickRandomPoints();
                    Platform.runLater(() -> pathfindBtn.setDisable(false));
                });
                this.executeTask();
            }
        });

        // 5. 寻路按钮逻辑
        pathfindBtn.setOnAction(e -> {
            if (pathfinder != null) {
                this.setTask(() -> {
                    maze.resetStatistics();
                    pathfinder.findPath(maze);
                });
                this.executeTask();
            }
        });
    }

    @Override
    protected void handleDataReset() {
        if (this.maze != null) {
            this.maze.initial();
            this.maze.resetStatistics();
        }
        // 同步 UI 状态
        this.mazeVisualizer.updateState(this.maze.getData(), null, null);
        this.pathfindBtn.setDisable(true);
        logArea.setText(LOG_RESET);
    }

    @Override
    protected void refresh(int[][] data, Object r, Object c) {
        // 更新画布
        mazeVisualizer.updateState(data, r, c);

        // 更新侧边栏日志 (适配 XML 中的 logArea)
        if (r != null && c != null) {
            logArea.setText(String.format(LOG_FORMAT, r, c, maze.getActionCount()));
        }
    }

    // --- 配置接口 ---

    public void setGenerator(MazeGeneratorStrategy<int[][]> generator) {
        this.generator = generator;
    }

    public void setPathfinder(PathfindingStrategy<int[][]> pathfinder) {
        this.pathfinder = pathfinder;
    }

    /**
     * 静态启动入口
     */
    public static void launch(BaseMaze<int[][]> maze,
                              MazeGeneratorStrategy<int[][]> generator,
                              PathfindingStrategy<int[][]> pathfinder) {
        Platform.runLater(() -> {
            try {
                MazeWindow<int[][]> window = new MazeWindow<>(maze);
                window.setGenerator(generator);
                window.setPathfinder(pathfinder);

                Stage stage = new Stage();
                window.start(stage);
                stage.setTitle("迷宫实验室 - 2D Grid");
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
}