# Algorithm observation callout (2026-09-19)

## Scope

- Read the existing `EventEnvelope` at the active playback cursor. Do **not** emit a second observation event to show a callout, and do not modify the structural event reducer.
- Derive a JavaFX-free, immutable `AlgorithmObservationCallout` from the *current* event plus the already reconstructed `AlgorithmObservationModel`. This gives `CandidatePruned` its recorded candidate ID and the reference from the preceding `CandidateAdded` event, without inventing a pruning reason.
- Publish to an independent `PresentationSurface` owned by the shell. Only its FX renderer changes the overlay card; it never reaches into a structure visualizer, scene nodes, snapshot store or camera/fit implementation.
- Standard observation events and all built-in algorithm observations have localized event titles. Custom `AlgorithmObservationEvent` implementations get a readable class-name fallback until a dedicated presenter is registered in a later extension.
- The card is drawn inside the algorithm viewport, upper-right, and moves below the selected-item card when that card is visible. Its overlay host reports no preferred size to the viewport; it does not affect structural layout or camera/fit.

## Timeline behavior

- Cursor on an observation event: display only that event's cue. Cursor on a structure/runtime event, no run, or module detach: hide the cue. The inspector's cumulative algorithm observation projection remains independently seekable.
- On one adjacent forward step within a run, fade the overlay from 50% to 100% opacity over 230 ms. Seeking, rewinding, switching runs or refreshing locale reconstructs the target cue without replaying skipped transitions. The overlay does not auto-dismiss on a timer while playback is paused.
- This is an observation *prompt* animation, **not** the graph-node/edge pruning animation. A BFS run currently publishes CandidateAdded/Selected but no CandidatePruned; it must not fake pruning. The optional canvas-level highlighting must be implemented separately through a stable reference-to-render-element mapping, without updating graph structure or redrawing its topology.

## Validation limitations

- `AlgorithmObservationCalloutTest` covers pruning references, real structure-event isolation, cursor index, generic and custom observations, run reset, and detach. A separate pure Java smoke test can run using the previously compiled core event classes.
- This environment lacks the offline Maven resources plugin and JavaFX runtime; successful FXML XML parsing and Java model compilation are **not** a full client build or actual UI/animation acceptance.
