package com.majortom.algorithms.core.snapshot;

/** Marker for immutable snapshots of concrete tree structure variants. */
public sealed interface TreeSnapshotState<T>
        permits GeneralTreeSnapshot, BinaryTreeSnapshot {
}
