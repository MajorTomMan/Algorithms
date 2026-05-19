package com.majortom.algorithms.core.maze.impl;

import com.majortom.algorithms.core.base.BaseStructure;
import com.majortom.algorithms.core.graph.BaseGraph;
import com.majortom.algorithms.core.graph.impl.UndirectedGraph;
import com.majortom.algorithms.core.maze.constants.MazeCellType;
import com.majortom.algorithms.core.maze.constants.MazeDirections;
import com.majortom.algorithms.core.maze.structure.MazeCell;
import com.majortom.algorithms.core.runtime.ExecutionContext;

import org.graphstream.graph.Edge;
import org.graphstream.graph.Node;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * 基于图结构的迷宫实现。
 *
 * <p>
 * 这个类不再复用二维数组迷宫的抽象基类，而是把“图迷宫”视为独立结构：
 * 算法层的主语是节点、边和邻接关系，可视化层如果需要方格画面，则读取节点上的布局坐标并自行投影。
 * </p>
 *
 * <p>
 * 换句话说，图迷宫里真正定义“能不能走”的是边，而不是二维数组上的上下左右相邻关系。
 * 坐标仍然保留在 {@link MazeCell} 中，但它们主要服务于展示和布局，不再驱动算法接口设计。
 * </p>
 */
public class GraphMaze extends BaseStructure<BaseGraph<MazeCell>> {

    /**
     * 图迷宫底层图模型。
     *
     * <p>
     * 这里保存的是真正决定连通关系的数据：节点代表位置，边代表可走通道。
     * 迷宫算法应优先围绕这个图模型思考，而不是围绕二维数组相邻关系思考。
     * </p>
     */
    private BaseGraph<MazeCell> data;
    /**
     * 可视化投影使用的总行数。
     */
    private final int rows;
    /**
     * 可视化投影使用的总列数。
     */
    private final int cols;
    /**
     * 当前图迷宫是否已经完成生成。
     */
    private boolean generated;
    /**
     * 图迷宫执行上下文。
     *
     * <p>
     * 结构层通过它把状态变化同步到执行帧和可视化层，
     * 让图算法仍然可以沿用统一的播放、暂停和回放机制。
     * </p>
     */
    private ExecutionContext<GraphMaze> executionContext;
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
        this.rows = rows;
        this.cols = cols;
        this.data = new UndirectedGraph<>("graph-maze");
        initialSilent();
    }

    /**
     * 接收图迷宫算法执行上下文。
     *
     * <p>
     * 参数上的泛型 {@code M} 用来兼容具体的图迷宫子类型，但结构层内部只保存
     * {@code ExecutionContext<GraphMaze>}，因为同步给可视化和时间轴时，主语始终是当前图迷宫快照。
     * </p>
     *
     * @param executionContext 图迷宫执行上下文
     */
    public <M extends GraphMaze> void setGraphMazeExecutionContext(ExecutionContext<M> executionContext) {
        @SuppressWarnings({ "unchecked", "rawtypes" })
        ExecutionContext<GraphMaze> casted = (ExecutionContext) executionContext;
        this.executionContext = casted;
    }

    /**
     * 初始化迷宫并发出初始帧。
     *
     * <p>
     * 这个方法适合“用户能看到新结构已经准备好”的场景。它会先重建图节点，
     * 再主动发出一帧，让控制器、时间轴和可视化器同步进入新的初始状态。
     * </p>
     */
    public void initial() {
        initialSilent();
        if (executionContext != null) {
            executionContext.sync(this, null, null, "graph-maze.initial", false);
        }
    }

    /**
     * 静默初始化图迷宫。
     *
     * <p>
     * 静默初始化只重建内存结构，不主动产出执行帧。它通常用于构造函数、
     * 快照复制和控制器内部重建，避免 UI 还没准备好时就触发渲染。
     * </p>
     */
    public void initialSilent() {
        this.generated = false;
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
        this.timeElapsedMs = 0L;
        this.extraMetrics.clear();
    }

    /**
     * 更新节点状态并自动同步执行帧。
     *
     * <p>
     * 这里是图迷宫结构层最核心的状态入口。算法层只需要说清楚“哪个节点变成什么状态”，
     * 结构层就会负责三件事：写入节点数据、累计统计、把这一步同步到执行上下文。
     * </p>
     *
     * @param nodeId   目标节点 ID，表示这次变化作用在哪个图节点上
     * @param type     新的格子状态，通常来自 {@link MazeCellType}
     * @param isAction true 表示这是一次结构动作，false 表示访问/比较类操作
     */
    public void setCellState(String nodeId, int type, boolean isAction) {
        MazeCell cell = getMazeCell(nodeId);
        if (cell == null) {
            return;
        }

        cell.setType(type);
        if (isAction) {
            incrementAction();
        } else {
            incrementCompare();
        }

        if (executionContext != null) {
            executionContext.sync(this, nodeId, null, null, isAction);
        }
    }

    /**
     * 按坐标更新节点状态。
     *
     * <p>
     * 这个重载主要给可视化兼容和少量布局相关操作使用。图算法更推荐直接传节点 ID。
     * </p>
     *
     * @param r        节点所在布局行
     * @param c        节点所在布局列
     * @param type     新的格子状态
     * @param isAction true 表示结构动作，false 表示访问/比较
     */
    public void setCellState(int r, int c, int type, boolean isAction) {
        setCellState(id(r, c), type, isAction);
    }

    /**
     * 获取图迷宫底层图结构。
     *
     * <p>
     * 执行层和快照导出会通过这个方法拿到底层数据。
     * 这里返回的仍是图模型，而不是二维投影。
     * </p>
     */
    @Override
    public BaseGraph<MazeCell> getData() {
        return data;
    }

    /**
     * 获取可视化投影行数。
     */
    public int getRows() {
        return rows;
    }

    /**
     * 获取可视化投影列数。
     */
    public int getCols() {
        return cols;
    }

    /**
     * 判断当前图迷宫是否已经完成生成。
     */
    public boolean isGenerated() {
        return generated;
    }

    /**
     * 设置图迷宫生成状态。
     *
     * @param generated true 表示图迷宫已经准备好进入求解阶段
     */
    public void setGenerated(boolean generated) {
        this.generated = generated;
    }

    /**
     * 根据节点 ID 连接两个节点。
     *
     * <p>
     * 这是图迷宫生成器最常用的入口之一。教学上可以把它理解成：
     * “把两个候选位置之间的潜在关系，正式变成一条可走通道”。
     * </p>
     *
     * @param fromId 起点节点 ID，表示通道一端
     * @param toId   终点节点 ID，表示通道另一端
     */
    public void connect(String fromId, String toId) {
        connect(fromId, toId, 1);
    }

    /**
     * 根据节点 ID 连接两个节点。
     *
     * <p>
     * 参数 {@code fromId/toId} 的意义不是“几何上的相邻格子”，而是“图中的两个候选节点”。
     * 方法内部会再验证它们在当前布局里是否允许相邻，从而兼顾图语义和方格布局约束。
     * </p>
     *
     * @param fromId 起点节点 ID
     * @param toId   终点节点 ID
     * @param weight 边权重，用于扩展带权迷宫或带权寻路教学
     */
    public void connect(String fromId, String toId, int weight) {
        MazeCell from = getMazeCell(fromId);
        MazeCell to = getMazeCell(toId);

        if (from == null || to == null) {
            return;
        }
        if (!isAdjacent(from, to)) {
            throw new IllegalArgumentException("Only adjacent cells can be connected.");
        }
        if (weight <= 0) {
            throw new IllegalArgumentException("weight must be positive.");
        }

        markAsRoadIfWall(fromId, true);
        markAsRoadIfWall(toId, true);
        data.addEdge(fromId, toId, weight);
    }

    /**
     * 按坐标连接两个节点。
     *
     * <p>
     * 这个重载保留给基于布局的辅助逻辑使用。真正的图算法建议直接使用节点 ID 版本，
     * 这样算法代码会更像“图搜索”，而不是“二维格子搜索”。
     * </p>
     */
    public void connect(int r1, int c1, int r2, int c2) {
        connect(id(r1, c1), id(r2, c2));
    }

    /**
     * 判断两个节点之间是否已经有边连接。
     *
     * <p>
     * 生成算法可以用它避免重复连边；寻路算法可以用它验证“两个节点理论上相邻，
     * 但当前迷宫里是否真的有路”。
     * </p>
     */
    public boolean hasConnection(String fromId, String toId) {
        return getEdgeBetween(fromId, toId) != null;
    }

    /**
     * 获取两个节点之间的边权重。
     *
     * <p>
     * 如果两点之间没有边，这里返回一个极大值，帮助带权算法把“没有通道”自然视为不可选路径。
     * </p>
     */
    public int getEdgeWeight(String fromId, String toId) {
        Edge edge = getEdgeBetween(fromId, toId);
        if (edge == null) {
            return Integer.MAX_VALUE;
        }
        Integer weight = (Integer) edge.getAttribute("weight");
        return weight == null ? 1 : weight;
    }

    /**
     * 设置节点通行代价。
     *
     * <p>
     * 这里的代价描述“进入这个节点的成本”。它和边权一起使用时，
     * 可以扩展出泥地、陷阱、山地等教学案例。
     * </p>
     */
    public void setCellCost(String nodeId, int cost) {
        if (cost <= 0) {
            throw new IllegalArgumentException("cost must be positive.");
        }

        MazeCell cell = getMazeCell(nodeId);
        if (cell != null) {
            cell.setCost(cost);
        }
    }

    /**
     * 获取节点通行代价。
     *
     * @param nodeId 目标节点 ID
     * @return 节点代价；节点不存在时返回极大值
     */
    public int getCellCost(String nodeId) {
        MazeCell cell = getMazeCell(nodeId);
        return cell == null ? Integer.MAX_VALUE : cell.getCost();
    }

    /**
     * 随机选择起点和终点。
     *
     * <p>
     * 这里从所有可通行节点里随机挑选两个不同节点，并分别标记为起点和终点。
     * 它通常发生在“迷宫已经生成完成，准备进入求解阶段”这个时机。
     * </p>
     */
    public void pickRandomPoints() {
        clearStartAndEnd();
        List<MazeCell> cells = availableCells();
        if (cells.size() < 2) {
            return;
        }

        Collections.shuffle(cells);
        setStart(id(cells.get(0).getRow(), cells.get(0).getCol()));
        setEnd(id(cells.get(1).getRow(), cells.get(1).getCol()));
    }

    /**
     * 图迷宫里“可通行路径”和“可通行格子”等价。
     *
     * <p>
     * 二维数组迷宫里，这个方法通常强调“从路径格子中选点”；而图迷宫里，
     * 一个节点只要不是墙，就可以看作落在可通行路径上，因此这里直接复用 {@link #pickRandomPoints()}。
     * </p>
     */
    public void pickRandomPointsOnAvailablePaths() {
        pickRandomPoints();
    }

    /**
     * 按节点 ID 设置起点。
     *
     * <p>
     * 起点既是图搜索的入口，也是可视化里的特殊状态。设置时如果原节点还是墙，
     * 会先把它转成道路，再标记为起点。
     * </p>
     *
     * @param startId 起点节点 ID
     */
    public void setStart(String startId) {
        MazeCell cell = getMazeCell(startId);
        if (cell == null) {
            return;
        }

        clearOldStart();
        this.startId = startId;
        markAsRoadIfWall(startId, false);
        setCellState(startId, MazeCellType.START, false);
    }

    /**
     * 按节点 ID 设置终点。
     *
     * @param endId 终点节点 ID
     */
    public void setEnd(String endId) {
        MazeCell cell = getMazeCell(endId);
        if (cell == null) {
            return;
        }

        clearOldEnd();
        this.endId = endId;
        markAsRoadIfWall(endId, false);
        setCellState(endId, MazeCellType.END, false);
    }

    /**
     * 清除寻路过程中的临时视觉状态。
     *
     * <p>
     * 它不会删除图边，也不会把起点终点抹掉；它只负责把 PATH、DEADEND、BACKTRACK
     * 这些求解过程状态还原为 ROAD，并清理 GraphStream 上的辅助高亮属性。
     * </p>
     */
    public void clearVisualStates() {
        data.getGraph().nodes().forEach(node -> {
            MazeCell cell = getMazeCell(node.getId());
            if (cell == null) {
                return;
            }

            int type = cell.getType();
            if (type != MazeCellType.WALL
                    && type != MazeCellType.START
                    && type != MazeCellType.END) {
                cell.setType(MazeCellType.ROAD);
            }

            node.removeAttribute("visited");
            node.removeAttribute("ui.class");
        });

        data.getGraph().edges().forEach(edge -> edge.removeAttribute("ui.class"));
        this.actionCount = 0;
    }

    /**
     * 清空图迷宫并重新初始化。
     *
     * <p>
     * 这里直接复用 {@link #initial()}，因为图迷宫的“清空”就是回到全节点、无通道、未生成的初始状态。
     * </p>
     */
    @Override
    public void clear() {
        initial();
    }

    /**
     * 重置统计并静默重建图迷宫。
     */
    @Override
    public void resetStatistics() {
        super.resetStatistics();
        initialSilent();
    }

    /**
     * 创建图迷宫快照。
     *
     * <p>
     * 执行层会把这个快照写入时间轴，因此这里必须深拷贝节点状态、节点代价和已存在的边，
     * 避免后续算法步骤污染历史帧。
     * </p>
     */
    @Override
    public GraphMaze copy() {
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
        copy.generated = this.generated;
        copy.actionCount = this.actionCount;
        copy.compareCount = this.compareCount;
        copy.timeElapsedMs = this.timeElapsedMs;
        copy.extraMetrics.putAll(this.extraMetrics);
        return copy;
    }

    /**
     * 以图结构视角暴露当前迷宫。
     *
     * <p>
     * 图算法如果需要直接访问底层图 API，可以从这里取到包装器。
     * </p>
     */
    public BaseGraph<MazeCell> asGraph() {
        return data;
    }

    /**
     * 将布局坐标转换为节点 ID。
     *
     * <p>
     * 坐标仍然保留在图迷宫里，但它的职责已经降级为“布局定位”。
     * 这个方法的意义是把展示友好的行列坐标翻译成图算法真正使用的节点标识。
     * </p>
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
     * 按节点 ID 读取节点中的迷宫业务数据。
     *
     * <p>
     * 返回的 {@link MazeCell} 同时携带布局坐标、状态和代价，因此它是图算法和可视化层之间的重要桥梁。
     * </p>
     */
    public MazeCell getMazeCell(String id) {
        Node node = data.getGraph().getNode(id);
        if (node == null) {
            return null;
        }
        return (MazeCell) node.getAttribute("data");
    }

    /**
     * 按布局坐标读取迷宫节点。
     *
     * <p>
     * 这个重载主要服务可视化和少量布局辅助逻辑。图算法更推荐直接使用节点 ID 入口。
     * </p>
     */
    public MazeCell getMazeCell(int r, int c) {
        if (isOverBorder(r, c)) {
            return null;
        }
        return getMazeCell(id(r, c));
    }

    /**
     * 获取指定图节点的业务数据。
     *
     * <p>
     * 这个名字强调“从图顶点读业务数据”，更适合图算法代码使用。
     * </p>
     */
    public MazeCell getVertexData(String id) {
        return getMazeCell(id);
    }

    /**
     * 获取指定节点 ID 的邻接节点 ID。
     *
     * <p>
     * 这里返回的是“当前真的有边相连的邻居”，而不是几何意义上的上下左右潜在邻居。
     * 所以它反映的是当前迷宫中真正可走的下一步。
     * </p>
     */
    public List<String> getNeighborIds(String id) {
        Node node = data.getGraph().getNode(id);
        if (node == null) {
            return Collections.emptyList();
        }

        List<String> result = new ArrayList<>();
        node.edges().forEach(edge -> result.add(edge.getOpposite(node).getId()));
        return result;
    }

    /**
     * 获取指定节点在当前布局规则下的候选邻居 ID。
     *
     * <p>
     * 这里返回的是“理论上允许连接的地理邻居”，而不是“当前已经通过边连通的邻居”。
     * 图迷宫生成算法通常先靠这个方法找候选扩展方向，再决定是否真正调用 {@link #connect(String, String)} 建边。
     * </p>
     *
     * <p>
     * 如果你在写 DFS/Prim 这类生成器，这个方法通常比 {@link #getNeighborIds(String)} 更重要；
     * 后者看的是现有通道，前者看的是潜在可连接关系。
     * </p>
     *
     * @param id 当前节点 ID
     * @return 在布局上四方向相邻的候选节点 ID 列表；节点不存在时返回空列表
     */
    public List<String> getCandidateNeighborIds(String id) {
        MazeCell origin = getMazeCell(id);
        if (origin == null) {
            return Collections.emptyList();
        }

        List<String> result = new ArrayList<>();
        for (int[] dir : MazeDirections.CARDINAL_DIRECTIONS) {
            int nextRow = origin.getRow() + dir[0];
            int nextCol = origin.getCol() + dir[1];
            if (isOverBorder(nextRow, nextCol)) {
                continue;
            }

            String candidateId = id(nextRow, nextCol);
            if (getMazeCell(candidateId) != null) {
                result.add(candidateId);
            }
        }
        return result;
    }

    /**
     * 判断某个布局位置是否属于“房间位”。
     *
     * <p>
     * 图迷宫如果想复用经典数组迷宫的外观，通常不会让所有坐标都直接进入生成树，
     * 而是只让奇数行、奇数列这些位置充当真正的房间节点。偶数位置则留给墙体或走廊槽位。
     * </p>
     *
     * @param r 行坐标
     * @param c 列坐标
     * @return true 表示这个位置可以作为生成树中的房间节点
     */
    public boolean isRoomCell(int r, int c) {
        return !isOverBorder(r, c) && r % 2 == 1 && c % 2 == 1;
    }

    /**
     * 判断指定节点 ID 是否落在房间位上。
     *
     * @param id 节点 ID
     * @return true 表示该节点适合作为生成树里的房间节点
     */
    public boolean isRoomCell(String id) {
        MazeCell cell = getMazeCell(id);
        return cell != null && isRoomCell(cell.getRow(), cell.getCol());
    }

    /**
     * 获取指定房间节点在“挖通规则”下的候选房间邻居。
     *
     * <p>
     * 普通候选邻居看的是一步上下左右；而经典迷宫生成更常用的是“两步房间邻居”：
     * 当前房间跨过一格墙槽，去寻找下一个房间节点。这样图结构版 DFS 就能复用数组迷宫的奇偶坐标语义。
     * </p>
     *
     * @param id 当前房间节点 ID
     * @return 与当前房间相隔两步、且仍位于房间位上的候选邻居列表
     */
    public List<String> getCarveCandidateNeighborIds(String id) {
        MazeCell origin = getMazeCell(id);
        if (origin == null || !isRoomCell(origin.getRow(), origin.getCol())) {
            return Collections.emptyList();
        }

        List<String> result = new ArrayList<>();
        for (int[] dir : MazeDirections.CARVE_DIRECTIONS) {
            int nextRow = origin.getRow() + dir[0];
            int nextCol = origin.getCol() + dir[1];
            if (!isRoomCell(nextRow, nextCol)) {
                continue;
            }

            String candidateId = id(nextRow, nextCol);
            if (getMazeCell(candidateId) != null) {
                result.add(candidateId);
            }
        }
        return result;
    }

    /**
     * 获取指定房间节点在“挖通规则”下的候选房间邻居数据。
     *
     * @param id 当前房间节点 ID
     * @return 候选房间邻居数据列表
     */
    public List<MazeCell> getCarveCandidateNeighbors(String id) {
        List<MazeCell> result = new ArrayList<>();
        for (String candidateId : getCarveCandidateNeighborIds(id)) {
            MazeCell cell = getMazeCell(candidateId);
            if (cell != null) {
                result.add(cell);
            }
        }
        return result;
    }

    /**
     * 随机选择一个房间节点作为生成起点。
     *
     * <p>
     * 这个方法只会从奇数行、奇数列的房间位中挑选起点，
     * 让图迷宫生成器可以直接复用经典数组迷宫的“房间/墙槽”布局规则。
     * </p>
     *
     * @param random 随机数生成器
     * @return 随机房间节点 ID；当当前布局里不存在合法房间位时返回 null
     */
    public String pickRandomRoomId(Random random) {
        List<String> roomIds = new ArrayList<>();
        data.getGraph().nodes().forEach(node -> {
            MazeCell cell = getMazeCell(node.getId());
            if (cell != null && isRoomCell(cell.getRow(), cell.getCol())) {
                roomIds.add(node.getId());
            }
        });
        if (roomIds.isEmpty()) {
            return null;
        }
        return roomIds.get(random.nextInt(roomIds.size()));
    }

    /**
     * 在两个房间节点之间挖通一条走廊。
     *
     * <p>
     * 这个方法是图迷宫对数组迷宫“打通中间那堵墙”的对应表达：
     * 传入的是相隔两步的房间节点，结构层会自动找到中间那一格走廊槽位，
     * 并把整条通道写成真正可走的图边和道路状态。
     * </p>
     *
     * @param fromId 起始房间节点 ID
     * @param toId 目标房间节点 ID
     */
    public void carvePassage(String fromId, String toId) {
        MazeCell from = getMazeCell(fromId);
        MazeCell to = getMazeCell(toId);

        if (from == null || to == null) {
            return;
        }
        if (!isRoomCell(fromId) || !isRoomCell(toId)) {
            throw new IllegalArgumentException("Only room cells can be carved into the generation tree.");
        }

        int rowDiff = Math.abs(from.getRow() - to.getRow());
        int colDiff = Math.abs(from.getCol() - to.getCol());
        boolean sameRow = from.getRow() == to.getRow();
        boolean sameCol = from.getCol() == to.getCol();
        boolean validDistance = (sameRow && colDiff == MazeDirections.CARVE_STEP)
                || (sameCol && rowDiff == MazeDirections.CARVE_STEP);
        if (!validDistance) {
            throw new IllegalArgumentException("Room cells must be two steps apart to carve a passage.");
        }

        int midRow = (from.getRow() + to.getRow()) / 2;
        int midCol = (from.getCol() + to.getCol()) / 2;
        String midId = id(midRow, midCol);

        markAsRoadIfWall(fromId, true);
        markAsRoadIfWall(midId, true);
        markAsRoadIfWall(toId, true);
        data.addEdge(fromId, midId, 1);
        data.addEdge(midId, toId, 1);
    }

    /**
     * 获取指定布局坐标的候选邻居 ID。
     *
     * <p>
     * 这个重载主要给仍然从布局角度思考的辅助逻辑使用。图算法本体更推荐直接传节点 ID。
     * </p>
     *
     * @param r 当前节点所在行
     * @param c 当前节点所在列
     * @return 在布局上四方向相邻的候选节点 ID 列表
     */
    public List<String> getCandidateNeighborIds(int r, int c) {
        if (isOverBorder(r, c)) {
            return Collections.emptyList();
        }
        return getCandidateNeighborIds(id(r, c));
    }

    /**
     * 获取指定节点的邻接单元格。
     *
     * <p>
     * 如果算法比起节点 ID 更关心节点状态、代价或布局坐标，可以直接使用这个入口。
     * </p>
     */
    public List<MazeCell> getNeighbors(String id) {
        List<MazeCell> result = new ArrayList<>();
        for (String neighborId : getNeighborIds(id)) {
            MazeCell cell = getMazeCell(neighborId);
            if (cell != null) {
                result.add(cell);
            }
        }
        return result;
    }

    /**
     * 获取指定节点在当前布局规则下的候选邻居单元格。
     *
     * <p>
     * 和 {@link #getCandidateNeighborIds(String)} 一样，它表达的是“还没连边之前理论上可以去哪里”，
     * 只是返回值直接换成了 {@link MazeCell}，便于生成算法同时读取坐标、状态或代价。
     * </p>
     *
     * @param id 当前节点 ID
     * @return 候选邻居单元格列表
     */
    public List<MazeCell> getCandidateNeighbors(String id) {
        List<MazeCell> result = new ArrayList<>();
        for (String candidateId : getCandidateNeighborIds(id)) {
            MazeCell cell = getMazeCell(candidateId);
            if (cell != null) {
                result.add(cell);
            }
        }
        return result;
    }

    /**
     * 获取指定布局坐标的候选邻居单元格。
     *
     * @param r 当前节点所在行
     * @param c 当前节点所在列
     * @return 候选邻居单元格列表
     */
    public List<MazeCell> getCandidateNeighbors(int r, int c) {
        if (isOverBorder(r, c)) {
            return Collections.emptyList();
        }
        return getCandidateNeighbors(id(r, c));
    }

    /**
     * 将图迷宫投影成二维状态网格，供方格可视化器使用。
     *
     * <p>
     * 这份投影是给展示层看的，不是给算法层算邻接关系用的。
     * 图迷宫真正的可通行关系仍然由边决定，而不是由这张二维网格决定。
     * </p>
     */
    public int[][] toCellTypeGrid() {
        int[][] grid = new int[rows][cols];
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                grid[r][c] = MazeCellType.WALL;
            }
        }

        data.getGraph().nodes().forEach(node -> {
            MazeCell cell = getMazeCell(node.getId());
            if (cell != null && !isOverBorder(cell.getRow(), cell.getCol())) {
                grid[cell.getRow()][cell.getCol()] = cell.getType();
            }
        });
        return grid;
    }

    /**
     * 判断布局坐标是否越界。
     *
     * <p>
     * 它只用于布局合法性检查，不代表图搜索中的“节点存在性”判断。
     * </p>
     */
    public boolean isOverBorder(int r, int c) {
        return r < 0 || r >= rows || c < 0 || c >= cols;
    }

    /**
     * 收集所有当前可通行的节点。
     *
     * <p>
     * 它常用于随机选择起点和终点。这里只看节点状态，不额外检查图连通分量。
     * </p>
     */
    private List<MazeCell> availableCells() {
        List<MazeCell> result = new ArrayList<>();
        data.getGraph().nodes().forEach(node -> {
            MazeCell cell = getMazeCell(node.getId());
            if (cell != null && cell.getType() != MazeCellType.WALL) {
                result.add(cell);
            }
        });
        return result;
    }

    /**
     * 如果节点当前还是墙，则把它标记为道路。
     *
     * <p>
     * 这个小步骤能保证“设置起点/终点”或“连边打通通道”不会发生语义冲突：
     * 先可通行，再谈特殊角色。
     * </p>
     */
    private void markAsRoadIfWall(String id, boolean isAction) {
        MazeCell cell = getMazeCell(id);
        if (cell != null && cell.getType() == MazeCellType.WALL) {
            setCellState(id, MazeCellType.ROAD, isAction);
        }
    }

    /**
     * 判断两个节点在布局坐标上是否四方向相邻。
     *
     * <p>
     * 虽然主接口已经改成图语义，但当前图迷宫仍然沿用方格布局，
     * 所以连接两个节点前仍需要这层几何约束校验。
     * </p>
     */
    private boolean isAdjacent(MazeCell a, MazeCell b) {
        int dr = Math.abs(a.getRow() - b.getRow());
        int dc = Math.abs(a.getCol() - b.getCol());
        return dr + dc == 1;
    }

    /**
     * 查询两个节点之间的边。
     *
     * <p>
     * 这是多个图语义方法的底层公共查询入口，例如“是否连通”和“边权是多少”。
     * </p>
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
     * 同时清理旧起点和旧终点。
     */
    private void clearStartAndEnd() {
        clearOldStart();
        clearOldEnd();
    }

    /**
     * 清理旧起点状态。
     */
    private void clearOldStart() {
        if (startId == null) {
            return;
        }
        MazeCell oldStart = getMazeCell(startId);
        if (oldStart != null && oldStart.getType() == MazeCellType.START) {
            oldStart.setType(MazeCellType.ROAD);
        }
        startId = null;
    }

    /**
     * 清理旧终点状态。
     */
    private void clearOldEnd() {
        if (endId == null) {
            return;
        }
        MazeCell oldEnd = getMazeCell(endId);
        if (oldEnd != null && oldEnd.getType() == MazeCellType.END) {
            oldEnd.setType(MazeCellType.ROAD);
        }
        endId = null;
    }
}
