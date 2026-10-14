# Graph：统一的多重图

`Graph<T>` 是唯一的图存储实现，天然支持平行边、相同节点值和独立边 ID。
普通的简单图是它的一个数据子集，不再提供 Simple/Multi 实现方式切换。

## 数据结构

- `Vertex<T>`：节点的稳定 ID 和业务值；即使两个值相等，也可以是两个不同节点。
- `Edge<T>`：独立 ID、起点、终点、可选业务载荷和权重。
- `GraphStructure<T>`：同一套图 API；`edges(from, to)` 返回全部平行边。
- `GraphSnapshot<T>`：同时保存节点、每一条边的身份与载荷，供保存和回放使用。

按业务值查找的 `vertex(value)` 以及单边 `edge(from, to)` 是便利方法，
在重名节点或平行边场景中应使用 ID 或返回集合的 API。
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
