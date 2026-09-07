package com.majortom.algorithms.core.snapshot;

/** Marker for immutable snapshots of concrete graph structure variants. */
public sealed interface GraphSnapshotState<T>
        permits GraphSnapshot, WeightedGraphSnapshot {
    boolean directed();
}
