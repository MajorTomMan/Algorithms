package com.majortom.algorithms.core.maze.algorithms.graph.generate;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import com.majortom.algorithms.core.maze.BaseGraphMazeAlgorithms;
import com.majortom.algorithms.core.maze.impl.GraphMaze;

public class DFSGraphMazeGenerator extends BaseGraphMazeAlgorithms<GraphMaze> {
    private final Set<String> visited = new HashSet<>();
    private final Random random = new Random();

    /**
     * 1. 随机选一个起点
     * 2. 标记这个点为已访问
     * 3. 从当前点的邻居里，随机选一个还没访问过的点
     * 4. 打通当前点和那个邻居之间的边
     * 5. 移动到那个邻居
     * 6. 重复这个过程
     * 7. 如果当前点周围没有没访问过的邻居，就退回上一个点
     * 8. 继续找别的没访问过的方向
     * 9. 直到所有点都访问过
     */
    @Override
    public void run(GraphMaze maze) {
        visited.clear();
        maze.initial();

        String startId = pickRandomStartId(maze);
        if (startId == null) {
            return;
        }
        maze.setStart(startId);
        visited.add(startId);
        dfs(maze, startId);
        maze.setGenerated(true);
    }

    /**
     * 从当前节点继续向外生长生成树。
     *
     * <p>
     * 这里递归的主语不再是二维坐标，而是图节点 ID。这样算法思维会更贴近图结构：
     * 先取当前节点的候选邻居，再把尚未访问过的邻居正式接入迷宫结果图。
     * </p>
     *
     * @param maze 当前图迷宫实体
     * @param id   当前递归处理的节点 ID
     */
    private void dfs(GraphMaze maze, String id) {
        List<String> neighbors = maze.getCarveCandidateNeighborIds(id);
        if (neighbors.isEmpty()) {
            return;
        }

        Collections.shuffle(neighbors);
        for (String neighborId : neighbors) {
            if (!visited.contains(neighborId)) {
                maze.carvePassage(id, neighborId);
                visited.add(neighborId);
                maze.setCellState(neighborId, 0, false);
                dfs(maze, neighborId);
            }
        }
    }

    /**
     * 为本轮生成随机选择一个起点节点。
     *
     * <p>
     * 当前图迷宫初始化后会为每个布局位置都创建节点，因此这里直接在行列范围内随机挑选一个坐标，
     * 再把它转换成图节点 ID。后续如果图迷宫变成“并非每个坐标都有节点”，再把这段逻辑下沉到结构层也很自然。
     * </p>
     *
     * @param maze 当前图迷宫
     * @return 随机起点节点 ID
     */
    private String pickRandomStartId(GraphMaze maze) {
        return maze.pickRandomRoomId(random);
    }
}
