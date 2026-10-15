# Graph：统一的多重图

`Graph<T>` 是唯一的图存储实现，天然支持平行边、相同节点值和独立边 ID。
普通的简单图是它的一个数据子集，不再提供 Simple/Multi 实现方式切换。

## 数据结构

- `Vertex<T>`：节点的稳定 ID 和业务值；即使两个值相等，也可以是两个不同节点。
- `Edge<T>`：独立 ID、起点、终点、可选业务载荷和权重。
- `GraphStructure<T>`：同一套图 API；`edges(from, to)` 返回全部平行边。
- `GraphSnapshot<T>`：同时保存节点、每一条边的身份与载荷，供保存和回放使用。

以 ID 作为节点和边的唯一身份：
`vertex(id)`、`edge(id)`、`removeEdge(id)`、`edges(from, to)`、
`outgoingEdges(vertex)`。节点值不具备唯一性，平行边也不应被折叠成
单条边或 `Map<Vertex, Edge>`。

已删除旧入口 `vertex(value)`、`edge(from, to)`、
`removeEdge(from, to)`、`adjacentEdges(vertex)`，不再保留
`@Deprecated` 兼容方法。业务值的文本解析/唯一性检查只由图编辑器负责，
算法使用稳定 ID 定位实体，Kruskal 不会再根据业务值找回结果顶点。
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

## 自定义值的文本展示

ALG 的算法实体由项目自行定义，使用已有的 `@ValueField` 声明可见属性，
不再注册独立的 `ValuePresenter` 或 `DisplayField` 展示配置。

```java
record City(
    @ValueId String code,
    @ValueField(value = {ValueDisplay.SUMMARY, ValueDisplay.DETAIL}, label = "城市") String name,
    @ValueField(label = "人口") long population
) {}
```

- `ValueAdapters.summary(value)`：读取标记为 `SUMMARY` 的字段，适用于画布、
  列表和其他轻量场景；不调用仅供 `DETAIL` 展示的 getter。
- `ValueAdapters.project(value)`：选中对象时读取 `DETAIL` 字段，返回
  `ValueProjection`，由 UI 统一生成属性行。
- `String`、数字、布尔值等基础类型无需注解，自动提供“值”字段；
  没有标记 `@ValueField` 的自定义类只显示类名，不自动调用 `toString()`。
- 嵌套对象默认只显示其类型名，不进行递归反射。
- `ValueAdapter<T>` 继续只负责文本解析、编辑和可选样本生成；
  注册解析器不会覆盖实体上的展示注解。

**图节点 ID 与业务对象的 `@ValueId` 分别负责拓扑身份与业务身份**，
画布摘要不用于比较实体或寻找边的端点。大型图应先读取摘要，
用户选择具体元素后再投影详细字段。

## 跨结构统一的值展示

数组、树、链表、栈、队列、哈希表和图统一采用 `VisualValue.compact()`
生成画布文字。实体本身和算法逻辑无需实现展示接口，项目内定义的业务类
继续通过 `@ValueField(SUMMARY/DETAIL)` 声明可见字段。

- 不可变基础值以及显式声明 `@ValueType(immutable = true)`（或经
  `ValueAdapters.declareImmutable` 保证不变）的实体，绘制帧时只读取
  `SUMMARY`；选中元素时经 `VisualValue.detailProjection()` 计算
  `DETAIL`。**只有整个可达对象图不变时才能声明 immutable**。
- 未声明不可变的业务对象，在创建展示帧时就缓存摘要和详情字段，
  旧帧的检查器直接读取已缓存的文本，不因后续对源对象的修改而变化。
  这保证**展示文本**稳定；若算法需要可重放的完整对象状态，仍须遵循
  框架原有快照/冻结约束，不能仅靠显示缓存代替数据快照。
- 检查器先显示结构属性（节点 ID、数组索引、链表指针、栈顶/队首位置等），
  再展示独立的“对象属性”。基础字符串/数字等单值类型已出现在结构属性中，
  不重复显示“值”。哈希条目的键属性和值属性分区展示。
- 迷宫与字符串结构保留专用的布局和状态，不强制变成通用对象节点。
