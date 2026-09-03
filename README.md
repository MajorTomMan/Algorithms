# Algorithms V2

Algorithms 是一个数据结构实验室 + 算法实验室。当前 V2 以领域结构、显式算法、事件驱动 Runtime 和单工作区双模式客户端为核心。

## 模块

```text
core/        UI 无关的 Runtime、Event、Registry、Timeline、Snapshot、Statistics、Logging、Scheduler
algorithms/  真实数据结构和算法实现
client/      JavaFX Workbench、可视化、动画播放、回放、结构快照 UI
server/      Spring Boot headless 算法目录与执行 API
other/       历史/练习代码，不属于 V2 生产注册表
```

依赖方向：

```text
client ──────┐
             ├──> core
algorithms ──┤
             └──> algorithms
server ─────────> core + algorithms
```

`core` 不依赖 JavaFX、FXML、Spring 或具体 UI 表现。

## V2 核心模型

### Registry：决定 WHO

生产实现通过 `META-INF/algorithms.factories` 注册，启动时由：

```text
resources KV -> ModuleLoader -> ModuleRegistry
```

解析。一个 key 对应一个当前实现，修改映射后重启即可，不存在热插拔类加载器。

当前一等结构：

- Array
- LinkedList
- Stack
- Queue
- Tree
- Graph
- HashTable
- String

HashTable 使用显式双类型 key，例如 `structure.hash-table.String.Integer`。

### Runtime：决定 HOW

`ExecutionRuntime` 管理一次执行的：

- runId / operationId / sequence
- start / pause / resume / live step / cancel
- Runtime 生命周期事件
- EventEnvelope 元数据
- 错误与取消

线程创建统一通过 `ExecutionScheduler`。算法和数据结构不自行创建执行线程。

### ExecutionEvent：描述 WHAT

算法和结构只发布有意义的领域事件，例如：

- Array：Inserted / Removed / Updated / Swapped
- Tree：Insert / Remove / Rotate / Compare / Visit
- Graph：Vertex / Edge / Visit
- String/KMP：Compare / Fallback / Match
- Runtime：Started / Paused / Resumed / Completed / Cancelled / Failed
- Snapshot：Created / Restored
- Logging：LogEvent

事件描述领域事实，不描述颜色、动画时长或 JavaFX 控件。

## Workbench 模型

Client 只有一个工作区，`Structure / Algorithm` 是同一工作区的两个互斥模式，任何屏幕尺寸都不会同时显示两套 Workbench。

- Structure 模式持有并编辑真实结构，可保存/恢复 Snapshot，并查看 Structure History。
- Algorithm 模式不持有第二份可编辑结构，只选择“当前结构快照”或“已保存 Snapshot”作为输入。
- 算法运行时从 Snapshot 构造独立临时副本；运行过程、回放和结果都不得反写当前 Structure。
- LinkedList / Stack / Queue / HashTable 等 structure-only 模块直接禁用 Algorithm 模式，不提供伪算法入口。
- Structure 模式隐藏算法执行工具条；只有进入 Algorithm 模式后才显示 Run / Pause / Step / Replay。
- Algorithm 模式会显示当前输入来源，并在尚未执行时直接预览选中的 Snapshot。
- 算法运行期间锁定模式和模块切换；运行结束后自动解锁。
- 已锁定到保存 Snapshot 的算法状态与当前 Structure 编辑相互独立。
- 窄窗口不会隐藏 Snapshot 面板。
- Maze 生成结果不会自动反写 Structure；需要显式“应用结果到结构”。Maze 寻路使用当前或已保存的 Maze Snapshot。

## 数据结构与算法

算法保留自己的领域方法，不经过万能 `run(Context)`：

```java
sort.sort(array);
graphBfs.traverse(graph, startNode);
kmp.search(string, pattern);
mazeGenerator.generate(input);
pathfinder.findPath(input);
avl.execute(input);
```

排序等算法直接依赖领域结构契约。结构动作由结构自身产生结构事件，算法只产生算法语义事件，避免一项操作产生两套重复事件。

Maze 的 BFS / DFS / Union-Find 生成器以及 A* / DFS Pathfinder 是独立真实实现，不通过 Strategy 枚举切换核心流程。

## Client Workbench

客户端共享一个 Workbench shell：

- Structure 模式：结构导航、结构操作、可视化、Snapshot、Structure Timeline
- Algorithm 模式：算法导航、参数、Snapshot 输入、算法可视化、日志/统计、执行 Timeline

Structure 模式持有唯一可编辑结构。Algorithm 模式只使用当前结构快照或已选择的 Snapshot 创建临时运行副本，不反写 Structure 模式。

Array / Tree / Graph / String 可以把保存的旧 Snapshot 直接选为 Algorithm 输入；这不会触发结构恢复，也不会修改当前结构。

Snapshot 数据属于 core，例如：

- `SequenceSnapshot`
- `BinaryTreeSnapshot`
- `GraphSnapshot`
- `HashTableSnapshot`
- `StringSnapshot`
- `MazeSnapshot`

JavaFX ViewState 不进入 core Snapshot。

## 执行与播放

Runtime 执行速度与 Client 动画播放速度是两个独立速度域：

```text
Algorithm / Structure
        ↓
ExecutionRuntime
        ↓ authoritative EventEnvelope stream
        ├── Recording / Timeline / Statistics / Logging
        └── JavaFX playback queue -> Reducer -> ViewState -> Visualizer
```

算法 worker 不因 JavaFX 动画 delay 或 UI 队列等待而 sleep。Client 可以独立 pause/step live playback；Runtime pause/resume/live-step/cancel 由 core `RunControl` 管理。

core 的 `ExecutionTiming` 只保存 event span 和 Runtime total duration。visual frame 与 playback duration 是 client presentation 数据。

## Server

Server 使用与 Client 相同的 `ModuleRegistry + ExecutionRuntime + ExecutionScheduler` headless 执行算法。

主要接口：

```text
GET  /api/v1/health
GET  /api/v1/algorithms
POST /api/v1/executions
GET  /api/v1/executions/{runId}
```

`POST /executions` 返回真正的 `runId`。

HTTP DTO 只存在于 server 边界；进入算法前会构造成真实领域结构/输入对象。

## 开发约束

V2 重构遵循：

- 不保留旧架构兼容层
- 允许 breaking refactor
- 不为此次重构新增测试源；修改并运行已有测试验证
- 不引入万能 Runner / Support / Context
- 不用 Provider/Factory/Adapter/Strategy/Manager/Service 链掩盖核心算法
- 算法仍然看起来像算法，数据结构仍然看起来像数据结构
- 普通 get/size/raw 不强制事件化
- core 不包含 UI toolkit 或 presentation 语义
- 方法调用和参数列表保持紧凑，避免无意义换行

## 离线验证

当前离线快照使用 OpenJDK 21 + Maven 3.9.16。主验证范围：

```bash
mvn -o -Dmaven.repo.local=<offline-repository> -pl client -am test
mvn -o -Dmaven.repo.local=<offline-repository> -pl server -am test
```
