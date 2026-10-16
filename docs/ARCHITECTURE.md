# Algorithms 框架架构说明

> 这份文档按源码阅读顺序组织：**设计目的 → 状态所有权 → 依赖边界 → 运行时序 → 线程与失败 → 扩展方式**。
> 类级 Javadoc 负责局部契约，这份文档负责跨模块关系。以当前 `dev` 分支实际实现为准。

## 1. 设计目标与不可违反的原则

本项目不是“算法直接调用 JavaFX 画图”，而是一个可脱离 UI 运行的算法/数据结构核心，加上事件驱动的桌面工作台。

1. **事实与呈现分离**：`Graph` / `Array` 等结构维护领域真值；`ExecutionEvent` 说明发生了什么；`EventReducer` 计算某一时刻应该看到什么；JavaFX 仅把计算好的视图状态画出来。
2. **执行与动画解耦**：`ExecutionRuntime` 不知道播放速度、画布、相机；播放速度不能反向影响已经记录的算法事实和结果。
3. **接口优先**：算法的 `@Algorithm.structure` 是 `@Structure` 能力接口；不让每个算法绑定具体控制器、JavaFX 节点或文件格式。
4. **可解释、可回放**：权威事件具有同一个运行 ID 和连续的 sequence；Reducer 依赖输入事件和前一状态，不依赖随机 UI 副作用。
5. **初始化和执行分离**：`@AlgorithmInitialize` 可以读数据、构建初始结构；`@AlgorithmEntry` 负责算法过程。一次运行共享实例和结构，不跨运行复用可变算法实例。
6. **数据模型与文件格式分离**：JSON/CSV reader 只反序列化；`GraphObjectMapper` 只解释对象中的图注解；业务引用或关联由调用方确定。

### 三种状态，三个所有者

| 状态 | 所有者 | 示例 | 是否权威 |
| --- | --- | --- | --- |
| **领域状态** | `structures` | `Graph<City>` 中的顶点/边、权重 | 是 |
| **可回放状态** | `client.runtime` 的 Reducer | `GraphViewState`、访问标记、当前检查边 | 来自权威事件的确定性投影 |
| **呈现状态** | `client.render` / JavaFX | ELK 坐标、镜头缩放、NodeView/EdgeView、动画 | 否 |

**禁止**让渲染器反向修改领域图，让 Reducer 直接修改 JavaFX 节点，或依靠视觉帧还原缺失的权威事件。

## 2. Maven 模块及依赖方向

仓库使用 Java 21，根目录聚合 `core`、`telemetry`、`structures`、`algorithms`、`practice`、`leetcode`、`client` 七个 Maven 模块。

```text
                       ┌────────────────────────────────┐
                       │       client (JavaFX UI)       │
                       │ controllers / reducers/render │
                       └───────────────┬────────────────┘
                                       │ 使用下层 API
                ┌──────────────┬───────┴───────────┐
                ▼              ▼                   ▼
           algorithms       practice             leetcode
                │              │                   │
                ▼              ▼                   ▼
           structures ──────► core ◄────────── telemetry
                │              ▲                   ▲
                └──────────────┘                   │
                    practice ─────────────────────┘
```

这是**概念上的主依赖关系**，不是 Maven POM 的逐条连线：
- `core`：注解、注册描述符、元数据、快照、事件、执行控制、通用 JSON/CSV。
- `structures`：数组、链表、栈、队列、树、图、哈希、迷宫等真实结构；依赖 core。
- `algorithms`：算法实现、算法发现、业务模型和算法侧资源加载门面；依赖 core/structures。
- `telemetry`：资源采样与性能 Profile；依赖 core，保持独立于 JavaFX。
- `practice`：练习题与其执行/交互逻辑；依赖 core、structures、telemetry。
- `leetcode`：练习算法集合；依赖 core、structures、algorithms。
- `client`：JavaFX/FXML、布局、交互、会话回放、日志、指标与渲染；消费以上模块。

**依赖规则**：领域核心不可引用 client；`core` 不依赖具体算法或图模型；图模型可使用 core 注解，但 JSON 加载器不可引用 City/Road。

## 3. 注册链：类如何出现在算法菜单

关键文件：
- `core.annotation.Algorithm`、`AlgorithmEntry`、`AlgorithmInitialize`、`Structure`、`ValueType`
- `core.registry.FrameworkClassScanner`、`AlgorithmDescriptor`、`ComponentRegistry`
- `structures.discovery.StructureDiscovery`
- `algorithms.discovery.AlgorithmDiscovery`、`ComponentDiscovery`
- `client.algorithm.AlgorithmCatalog`、`client.runtime.value.ValueAdapters`

```text
编译的 classpath
   │
   ├─ @Structure 接口 ──► StructureDiscovery ──┐
   │                                         ├─► ComponentRegistry
   ├─ @Algorithm 类 ───► AlgorithmDiscovery ──┘            │
   │                                                      ▼
   │                                               AlgorithmCatalog
   │                                                      │
   └─ @ValueType 实体 ─► ValueAdapters ─► 类型选择器 ──────┘
                                                │
                                      按结构 + Class 类型过滤
                                                ▼
                                         工作台算法列表
```

- `@Algorithm(id=..., types=..., structure=...)` 的 ID 是稳定键；`name` 是文字，不参与类型匹配。
- `@AlgorithmEntry` 必须唯一；`@AlgorithmInitialize` 至多一个，且 public、非 static、返回 void，与 entry 接收同一个泛型结构类型。
- `AlgorithmDescriptor.invoke(structure)` **每次新建**算法实例，先初始化再执行；直接调用算法方法不会触发注解生命周期。
- 图算法对当前顶点 `City.class` 做精确类型匹配；`Object.class` 通用算法只作后备，同名精确算法覆盖它。
- `@ValueType(name="城市", value=GraphStructure.class, immutable=true)` 允许客户端按注解发现 City、显示“城市”，并限制它仅适用于图能力接口。
- `ValueAdapters` 当前只自动扫描 `com.majortom.algorithms.algorithm` 包下已编译的带 `@ValueType` 的具体实体；实体放在包外或未参与客户端 classpath 时不会自动出现。内置类型仍用显式适配器。
- 类型选择器的可见名称不是注册键；实际仍传递 `Class<?>`。声称 immutable=true 是开发者保证**整个可达对象图**不可变，record 本身不足以深冻结对象。

### 注册不到算法时先看哪里

1. 源文件是否已编译且在客户端 classpath？
2. 类是否在 AlgorithmDiscovery 的扫描根包下并标注 `@Algorithm`？
3. `@AlgorithmEntry` / `@AlgorithmInitialize` 参数与泛型签名是否符合注册校验？
4. 当前 `GraphController.runtimeValueType` 是否与 `@Algorithm.types` 一致？
5. `@ValueType` 是否能被 ValueAdapters 发现、且声明的结构契约允许当前模块？

## 4. 算法执行链：从点击按钮到真实计算

```text
MainController / GraphController.handleAlgorithmStart()
  │ 选定算法 + 捕获输入 GraphSnapshot
  ▼
Graph.fromSnapshot(...)                 # 隔离算法副本
  ▼
BaseController.startAlgorithm(...)
  ▼
LocalClientExecutionService / LocalAlgorithmExecution
  │ 创建 ExecutionSession、worker、RunControl 和有界事件存储
  ▼
ExecutionRuntime.execute(...)
  │ RunStarted; 绑定 ThreadLocal 事件上下文
  ▼
AlgorithmDescriptor.invoke(graph)
  ├─ new 算法实例
  ├─ @AlgorithmInitialize (可选；本次运行)
  └─ @AlgorithmEntry
       │
       ├─ Graph 结构修改 ─► ExecutionEvents.structure(...)
       └─ 算法观察事实 ─► ExecutionEvents.algorithm(...)
                              │ 检查暂停/单步/取消许可
                              ▼
                    RuntimeEventContext / EventEnvelope
                              │ runId / sequence / timestamp
                              ▼
                EventSink：权威存储 + 时间锚点 + UI 观察队列
```

### 为何算法使用输入副本

工作台编辑区中的图和算法运行中的图不是同一实例。图控制器先将当前或选定的 `GraphSnapshot` 转成独立的 `Graph`，再交给描述符执行。这样用户可以回放算法、失败后查看历史，而不会悄悄改写原始编辑数据。

带 `@AlgorithmInitialize` 的算法允许从空图运行；初始化阶段通过 `Graph.initialize(snapshot)` 发出 `GraphStructureEvent.Reset`，后续可视化通过 reducer 在该事件发生时整体更新图。**注意方向必须一致**：`Graph` 的有向/无向属性在构造时固定，`initialize` 拒绝不匹配的方向。

### 控制点和失败语义

- `ExecutionRuntime` 本身是**同步调用**；后台线程由 `LocalAlgorithmExecution` 配合 `ExecutionScheduler` 创建。
- `DefaultExecutionControl` 使用 Condition 协作等待。单步释放一个领域事件许可，不等同于视觉播放的一帧。
- `ExecutionEvents` 使用 ThreadLocal，**不会自动传播到算法自行创建的新线程**。不在运行上下文中发出的事件不会进入当前历史。
- Runtime 分别发布 Started / Completed / Failed / Cancelled。结构或算法层不应该伪造这些生命周期事件。
- `RuntimeEventContext` 为事件分配连续 sequence；投递失败不允许被当成正常事件流。
- `ExecutionSession.runtimeCompletion` 表示实际运算结束；`presentationCompletion` 代表 UI 观察者队列也处理完毕。二者不能混用。
- 新执行的 generation 用于屏蔽来自旧会话的过期异步 UI 更新；会话结束或关闭要释放调度器与观察通道。

## 5. 从领域事实到回放、统计及动画

```text
EventEnvelope（唯一权威事件流）
      │
      ├─► ExecutionAnchorRecorder ─► 真实执行时间锚点
      │
      ├─► StatisticsReducer       ─► 事件指标
      │
      └─► ReductionCursor（顺序校验）
                    │
                    ▼
              EventReducer<S>   # 纯归约，不操作 UI
                    │
                    ▼
             GraphViewState 等
                    │
      ┌─────────────┴─────────────────┐
      ▼                               ▼
直播时增量呈现              ReducedEventTimeline（持有完整事件和稀疏检查点）
      │                               │
      │                         PlaybackController
      │                         play/pause/step/seek
      └─────────────┬─────────────────┘
                    ▼
           StructureRenderDriver
                    ▼
              RenderFramework
```

- `GraphStructureEvent` 表示真实结构变更，例如 VertexAdded、EdgeWeightChanged、Reset。
- `AlgorithmEvent` 表示观察/决策，例如 Visited、Examined。算法访问某顶点不代表图真的新增了顶点。
- `GraphEventReducer` 接收两类事实，将拓扑和暂态访问标记投影为不可变 `GraphViewState`。
- `ReducedEventTimeline` 将事件映射到**可见帧**，非视觉日志/生命周期不制造空白帧；保存稀疏检查点用于倒退和随机跳转。
- `PlaybackController` **不重新执行算法**；改变动画速度只影响帧调度，不修改事件内容。
- 事件序列是唯一权威；渲染可以合并高频可合并的状态通知，但不能擅自丢弃或重排原始领域事实。

## 6. 渲染链：为什么不允许算法直接操控 JavaFX

主要角色：
- `StructurePresenter`：根据新旧 ViewState 决定 Structural / Presentation 意图。
- `StructureVisualization`：将纯视图状态转换为 LayoutRequest。
- `StructureRenderDriver`：保存最近状态并提交 RenderIntent。
- `DefaultRenderFramework`：统一管理会话 revision、邮箱、布局、相机与 FX 提交。
- `RenderMailbox`：按会话执行 latest-wins、合并及有序帧背压。
- `LayoutCoordinator`：几何输入相同时复用布局，不同时选择布局引擎并后台计算。
- `GraphVisualizer`：FX 线程最终持有 NodeView、EdgeView、选择和动画对象。
- `RenderRuntime`：渲染依赖组合根，负责共享实例和布局引擎注册。

```text
GraphViewState
   ▼
GraphPresenter ──► RenderIntent
   ▼
StructureRenderDriver
   ▼
DefaultRenderFramework / RenderMailbox
   ├─ Capture/Describe ─► LayoutCoordinator ─► ELK 等 LayoutEngine（后台）
   └─ Apply/Pulse/Camera/Commit ─► FxExecutor ─► GraphVisualizer（FX 线程）
```

同一个图发生 **Visited** 变化，不应重新对全部节点运行 ELK。只有拓扑、标签几何或方向等布局事实变化才发结构意图。过期布局由会话代际/revision 保护，不能逆向覆盖更新的模型。RenderMailbox 允许明确标记可合并的展示帧 latest-wins，但要求保序的播放帧必须仍保序；队列有上限，不能无限消耗内存。

**线程所有权：** 算法 worker 不碰 FX Node；LayoutExecutor 不碰 FX Node；RenderScheduler 协调请求；所有 JavaFX 场景修改由 FxExecutor 在 FX 线程完成。

## 7. 注解式业务数据和路网建图

```text
classpath JSON/CSV
  ▼
AlgorithmResourceLoader / core.io
  ▼
City、Road、Region 普通 Java 数据
  ▼
GraphMappingMetadata (@GraphNodes / @GraphEdges)
  ▼
GraphObjectMapper（先顶点，后边；@ValueId 关联）
  ▼
Graph.from(...) ─► GraphSnapshot ─► Graph.initialize(...) / Reset
```

- `City.id` 是业务 ID；顶点 `Vertex.id()` 是另一套结构生成 long ID。
- `Road.from`、`Road.to` 目前声明为 **City 类型**。因此直接 `load(..., List<Road>)` 时，roads.json 必须存嵌套 City 对象；当前 demo 的 roads.json 正是这种格式。
- 如果未来把 roads.json 改成 `"from": "sz"`，必须显式解决字符串到 City 的业务引用，或调整数据模型/映射策略。**通用 JSON loader 不应偷偷猜测跨文件关联。**
- `GraphObjectMapper` 同一引用优先按实例匹配，不同实例按运行时类加 `@ValueId` 匹配；有向双向道路映射为两条独立边 ID。
- `Graph.initialize` 先在临时图重建并校验、再替换内部状态并发出一次 Reset，不应该为了做初始化在 UI 里手工逐顶点调用新增事件。

**当前实现状态：** `ShortestPath` 类已注册为“最短路径”，并带 `@AlgorithmInitialize` 读取路网；但它的 `@AlgorithmEntry execute` 目前仍是空方法，**尚无最短路径计算逻辑**。注册成功不等于算法已经实现。

## 8. 遥测、日志和运行历史

`telemetry` 与 `core.runtime` 是两条不同关注点：
- Runtime 负责算法运行的事实生命周期、执行序号与取消。
- `TelemetryFramework` / `TelemetrySession` 负责资源采样，`TelemetryScopeId` 定义结构/算法/练习的稳定范围。
- `TelemetryStore` 保存终态 Profile，设有单范围与全局数量上限；JavaFX 只展示不可变结果。
- `ExecutionAnchorRecorder` 用 `System.nanoTime` 记录真实事实发生时刻，和 RenderClock 动画调度耗时不同。
- 事件导出、日志、回放应该引用同一个 runId/sequence，不能用 FX 帧数替代真实事件数量。

## 9. 修改某层会影响哪里

| 修改点 | 直接影响 | 必须同步关注 | 不应该动 |
| --- | --- | --- | --- |
| 新增 `@Algorithm` 实现 | AlgorithmDiscovery / Registry | `@AlgorithmEntry` 签名、当前类型、算法选择器 | RenderFramework |
| 增加 `@ValueType` 实体 | ValueAdapters / 类型选择器 | `value` 限制、immutable、解析规则 | 通用 JSON reader 的业务逻辑 |
| 修改 `@GraphEdges` 或模型 | GraphMappingMetadata / ObjectMapper | endpoint 业务键、方向、多重边 | JavaFX 节点布局规则 |
| 修改 `Graph.initialize` | 真实结构状态 | 原子校验、Reset 事件、快照方向 | UI 手工同步顶点 |
| 修改 Runtime 事件 | 事件序列与统计 | Reducer、导出、序列校验 | 直接操作 FX |
| 修改某个 Reducer | 直播/重放 ViewState | 幂等、纯归约、可见帧定义 | Graph 真实拓扑 |
| 修改 Presenter | 是否重新布局 | Geometry/Presentation 分类 | 算法权威事实 |
| 修改 RenderMailbox | 请求背压 | 帧顺序、会话 revision | EventEnvelope 内容 |
| 修改遥测 | Profile 指标 | 范围 ID、采样边界和 UI 展示 | 算法排序/计算语义 |

## 10. 新增功能时的推荐路径

### 新增算法

在 `algorithms` 中实现 `@Algorithm` 类；保留一个 `@AlgorithmEntry`。
只有确实需要每次执行前读取数据或初始化结构，才加 `@AlgorithmInitialize`。
算法调用结构接口，并用 AlgorithmEvent 发布可观察的事实，不直接编写 JavaFX 动画。
客户端依靠结构与 Class 类型自动选择算法。

### 新增实体类型

使用 `@ValueType(name=..., value=某结构契约.class, immutable=...)`，
并用 `@ValueId` 声明稳定业务键、用 `@ValueField` 声明可展示属性。
把类编译进算法包扫描路径。不要把中文 `name` 当成类型 key，
也不要把用户可编辑 JSON 的解析策略与业务文件加载混为一谈。

### 新增可视化结构

先在 `structures` 建契约、实现真实操作和结构事件，
再在 `client.runtime` 建 ViewState/Reducer，
由 Presenter 决定 RenderIntent，最后增加布局语义及 FX renderer。
不要直接在事件发射方操作布局或摄像机。

## 11. 注释维护约定（参考 Android/AOSP 的阅读体验）

- **类 Javadoc** 先说明角色、为什么存在、归谁所有、在哪条调用链上。
- **公共方法 Javadoc** 说明调用前置条件、状态变更、异常及线程/所有权约束；非显而易见的方法写 `@param` / `@return`。
- **复杂实现处行内注释** 解释“为什么这样做”和必须保持的不变量，而不是逐行翻译 Java 语法。
- **跨层引用** 写真实类名和方法名，并指出上游/下游影响。避免“线程安全”“原子操作”等无法由实现保证的空泛声称。
- **状态与线程** 必须标注事实真值、回放投影、UI 所有权、worker/FX 边界、关闭后行为。
- **不要在注释中承诺尚未实现的功能**；例如当前 ShortestPath 仍是执行入口占位。
- 修改运行逻辑后应同步更新直接关联的注释和本文档，防止文档与代码长期漂移。

## 12. 构建与仓库约束

仓库的 `agent.md` 明确允许 JSON/CSV 示例和测试数据，但**禁止新增测试代码及 CI 测试任务**。
CI 只检查正式源码编译：

```bash
mvn -B -ntp -Dmaven.test.skip=true compile
```

本文档与源码 Javadoc 属于架构说明，不用于引入新的测试运行链路。
