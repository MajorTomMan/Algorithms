package com.majortom.algorithms.visualization.structure;

import com.majortom.algorithms.core.snapshot.StructureSnapshot;

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

    /** Renders a saved snapshot as a read-only preview without mutating the live structure. */
    void previewStructureSnapshot(StructureSnapshot<S> snapshot);

    /** Returns a short, localized description for a snapshot card. */
    String describeStructureSnapshot(S state);

    /** Primary overview value used while previewing a saved snapshot. */
    default String snapshotPrimaryCount(S state) {
        return "—";
    }

    /** Secondary overview value used while previewing a saved snapshot. */
    default String snapshotSecondaryCount(S state) {
        return "—";
    }
}
