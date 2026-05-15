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
 * <p>这个类把“迷宫”翻译成“图”：每个格子是一个 GraphStream 节点，两个相邻且可通行的格子之间
 * 用一条边连接。这样一来，迷宫生成和寻路就可以复用图算法的思维：节点代表位置，边代表可走的通道，
 * 边权和格子代价则可以继续扩展给 Dijkstra、A* 等带权寻路算法。</p>
 *
 * <p>联动链路是本类最重要的设计点：算法修改格子状态时调用 {@link #setCellState(int, int, int, boolean)}，
 * 结构层会更新 {@link MazeCell}，再通过 {@link ExecutionContext} 产出执行帧，最后由可视化层读取快照并重绘。
 * 因此算法实现者通常只需要关心“访问哪个节点、连接哪条边、标记哪个格子”，不需要直接操作 UI。</p>
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
     * {@code ExecutionContext<GraphMaze>}。这里做一次受控桥接，让图迷宫仍能复用
     * {@link BaseMaze#setCellState(int, int, int, boolean)} 的自动同步能力。</p>
     *
     * <p>换句话说：算法层把“我走到了这个格子”告诉结构层；结构层通过上下文把这一步包装成执行帧；
     * 可视化层再根据执行帧渲染画面。这段桥接代码就是为了让图迷宫也能插进同一条执行管线。</p>
     *
     * @param executionContext 图迷宫执行上下文
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void setGraphMazeExecutionContext(ExecutionContext<GraphMaze> executionContext) {
        this.executionContext = (ExecutionContext) executionContext;
    }

    /**
     * 初始化迷宫并发出初始帧。
     *
     * <p>这个方法适合“用户能看到变化”的初始化场景。它先静默重建图节点，再通过执行上下文发出一帧，
     * 让控制器、时间轴和可视化器都能知道：新的图迷宫状态已经准备好了。</p>
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
     *
     * <p>静默初始化只重置内存结构，不主动产帧。它常用于构造函数、快照复制和控制器内部重建，
     * 避免在 UI 还没有准备好时触发渲染。</p>
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
     *
     * <p>这是 {@link BaseMaze#setCellState(int, int, int, boolean)} 的底层落点。
     * 上层负责统计和同步，本方法只负责把状态真正写入节点里的 {@link MazeCell}。</p>
     *
     * @param r 行坐标
     * @param c 列坐标
     * @param type 新的迷宫单元格类型
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
     *
     * <p>越界或节点缺失时统一按墙处理，这能让算法在探索邻居时少写很多防御代码。</p>
     *
     * @param r 行坐标
     * @param c 列坐标
     * @return 单元格类型；不可访问位置返回 {@link MazeConstant#WALL}
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
     * <p>在图迷宫里，“打通墙”不是删除数组里的墙，而是在两个节点之间建立边。
     * 生成算法每连接一次，相当于为寻路算法新增一条可走通道。</p>
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
     * <p>本方法会先校验坐标合法性、相邻性和权重，再把两端格子标记为道路并创建图边。
     * 对算法教学来说，这一步可以理解成“把二维坐标上的通道，映射成图上的边”。</p>
     *
     * @param r1 第一个格子行坐标
     * @param c1 第一个格子列坐标
     * @param r2 第二个格子行坐标
     * @param c2 第二个格子列坐标
     * @param weight 边权重，必须为正数
     * @throws IllegalArgumentException 两个格子不相邻，或权重不是正数时抛出
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
     *
     * <p>生成器可以用它避免重复连边；寻路器可以用它判断两个相邻坐标是否真的可通行。</p>
     *
     * @param r1 第一个格子行坐标
     * @param c1 第一个格子列坐标
     * @param r2 第二个格子行坐标
     * @param c2 第二个格子列坐标
     * @return 已存在通道时返回 true
     */
    public boolean hasConnection(int r1, int c1, int r2, int c2) {
        if (isOverBorder(r1, c1) || isOverBorder(r2, c2)) {
            return false;
        }

        return getEdgeBetween(id(r1, c1), id(r2, c2)) != null;
    }

    /**
     * 获取两个格子之间的边权重。
     *
     * <p>如果两个格子之间没有边，返回一个很大的代价，让带权算法自然地避开这条不存在的路。</p>
     *
     * @param r1 第一个格子行坐标
     * @param c1 第一个格子列坐标
     * @param r2 第二个格子行坐标
     * @param c2 第二个格子列坐标
     * @return 边权；不可达时返回 {@link Integer#MAX_VALUE}
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
     *
     * <p>边权描述“从 A 到 B 的通道成本”，格子代价描述“进入这个格子本身的成本”。
     * 两者可以组合出更丰富的教学案例，例如泥地、陷阱、山地等不同地形。</p>
     *
     * @param r 行坐标
     * @param c 列坐标
     * @param cost 通行代价，必须为正数
     * @throws IllegalArgumentException cost 不是正数时抛出
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
     *
     * @param r 行坐标
     * @param c 列坐标
     * @return 格子代价；越界或节点缺失时返回 {@link Integer#MAX_VALUE}
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
     *
     * <p>生成完成后，控制器或算法可以调用这个方法为寻路阶段准备输入。
     * 方法只在道路、路径等非墙格子中选择，避免把起点或终点放进不可达位置。</p>
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
     *
     * <p>图迷宫里“可通行路径”和“可通行格子”在这里等价，因此直接复用 {@link #pickRandomPoints()}。
     * 保留这个方法是为了和二维数组迷宫的统一接口对齐。</p>
     */
    @Override
    public void pickRandomPointsOnAvailablePaths() {
        pickRandomPoints();
    }

    /**
     * 设置起点。
     *
     * <p>起点既是 {@link MazeCell} 的状态，也是图节点 ID。算法可以通过 {@link #getStartId()}
     * 获得图算法入口；可视化器则通过格子类型把它画成起点样式。</p>
     *
     * @param r 行坐标
     * @param c 列坐标
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
     *
     * <p>终点和起点一样，同时服务图算法入口判断、执行帧快照和可视化标识。</p>
     *
     * @param r 行坐标
     * @param c 列坐标
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

    /**
     * 清除寻路过程中的临时视觉状态。
     *
     * <p>这个方法不会删除图节点和边，也不会清掉起点终点；它只把 PATH、DEADEND、BACKTRACK 等
     * 寻路过程状态还原为 ROAD，并清理 GraphStream 上用于 UI 高亮的属性。这样同一个迷宫可以重复跑不同寻路算法。</p>
     */
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
     *
     * <p>图层只认识字符串节点 ID，迷宫层更自然地使用行列坐标。这个方法就是两种表示法之间的桥。</p>
     *
     * @param r 行坐标
     * @param c 列坐标
     * @return 形如 {@code row,col} 的节点 ID
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
     *
     * <p>返回的不是几何意义上的上下左右，而是图中已经连边的真实邻居，
     * 因此它反映的是“当前迷宫里真正能走的下一步”。</p>
     *
     * @param r 行坐标
     * @param c 列坐标
     * @return 与该格子连通的节点 ID 列表
     */
    public List<String> getNeighborIds(int r, int c) {
        if (isOverBorder(r, c)) {
            return Collections.emptyList();
        }

        return getNeighborIds(id(r, c));
    }

    /**
     * 获取指定节点 ID 的邻接节点 ID。
     *
     * <p>图算法通常从节点 ID 出发遍历，所以这个重载是 BFS、DFS、Dijkstra 一类算法的主要入口。</p>
     *
     * @param id 当前节点 ID
     * @return 与该节点通过边相连的邻居 ID；节点不存在时返回空列表
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
     *
     * <p>当算法更关心行列、类型或代价，而不是节点 ID 时，可以使用这个方法。</p>
     *
     * @param r 行坐标
     * @param c 列坐标
     * @return 相邻且已连通的迷宫单元格列表
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
     *
     * <p>这个列表用于挑选起点和终点。它只关心格子状态，不检查图连通性；
     * 如果后续希望保证起终点互相可达，可以在这里叠加连通分量判断。</p>
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
