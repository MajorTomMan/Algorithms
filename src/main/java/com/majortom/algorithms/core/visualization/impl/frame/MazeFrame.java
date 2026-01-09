package com.majortom.algorithms.core.visualization.impl.frame;

import com.majortom.algorithms.core.maze.BaseMaze;
import com.majortom.algorithms.core.maze.strategies.MazeGeneratorStrategy;
import com.majortom.algorithms.core.maze.strategies.PathfindingStrategy;
import com.majortom.algorithms.core.visualization.BaseFrame;
import com.majortom.algorithms.core.visualization.impl.panel.MazePanel;

import javax.swing.*;

public class MazeFrame<T> extends BaseFrame<int[][]> {

    private static final String TITLE_PREFIX = "迷宫算法可视化 - ";
    private static final String BTN_GENERATE = "生成迷宫";
    private static final String BTN_PATHFIND = "开始寻路";
    private static final String LOG_RESET = "状态:\n迷宫已重置。";
    private static final String LOG_FORMAT = "实时坐标:\nRow: %s\nCol: %s\n操作: %d";

    private final BaseMaze<int[][]> maze;
    private final MazePanel mazePanel;

    private MazeGeneratorStrategy<int[][]> generator;
    private PathfindingStrategy<int[][]> pathfinder;
    private JButton pathfindBtn;

    public MazeFrame(BaseMaze<int[][]> maze, int cellSize) {
        super(TITLE_PREFIX + maze.getClass().getSimpleName());
        this.maze = maze;

        // 1. 初始化画布
        this.mazePanel = new MazePanel(maze.getData(), cellSize);

        // 2. 使用 MigLayout 约束添加画布（带滚动条）
        JScrollPane scrollPane = new JScrollPane(mazePanel);
        scrollPane.setBorder(null);
        add(scrollPane, "center, grow");

        // 3. 注册监听器并初始化
        this.maze.setListener(this);
        setupMazeActions();

        initAndLaunch();
    }

    private void setupMazeActions() {
        startBtn.setText(BTN_GENERATE);

        // 使用 FlatLaf 风格注入寻路按钮
        // 1. 初始化寻路按钮
        pathfindBtn = new JButton(BTN_PATHFIND);
        pathfindBtn.putClientProperty("JButton.buttonType", "roundRect");
        pathfindBtn.setEnabled(false);

        // 插入到 startBtn 之后
        controlPanel.add(pathfindBtn, "gapleft 5");

        // A. 生成按钮逻辑
        startBtn.addActionListener(e -> {
            if (generator != null) {
                this.setTask(() -> {
                    maze.initial();
                    maze.resetStatistics();
                    generator.generate(maze);
                    maze.pickRandomPoints();
                    SwingUtilities.invokeLater(() -> pathfindBtn.setEnabled(true));
                });
                this.executeTask();
            }
        });

        // B. 寻路按钮逻辑
        pathfindBtn.addActionListener(e -> {
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
        this.mazePanel.updateData(this.maze.getData(), null, null);
        this.pathfindBtn.setEnabled(false);
        dataListArea.setText(LOG_RESET);
    }

    @Override
    protected void refresh(int[][] data, Object r, Object c) {
        mazePanel.updateData(data, r, c);
        if (r != null && c != null) {
            dataListArea.setText(String.format(LOG_FORMAT, r, c, maze.getActionCount()));
        }
    }

    public void setGenerator(MazeGeneratorStrategy<int[][]> generator) {
        this.generator = generator;
    }

    public void setPathfinder(PathfindingStrategy<int[][]> pathfinder) {
        this.pathfinder = pathfinder;
    }

    public static void launch(BaseMaze<int[][]> maze, int cellSize,
            MazeGeneratorStrategy<int[][]> generator,
            PathfindingStrategy<int[][]> pathfinder) {
        SwingUtilities.invokeLater(() -> {
            MazeFrame<int[][]> frame = new MazeFrame<>(maze, cellSize);
            frame.setGenerator(generator);
            frame.setPathfinder(pathfinder);
        });
    }
}