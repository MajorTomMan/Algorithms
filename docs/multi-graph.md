# MultiGraph：注解式批量构图（V1）

保留原有 `Graph<T>` / `Tree<T>`，新增 `MultiGraph<T>`。图的方向由
`GraphDirection.DIRECTED` 或 `UNDIRECTED` 决定，不通过注解声明业务关系。

## 批量数据类型

```java
@ValueType(value = MultiGraphStructure.class, immutable = true)
record City(@ValueId String code,
            @ValueField({ValueDisplay.SUMMARY, ValueDisplay.DETAIL}) String name) {}

@ValueType(immutable = true)
record Railway(@GraphFrom City from, @GraphTo City to,
               @GraphWeight double kilometers, String line) {}

@ValueType(immutable = true)
record Road(@GraphFrom City from, @GraphTo City to,
            @GraphBidirectional boolean twoWay, @GraphWeight double kilometers) {}

record TransportNetwork(@GraphNodes List<City> cities,
                        @GraphEdges List<Railway> railways,
                        @GraphEdges List<Road> roads) {}
```

```java
City sz = new City("440300", "深圳");
City gz = new City("440100", "广州");
City dg = new City("441900", "东莞"); // 保留为孤立节点

TransportNetwork data = new TransportNetwork(
    List.of(sz, gz, dg),
    List.of(new Railway(sz, gz, 102, "广深铁路")),
    List.of(new Road(sz, gz, true, 125)));

MultiGraph<City> graph = MultiGraph.from(GraphDirection.DIRECTED, City.class, data);
// 3 个节点、3 条边：铁路单向 + 公路正反两个方向。
```

调用方负责决定关系并提供包含端点的边对象；框架仅根据标记读取成员并构建图，
不推断 `Road`、`Railway` 的业务含义。支持字段、record 组件、零参数方法；
`@GraphNodes` / `@GraphEdges` 接受单个值、数组或 `Iterable`。
`@GraphFrom` 与 `@GraphTo` 必须各出现一次；
`@GraphBidirectional` 为可选布尔字段，`@GraphWeight` 为可选数值字段（默认 1）。

节点之间的关联优先采用相同 Java 对象实例；如在业务类型中标记 `@ValueId`，
也可用同类型、同业务键的不同对象实例引用已声明的节点。
同类型重复业务键、缺失端点、非法权重将被拒绝。
任意未列在 `@GraphNodes` 中的关系端点**不会**被自动偷偷创建。

## 增量操作

```java
MultiGraph<City> graph = new MultiGraph<>(GraphDirection.DIRECTED);
var a = graph.addVertex(sz);
var b = graph.addVertex(gz);
graph.addEdge(a, b, new Railway(sz, gz, 102, "广深铁路"));
var pair = graph.addBiEdge(a, b, new Road(sz, gz, true, 125));
graph.removeEdge(pair.forward().id());
```

`MultiGraph` 按节点 ID 保存节点，不按对象的 `equals()` 去重。
同一端点可以有多个不同的边。无向图直接调用 `addEdge`；
`addBiEdge` 仅适用于有向图，原子添加两个方向。

`snapshot()` / `initialize()` 支持整体加载，批量初始化发布一个 Reset 结构事件，
而不是逐条事件。快照**不深拷贝**业务对象：用于历史回放时，业务值和边载荷须保持不可变。
可视化展示层使用核心 `ValueMetadata` 元数据统一读取 `@ValueField` 摘要和详情。
MultiGraph 已有独立 JavaFX 工作台、平行边布局和 BFS/DFS 结构算法。
在图形工作台中可输入批量类的 Java 完整类名，粘贴 JSON 或选择 JSON 文件批量加载。
只支持已编译并存在于应用 classpath 上的 Java 类；不动态编译用户代码。

## 验证

```bash
mvn -pl structures -am test-compile
# 无需 JUnit：编译后按项目 classpath 手动运行 MultiGraphChecks.main()
```

`ValueType` 注解声明元素适用结构和展示名称；`ValueMetadata` 将 `@ValueId`
和 `@ValueField` 元数据统一缓存，类型可直接按需发现，不要求为每个对象手工注册
`ValueAdapter`。这不意味着自定义对象都自动支持文本解析、随机生成或深度不可变快照。

### JavaFX 导入自定义批量 JSON

工作台的 **MultiGraph** 模块内，填写类完整名称（例如
`com.example.model.TransportNetwork`），粘贴符合该类型的 JSON，
或通过“选择 JSON 文件”读入本地文件，然后点“导入图数据”。
JSON 通过 Jackson 反序列化为该 Java 对象，之后交给
`MultiGraph.from(direction, data)` 统一解析 `@GraphNodes`、
`@GraphEdges` 和端点注解。反序列化和构图不在 JavaFX UI 线程执行；
成功提交前保留旧图。编译产物必须在 JavaFX 程序的 classpath 中。

示例 JSON：

```json
{
  "cities": [
    {"code": "sz", "name": "深圳"},
    {"code": "gz", "name": "广州"}
  ],
  "railways": [
    {"from": {"code": "sz", "name": "深圳"},
     "to": {"code": "gz", "name": "广州"},
     "kilometers": 102, "line": "广深铁路"}
  ],
  "roads": [
    {"from": {"code": "sz", "name": "深圳"},
     "to": {"code": "gz", "name": "广州"},
     "twoWay": true}
  ]
}
```

同一类型和 `@ValueId` 的不同 Java 实例会映射到批量节点集合中的同一个节点。
所有端点必须已出现在 `@GraphNodes` 中；这是明确的构图约束，而不是自动
递归关系发现。方向、权重、是否双向都由用户自定义的边类型提供。

### 运行算法时的不可变约束

`MultiGraph.from()` 批量构图本身不要求业务类添加 `immutable = true`。
但是工作台在运行 BFS/DFS 及历史回放前，要求节点值与边载荷都是可安全回放的不可变对象。
如果业务类型及其**整个可达对象图**确实不可变，可以用 `@ValueType(immutable = true)`
明确声明；标注了 `record` **不自动等于深度不可变**（record 内部可能持有可变 List）。
需要可变业务对象时，应由开发者提供冻结/复制策略，不能仅靠注解绕过回放安全检查。
