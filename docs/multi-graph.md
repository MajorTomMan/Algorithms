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
MultiGraph 现在位于统一的 **图 / Graph** 工作台中，作为「实现方式 / Implementation」的第二个选项；不再单独占据左侧的结构家族入口。保留平行边布局和 BFS/DFS 结构算法。
多重图批量构建与数据导入由 Java 代码完成，不在 JavaFX 工作台增加专用导入控件。
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

Java 代码中提供批量对象类型（例如
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

## 大规模图的自适应可视化

MultiGraph 和原有 Graph 共用渲染底座，不限制实际节点或边数量。为了避免大图触发
ELK 的高昂开销以及力导向布局的成对排斥计算，图可视化应用以下默认策略：

| 条件 | 渲染策略 |
|---|---|
| 节点 ≤ 180 且边 ≤ 450 | 保留原来的图布局选择（有向无环使用 ELK，否则力导向） |
| 节点 > 180 或边 > 450 | 用并查集划分不连通分量，逐分量采用确定性网格布局 |
| 节点 ≤ 80 且边 ≤ 450 | 允许完整的节点 ID 标签 |
| 节点 ≤ 80 且边 ≤ 160 | 允许全部边权/边载荷标签 |
| 节点 > 80 或边 > 450 | 默认不在全部节点上绘制文字，选中时展示摘要 |
| 大于动画阈值 | 禁用逐元素布局切换动画，避免批量入图同时触发过量动画 |

这里所有阈值统一放在 `GraphRenderScale`，属于**展示策略**，不会裁剪
`MultiGraph` 的真实节点、边、权重或快照。大图仍然保留平行边分离路径和
节点/边选中功能，完整业务字段通过 `@ValueField` 在选中对象后按需读取。
BFS/DFS 观察事件使用端点索引更新本次节点及边高亮，减少重复 JavaFX 更新。

**输入性能**：选择 JSON 文件后不会先把全文放进 JavaFX TextArea；点击导入后在
后台线程读取文件、反序列化和构图。粘贴 JSON 仍受 TextArea 本身的大文本容量限制。

**限制**：当前大图采用确定性网格而不是保留地理坐标的地图布局，
JavaFX 仍会为每个实际节点和边创建图形对象，尚未实现 Canvas 批量绘制、
视口虚拟化或百万级数据分页。此策略主要消除高复杂度布局和无意义的标签/高亮开销；
超大图的实际 FPS 和内存占用仍需在目标桌面设备上测试。

### 普通图与多重图的工作台选择

工作台侧栏仅保留一个「图 / Graph」导航项。进入结构界面后：

- **实现方式：普通图**：继续使用 `GraphStructure<T>`、现有节点/边编辑操作与普通图算法；
- **实现方式：多重图**：使用 `MultiGraphStructure<T>`、JSON/自定义批量对象导入与多重图 BFS/DFS；
- **算法界面**：算法列表由当前实际结构契约过滤，普通图不显示多重图算法，反之亦然；
- **有向/无向**：是选定实现内部的独立设置，不由「实现方式」决定。

切换实现方式当前会重新创建相应的结构控制器，并清除与前一实现不兼容的工作台快照，
防止把 `GraphSnapshot<T>` 误恢复为 `MultiGraphSnapshot<T>`。
如需保留当前构图数据，请在切换前导出或保存对应数据。
这只是前端归类调整：底层 `Graph<T>` 与 `MultiGraph<T>` 仍完全独立，
注解构图、单向/双向连接及多重边 API 均不变。

### 本地离线构建依赖说明

项目需要 Spring Boot 父 POM、JavaFX、Jackson、ELK 等完整 Maven 本地仓库。
仅有 ELK 0.12.0 的 jar/pom 无法进行完整离线构建；
离线执行 `mvn -o -pl client -am test-compile` 需要事先下载全部父 POM、
插件及传递依赖。GitHub Actions 的 Java 21 reactor install 与回归测试
用于验证最新 `dev` 代码；本地旧版源码快照的结果不能代表最新分支。

### 图结构 UI 复用约定

Graph 与 MultiGraph 使用**同一套** `GraphControls.fxml`，包含实现方式、
有向/无向选择、节点增删、边增删、权重编辑、算法选择与执行。
切换到多重图不会出现 JSON 文本框、文件选择、批量导入或额外示例按钮。
多重图数据导入在代码中调用 `MultiGraph.from(direction, batch)` 或
`MultiGraphController.loadBatch(batch)`；无需编写任何专用 FXML。

多重边存在多条同端点关系时，修改/删除操作需要先点击目标边，以明确指定边 ID；
节点值重复时同样需要先点击目标节点。此 UI 不会根据业务对象
`equals()` 擅自合并或删除节点、边。
