package com.majortom.algorithms.core.visualization.impl.frame;

import com.majortom.algorithms.core.maze.BaseMaze;
import com.majortom.algorithms.core.maze.strategies.MazeGeneratorStrategy;
import com.majortom.algorithms.core.maze.strategies.PathfindingStrategy;
import com.majortom.algorithms.core.visualization.BaseFrame;
import com.majortom.algorithms.core.visualization.impl.panel.MazePanel;

import javax.swing.*;
import java.awt.BorderLayout;

public class MazeFrame<T> extends BaseFrame<int[][]> {
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
        super("迷宫实验室 - " + maze.getClass().getSimpleName());
        this.maze = maze;

        // 1. 初始化画布
        this.mazePanel = new MazePanel(maze.getData(), cellSize);

        // 建议增加滚动支持，防止大迷宫缩成一点
        JScrollPane scrollPane = new JScrollPane(mazePanel);
        scrollPane.setBorder(null);
        add(scrollPane, BorderLayout.CENTER);

        // 2. 注册监听器
        this.maze.setListener(this);

        // 3. 配置交互动作（包含双按钮逻辑）
        setupActions();

        // 4. 启动窗体（计算布局并居中）
        initAndLaunch();

        // 关键修复：强制 pack 以适应 MazePanel 的 PreferredSize
        this.pack();
        this.setLocationRelativeTo(null);
    }

    /**
     * 设置生成策略
     */
    public void setGenerator(MazeGeneratorStrategy<int[][]> generator) {
        this.generator = generator;
    }

    /**
     * 设置寻路策略
     */
    public void setPathfinder(PathfindingStrategy<int[][]> pathfinder) {
        this.pathfinder = pathfinder;
    }

    /**
     * 核心逻辑：配置迷宫控制按钮
     */
    private void setupActions() {
        // 修改基类按钮文本
        startBtn.setText("生成迷宫");

        // 动态添加寻路按钮
        pathfindBtn = new JButton("开始寻路");
        pathfindBtn.setEnabled(false); // 初始不可点
        controlPanel.add(pathfindBtn, 2);

        // A. 生成按钮逻辑
        startBtn.addActionListener(e -> {
            if (generator != null) {
                // 装载“生成”任务并执行
                this.setTask(() -> {
                    maze.initial();
                    maze.resetStatistics();
                    generator.generate(maze);
                    // 结束后启用寻路按钮
                    SwingUtilities.invokeLater(() -> pathfindBtn.setEnabled(true));
                });
                // 调用基类执行线程的方法（假设为 executeTask）
                this.executeTask();
            }
        });

        // B. 寻路按钮逻辑
        pathfindBtn.addActionListener(e -> {
            if (pathfinder != null) {
                // 装载“寻路”任务并执行
                this.setTask(() -> {
                    maze.resetStatistics();
                    pathfinder.findPath(maze);
                });
                this.executeTask();
            }
        });

        // C. 重置按钮逻辑（保留并扩展成功代码中的逻辑）
        resetBtn.addActionListener(e -> {
            maze.initial();
            maze.resetStatistics();
            startTime = 0;
            pathfindBtn.setEnabled(false); // 重置后禁用寻路
            refresh(maze.getData(), null, null);
            startBtn.setEnabled(true);
            dataListArea.setText("状态:\n迷宫已重置。");
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
    }

    @Override
    protected void refresh(int[][] data, Object r, Object c) {
        // 更新图形画布
        mazePanel.updateData(data, r, c);

        // 更新侧边栏状态
        if (r != null && c != null) {
            dataListArea.setText(String.format("实时坐标:\nRow: %s\nCol: %s\n操作: %d",
                    r, c, maze.getActionCount()));
        }
    }

    /**
     * 静态工厂启动入口
     */
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