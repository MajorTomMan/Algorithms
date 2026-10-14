# MultiGraph：统一注解与数据输入

`MultiGraph<T>` 是可存储平行边、孤立顶点、正反向关系的泛型多重图；
`Graph<T>` 与 `MultiGraph<T>` 在客户端共用 `GraphControls.fxml`，不提供单独的导入界面。

## 数据模型与两个注解

`@GraphNodes` 表示一个或多个节点，框架会递归读取**每个节点内部**再次声明的
`@GraphNodes`。这仅用于发现节点，不会自动生成父子关系边。

`@GraphEdges` 标记边对象的集合；其 `from` 和 `to` 指向**边对象本身**的字段、
record accessor 或 JavaBean getter 名称。`weight` 可选（默认 `1.0`），
`bidirectional` 可选（指定边对象中的布尔字段；仅有向图创建反向边）。
多条边即使端点一致也不会合并。

```java
record Region(String code, String name, @GraphNodes List<Region> children) {}
record Road(Region start, Region end, double km, boolean twoWay) {}
record Network(
    @GraphNodes List<Region> provinces,
    @GraphEdges(from = "start", to = "end", weight = "km", bidirectional = "twoWay")
    List<Road> roads) {}

MultiGraph<Region> graph = MultiGraph.from(
    GraphDirection.DIRECTED, Region.class, network);
```

不要求业务实体实现 `GraphNode` / `GraphEdge` 接口，不要求额外的包装实体或
`@ValueType`。不同类型的节点可以使用共同的 Java 父类型，或者显式使用 `Object.class`。
递归沿着有标记的属性前进，支持单值、`Iterable`、数组、`Map` 的值和 `Optional`。
遍历使用对象身份防止循环引用；相同对象不会重复建点。通过已有的可选
`@ValueId` 可匹配端点里**不同实例但相同业务 ID**的节点。重复业务 ID、
不存在的端点、字段名错误、非数值权重都会明确报错。

## 两条数据输入途径

代码输入直接使用 Java 对象：

```java
MultiGraph<Region> graph = MultiGraph.from(GraphDirection.DIRECTED, Region.class, network);
// 或者使用 graph.addVertex(...) / graph.addEdge(...)
```

外部文件输入复用 `core.io`，**不创建 MultiGraph 专用 JSON Loader**：

```java
JsonDataCodec<Network> codec = new JsonDataCodec<>(Network.class);
Network network = FileDataIO.read(Path.of("road-network.json"), codec);
MultiGraph<Region> graph = MultiGraph.from(GraphDirection.DIRECTED, Region.class, network);
```

`JsonDataCodec` 只处理输入格式，`MultiGraph.from()` 读取注解和图拓扑。
通过 JSON 提供边端点的独立对象实例时，建议使用业务 ID（`@ValueId`）关联。
当前没有将业务 JSON 字段名自动推断为节点身份的规则。

## 快照读写与恢复

`MultiGraphSnapshot<T>` 保存每个顶点、边的**独立数字 ID**。
整体替换请使用 `MultiGraph.fromSnapshot(snapshot)` 或 `initialize(snapshot)`，
不要让 JSON 业务对象的解析过程覆盖快照原有 ID。

```java
var codec = new JsonDataCodec<>(new TypeReference<MultiGraphSnapshot<Region>>() {});
FileDataIO.write(Path.of("saved-graph.json"), graph.snapshot(), codec);
MultiGraph<Region> restored = MultiGraph.fromSnapshot(
    FileDataIO.read(Path.of("saved-graph.json"), codec));
```

业务值和边载荷不会被快照深拷贝。要参与历史回放，它们必须确实不可变，
或有明确的冻结/复制策略。对于不提供文本解析器的自定义节点类型，
节点应通过 Java 代码或外部反序列化创建；共享的 JavaFX 控制面板不会擅自构造对象。

## 界面和算法

普通图和多重图共用界面，通过“实现方式”切换。
算法列表按 `GraphStructure` 与 `MultiGraphStructure` 契约区分，
泛型节点类型会参与匹配；BFS/DFS 等 `Object` 类型通用算法仍然适用。
UI 不提供专门的批量 JSON 输入、文件选择或导入按钮。
大图沿用图可视化的自适应布局/标签策略；该策略不影响真实边和节点数量。