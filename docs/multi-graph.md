# MultiGraph：注解式批量构图（V1）

保留原有 \`Graph<T>\` / \`Tree<T>\`，新增 \`MultiGraph<T>\`。图的方向由
\`GraphDirection.DIRECTED\` 或 \`UNDIRECTED\` 决定，不通过注解声明业务关系。

## 批量数据类型

\`\`\`java
@ValueType(MultiGraphStructure.class)
record City(@ValueId String code,
            @ValueField({ValueDisplay.SUMMARY, ValueDisplay.DETAIL}) String name) {}

record Railway(@GraphFrom City from, @GraphTo City to,
               @GraphWeight double kilometers, String line) {}

record Road(@GraphFrom City from, @GraphTo City to,
            @GraphBidirectional boolean twoWay, @GraphWeight double kilometers) {}

record TransportNetwork(@GraphNodes List<City> cities,
                        @GraphEdges List<Railway> railways,
                        @GraphEdges List<Road> roads) {}
\`\`\`

\`\`\`java
City sz = new City("440300", "深圳");
City gz = new City("440100", "广州");
City dg = new City("441900", "东莞"); // 保留为孤立节点

TransportNetwork data = new TransportNetwork(
    List.of(sz, gz, dg),
    List.of(new Railway(sz, gz, 102, "广深铁路")),
    List.of(new Road(sz, gz, true, 125)));

MultiGraph<City> graph = MultiGraph.from(GraphDirection.DIRECTED, City.class, data);
// 3 个节点、3 条边：铁路单向 + 公路正反两个方向。
\`\`\`

调用方负责决定关系并提供包含端点的边对象；框架仅根据标记读取成员并构建图，
不推断 \`Road\`、\`Railway\` 的业务含义。支持字段、record 组件、零参数方法；
\`@GraphNodes\` / \`@GraphEdges\` 接受单个值、数组或 \`Iterable\`。
\`@GraphFrom\` 与 \`@GraphTo\` 必须各出现一次；
\`@GraphBidirectional\` 为可选布尔字段，\`@GraphWeight\` 为可选数值字段（默认 1）。

节点之间的关联优先采用相同 Java 对象实例；如在业务类型中标记 \`@ValueId\`，
也可用同类型、同业务键的不同对象实例引用已声明的节点。
同类型重复业务键、缺失端点、非法权重将被拒绝。
任意未列在 \`@GraphNodes\` 中的关系端点**不会**被自动偷偷创建。

## 增量操作

\`\`\`java
MultiGraph<City> graph = new MultiGraph<>(GraphDirection.DIRECTED);
var a = graph.addVertex(sz);
var b = graph.addVertex(gz);
graph.addEdge(a, b, new Railway(sz, gz, 102, "广深铁路"));
var pair = graph.addBiEdge(a, b, new Road(sz, gz, true, 125));
graph.removeEdge(pair.forward().id());
\`\`\`

\`MultiGraph\` 按节点 ID 保存节点，不按对象的 \`equals()\` 去重。
同一端点可以有多个不同的边。无向图直接调用 \`addEdge\`；
\`addBiEdge\` 仅适用于有向图，原子添加两个方向。

\`snapshot()\` / \`initialize()\` 支持整体加载，批量初始化发布一个 Reset 结构事件，
而不是逐条事件。快照**不深拷贝**业务对象：用于历史回放时，业务值和边载荷须保持不可变。
可视化展示层能从 \`@ValueField\` 提取显式摘要和详情，但完整 MultiGraph JavaFX
交互控制器、算法适配器仍属于后续集成工作。
