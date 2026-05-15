package com.majortom.algorithms.core.maze.impl;

import com.majortom.algorithms.core.graph.BaseGraph;
import com.majortom.algorithms.core.graph.impl.UndirectedGraph;
import com.majortom.algorithms.core.maze.BaseMaze;
import com.majortom.algorithms.core.maze.constants.MazeConstant;
import com.majortom.algorithms.core.maze.structure.MazeCell;
import com.majortom.algorithms.core.runtime.ExecutionContext;

import org.graphstream.graph.Edge;
import org.graphstream.graph.Node;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 基于图结构的迷宫实现。
 *
 * <p>每个迷宫格子对应一个 GraphStream 节点，相邻可通行格子之间通过边连接。
 * 这种结构适合演示 BFS、DFS、A* 等图式寻路算法，也能保存边权和格子通行代价。</p>
 */
public class GraphMaze extends BaseMaze<BaseGraph<MazeCell>> {

    /**
     * 起点节点 ID。
     */
    private String startId;

    /**
     * 终点节点 ID。
     */
    private String endId;

    /**
     * 创建图迷宫。
     *
     * @param rows 行数
     * @param cols 列数
     */
    public GraphMaze(int rows, int cols) {
        super(rows, cols);
        this.data = new UndirectedGraph<>("graph-maze");
        initialSilent();
    }

    /**
     * 接收图迷宫算法专用执行上下文。
     *
     * <p>{@link BaseMaze} 的历史签名面向 {@code BaseMaze<T>}，而图迷宫控制器会创建
     * {@code ExecutionContext<GraphMaze>}。这里做一次受控桥接，让图迷宫仍能在
     * {@link #setCellState(int, int, int, boolean)} 中自动产帧。</p>
     *
     * @param executionContext 图迷宫执行上下文
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void setGraphMazeExecutionContext(ExecutionContext<GraphMaze> executionContext) {
        this.executionContext = (ExecutionContext) executionContext;
    }

    /**
     * 初始化迷宫并发出初始帧。
     */
    @Override
    public void initial() {
        initialSilent();

        if (executionContext != null) {
            executionContext.sync(this, -1, -1, "maze.initial", false);
        }
    }

    /**
     * 静默初始化全部格子节点。
     */
    @Override
    public void initialSilent() {
        this.isGenerated = false;
        this.startId = null;
        this.endId = null;

        data.clear();

        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                data.addVertex(id(r, c), new MazeCell(r, c));
            }
        }

        this.compareCount = 0;
        this.actionCount = 0;
    }

    /**
     * 更新指定格子的状态。
     */
    @Override
    protected void updateInternalData(int r, int c, int type) {
        if (isOverBorder(r, c)) {
            return;
        }

        MazeCell cell = getMazeCell(r, c);

        if (cell != null) {
            cell.setType(type);
        }
    }

    /**
     * 读取指定格子的状态。
     */
    @Override
    public int getCell(int r, int c) {
        if (isOverBorder(r, c)) {
            return MazeConstant.WALL;
        }

        MazeCell cell = getMazeCell(r, c);

        if (cell == null) {
            return MazeConstant.WALL;
        }

        return cell.getType();
    }

    /**
     * 连接两个相邻格子，边权默认为 1。
     *
     * @param r1 第一个格子行坐标
     * @param c1 第一个格子列坐标
     * @param r2 第二个格子行坐标
     * @param c2 第二个格子列坐标
     */
    public void connect(int r1, int c1, int r2, int c2) {
        connect(r1, c1, r2, c2, 1);
    }

    /**
     * 连接两个相邻格子。
     *
     * @param r1 第一个格子行坐标
     * @param c1 第一个格子列坐标
     * @param r2 第二个格子行坐标
     * @param c2 第二个格子列坐标
     * @param weight 边权重，必须为正数
     */
    public void connect(int r1, int c1, int r2, int c2, int weight) {
        if (isOverBorder(r1, c1) || isOverBorder(r2, c2)) {
            return;
        }

        if (!isAdjacent(r1, c1, r2, c2)) {
            throw new IllegalArgumentException("Only adjacent cells can be connected.");
        }

        if (weight <= 0) {
            throw new IllegalArgumentException("weight must be positive.");
        }

        markAsRoadIfWall(r1, c1);
        markAsRoadIfWall(r2, c2);

        data.addEdge(id(r1, c1), id(r2, c2), weight);
    }

    /**
     * 判断两个格子之间是否已经有边连接。
     */
    public boolean hasConnection(int r1, int c1, int r2, int c2) {
        if (isOverBorder(r1, c1) || isOverBorder(r2, c2)) {
            return false;
        }

        return getEdgeBetween(id(r1, c1), id(r2, c2)) != null;
    }

    /**
     * 获取两个格子之间的边权重。
     */
    public int getEdgeWeight(int r1, int c1, int r2, int c2) {
        if (isOverBorder(r1, c1) || isOverBorder(r2, c2)) {
            return Integer.MAX_VALUE;
        }

        Edge edge = getEdgeBetween(id(r1, c1), id(r2, c2));

        if (edge == null) {
            return Integer.MAX_VALUE;
        }

        Integer weight = (Integer) edge.getAttribute("weight");
        return weight == null ? 1 : weight;
    }

    /**
     * 设置格子通行代价。
     */
    public void setCellCost(int r, int c, int cost) {
        if (isOverBorder(r, c)) {
            return;
        }

        if (cost <= 0) {
            throw new IllegalArgumentException("cost must be positive.");
        }

        MazeCell cell = getMazeCell(r, c);

        if (cell != null) {
            cell.setCost(cost);
        }
    }

    /**
     * 获取格子通行代价。
     */
    public int getCellCost(int r, int c) {
        if (isOverBorder(r, c)) {
            return Integer.MAX_VALUE;
        }

        MazeCell cell = getMazeCell(r, c);

        if (cell == null) {
            return Integer.MAX_VALUE;
        }

        return cell.getCost();
    }

    /**
     * 从可通行格子中随机选择起点和终点。
     */
    @Override
    public void pickRandomPoints() {
        clearStartAndEnd();

        List<MazeCell> cells = availableCells();

        if (cells.size() < 2) {
            return;
        }

        Collections.shuffle(cells);

        MazeCell start = cells.get(0);
        MazeCell end = cells.get(1);

        setStart(start.getRow(), start.getCol());
        setEnd(end.getRow(), end.getCol());
    }

    /**
     * 在可通行路径上随机选择起点和终点。
     */
    @Override
    public void pickRandomPointsOnAvailablePaths() {
        pickRandomPoints();
    }

    /**
     * 设置起点。
     */
    public void setStart(int r, int c) {
        if (isOverBorder(r, c)) {
            return;
        }

        clearOldStart();

        this.startId = id(r, c);

        markAsRoadIfWall(r, c);
        setCellState(r, c, MazeConstant.START, false);
    }

    /**
     * 设置终点。
     */
    public void setEnd(int r, int c) {
        if (isOverBorder(r, c)) {
            return;
        }

        clearOldEnd();

        this.endId = id(r, c);

        markAsRoadIfWall(r, c);
        setCellState(r, c, MazeConstant.END, false);
    }

    @Override
    public void clearVisualStates() {
        data.getGraph().nodes().forEach(node -> {
            MazeCell cell = getMazeCell(node.getId());

            if (cell == null) {
                return;
            }

            int type = cell.getType();

            if (type != MazeConstant.WALL &&
                    type != MazeConstant.START &&
                    type != MazeConstant.END) {
                cell.setType(MazeConstant.ROAD);
            }

            node.removeAttribute("visited");
            node.removeAttribute("ui.class");
        });

        data.getGraph().edges().forEach(edge -> {
            edge.removeAttribute("ui.class");
        });

        this.actionCount = 0;
    }

    /**
     * 清空并重新初始化迷宫。
     */
    @Override
    public void clear() {
        initial();
    }

    /**
     * 创建图迷宫快照。
     *
     * @return 当前图迷宫的独立副本
     */
    @Override
    public BaseMaze<BaseGraph<MazeCell>> copy() {
        GraphMaze copy = new GraphMaze(this.rows, this.cols);

        copy.initialSilent();

        this.data.getGraph().nodes().forEach(oldNode -> {
            MazeCell oldCell = getMazeCell(oldNode.getId());
            MazeCell newCell = copy.getMazeCell(oldNode.getId());

            if (oldCell == null || newCell == null) {
                return;
            }

            newCell.setType(oldCell.getType());
            newCell.setCost(oldCell.getCost());
        });

        this.data.getGraph().edges().forEach(oldEdge -> {
            String fromId = oldEdge.getSourceNode().getId();
            String toId = oldEdge.getTargetNode().getId();

            Integer weight = (Integer) oldEdge.getAttribute("weight");
            copy.data.addEdge(fromId, toId, weight == null ? 1 : weight);
        });

        copy.startId = this.startId;
        copy.endId = this.endId;

        this.copyStateTo(copy);

        return copy;
    }

    /**
     * 将迷宫作为图结构暴露给图算法。
     *
     * @return 图结构包装器
     */
    public BaseGraph<MazeCell> asGraph() {
        return data;
    }

    /**
     * 将行列坐标转换为节点 ID。
     */
    public String id(int r, int c) {
        return r + "," + c;
    }

    /**
     * 获取起点节点 ID。
     */
    public String getStartId() {
        return startId;
    }

    /**
     * 获取终点节点 ID。
     */
    public String getEndId() {
        return endId;
    }

    /**
     * 按坐标获取迷宫单元格。
     */
    public MazeCell getMazeCell(int r, int c) {
        if (isOverBorder(r, c)) {
            return null;
        }

        return getMazeCell(id(r, c));
    }

    /**
     * 按节点 ID 获取迷宫单元格。
     */
    public MazeCell getMazeCell(String id) {
        Node node = data.getGraph().getNode(id);

        if (node == null) {
            return null;
        }

        return (MazeCell) node.getAttribute("data");
    }

    /**
     * 按图节点 ID 读取节点中的业务数据。
     *
     * <p>这个方法是给图迷宫算法和可视化层预留的稳定入口，避免调用方直接依赖
     * GraphStream 的属性名。</p>
     *
     * @param id 图节点 ID
     * @return 节点对应的迷宫单元格；节点不存在时返回 null
     */
    public MazeCell getVertexData(String id) {
        return getMazeCell(id);
    }

    /**
     * 获取指定坐标的邻接节点 ID。
     */
    public List<String> getNeighborIds(int r, int c) {
        if (isOverBorder(r, c)) {
            return Collections.emptyList();
        }

        return getNeighborIds(id(r, c));
    }

    /**
     * 获取指定节点 ID 的邻接节点 ID。
     */
    public List<String> getNeighborIds(String id) {
        Node node = data.getGraph().getNode(id);

        if (node == null) {
            return Collections.emptyList();
        }

        List<String> result = new ArrayList<>();

        node.edges().forEach(edge -> {
            Node opposite = edge.getOpposite(node);
            result.add(opposite.getId());
        });

        return result;
    }

    /**
     * 获取指定坐标的邻接单元格。
     */
    public List<MazeCell> getNeighbors(int r, int c) {
        if (isOverBorder(r, c)) {
            return Collections.emptyList();
        }

        List<MazeCell> result = new ArrayList<>();

        for (String neighborId : getNeighborIds(r, c)) {
            MazeCell cell = getMazeCell(neighborId);

            if (cell != null) {
                result.add(cell);
            }
        }

        return result;
    }

    /**
     * 获取所有可通行格子。
     */
    private List<MazeCell> availableCells() {
        List<MazeCell> result = new ArrayList<>();

        data.getGraph().nodes().forEach(node -> {
            MazeCell cell = getMazeCell(node.getId());

            if (cell != null && cell.getType() != MazeConstant.WALL) {
                result.add(cell);
            }
        });

        return result;
    }

    /**
     * 如果格子仍是墙，则标记为道路。
     */
    private void markAsRoadIfWall(int r, int c) {
        MazeCell cell = getMazeCell(r, c);

        if (cell != null && cell.getType() == MazeConstant.WALL) {
            cell.setType(MazeConstant.ROAD);
        }
    }

    /**
     * 判断两个坐标是否四方向相邻。
     */
    private boolean isAdjacent(int r1, int c1, int r2, int c2) {
        int dr = Math.abs(r1 - r2);
        int dc = Math.abs(c1 - c2);

        return dr + dc == 1;
    }

    /**
     * 查询两个节点之间的边。
     */
    private Edge getEdgeBetween(String fromId, String toId) {
        Node from = data.getGraph().getNode(fromId);

        if (from == null || data.getGraph().getNode(toId) == null) {
            return null;
        }

        return from.edges()
                .filter(edge -> edge.getOpposite(from).getId().equals(toId))
                .findFirst()
                .orElse(null);
    }

    /**
     * 清理现有起点和终点。
     */
    private void clearStartAndEnd() {
        clearOldStart();
        clearOldEnd();
    }

    /**
     * 清理旧起点。
     */
    private void clearOldStart() {
        if (startId == null) {
            return;
        }

        MazeCell oldStart = getMazeCell(startId);

        if (oldStart != null && oldStart.getType() == MazeConstant.START) {
            oldStart.setType(MazeConstant.ROAD);
        }

        startId = null;
    }

    /**
     * 清理旧终点。
     */
    private void clearOldEnd() {
        if (endId == null) {
            return;
        }

        MazeCell oldEnd = getMazeCell(endId);

        if (oldEnd != null && oldEnd.getType() == MazeConstant.END) {
            oldEnd.setType(MazeConstant.ROAD);
        }

        endId = null;
    }
}
