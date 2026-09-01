package com.majortom.algorithms.visualization.structure;

/**
 * Capability exposed by a module that can save and restore its editable
 * structure state.
 *
 * @param <S> module view-state type
 */
public interface StructureSnapshotSupport<S> {

    /** Captures the current editable structure, excluding transient algorithm state. */
    StructureSnapshot<S> captureStructureSnapshot();

    /** Replaces the module's editable input with a previously captured state. */
    void restoreStructureSnapshot(StructureSnapshot<S> snapshot);

    /** Returns a short, localized description for a snapshot card. */
    String describeStructureSnapshot(S state);
}
