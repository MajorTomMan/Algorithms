# Algorithms V2 Workbench Execution / Timeline Closure Audit

- Date: 2026-09-05
- Target branch: `dev`
- Audited HEAD: `b29099b024dd8095bbce633552ebed777fe3712a`
- Audited commit message: `优化代码并重构Ui`
- Scope: Workbench UI, Runtime presentation synchronization, Timeline / Replay mapping, Snapshot restore UX, Maze event replay closure

---

## 1. Conclusion

The current V2 architecture does **not** need another large refactor. The Structure / Runtime / Reducer / ViewState / JavaFX / ELK / GestureFX split is still valid.

The remaining work is now a **closure problem**, not an architecture redesign problem.

The highest-priority issue is that the current Workbench mixes two different coordinate systems:

```text
Authoritative Event Index
0 1 2 3 4 5 6 7 ...

Visible Frame Index
0   1     2   3 ...
```

A Runtime event does not necessarily produce a visual frame. Therefore:

```text
eventIndex != frameIndex
```

Any code that converts one to the other by direct indexing or by percentage is incorrect.

Before further UI polish, finish these items in order:

```text
P1-1  Replay frameIndex -> EventEnvelope exact mapping
P1-2  Timeline eventIndex -> frameIndex exact mapping
P1-3  Live presentationEvent ordering
P1-4  Restore Snapshot confirmation
P1-5  Maze final-state replay closure
```

Then continue with selection coverage, Timeline geometry, dock collapse, button sizing and I18N.

---

# 2. Items already fixed correctly

## 2.1 Tree selection stale-state issue

The Tree selection path is now substantially correct.

Current code clears selection when:

- selected node becomes invalid;
- selected node is deleted;
- a Tree snapshot is restored;
- Tree data is reset.

The parent input is also explicitly cleared for root nodes:

```java
parentIdField.setText(parent == null ? "" : Long.toString(parent.getId()));
```

This resolves the previous Child -> Root parent-id residue problem.

Status:

```text
Tree node selection             PASS
Tree parent field synchronization PASS
Delete selected node cleanup     PASS
Snapshot restore cleanup         PASS
Reset cleanup                    PASS
```

Do not redesign this part again unless a new reproducible bug appears.

---

## 2.2 Narrow-screen minimum-width deadlock

The previous hard minimum width combination was reduced.

Current minimums are approximately:

```text
Family rail       118
Control rail      260
Inspector         300
```

Top-bar zones also received lower minimum widths.

This is an effective fix for the previous hard-width conflict.

Status: **basically usable**, but not yet a fully adaptive layout because side areas are still persistent rather than collapsible drawers.

---

# 3. P1-1 — Replay uses frame index as event index

## 3.1 Current problem

`ReducedEventTimeline` correctly maintains two concepts:

```java
private final List<EventEnvelope> events;
private final List<Integer> frameEventIndexes;
```

It also already exposes the correct mapping API:

```java
public EventEnvelope event(int frameIndex)
public int eventIndex(int frameIndex)
```

However, the presentation layer currently obtains the replay cursor from `PlaybackController.currentIndex()` and then indexes directly into the authoritative event list.

Conceptually the current logic is equivalent to:

```java
int index = active.currentIndex(); // this is a visible frame index
presentationEvent.set(events.get(index)); // this expects an event index
```

That is incorrect.

Example:

```text
event 0  RunStarted      no visual frame
event 1  Log             no visual frame
event 2  Compared        frame 0
event 3  Log             no visual frame
event 4  Swapped         frame 1
```

When the replay cursor is at frame 1, the visible state is based on event 4, but `events.get(1)` is event 1.

This can make the Workbench show:

```text
Canvas       = Swapped
Current Step = Log
```

That breaks the core invariant that the event inspector and the rendered state describe the same execution point.

## 3.2 Required fix

Use `ReducedEventTimeline` as the mapping authority.

Recommended direction:

```java
int frameIndex = active.currentIndex();
if (lastTimeline != null
        && frameIndex >= 0
        && frameIndex < lastTimeline.size()) {
    presentationEvent.set(lastTimeline.event(frameIndex));
}
```

Do not duplicate `frameEventIndexes` outside `ReducedEventTimeline`.

## 3.3 Acceptance criteria

For an event stream containing lifecycle/log/nonvisual events between visual events:

```text
Replay frame N
-> rendered ViewState is derived from event E
-> Current Step shows event E
-> Inspector shows event E
-> Timeline current marker corresponds to event E
```

This must remain true for forward replay, backward step, direct seek and replay restart.

---

# 4. P1-2 — Timeline event click uses percentage instead of exact mapping

## 4.1 Current problem

The current Timeline marker click flow effectively does:

```text
eventIndex
  -> eventIndex / eventCount
  -> progress
  -> progress * frameCount
  -> frameIndex
```

This is not an event-to-frame mapping. It is only a proportional approximation.

It becomes increasingly wrong when the authoritative stream contains:

- `RunStartedEvent`
- `RunPausedEvent`
- `RunResumedEvent`
- `LogEvent`
- lifecycle terminal events
- other reducer events that intentionally do not produce visual frames

## 4.2 Correct semantic model

A Timeline marker represents an **authoritative event position**.

Replay operates on a **visible frame position**.

Therefore the mapping must come from the Timeline's actual `frameEventIndexes`.

Recommended API additions on `ReducedEventTimeline`:

```java
public int frameIndexAtOrBeforeEvent(int eventIndex)
```

Optionally also:

```java
public int frameIndexAtOrAfterEvent(int eventIndex)
```

or:

```java
public OptionalInt exactFrameIndexForEvent(int eventIndex)
```

The implementation should use binary search over `frameEventIndexes`.

Recommended behavior for clicking a nonvisual event:

```text
click event E
-> Inspector selects exact event E
-> Canvas seeks to nearest visual frame at-or-before E
```

This preserves both facts:

1. user selected exact event E;
2. canvas shows the latest visual state valid at event E.

## 4.3 Do not use progress as the semantic API

`seekTimeline(double progress)` is acceptable for a Slider.

It should **not** be the API used for exact Timeline event navigation.

Add an explicit method such as:

```java
public final boolean seekTimelineEvent(int eventIndex)
```

or:

```java
public final boolean seekTimelineFrame(int frameIndex)
```

with the mapping kept in the runtime/timeline layer.

## 4.4 Acceptance criteria

Given 100 authoritative events but only 23 visual frames:

```text
click event #87
```

must never be translated using `87 / 99 * 22`.

The target frame must be obtained from `frameEventIndexes`.

---

# 5. P1-3 — Live Current Step is updated before the visual state

## 5.1 Current ordering

The local execution observer currently follows this general order:

```java
liveEventConsumer.accept(event);
Reduction<S> reduction = reductionCursor.accept(event);
if (reduction.visualFrame()) {
    viewStateConsumer.accept(reduction.state());
}
statisticsConsumer.accept(reductionCursor.statistics());
```

`liveEventConsumer` updates `presentationEvent`.

Therefore the UI can briefly enter this state:

```text
Current Step = event #42
Canvas       = state after event #41
```

Even if the delay is short, it is semantically backwards.

## 5.2 Required ordering

For a single dispatched event, update the reducer and visual state first, then publish the presentation cursor:

```java
Reduction<S> reduction = reductionCursor.accept(event);

if (reduction.visualFrame()) {
    viewStateConsumer.accept(reduction.state());
}

statisticsConsumer.accept(reductionCursor.statistics());
liveEventConsumer.accept(event);
```

The exact ordering of statistics versus event inspector may remain a presentation decision, but the canvas must not lag behind the event label.

Recommended invariant:

```text
Event E accepted
    -> Reducer applies E
    -> visual frame for E rendered if needed
    -> statistics for E visible
    -> Current Step points to E
```

## 5.3 Acceptance criteria

During slow playback / single step:

```text
Current Step
Inspector
Canvas highlight/state
Statistics
```

must all correspond to the same accepted event boundary.

---

# 6. P1-4 — Restore Snapshot confirmation was removed

## 6.1 Current regression

A previous commit introduced confirmation before replacing the live structure with a saved Snapshot.

The latest UI refactor removed:

```text
Alert
ButtonType
confirmSnapshotRestore(...)
```

and the restore path now directly restores the selected snapshot.

## 6.2 Why confirmation belongs here

Snapshot restore is not a harmless navigation action.

Its semantic effect is:

```text
saved snapshot
      -> replace current live editable Structure
```

This can discard the user's current unsaved Structure state.

A confirmation is therefore normal destructive-action UX, not defensive-programming noise.

## 6.3 Required fix

Restore a small confirmation boundary before mutation.

Recommended message semantics:

```text
Restore snapshot <short-id>?

This replaces the current live structure.
The saved snapshot itself remains unchanged.
```

The action must remain disabled while an algorithm execution is running.

## 6.4 Acceptance criteria

```text
Cancel -> live Structure unchanged
OK     -> live Structure replaced exactly once
```

No Snapshot object itself is mutated.

---

# 7. P1-5 — Maze live final state and replay final state are still different

The Maze work moved in the correct direction: generation now emits factual `ObservationEvent`s.

Examples include:

```text
Visited(cell)
Examined(from, to)
Backtracked(cell)
```

That gives Maze a real execution-observation stream instead of checkpoint-only animation.

However the final-state closure is incomplete.

---

## 7.1 Graph Maze generation

The generator builds real graph edges in the algorithm result:

```java
List<GraphSnapshot.Edge> edges
```

But the reducer currently treats `Examined(from, to)` primarily as an active-cell observation and does not reconstruct the accepted maze graph edge set.

At run completion, `MazeController` obtains the `GraphSnapshot` result and directly renders a completed ViewState.

Therefore:

```text
Live final screen
= complete graph result

Replay final frame
= only what reducer reconstructed from events
```

These may differ.

### Required closure

The event stream must contain enough factual information to reconstruct accepted graph edges.

Do not infer every `Examined` edge as accepted unless that is guaranteed by the algorithm.

Prefer one of these two approaches:

### Option A — explicit factual observation

Add a small neutral event such as:

```text
Connected(fromRef, toRef)
```

or another appropriately named domain fact emitted only when an edge is actually accepted into the maze.

Reducer:

```text
Connected -> append graph edge to MazeViewState.graphEdges
```

### Option B — final factual snapshot boundary

If generation algorithms cannot expose accepted edges cleanly through process observations, emit one explicit final result/snapshot fact before `RunCompletedEvent`.

Avoid UI-specific events.

Option A is preferred when it remains simple.

---

## 7.2 Maze pathfinding

Current pathfinding returns:

```java
List<GridPoint>
```

and the controller directly places that list into the final ViewState.

Replay only knows what process observations told the reducer.

Thus the final solved path can exist on the live result screen but not in the replayed terminal frame.

### Required closure

The event stream needs one factual source for final path membership.

Possible small event:

```text
PathCell(CoordinateRef)
```

or a final factual path result event.

Do not make the visualizer read the algorithm return value during replay.

## 7.3 Final invariant

After completion:

```text
live final ViewState
== replay final ViewState
```

for all Maze generation/pathfinding modes.

The comparison should include:

```text
openCells
graphEdges
visited
path
entrance
exit
completed
```

Transient `active` / `backtracked` state may be cleared at terminal completion if that is the intended visual rule.

---

# 8. P2 — Structure selection coverage is incomplete

The Workbench currently wires detailed selection mainly for Tree.

Desired eventual coverage:

```text
Tree Node       DONE
Array Cell      TODO
Linked Node     TODO if useful
Graph Vertex    TODO
Graph Edge      TODO
Hash Slot       TODO when Hash implementation exists
String Cell     TODO if inspector requires it
```

This is not a P1 architecture issue.

Use family-specific selection payloads. Do not introduce a universal `SelectedThing` model solely for UI convenience.

Selection remains presentation/controller state, not canonical Structure state.

---

# 9. P2 — Timeline markers still have presentation limitations

Current marker construction limits visible markers to approximately 15 sampled authoritative events.

That is acceptable as a temporary density cap, but the layout is still a sequential `HBox`, so visual spacing does not represent actual event position.

Eventually prefer a bounded Timeline track:

```text
|------------------------------------------------|
0                                              N-1
   marker x-position = eventIndex / (N - 1)
```

Important distinction:

- **marker geometry** may use proportional event position;
- **event -> replay frame navigation** must use exact Timeline mapping.

Do not confuse the two.

---

# 10. P2 — Bottom docks are still fixed-height

Current Structure History and Algorithm Timeline docks keep fixed/minimum heights around the bottom of the Workbench.

Recommended later UX:

```text
expanded
collapsed header only
```

The canvas should gain the freed vertical space when collapsed.

This is UI polish and should happen only after P1 Timeline semantics are correct.

---

# 11. P2 — Operation button sizing still risks text clipping

The current screenshot-convergence CSS constrains operation buttons roughly to:

```css
-fx-pref-width: 86px;
-fx-min-width: 72px;
-fx-max-width: 120px;
```

This is too aggressive for labels such as:

```text
ADD ROOT
ADD CHILD
REMOVE NODE
APPLY RESULT
```

Prefer content-driven sizing within the rail:

```text
min width = useful floor
pref width = computed/content-driven
max width = available rail width
```

Do not solve this by shrinking font size per button.

---

# 12. P2 — Workbench shell I18N remains incomplete

A number of shell labels are still direct English strings such as:

```text
STRUCTURE
ALGORITHM
SNAPSHOTS
STRUCTURE HISTORY
RUN SUMMARY
EXECUTION TIMELINE
RUN ALGORITHM
LIVE
```

They should eventually use the existing `I18N` binding path.

This is not a blocker for Runtime / Timeline correctness.

---

# 13. Recommended implementation order

## Phase W1 — Exact Timeline semantics

Change only the Runtime presentation / replay mapping surface.

1. Fix replay `frameIndex -> EventEnvelope` mapping.
2. Add exact `eventIndex -> frameIndex` lookup to `ReducedEventTimeline`.
3. Add explicit event/frame seek API to `BaseController`.
4. Remove percentage conversion from marker click navigation.
5. Add regression probes/tests.

Do not touch Structure contracts.

---

## Phase W2 — Live presentation synchronization

1. Reorder LocalAlgorithmExecution observer application.
2. Ensure reducer/render happens before Current Step publication.
3. Verify pause/step mode visually and with deterministic probes.

---

## Phase W3 — Snapshot restore UX

1. Restore confirmation.
2. Keep restore disabled while running.
3. Verify cancel/no mutation and confirm/one mutation.

---

## Phase W4 — Maze replay closure

1. Define the minimal missing factual Maze observations.
2. Make reducers reconstruct accepted graph edges.
3. Make reducers reconstruct final path.
4. Remove direct-result-only final-state dependency from replay semantics.
5. Assert live final state equals replay final state.

The controller may still keep the algorithm result for `Apply Result`; it simply must not be required to reconstruct replay.

---

## Phase W5 — UI closure

After W1-W4:

1. Array / Graph selection.
2. Timeline proportional marker geometry.
3. Collapsible bottom docks.
4. Content-driven operation button sizing.
5. Safe-area checks for overlays.
6. I18N cleanup.

---

# 14. Regression tests / smoke probes required

No large new test framework is required, but these cases must be mechanically checked.

## Timeline mapping probe

Construct an event stream such as:

```text
0 RunStarted      nonvisual
1 Log             nonvisual
2 Compared        frame 0
3 Log             nonvisual
4 Swapped         frame 1
5 RunPaused       nonvisual
6 RunResumed      nonvisual
7 Compared        frame 2
8 RunCompleted    terminal frame depending reducer
```

Assert:

```text
frame 0 -> event 2
frame 1 -> event 4
frame 2 -> event 7
```

and exact event seek returns the correct nearest visual state.

## Live synchronization probe

At one-step speed:

```text
presentationEvent == event used to produce current rendered ViewState
```

for each visual event.

## Snapshot restore probe

```text
restore cancelled -> structure snapshot before == after
restore confirmed -> structure snapshot after == selected saved snapshot
```

## Maze replay probe

For both Array and Graph generation:

```text
live terminal state == replay terminal state
```

For pathfinding:

```text
live terminal path == replay terminal path
```

---

# 15. Architecture constraints to keep frozen

Do not solve these issues by introducing another architecture layer.

Keep:

```text
Canonical Structure
    -> real mutation
    -> StructureEvent

Algorithm execution
    -> ObservationEvent for factual non-mutation execution facts
    -> Runtime lifecycle events

Event stream
    -> Reducer
    -> family-specific ViewState
    -> JavaFX visual elements
    -> ELK layout where applicable
    -> GestureFX viewport
```

Do not introduce:

- universal VisualModel;
- universal Graph abstraction for all structures;
- UI-specific algorithm events;
- fake StructureEvents for read-only algorithm observations;
- controller-side manual statistics as a replacement for factual events;
- percentage-based Event/Frame mapping;
- another V3 rewrite.

---

# 16. Final status matrix

```text
Structure / Contract                 PASS
Registry / module boundaries         PASS
JavaFX / ELK / GestureFX separation  PASS
Tree selection cleanup               PASS
Narrow-screen hard constraints       MOSTLY PASS

Replay frame -> event mapping        FAIL
Timeline event -> frame mapping      FAIL
Live event / canvas synchronization  FAIL
Snapshot restore confirmation        REGRESSED
Maze final replay closure            FAIL

Array / Graph selection              TODO
Timeline geometry                    TODO
Bottom dock collapse                 TODO
Button clipping cleanup              TODO
Shell I18N                           TODO
```

The next code change should focus on **W1 exact Timeline semantics** before additional visual redesign.