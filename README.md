Algorithms

Algorithms 是一个面向数据结构学习、算法实验、执行过程观察和服务端运行的数据结构与算法项目。项目由领域结构、算法实现、执行运行时、桌面客户端和服务端组成。

项目结构

core/        执行运行时、事件、注册表、Timeline、Snapshot、Statistics、Logging、Scheduler
algorithms/  数据结构与算法实现
client/      JavaFX 工作区、可视化、动画播放、Snapshot、回放和执行控制
server/      Spring Boot 算法目录与执行 API
other/       练习、示例和独立程序

主要依赖关系：

algorithms ─────> core
client ─────────> core + algorithms
server ─────────> core + algorithms
other ──────────> algorithms

core 是 UI 无关的纯 Java 层，不依赖 JavaFX、FXML 或 Spring Web。

运行环境

JDK 21

Maven

JavaFX 21

Spring Boot

项目根目录是 Maven 多模块工程。

常用验证命令：

mvn -pl client -am test
mvn -pl server -am test

构建客户端：

mvn -pl client -am package

客户端入口：

com.majortom.algorithms.launcher.LauncherMain

服务端入口：

com.majortom.algorithms.server.AlgorithmsServerApplication

数据结构

生产注册表提供以下一等结构：

Array

LinkedList

Stack

Queue

Tree

Graph

HashTable

String

结构通过领域接口暴露真实操作，例如：

Array：get / set / insert / remove / swap

LinkedList：insert / remove / update

Stack：push / pop / peek

Queue：enqueue / dequeue / front / rear

Tree：insert / remove / find / rotate

Graph：add/remove vertex、add/remove edge、neighbors

HashTable：put / get / remove

String：replace / insert / remove / update

结构可以暴露必要的 raw 数据，使算法直接工作在真实结构上，而不是可视化模型上。

算法

算法保留明确的领域方法，例如：

sort.sort(array);
graphBfs.traverse(graph, startNode);
kmp.search(string, pattern);
mazeGenerator.generate(input);
pathfinder.findPath(input);
avl.execute(input);

算法实现不依赖 JavaFX，也不直接控制动画。结构变化和算法语义通过领域事件表达。

模块注册

结构与算法通过：

META-INF/algorithms.factories

注册。

启动时由：

ModuleLoader -> ModuleRegistry

加载。

示例：

structure.array.Integer=com.majortom.algorithms.library.structure.MutableArray
algorithm.array.Integer.quick-sort=com.majortom.algorithms.library.sort.IntegerQuickSort
structure.string.String=com.majortom.algorithms.library.structure.MutableString
algorithm.string.String.kmp=com.majortom.algorithms.library.string.KmpSearch
structure.hash-table.String.Integer=com.majortom.algorithms.library.structure.MutableHashTable

Registry 负责根据结构类型、值类型和算法标识找到具体实现。

执行模型

一次执行由 ExecutionRuntime 管理。

Runtime 负责：

runId / operationId

事件 sequence

start / pause / resume / step / cancel

生命周期状态

错误处理

EventEnvelope 元数据

执行线程由 ExecutionScheduler 管理。领域结构和算法不自行维护执行线程。

领域事件由 Runtime 包装成 EventEnvelope：

runId
operationId
sequence
timestamp
source
event

同一事件流可以同时提供给 Timeline、Statistics、Logging、Client playback 和 Server recording。

Client 工作区

客户端使用一个工作区，并提供两种互斥模式：

Structure

负责：

编辑真实结构

结构可视化

保存 Snapshot

恢复 Snapshot

查看结构历史

Structure 模式持有唯一可编辑结构。

Algorithm

负责：

选择算法

设置算法参数

选择算法输入 Snapshot

运行 / 暂停 / 恢复 / 单步

Timeline 回放

日志与统计

算法输入来自：

当前结构生成的 Snapshot；或

已保存的 Snapshot。

算法运行时从 Snapshot 创建独立运行数据，不修改 Structure 模式中的真实结构。

Maze 生成结果先作为算法结果存在，需要显式应用后才写入 Structure。

Snapshot

Snapshot 是完整结构状态的数据表示，定义在 core。

主要类型包括：

SequenceSnapshot

BinaryTreeSnapshot

GraphSnapshot

HashTableSnapshot

StringSnapshot

MazeSnapshot

Snapshot 不包含 JavaFX ViewState。

保存的 Snapshot 可以恢复到 Structure，也可以单独作为 Algorithm 输入。

Timeline 与播放

Runtime 产生权威 EventEnvelope 流：

Structure / Algorithm
        ↓
ExecutionRuntime
        ↓
EventEnvelope
        ├── Recording
        ├── Timeline
        ├── Statistics
        ├── Logging
        └── JavaFX Playback -> Reducer -> ViewState -> Visualizer

算法执行速度和 JavaFX 播放速度互相独立。动画延迟只影响 presentation playback，不阻塞算法 worker。

runtimeCompletion 表示执行本身完成，presentationCompletion 表示客户端播放完成。

Server API

服务端使用与客户端相同的 Registry、Runtime 和 Scheduler 运行算法。

接口：

GET  /api/v1/health
GET  /api/v1/algorithms
POST /api/v1/executions
GET  /api/v1/executions/{runId}

执行请求格式：

{
  "algorithmId": "quick-sort",
  "input": {
    "values": [5, 3, 8, 1]
  }
}

POST /api/v1/executions 返回 runId，随后可通过运行 ID 查询执行状态。

Server 的 HTTP DTO 只负责传输数据。进入执行层后，输入会转换为领域结构或算法输入对象。

扩展原则

新增结构或算法时：

定义清晰的领域操作或算法方法。

实现必要的领域事件。

在 META-INF/algorithms.factories 注册实现。

需要桌面展示时，在 Client 增加 reducer、visualizer 和控制逻辑。

需要 HTTP 调用时，在 Server 增加窄 DTO 和输入转换。

核心算法逻辑应保留在具体结构和算法实现中，Runtime 只管理执行生命周期和公共运行能力。