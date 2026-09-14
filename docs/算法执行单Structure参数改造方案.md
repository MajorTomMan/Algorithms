# 算法执行单 Structure 参数改造方案

## 1. 背景

当前算法注册已经采用 `@Algorithm + @AlgorithmEntry + @Structure` 的发现模型，但算法入口仍允许多参数，因此 Client / Server 仍需要根据方法参数数量、参数类型和返回类型判断算法属于遍历、最小生成树、迷宫生成、寻路、字符串搜索等哪一类，再决定如何组装参数和执行。

这会把算法语义重新泄漏到框架层，导致新增算法时仍可能需要修改 `AlgorithmCatalog`、Controller 或 Server Handler。

本次改造将算法执行契约收紧为：

```text
Algorithm = Structure -> Result
```

框架只负责把当前兼容的 Structure 交给算法执行。算法所需的起点、终点、随机种子、临时结构、辅助状态等由算法实现自行决定，框架不负责动态参数绑定，也不对算法进行 Sort / Traverse / MST / Search / Pathfind 等操作分类。

基线：`dev@ad72051a3f8d8f52e0328d297a79453986d6481b`

---

## 2. 核心原则

### 2.1 Algorithm 只有一个动作：执行

保留：

```java
@Algorithm(
    id = "graph-bfs",
    name = "Breadth First Search",
    type = Integer.class,
    structure = GraphStructure.class
)
public final class GraphBfs {

    @AlgorithmEntry
    public List<Integer> traverse(GraphStructure<Integer> graph) {
        ...
    }
}
```

框架不关心方法名是 `sort`、`traverse`、`build`、`search` 还是 `run`，只认唯一的 `@AlgorithmEntry`。

不要新增：

```text
@Sort
@Traverse
@MinimumSpanningTree
@Pathfind
@Search
AlgorithmOperation
operation = "..."
```

这些分类不属于算法执行框架。

### 2.2 Entry 只接受一个 Structure

所有 `@AlgorithmEntry` 必须满足：

1. `public`
2. 一个 Algorithm 类中有且只有一个 `@AlgorithmEntry`
3. Entry 恰好有一个参数
4. 唯一参数必须与 `@Algorithm.structure` 声明的 Structure Contract 兼容
5. 返回类型不限
6. `void` 合法

即：

```java
@AlgorithmEntry
public R method(S structure)
```

其中 `S` 是 Structure Contract，`R` 可以是任意返回类型或 `void`。

### 2.3 框架不处理算法动态参数

禁止引入：

```text
@Param
@Input
@Output
@Start
@Goal
@Pattern
@Seed
ArgumentResolver
InputProvider
动态参数表单生成
通用 DI
```

例如 BFS 原入口：

```java
public List<Integer> traverse(GraphStructure<Integer> graph, Integer start)
```

改为：

```java
public List<Integer> traverse(GraphStructure<Integer> graph) {
    Integer start = chooseStart(graph);
    ...
}
```

起点如何选择属于算法实现自由度，不属于框架执行契约。

### 2.4 Event 表达过程，return 表达结果

执行过程继续通过现有事件链表达：

```text
Algorithm
  -> StructureEvent / ObservationEvent
  -> EventReducer
  -> ViewState
  -> Visualizer
```

算法返回值只表示最终结果：

```text
Structure
  -> Algorithm
      -> Event Stream     -> Visualization / Timeline / Replay
      -> Result           -> ExecutionResult.output
```

Result 不参与算法分类，也不参与 Replay 驱动。

---

## 3. `@Algorithm` 与 `@AlgorithmEntry`

### 3.1 `@Algorithm` 保持现状

继续保留：

```java
public @interface Algorithm {
    String id();
    String name() default "";
    Class<?> type();
    Class<?> structure();
}
```

职责保持为：

```text
id        -> 算法身份
name      -> 展示名称
type      -> Structure value type
structure -> Structure capability / compatibility contract
```

本次不把方法签名推导结果反写进 `@Algorithm`，也不删除 `type` / `structure`。先保留显式声明并由 Discovery 做一致性校验。

### 3.2 `@AlgorithmEntry` 保持现状

`@AlgorithmEntry` 继续表示算法唯一可执行入口，不增加 operation/category 等属性。

---

## 4. AlgorithmDiscovery 改造

### 4.1 收紧 Entry 校验

当前 Discovery 已经要求恰好一个 `@AlgorithmEntry`，本次继续增加单 Structure 参数约束。

目标逻辑：

```java
private Method findEntryPoint(
        Class<?> implementation,
        Class<?> structureContract) {

    List<Method> entries = Arrays.stream(implementation.getMethods())
            .filter(method -> method.isAnnotationPresent(AlgorithmEntry.class))
            .toList();

    if (entries.size() != 1) {
        throw new RegistrationException(
                "Algorithm " + implementation.getName()
                        + " must expose exactly one @AlgorithmEntry method, found "
                        + entries.size());
    }

    Method entry = entries.getFirst();

    if (entry.getParameterCount() != 1) {
        throw new RegistrationException(
                "Algorithm entry must accept exactly one Structure parameter: "
                        + implementation.getName() + "#" + entry.getName());
    }

    Class<?> parameterType = entry.getParameterTypes()[0];
    if (!structureContract.isAssignableFrom(parameterType)) {
        throw new RegistrationException(
                "Algorithm entry Structure parameter does not match @Algorithm.structure: "
                        + implementation.getName());
    }

    return entry;
}
```

最终 assignability 方向应结合当前 Structure 接口层级固定为一个明确规则，不保留双向兼容作为长期方案。

### 4.2 Value Type 校验只检查唯一 Structure 参数

改造后不要再从所有参数和返回值收集泛型类型。

只检查 Entry 第 0 个 Structure 参数：

```java
@Algorithm(
    type = Integer.class,
    structure = GraphStructure.class
)
```

对应：

```java
@AlgorithmEntry
public List<Integer> traverse(GraphStructure<Integer> graph)
```

只验证：

```text
GraphStructure<Integer>
               ^
               必须与 @Algorithm.type 一致
```

返回值 `List<Integer>`、`GraphSnapshot<Integer>`、`boolean`、`int`、`void` 等不参与 Structure value type 推断。

---

## 5. AlgorithmDescriptor 改造

当前多参数入口：

```java
invoke(Object... arguments)
```

改为：

```java
public Object invoke(Object structure) {
    Object instance = newInstance();
    try {
        return entryPoint.invoke(instance, structure);
    } catch (...) {
        ...
    }
}
```

删除 `invoke(Object...)` 兼容入口。

不要保留一个“以后也许还有多参数算法”的逃生口，否则 Controller / Server 很快又会重新出现签名判断。

Framework Contract 从 API 层固定为：

```text
Algorithm = Structure -> Result
```

---

## 6. 现有算法迁移规则

### 6.1 Sort

原本只有 Structure 参数的算法基本无需修改：

```java
@AlgorithmEntry
public void sort(ArrayStructure<Integer> array) {
    ...
}
```

### 6.2 Graph Traversal

原：

```java
@AlgorithmEntry
public List<Integer> traverse(
        GraphStructure<Integer> graph,
        Integer start) {
    ...
}
```

改：

```java
@AlgorithmEntry
public List<Integer> traverse(GraphStructure<Integer> graph) {
    Integer start = chooseStart(graph);
    ...
}
```

BFS / DFS 如何选起点是算法自己的实现细节。

### 6.3 Minimum Spanning Tree

不要继续：

```java
void build(source, result)
```

改为单输入 + 返回结果：

```java
@AlgorithmEntry
public GraphSnapshot<Integer> build(
        WeightedGraphStructure<Integer> graph) {
    ...
    return resultSnapshot;
}
```

结果 Structure / Snapshot 不应伪装成第二个 Algorithm Input。

### 6.4 Maze Pathfinder

原：

```java
find(GridMaze maze, GridPoint start, GridPoint goal)
```

改为：

```java
@AlgorithmEntry
public List<GridPoint> find(MazeStructure maze) {
    GridPoint start = ...;
    GridPoint goal = ...;
    ...
}
```

如果 Maze 本身已有 entrance / exit，则算法直接使用；否则算法自行决定默认规则。本次不能为了 Maze 特例恢复第二、第三参数。

### 6.5 Maze Generator

原：

```java
generate(MazeDimensions dimensions, long seed)
```

不符合新 Contract。

应改为真正的 Structure 输入：

```java
@AlgorithmEntry
public MazeSnapshot generate(MazeStructure maze) {
    ...
}
```

尺寸、seed、生成策略相关状态如何确定属于 Maze 模型或算法实现问题，不属于 Algorithm Framework 动态参数问题。

### 6.6 String Search

原：

```java
search(StringStructure text, String pattern)
```

改为：

```java
@AlgorithmEntry
public List<Integer> search(StringStructure text) {
    ...
}
```

如果 pattern 仍需外部可配置，应在 String Workbench / Structure 模型层解决，而不是给 Algorithm Framework 增加第二参数。

---

## 7. AlgorithmCatalog 改造

逐步删除通过 Method Signature 判断算法类别的逻辑，包括但不限于：

```text
graphTraversals()
stringSearches()
arrayMazeGenerators()
graphMazeGenerators()
arrayMazePathfinders()
signature(...)
```

算法能否出现在当前页面只根据：

```text
Structure Compatibility
+
Value Type
```

目标调用：

```java
registry.compatibleAlgorithms(
        currentStructureContract,
        runtimeValueType)
```

不要再通过：

```text
参数个数
参数 Class
返回类型
方法名
```

推断算法属于 Traversal / MST / Generator / Pathfinder 等类别。

---

## 8. Controller 改造

Controller 不再负责识别算法类型。

禁止继续存在类似：

```java
if (descriptor.structureContract().equals(WeightedGraphStructure.class)
        && descriptor.entryPoint().getParameterCount() == 2
        && ...) {
    runMinimumSpanning(...);
} else {
    runTraversal(...);
}
```

目标统一为：

```java
AlgorithmDescriptor descriptor =
        algorithm(selectedAlgorithmId(), runtimeValueType);

startAlgorithm(
        descriptor.id(),
        inputSnapshot,
        () -> descriptor.invoke(currentAlgorithmStructure()),
        () -> createReducer(inputSnapshot)
);
```

无论下拉框选择 BFS、DFS、Prim、Kruskal、Dijkstra 或其他兼容算法，Controller 的 Algorithm Invocation Path 必须一致。

Controller 不允许：

```text
按 algorithm id 分支
按 entry parameter count 分支
按 entry parameter type 分支
按 return type 分支决定如何执行
```

---

## 9. 可视化保持独立

本次不重构：

```text
ExecutionEvent
EventEnvelope
EventReducer
ViewState
Visualizer
Timeline
PlaybackController
StatisticsReducer
```

算法运行时仍通过现有 StructureEvent / ObservationEvent 表达过程。

例如 Graph：

```text
Visited
Examined
Selected
...
```

Reducer 继续只根据事件生成 ViewState，不读取 Algorithm Java Method Signature。

规则：

```text
Algorithm Invocation Contract
    Structure -> Result

Visualization Contract
    Event -> Reducer -> ViewState -> Visualizer
```

二者互不依赖。

---

## 10. Result Contract

返回值允许：

```text
void
boolean
int
T
List<T>
StructureSnapshot
自定义 Result
```

Runtime 统一：

```java
Object output = descriptor.invoke(structure);
```

结果进入：

```text
ExecutionResult.output
```

必须遵守：

```text
Event  = 执行过程
Result = 最终结果
```

禁止：

```java
if (returnType == List.class) {
    // traversal
}

if (returnType == GraphSnapshot.class) {
    // mst
}
```

返回类型只能影响 Presentation 如何展示最终结果，不能用于：

```text
算法分类
算法调用
下拉框兼容性
Server dispatcher 选择
```

---

## 11. Server 改造

当前 Server 的多个 API Handler 中存在基于 AlgorithmDescriptor 方法签名的 `supports()` 判断。

单 Structure 参数改造后，Server 应收敛为：

```text
Request
  -> Transport Adapter 构造对应 Structure
  -> descriptor.invoke(structure)
  -> ExecutionResult.output
  -> Response
```

Server 可以理解自己的 API Request DTO，但不应该理解具体 Algorithm Java Signature。

逐步移除：

```text
IntegerSortHandler
GraphTraversalHandler
ArrayMazeGeneratorHandler
GraphMazeGeneratorHandler
ArrayMazePathfinderHandler
StringSearchHandler
```

中纯粹为了不同 Algorithm 参数数量 / 参数类型而存在的协议分支。

如果 Handler 仍需要保留，其职责应是：

```text
API DTO <-> Structure / Result
```

而不是：

```text
判断这是哪一种算法然后决定怎么调用 Method
```

---

## 12. 非目标

本次明确不做：

```text
动态参数系统
Algorithm Operation 分类系统
自动 UI Form 生成
参数注解体系
通用依赖注入
自动 Result Renderer 框架
重新设计 Event Contract
重新设计 Workbench UI Framework
```

特别是本次不要修改刚完成的：

```text
WorkbenchUiFramework
PlaybackToolbar
缩放 / compact / narrow 布局框架
```

除非单 Structure Algorithm Contract 的编译迁移确实需要最小适配。

---

## 13. 验收标准

改造完成后必须满足：

1. 所有 `@AlgorithmEntry` 恰好一个参数。
2. 唯一参数必须是对应的 Structure Contract / 合法兼容实现。
3. 0 参数或 2+ 参数 Algorithm 在 Discovery 阶段立即注册失败。
4. `AlgorithmDescriptor` 不再暴露 `invoke(Object...)`。
5. `AlgorithmDescriptor` 只允许 `invoke(Object structure)`。
6. Entry 返回值不参与 Algorithm 分类。
7. Value Type 校验只基于唯一 Structure 参数，不基于返回值。
8. `AlgorithmCatalog` 不再通过 Method Signature 判断算法类别。
9. `GraphController` 不再区分 Traversal / MST 的调用方式。
10. `MazeController` 不再根据 Entry Signature 决定 Algorithm 调用方式。
11. String / Tree / Array / LinkedList 等 Controller 不得通过 Algorithm ID 或 Entry Signature 判断执行协议。
12. Server 不再通过 Entry Signature 识别 Algorithm 类型。
13. Event / Timeline / Replay / Statistics 保持正常。
14. `void` Algorithm 正常执行。
15. 有返回值 Algorithm 的 output 正确进入 `ExecutionResult`。
16. 新增针对现有 Structure 的 Algorithm 时，仅新增 Algorithm 实现即可进入兼容算法集合。
17. 新增 Algorithm 不应要求修改 `AlgorithmCatalog`、Controller 或 Server dispatcher 才能“让框架认识它”。
18. `mvn clean test` 通过。

---

## 14. Code Review 红线

新增 Algorithm 时，如果仍需要修改下面任一组件才能让框架识别并执行该算法：

```text
AlgorithmCatalog
GraphController
TreeController
MazeController
StringController
ArrayController
Server Algorithm Dispatcher
```

则说明算法语义仍泄漏到框架，本次改造未完成。

允许因为新增 Structure Capability / 新 Workbench Module 本身而增加对应结构或页面代码；但对“已有 Structure 下新增一个 Algorithm”而言，框架执行层必须零改动。

---

## 15. 最终目标

开发者只需要写：

```java
@Algorithm(
    id = "graph-bfs",
    name = "Breadth First Search",
    type = Integer.class,
    structure = GraphStructure.class
)
public final class GraphBfs {

    @AlgorithmEntry
    public List<Integer> traverse(GraphStructure<Integer> graph) {
        ...
    }
}
```

框架统一：

```java
AlgorithmDescriptor descriptor = ...;
Object result = descriptor.invoke(currentStructure);
```

算法自由决定内部细节；框架只负责 Structure 兼容性、执行生命周期、Event 记录、Timeline / Replay / Statistics 和最终 Result 保存。
