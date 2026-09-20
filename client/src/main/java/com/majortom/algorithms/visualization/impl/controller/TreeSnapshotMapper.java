package com.majortom.algorithms.visualization.impl.controller;

import com.majortom.algorithms.core.snapshot.BinaryTreeSnapshot;
import com.majortom.algorithms.core.snapshot.GeneralTreeSnapshot;
import com.majortom.algorithms.structure.tree.AVLTree;
import com.majortom.algorithms.structure.tree.AVLTreeNode;
import com.majortom.algorithms.structure.tree.GeneralTreeNode;
import java.util.List;
import static com.majortom.algorithms.visualization.impl.controller.TreeNodeQueries.left;
import static com.majortom.algorithms.visualization.impl.controller.TreeNodeQueries.right;
import static com.majortom.algorithms.visualization.impl.controller.TreeNodeQueries.avlHeight;

/** Stateless conversions between tree nodes and immutable snapshot data. */
final class TreeSnapshotMapper {
    private TreeSnapshotMapper() {}

    static GeneralTreeSnapshot.Node<Object> snapshotGeneralNode(GeneralTreeNode<Object> node) {
        if (node == null) {
            return null;
        }
        List<GeneralTreeSnapshot.Node<Object>> children = node.getChildren().stream()
                .map(TreeSnapshotMapper::snapshotGeneralNode)
                .toList();
        return new GeneralTreeSnapshot.Node<>(node.getId(), node.getValue(), children);
    }

    static BinaryTreeSnapshot.Node<Object> snapshotBinaryNode(AVLTreeNode<Object> node) {
        if (node == null) {
            return null;
        }
        return new BinaryTreeSnapshot.Node<>(
                node.getId(),
                node.getValue(),
                snapshotBinaryNode(left(node)),
                snapshotBinaryNode(right(node)));
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    static AVLTree avlFromSnapshot(BinaryTreeSnapshot<Object> snapshot) {
        return AVLTree.fromRestoredRoot((AVLTreeNode) restoreAvlNode(snapshot.root()));
    }

    static AVLTreeNode<Object> restoreAvlNode(BinaryTreeSnapshot.Node<Object> node) {
        if (node == null) {
            return null;
        }
        AVLTreeNode<Object> left = restoreAvlNode(node.left());
        AVLTreeNode<Object> right = restoreAvlNode(node.right());
        int height = Math.max(avlHeight(left), avlHeight(right)) + 1;
        return new AVLTreeNode<>(node.id(), node.value(), height, left, right);
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
