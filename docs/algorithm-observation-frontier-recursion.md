# Generic algorithm observations: frontier and recursion

Scope: incremental extension on top of the search/cache observations and the algorithm-only PresentationSurface for the `dev 9e15f18` workbench. No StructureEvent definition, structure reducer, RenderFrame, or existing comparison/visit/backtrack event was changed.

## Producers

```java
Observations.candidateAdded("graph-bfs", "vertex-7", new ObservationEvent.EntityRef("graph-vertex", 7));
Observations.candidateSelected("graph-bfs", "vertex-7");
Observations.candidateRejected("graph-bfs", "vertex-7");
Observations.candidatePruned("graph-bfs", "vertex-7");
Observations.callEntered("call-7", "call-5", "dfs(vertex=7)");
Observations.callReturned("call-7", "done");
```

`frontierId` groups independent search queues/candidate sets in one execution. `candidateId` identifies a logical candidate; it must be stable and unique within its frontier while the candidate is tracked. The reference is a pre-existing `ObservationEvent.Reference`. A candidate `Selected` is no longer pending but remains visible as a fact; `Rejected` and `Pruned` are terminal statuses. A second `Added` of the same candidate ID resets its status to pending. These are presentation/history facts, **not** commands to push/pop an actual queue or mutate any structure.

`callId` identifies one active logical invocation. `parentCallId` is `null` at the root, otherwise equals the current top call ID; return events must match that top ID. The call-stack presentation ignores malformed parent/return sequences and exposes a diagnostic pulse rather than corrupting historical state. The depth is derived from enters and returns; no separate depth event is generated, avoiding contradictory facts. `label` and `resultSummary` are display strings and must not carry mutable object references.

## Consumer

The existing `AlgorithmObservationTimeline` produces JavaFX-free immutable prefix states: `frontiers/currentFrontierId/callStack`, alongside the previous `searches/recentCache` state. `FxAlgorithmObservationRenderer` renders up to four recent candidates and five recent stack frames with complete pending/selected/pruned counts and total call depth; it does not create or touch structure nodes. Forward-adjacent events use the existing pulse animation; random seek/backward seek restores the corresponding prefix deterministically.

The example graph BFS emits candidate add/select around its existing `Visited`/`Examined` observations. AVL inorder traversal emits call enter/return around its existing `Visited`/`Examined` observations. Both retain their actual data structure operations and algorithm return values.

## Constraints / follow-ups

- Observations are read-only; they do not issue `StructureEvent` or change an actual graph/tree/array.
- The frontier projection is a candidate status history, not a replacement for a real queue. Its immutable prefix snapshots retain all emitted candidates; extremely large searches should later adopt a bounded/checkpoint strategy to prevent quadratic history growth.
- Event references and custom values follow the existing immutable/freeze-for-replay contract.
- Full Maven/JavaFX runtime validation requires the project dependency cache and a JavaFX-capable environment. This patch is verified with Java 21 standalone compilation and executable event/algorithm smoke tests; it has not been UI-click-tested.
