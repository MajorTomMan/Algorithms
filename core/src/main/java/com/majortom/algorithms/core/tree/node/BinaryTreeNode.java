package com.majortom.algorithms.core.tree.node;

import java.util.ArrayList;
import java.util.List;

public abstract class BinaryTreeNode<T> extends TreeNode<T> {
    public BinaryTreeNode<T> left;
    public BinaryTreeNode<T> right;

    public BinaryTreeNode(T data) {
        super(data);
    }

    @Override
    public List<BinaryTreeNode<T>> getChildren() {
        List<BinaryTreeNode<T>> list = new ArrayList<>(2);
        list.add(left);
        list.add(right);
        return list;
    }
}
