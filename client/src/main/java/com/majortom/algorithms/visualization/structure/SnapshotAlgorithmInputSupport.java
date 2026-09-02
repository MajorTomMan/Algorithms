package com.majortom.algorithms.visualization.structure;

import com.majortom.algorithms.core.snapshot.StructureSnapshot;

/** Allows an algorithm-capable workbench to use a saved structure snapshot without restoring it. */
public interface SnapshotAlgorithmInputSupport<S> {

    void useSnapshotAsAlgorithmInput(StructureSnapshot<S> snapshot);

    void useCurrentStructureAsAlgorithmInput();

    String algorithmInputSnapshotId();
}
