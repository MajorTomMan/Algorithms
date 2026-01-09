package com.majortom.algorithms.core.visualization.impl.frame;

import com.majortom.algorithms.core.maze.BaseMaze;
import com.majortom.algorithms.core.visualization.BaseFrame;
import com.majortom.algorithms.core.visualization.impl.panel.MazePanel;
import java.awt.BorderLayout;
import javax.swing.SwingUtilities;

/**
 * 迷宫可视化窗体实现（Maze Visualization Frame）
 * 职责：
 * 1. 资源协调：绑定 BaseMaze 算法与 MazePanel 渲染组件。
 * 2. 状态映射：将算法坐标 (r, c) 透传给画布进行“探针”渲染。
 * 3. 规范重置：实现 handleDataReset 以支持即时终止生成并回归初始地图。
 */
public class MazeFrame extends BaseFrame<int[][]> {
    /** 迷宫算法逻辑对象 */
    private final BaseMaze maze;
    /** 负责二维网格渲染的画布 */
    private final MazePanel mazePanel;

    /**
     * 构造函数
     * * @param maze 迷宫算法实例
     * 
     * @param cellSize 单个网格的显示尺寸（像素）
     */
    public MazeFrame(BaseMaze maze, int cellSize) {
        super("迷宫生成实验室 - " + maze.getClass().getSimpleName());
        this.maze = maze;

        // 1. 根据算法当前的地图状态创建 MazePanel
        this.mazePanel = new MazePanel(maze.getMap(), cellSize);
        add(mazePanel, BorderLayout.CENTER);

        // 2. 注册监听器
        this.maze.setListener(this);

        // 3. 配置初始任务逻辑
        setupActions();

        // 4. 启动窗体
        initAndLaunch();
    }

    /**
     * 配置迷宫控制逻辑
     */
    private void setupActions() {
        // 预装载算法任务：点击“开始执行”时，在子线程运行 generate()
        this.setTask(() -> {
            // 每次生成前先重置统计数据
            maze.resetStatistics();
            maze.generate();
        });
    }

    /**
     * 实现 BaseFrame 要求的统一重置接口
     * 负责停止线程后的数据清理工作
     */
    @Override
    protected void handleDataReset() {
        // 1. 调用算法层方法：将地图回归到初始状态（通常是全墙状态）
        if (this.maze != null) {
            this.maze.initial(); // 重置二维数组内容
            this.maze.resetStatistics(); // 重置步数、比较数等统计
        }

        // 2. 更新画布：传入重置后的地图，并将焦点 (r, c) 置为 null
        this.mazePanel.updateData(this.maze.getMap(), null, null);

        // 3. 更新侧边栏状态
        dataListArea.setText("实时序列:\n迷宫已重置为初始状态。");
    }

    /**
     * 实现 refresh 接口：定义迷宫生成的渲染节奏
     * * @param data 最新迷宫矩阵状态
     * 
     * @param r 当前算法操作的行坐标 (Integer)
     * @param c 当前算法操作的列坐标 (Integer)
     */
    @Override
    protected void refresh(int[][] data, Object r, Object c) {
        // 更新图形画布，Panel 会利用 r 和 c 绘制高亮的探针位置
        mazePanel.updateData(data, r, c);

        // 可选：在侧边栏显示当前探针的具体位置
        if (r != null && c != null) {
            dataListArea.setText("探针位置:\nRow: " + r + "\nCol: " + c);
        }
    }

    /**
     * 静态工厂启动入口
     * * @param maze 算法实现
     * 
     * @param cellSize 网格大小
     */
    public static void launch(BaseMaze maze, int cellSize) {
        SwingUtilities.invokeLater(() -> {
            MazeFrame frame = new MazeFrame(maze, cellSize);
            frame.initAndLaunch();
        });
    }
}