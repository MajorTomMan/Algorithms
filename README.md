# Algorithms V2

Algorithms 是一个数据结构与算法实验室。当前 V2 的主线是：**唯一 canonical Structure → factual StructureEvent / ObservationEvent → Runtime/Timeline → Reducer/ViewState → JavaFX + GestureFX + ELK Visualizer**。

## 模块

```text
core/        Runtime、Event Contract、Registry、Timeline、Snapshot、Statistics、Logging、Scheduler
algorithms/  canonical 数据结构、算法实现、Algorithm auto-discovery provider
client/      JavaFX + AtlantaFX Workbench、Reducer/ViewState、Layout、Visualizer、Playback
server/      Spring Boot headless API
other/       历史练习/教学代码，不属于 V2 production registry
```

依赖方向：

```text
core
 ↑
algorithms
 ↑       ↑
client  server
```

`core` 不依赖 `algorithms`、JavaFX、AtlantaFX 或 Spring。`core` 只定义跨模块 SPI/Contract；例如 `ModuleDiscovery` 在 core，真正认识 `Sort/TreeAlgorithm/GraphTraversal` 的 `AlgorithmModuleDiscovery` 在 algorithms。

Registry 的 `ValueType` 只描述算法所属的数据值类型，允许值固定为：

```text
Integer / Long / Double / Float / Boolean / Character / Byte / Short / String
```

`ValueType` 不表示整个算法输入类型。

## Canonical Structure

`algorithms/src/main/java` 当前正式基础结构：

```text
Array<T>                         -> ArrayStructure<T>
LinkedList<T>                    -> LinkedStructure / StackStructure / QueueStructure
Tree<T>                          -> GeneralTreeStructure<T>（General/N-ary Tree）
Graph<T>                         -> GraphStructure<T>（directedness 为配置）
String                           -> StringStructure
AVLTree<T>                       -> SearchTreeStructure<T>（Tree algorithm runtime 使用）
HashTableStructure<K,V>          -> 仅 Contract，当前没有 concrete implementation
```

Registry 当前 structure key：

```text
structure.array.Integer
structure.linked-list.Integer
structure.stack.Integer
structure.queue.Integer
structure.tree.Integer
structure.graph.Integer
structure.string.String
```

HashTable **没有 production registration**，Workbench 不把“只有 Contract”伪装成已实现结构。

### Tree family 当前范围

当前 V2 正式完成：

```text
TreeNode<T>                 -> 仅所有 Tree 共有的 id / value
GeneralTreeNode<T>          -> ordered children
BinaryTreeNode<T>           -> left / right
AVLTreeNode<T>              -> height / subtree metrics
Tree<T>                     -> General/N-ary Tree
AVLTree<T>                  -> AVL Search Tree
```

`RedBlackTree` 与 canonical `HuffmanTree` **本轮未实现，明确列为 future implementation**。旧 `basic/HuffmanTree` 已从 production source 删除；历史 Huffman 练习保留在 `other`，不代表 V2 Tree family 已支持 Huffman。

`GeneralTreeStructure` 的真实领域能力包括：

```text
addRoot
addChild(parent[, index], value)
set(node, value)
remove(node)
move(node, newParent[, index])
```

`move` 会维护树不变量并拒绝形成 cycle；不会通过 Controller 直接修改 children。

## Event 边界

Structure Contract 不暴露 `raw()` 或 mutable collection/array 逃生口。

算法代码不显式发布 visualization-driving Structure Event。真正结构变化只能来自 Structure mutation；read-only 执行事实通过最小 ObservationEvent 表达：

```text
UI / Algorithm
      ↓
Structure API / Node mutation
      ↓
factual StructureEvent
      ↓
Runtime / Timeline
```

典型 factual Event：

```text
Array       Inserted / Removed / Updated / Swapped
Linked      NodeInserted / NodeRemoved / ValueChanged / NextChanged / PreviousChanged
Tree        NodeInserted / NodeRemoved / ValueChanged / RootChanged / ChildInserted / ChildRemoved / LeftChanged / RightChanged
Graph       VertexAdded / VertexRemoved / EdgeAdded / EdgeRemoved
String      Inserted / Removed / Updated / Replaced
Runtime     Started / Paused / Resumed / Completed / Cancelled / Failed
Snapshot    Created / Restored
Logging     LogEvent
```

Compare / Pivot / Visit / KMP Fallback 等算法意图不再作为 Structure truth source。

## Execution Closure

Runtime lifecycle 正式状态为 `NOT_STARTED / RUNNING / PAUSED / COMPLETED / CANCELLED / FAILED`。Pause/Resume 可以完整 recording + replay；EventEnvelope sequence 只在 sink 成功接收后提交，因此 delivery failure 不会留下 sequence hole。

Live Step 的 gate 只作用于可执行观察单位：

```text
checkpoint / StructureEvent / ObservationEvent
                 ↓
             RunControl
RUNNING                 -> pass
PAUSED + step permit    -> consume one permit and pass one unit
PAUSED + no permit      -> block
CANCELLED               -> abort
```

Log/Snapshot 等辅助事件仍可进入 Timeline，但不会消费 Live Step permit。Statistics 只从 StructureEvent / ObservationEvent 的 `StatisticsContribution` 聚合，不由 UI 推测。

Snapshot reconstruction 会验证 canonical invariant；Server execution catalog 以 ModuleRegistry availability 为 implementation truth，并对 scheduler reject、bounded retention、result/failure query 和 HTTP error contract 做显式处理。

## Algorithm I/O / Registry / auto-discovery

`algorithms` 不维护为了框架统一而存在的 `AlgorithmInput / AlgorithmOutput / XxxInput / XxxOutput`。Java 方法签名本身就是算法 I/O Contract。当前正式算法 API：

```java
void Sort<T>.sort(ArrayStructure<T> array);
List<T> GraphTraversal<T>.traverse(GraphStructure<T> graph, T startNode);
List<Integer> StringSearch.search(StringStructure target, java.lang.String pattern);
void AvlTreeCommands.execute(AVLTree<Integer> tree, List<AvlCommand> commands);
GridMaze ArrayMazeGenerator.generate(MazeDimensions dimensions, long seed);
List<GridPoint> ArrayMazePathfinder.findPath(GridMaze maze, GridPoint start, GridPoint goal);
GraphSnapshot<T> GraphMazeGenerator<T>.generate(MazeDimensions dimensions, long seed);
```

`GridMaze`、`GridPoint`、`MazeDimensions`、Structure 与 Snapshot 是有独立领域/边界语义的对象，不属于 I/O wrapper。

显式实现可继续通过：

```text
META-INF/algorithms.factories
```

注册。Algorithm 还通过 `ServiceLoader<ModuleDiscovery>` 自动发现 family interface implementation；显式 registry key 优先覆盖自动发现结果。

新增算法不要求修改 `MainController`。Workbench 从 `ModuleRegistry` 读取 family / value type / algorithm id。

## Snapshot 与隔离

Structure 模式拥有唯一可编辑 live Structure。Algorithm 模式只使用当前结构快照或已保存 Snapshot 创建隔离运行副本，不反写 live Structure。

Snapshot reconstruction 是**初始化边界**，不是普通 Structure mutation：

```text
Graph.fromSnapshot(...)
Tree.fromSnapshot(...)
AVLTree.fromRestoredRoot(...)
```

不再提供 `graph.restore(...) / tree.restore(...) / avl.restore(...)` 这种可对现有实例直接替换内部状态的公共写入口。

Snapshot state 属于 core 数据契约，例如：

```text
SequenceSnapshot
GeneralTreeSnapshot
BinaryTreeSnapshot
GraphSnapshot
StringSnapshot
MazeSnapshot
```

JavaFX ViewState 不进入 core Snapshot。

## Workbench / Visualization

Client 只有一个 Workbench，一个 Visualizer 区域；Structure / Algorithm 是互斥模式。

Visualization 技术路线固定为：

```text
StructureEvent / ObservationEvent
   ↓
Reducer
   ↓
ViewState
   ↓
family-specific layout adapter
   ↓
JavaFX project-owned Node/Edge/CSS/Animation
   ↓
VisualizationSurface / GestureFX viewport
```

职责边界：

```text
JavaFX      -> visual element / CSS / animation
AtlantaFX   -> Workbench theme / controls
GestureFX   -> center canvas zoom / pan / fit / center / reset
ELK         -> client-only transient layout calculation
```

Array/String/Stack/Queue/LinkedList/Tree/Graph 使用 family-specific ELK adapter；Maze 保留 project-owned Canvas/Grid Layout，只复用 GestureFX viewport。ELK graph model 是瞬态计算对象，不进入 Structure/Event/Snapshot/ViewState/core/algorithms/server。

Playback speed 是 presentation-only：x8 按基础动画时长的 `1/8` 播放，x16 与 timeline scrub 直接 snap，避免积压无界 JavaFX animation backlog。

## Server

Server 与 Client 共用 `ModuleRegistry + ExecutionRuntime + algorithms`，但不依赖 client。

HTTP DTO 只属于 server 边界；进入 algorithms 后使用真实领域 Structure / 参数对象。

```text
HTTP JSON
    ↓
Server Request DTO
    ↓
Structure / 普通 Java 参数
    ↓
Algorithm
    ↓
真实 Java 返回值
```

Server 可以把返回值转换为 HTTP 表达，但不会要求 `algorithms` 再维护一套 Input/Output。`ExecutionRequest`、`core.runtime.ExecutionResult` 与 `core.snapshot.*` 保留各自边界职责。

## `other` 模块

`other` 仅保存练习、历史代码和第三方教材示例，不属于 canonical family。生产源码中已经删除的旧重复实现不会为了让 `other` 编译而加回 compatibility layer；示例应迁移到当前 API，或在 `other` 内自包含。直接依赖 Princeton `algs4.jar` 的教材专项示例保留在 `other/examples/princeton/`，不进入默认 Maven reactor compile。

## 开发约束

- 不保留旧架构 compatibility wrapper。
- 不重新引入 `raw()` mutable state。
- 不让 Controller/Algorithm 建立第二条 Structure Event 路径。
- 不引入万能 Runner / giant Context / universal VisualModel。
- 不引入 GraphStream、FXDiagram、Graphviz、SmartGraph、WebView+D3 或 universal Diagram model；ELK 仅作为 client transient layout engine。
- 公共父类只保存所有子类型真正共有的领域状态。
- Snapshot reconstruction 只能用于初始化/隔离副本恢复，不作为普通编辑 API。

## 验证

Execution Closure 在验收阶段使用最小 contract regression tests 验证 Runtime / Snapshot / Server 正式语义；这些临时测试在验证通过后删除，正式源码不保留 `src/test`。最终交付以 production reactor `clean compile` 与运行级 smoke 为准：

```bash
mvn clean compile
```

Closure 至少验证：

```text
Pause / Resume recording + replay
sink failure 后 sequence contiguous
PAUSED Live Step 精确放行一个 checkpoint / StructureEvent / ObservationEvent
BFS / KMP / Maze factual observation + statistics
Graph / General Tree / AVL Snapshot invariant rejection
Server scheduler reject / bounded retention / result & failure query / HTTP errors
ELK failure containment
JavaFX startup smoke
Snapshot isolation
Timeline replay deterministic
x8 playback duration scaling
x16 playback no-animation backlog
```
