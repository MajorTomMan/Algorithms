# Algorithm-only search/cache presentation (increment on 2026-09-19)

## Application order / scope

This patch is an **increment after** both `Algorithms-dev-9e15f18-inputtype-fieldpresentation-combined.patch` and `Algorithms-dev-9e15f18-generic-search-cache-observation.patch`. It changes neither `StructureEvent` nor any structure Reducer, visualizer, layout, camera, or RenderFrame. The delivery overlay contains only changed files, not a complete dev checkout. It has not been pushed to GitHub.

## Data path

`EventEnvelope` → `AlgorithmObservationTimeline.at(events, exactEventIndex)` → immutable `AlgorithmObservationModel` → existing `MutablePresentationModelSource` → `PresentationSurfacePort` → `FxAlgorithmObservationRenderer.commit(...)` → optional card inside existing event inspector. The card uses the inspector's existing ScrollPane; no FXML/CSS changes. Only algorithms that publish the prior patch's search/cache events get a card; existing algorithms do not gain new events or new playback steps.

The reducer accepts search and cache facts only. Events from other runs reset the model; structure events and generic observation events do not change search/cache facts. It stores bounded **recent cache operations** (up to eight); these must not be misrepresented as the algorithm's complete memo table. The current search tracks target, candidate, found count, probe count and completion. `ValueRef` display text is copied at projection time; the authoritative original event still obeys the project's snapshot/freeze policy.

On forward adjacent event movement, the active event caption gets a small fade pulse, committed only inside the registered RenderFramework presentation renderer. Backward seek, replay jumps, new runs and fresh module sessions reconstruct the appropriate event prefix without launching historical transitions. Structure nodes are deliberately not highlighted by this patch; there is no change to existing structural animation sequencing.

## Tests and limits

- Java 21 standalone compilation of `AlgorithmObservationModel` and `AlgorithmObservationTimeline` against the previous core observation patch; standalone smoke checks deterministic backward/forward seek, distinct run isolation, cache preview bound and no effects from a generic observation: PASS.
- JUnit 5 tests were added but **not executed** (Maven dependencies and JavaFX are absent from this container).
- Full project Maven build, JavaFX launch and visual animation QA: **not verified**. The renderer/MainController integration must be compiled and visually inspected against your complete local `dev` + previous two patches.
- This is the algorithm inspector's search/cache animation, not yet a node/edge-specific visualization of these events on every structure canvas. Those can be built as separate opt-in overlays when desired.
