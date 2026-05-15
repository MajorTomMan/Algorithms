package com.majortom.algorithms.core.maze;

/**
 * 图迷宫生成算法标记接口。
 *
 * <p>这个接口不声明方法，只负责给算法“贴标签”。当某个类同时继承
 * {@link BaseGraphMazeAlgorithms} 并实现 {@code GraphMazeGenerator} 时，控制器和注册表就能知道：
 * 它属于图迷宫的生成阶段，而不是寻路阶段。</p>
 *
 * <p>教学上可以把生成器理解为“铺路的人”：它通过连接 {@code GraphMaze} 中的节点来打开通道，
 * 再由执行上下文把每一次连接或状态变化推送给可视化层。</p>
 */
public interface GraphMazeGenerator {
}
