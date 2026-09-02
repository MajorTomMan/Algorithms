package com.majortom.algorithms.library.structure;

import com.majortom.algorithms.library.basic.tree.TreeNode;

public interface RotatableTree<T> extends TreeStructure<T> {
    void rotateLeft(TreeNode<T> node);
    void rotateRight(TreeNode<T> node);
}
