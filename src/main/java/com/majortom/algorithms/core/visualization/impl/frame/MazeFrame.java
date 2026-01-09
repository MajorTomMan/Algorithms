package com.majortom.algorithms.core.visualization.impl.frame;

import com.majortom.algorithms.core.maze.BaseMaze;
import com.majortom.algorithms.core.visualization.BaseFrame;
import com.majortom.algorithms.core.visualization.impl.panel.MazePanel;
import java.awt.BorderLayout;
import javax.swing.SwingUtilities;

/**
 * 迷宫可视化窗体实现（Maze Visualization Frame）
 * * 职责：
 * 1. 资源协调：将 BaseMaze 算法实例与 MazePanel 渲染组件进行绑定。
 * 2. 状态映射：将算法同步过来的二维坐标（r, c）映射为画布上的“探针”焦点。
 * 3. 流程控制：管理迷宫生成的子线程启动、统计数据重置及网格初始化。
 */
public class MazeFrame extends BaseFrame<int[][]> {
    /** 迷宫算法逻辑对象 */
    private final BaseMaze maze;
    /** 负责二维网格渲染的画布 */
    private final MazePanel mazePanel;

    /**
     * 构造函数
     * 
     * @param maze     迷宫算法实例（如 RecursiveBacktrackerMaze）
     * @param cellSize 单个网格的显示尺寸（像素）
     */
    public MazeFrame(BaseMaze maze, int cellSize) {
        super("迷宫生成实验室 - " + maze.getClass().getSimpleName());
        this.maze = maze;

        // 根据算法当前的地图状态创建 MazePanel
        this.mazePanel = new MazePanel(maze.getMap(), cellSize);
        add(mazePanel, BorderLayout.CENTER);

        // 将当前窗体注册为算法的同步监听器（SyncListener）
        // 算法内部在“拆墙”或“移动”时，会回调此窗体的 onSync 方法。
        this.maze.setListener(this);

        // 定义开始生成与重置地图的动作
        setupActions();

        // 计算布局并居中显示
        initAndLaunch();
    }

    /**
     * 配置迷宫控制逻辑
     */
    private void setupActions() {
        // 1. 预装载算法任务（装弹阶段）
        // 定义点击“开始”按钮时在子线程执行的具体闭包逻辑
        this.setTask(() -> {
            // 运行前重置算法内部统计数据（如探索步数、路径长度等）
            maze.resetStatistics();
            // 执行具体的迷宫生成逻辑（触发同步回调更新 UI）
            maze.generate();
        });
        // 重置按钮逻辑
        resetBtn.addActionListener(e -> {
            maze.initial(); // 将地图回归到全墙（或初始）状态
            maze.resetStatistics(); // 重置计数器
            startTime = 0; // 重置基类计时器

            // 立即同步一帧初始界面，清除所有“探针”高亮
            refresh(maze.getMap(), null, null);
            startBtn.setEnabled(true);
        });
    }

    /**
     * 实现 refresh 接口：定义迷宫数据的刷新细节
     * 
     * @param data 最新迷宫矩阵状态
     * @param r    当前算法操作的行坐标（Row）
     * @param c    当前算法操作的列坐标（Col）
     */
    @Override
    protected void refresh(int[][] data, Object r, Object c) {
        // 将二维数据和当前的坐标焦点传递给 Panel 进行重绘
        // 此处的 r 和 c 会被 MazePanel 用于绘制黄色光标“探针”
        mazePanel.updateData(data, r, c);
    }

    /**
     * 静态工厂启动入口
     * 
     * @param maze     算法实现
     * @param cellSize 网格大小
     */
    public static void launch(BaseMaze maze, int cellSize) {
        SwingUtilities.invokeLater(() -> {
            MazeFrame frame = new MazeFrame(maze, cellSize);
            frame.initAndLaunch();

            // 装载任务：设置待执行的任务闭包
            // 当用户点击 startBtn 时，基类会自动调用此任务
            frame.setTask(maze::generate);
        });
    }
}