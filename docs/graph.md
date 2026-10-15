# Graph：统一的多重图

`Graph<T>` 是唯一的图存储实现，天然支持平行边、相同节点值和独立边 ID。
普通的简单图是它的一个数据子集，不再提供 Simple/Multi 实现方式切换。

## 数据结构

- `Vertex<T>`：节点的稳定 ID 和业务值；即使两个值相等，也可以是两个不同节点。
- `Edge<T>`：独立 ID、起点、终点、可选业务载荷和权重。
- `GraphStructure<T>`：同一套图 API；`edges(from, to)` 返回全部平行边。
- `GraphSnapshot<T>`：同时保存节点、每一条边的身份与载荷，供保存和回放使用。

以 ID 作为节点和边的唯一身份：
`vertex(id)`、`edge(id)`、`edges(from, to)`、`outgoingEdges(vertex)`。
旧的按值查找 `vertex(value)` 在重名节点下会抛出异常，
`edge(from, to)` 和 `removeEdge(from, to)` 在平行边下会抛出异常；
`adjacentEdges(vertex)` 无法完整表达平行边，也会拒绝有歧义的结果。
它们仅作为兼容便利方法保留，不得用于多重图的核心操作。
`addEdge` 始终创建独立边，不再将重复端点对应的权重覆盖。
图的方向由 `GraphDirection.DIRECTED/UNDIRECTED` 指定。

## 两条输入路径

程序可以直接添加节点和边：

```java
Graph<String> graph = new Graph<>(GraphDirection.DIRECTED);
var a = graph.addVertex("深圳");
var b = graph.addVertex("广州");
graph.addEdge(a, b, "高铁", 102);
graph.addEdge(a, b, "高速", 125); // 一对节点，两条独立的边
```

业务对象使用 `@GraphNodes` 和 `@GraphEdges(from, to, weight, bidirectional)`
由 `Graph.from(direction, nodeType, data)` 读取。
不要求业务实体实现图框架接口；可选 `@ValueId` 能关联来自 JSON 的独立端点实例。
递归探测拥有循环引用保护，节点和边只通过被标记的成员发现。

外部输入复用通用 `core.io`，不引入图专用 JSON Loader：

```java
JsonDataCodec<Network> codec = new JsonDataCodec<>(Network.class);
Network network = FileDataIO.read(Path.of("network.json"), codec);
Graph<City> graph = Graph.from(GraphDirection.DIRECTED, City.class, network);
```

## 算法与界面

BFS/DFS 只各注册一份、支持不同节点类型，按稳定节点 ID 遍历，
包含孤立节点和所有连通分量。Kruskal 等具体算法保留必要的图方向约束。
可视化继续复用 `GraphControls.fxml`，保留有向/无向选择，
不再显示底层实现方式选择。

`Graph.fromSnapshot` 静默构造，`initialize` 才发布整体替换事件，
以免临时重建触发额外的结构事件。

## 图编辑器与随机数据

- 用户点击图上的节点或边时，修改和删除操作使用所选对象的**稳定 ID**。
- 手工输入 `#123` 可指定节点 ID 123（节点值相等、没有文本解析器时也适用）。
- 手工输入普通业务值时，只有唯一节点值才可自动定位；有歧义会要求选择具体对象。
- 编辑已有边的权重或删除某一条平行边，应先在画布中选中那条边。
- 随机图默认允许平行边和自环，且严格生成所要求的边数；可通过代码参数禁用。
- 文本批量输入可以重复声明相同端点的边或自环，但节点列表仍要求业务值唯一，
  因为此处的简易文本格式没有单独的节点 ID 字段。业务节点值相同的图请使用 Java 对象/注解输入。
- 自环对节点的度数贡献为 2；可视化每条边保留独立 ID。
