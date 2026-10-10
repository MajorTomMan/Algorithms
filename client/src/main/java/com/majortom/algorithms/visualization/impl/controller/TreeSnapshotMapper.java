package com.majortom.algorithms.visualization.impl.controller;

import com.majortom.algorithms.core.snapshot.BinaryTreeSnapshot;
import com.majortom.algorithms.core.snapshot.GeneralTreeSnapshot;
import com.majortom.algorithms.structure.tree.AVLTree;

/** Stateless conversions between tree nodes and immutable snapshot data. */
final class TreeSnapshotMapper {
    private TreeSnapshotMapper() {}

    @SuppressWarnings({"rawtypes", "unchecked"})
    static AVLTree avlFromSnapshot(BinaryTreeSnapshot<Object> snapshot) {
        return AVLTree.fromSnapshot((BinaryTreeSnapshot) snapshot);
    }

    static int generalHeight(GeneralTreeSnapshot.Node<Object> node) {
        if (node == null) {
            return 0;
        }
        int maxChildHeight = 0;
        for (GeneralTreeSnapshot.Node<Object> child : node.children()) {
            maxChildHeight = Math.max(maxChildHeight, generalHeight(child));
        }
        return maxChildHeight + 1;
    }

    static int binaryHeight(BinaryTreeSnapshot.Node<Object> node) {
        if (node == null) {
            return 0;
        }
        return Math.max(binaryHeight(node.left()), binaryHeight(node.right())) + 1;
    }
}
