package com.majortom.algorithms.core.tree.impl;

import java.util.List;

import com.majortom.algorithms.core.tree.BaseTree;
import com.majortom.algorithms.core.tree.node.AVLTreeNode;
import com.majortom.algorithms.core.tree.node.BinaryTreeNode;
import com.majortom.algorithms.core.tree.node.TreeNode;

public class AVLTreeEntity<T extends Comparable<T>> extends BaseTree<T> {
    @Override
    protected BaseTree<T> createEmptyTree() {
        return new AVLTreeEntity<>();
    }

    @Override
    protected TreeNode<T> createNodeInstance(T data) {
        return new AVLTreeNode<>(data);
    }

    @Override
    protected void linkChildren(TreeNode<T> parent, List<TreeNode<T>> children) {
        if (parent instanceof BinaryTreeNode) {
            BinaryTreeNode<T> node = (BinaryTreeNode<T>) parent;
            if (children.size() > 0)
                node.left = (BinaryTreeNode<T>) children.get(0);
            if (children.size() > 1)
                node.right = (BinaryTreeNode<T>) children.get(1);
        }
    }
}