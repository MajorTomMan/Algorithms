package com.majortom.algorithms.core.visualization.impl.frame;

import com.majortom.algorithms.core.maze.BaseMaze;
import com.majortom.algorithms.core.maze.strategies.MazeGeneratorStrategy;
import com.majortom.algorithms.core.maze.strategies.PathfindingStrategy;
import com.majortom.algorithms.core.visualization.BaseFrame;
import com.majortom.algorithms.core.visualization.impl.panel.MazePanel;

import javax.swing.*;
import java.awt.BorderLayout;

public class MazeFrame<T> extends BaseFrame<int[][]> {

    // --- UI 文本常量 ---
    private static final String TITLE_PREFIX = "迷宫算法可视化 - ";
    private static final String BTN_GENERATE = "生成迷宫";
    private static final String BTN_PATHFIND = "开始寻路";
    private static final String LOG_RESET = "状态:\n迷宫已重置。";
    private static final String LOG_FORMAT = "实时坐标:\nRow: %s\nCol: %s\n操作: %d";

    // --- 布局常量 ---
    private static final int PATHFIND_BTN_INDEX = 2; // 寻路按钮在控制面板中的插入位置

    /** 迷宫算法逻辑对象（此处 T 为 int[][]） */
    private final BaseMaze<int[][]> maze;
    /** 负责渲染的画布 */
    private final MazePanel mazePanel;

    /** 策略插槽 */
    private MazeGeneratorStrategy<int[][]> generator;
    private PathfindingStrategy<int[][]> pathfinder;

    /** 额外的寻路控制按钮 */
    private JButton pathfindBtn;

    public MazeFrame(BaseMaze<int[][]> maze, int cellSize) {
        super(TITLE_PREFIX + maze.getClass().getSimpleName());
        this.maze = maze;

        // 1. 初始化画布
        this.mazePanel = new MazePanel(maze.getData(), cellSize);

        // 增加滚动支持
        JScrollPane scrollPane = new JScrollPane(mazePanel);
        scrollPane.setBorder(null);
        add(scrollPane, BorderLayout.CENTER);

        // 2. 注册监听器
        this.maze.setListener(this);

        // 3. 配置交互动作
        setupActions();

        // 4. 启动窗体
        initAndLaunch();

        // 强制 pack 以适应 MazePanel 的 PreferredSize
        this.pack();
        this.setLocationRelativeTo(null);
    }

    public void setGenerator(MazeGeneratorStrategy<int[][]> generator) {
        this.generator = generator;
    }

    public void setPathfinder(PathfindingStrategy<int[][]> pathfinder) {
        this.pathfinder = pathfinder;
    }

    private void setupActions() {
        // 修改基类按钮文本
        startBtn.setText(BTN_GENERATE);

        // 动态添加寻路按钮
        pathfindBtn = new JButton(BTN_PATHFIND);
        pathfindBtn.setEnabled(false);
        controlPanel.add(pathfindBtn, PATHFIND_BTN_INDEX);

        // A. 生成按钮逻辑
        startBtn.addActionListener(e -> {
            if (generator != null) {
                this.setTask(() -> {
                    maze.initial();
                    maze.resetStatistics();
                    generator.generate(maze);
                    maze.pickRandomPoints();
                    // 生成完成后，通过事件队列启用寻路按钮
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

        // C. 重置按钮逻辑
        resetBtn.addActionListener(e -> {
            handleDataReset();
            startTime = 0;
            startBtn.setEnabled(true);
            dataListArea.setText(LOG_RESET);
        });
    }

    @Override
    protected void handleDataReset() {
        if (this.maze != null) {
            this.maze.initial();
            this.maze.resetStatistics();
        }
        // 重置时清空画布标记并禁用寻路
        this.mazePanel.updateData(this.maze.getData(), null, null);
        this.pathfindBtn.setEnabled(false);
    }

    @Override
    protected void refresh(int[][] data, Object r, Object c) {
        // 更新图形画布
        mazePanel.updateData(data, r, c);

        // 更新侧边栏状态，消除字符串模板硬编码
        if (r != null && c != null) {
            dataListArea.setText(String.format(LOG_FORMAT, r, c, maze.getActionCount()));
        }
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