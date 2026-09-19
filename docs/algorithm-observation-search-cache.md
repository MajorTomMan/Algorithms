# 跨结构算法观察事件：搜索与缓存（第一批）

基线：`MajorTomMan/Algorithms` 的 `dev 9e15f18`。本批只新增算法观察**事实**、发布便捷入口与独立指标；不修改 `StructureEvent`、已有结构 Reducer、RenderFrame、Kruskal、UI 布局或既有算法执行流。

## 事件边界

- 已有的 `Visited`、`Examined`、`Compared`、`Matched`、`Backtracked`、`PathTraced`、`PathFound` 不变；它们记录通用访问/比较/路径事实。
- `AlgorithmObservationEvent` 是 `ObservationEvent` 新许可的 `non-sealed` 子接口。具体算法可以 `record CustomFact(...) implements AlgorithmObservationEvent`，通过 `Observations.algorithm(new CustomFact(...))` 发布，而不修改原 `ObservationEvent` 的 `permits` 或运行时调度器。
- `EventEnvelope` 的 `operationId/runId/sequence` 仍由原运行时提供；`searchId/cacheId` 是**同一次运行内**区分多次搜索或多张备忘表的逻辑标识，不作为全局算法身份。
- 结构 Reducer 对新观察事件保持不处理、结构状态不变；当前 UI 的现有观察标记与事件详情兜底文本仍可呈现新事件，**未新增专属动画 Presenter**。
- 观察事件不是结构读写指令；`CacheStored` 指算法内部的记忆化表，不等于在图中插入边、在数组中更改元素或向结构写入缓存。新事件可触发原有单步/暂停 checkpoint，因此启用事件的算法可见步数会增加。

## 新事件

| 事件 | 事实 | 独立指标 |
|---|---|---|
| `SearchStarted(searchId, target)` | 开始一个逻辑搜索 | 无 |
| `SearchProbed(searchId, candidate)` | 检查一个候选位置 | `searchProbes` |
| `SearchFound(searchId, result)` | 得到一个命中结果 | `searchResults` |
| `SearchCompleted(searchId, resultCount)` | 结束搜索；0 表示无结果 | 无 |
| `CacheHit(cacheId, key)` | 记忆化查询命中 | `cacheHits` |
| `CacheMiss(cacheId, key)` | 记忆化查询未命中 | `cacheMisses` |
| `CacheStored(cacheId, key)` | 缓存写入或替换 | `cacheStores` |
| `CacheEvicted(cacheId, key)` | 算法内部移除某缓存项 | `cacheEvictions` |

**计数语义**：`SearchProbed` 不会重复计入 `Visited/Compared`；`SearchFound` 不会重复计入旧 `Matched`；缓存事件不计入结构插入/删除/更新。一次搜索是否会发布多个 `SearchFound`，由算法决定；`SearchCompleted.resultCount` 是算法提供的本次最终结果数，不由框架反推。

## 使用示例

```java
String searchId = "find-person";
Observations.searchStarted(searchId, new ObservationEvent.ValueRef("李四"));
Observations.searchProbed(searchId, new ObservationEvent.IndexRef("array", index));
if (matched) Observations.searchFound(searchId, new ObservationEvent.IndexRef("array", index));
Observations.searchCompleted(searchId, matched ? 1 : 0);

String cacheId = "memo-fibonacci";
String cacheKey = Integer.toString(n);
if (memo.containsKey(n)) {
    Observations.cacheHit(cacheId, cacheKey);
} else {
    Observations.cacheMiss(cacheId, cacheKey);
    memo.put(n, result);
    Observations.cacheStored(cacheId, cacheKey);
}
```

**快照安全**：缓存 key 是算法明确提供的不可变字符串标识，不是隐式 `Object.toString()` 结构身份；`ValueRef` 若指向可变对象，必须先满足项目既有的冻结/不可变契约才能用在历史回放。`SearchFound` 使用数组位置、稳定节点 ID 或坐标，不应为了展示而存入 JavaFX 节点。新事件不传递缓存结果对象，也不声称已支持任意可变值深拷贝。

## 后续阶段（本批不实现）

为搜索/缓存增加可选算法展示状态 Reducer 与 Presenter：候选节点高亮、命中标注、命中率卡片、缓存槽展示。展示状态独立于结构状态，明确注册后才消耗专属事件；没有 Presenter 的算法仍可记录、统计、单步和执行。将通用事件添加到现有 KMP、图搜索或动态规划算法，需要按该算法的真实查询语义逐个接线和回归；本批不批量给所有算法添加额外执行帧。
