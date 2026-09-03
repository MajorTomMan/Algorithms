package com.majortom.algorithms.library.basic.tree;

import com.majortom.algorithms.core.event.structure.TreeStructureEvent;
import com.majortom.algorithms.core.runtime.ExecutionEvents;

import java.util.Arrays;
import java.util.List;

public abstract class BinaryTreeNode<T> extends TreeNode<T> {
    private BinaryTreeNode<T> left;
    private BinaryTreeNode<T> right;

    protected BinaryTreeNode(T value) {
        super(value);
    }

    public BinaryTreeNode<T> getLeft() {
        return left;
    }

    public BinaryTreeNode<T> getRight() {
        return right;
    }

    public void setLeft(BinaryTreeNode<T> left) {
        if (this.left == left) {
            return;
        }
        Long previousId = id(this.left);
        this.left = left;
        ExecutionEvents.emit(new TreeStructureEvent.LeftChanged(getId(), previousId, id(left)));
    }

    public void setRight(BinaryTreeNode<T> right) {
        if (this.right == right) {
            return;
        }
        Long previousId = id(this.right);
        this.right = right;
        ExecutionEvents.emit(new TreeStructureEvent.RightChanged(getId(), previousId, id(right)));
    }


    protected void initializeChildren(BinaryTreeNode<T> left, BinaryTreeNode<T> right) {
        this.left = left;
        this.right = right;
    }

    @Override
    public List<BinaryTreeNode<T>> getChildren() {
        return Arrays.asList(left, right);
    }

    private static Long id(BinaryTreeNode<?> node) {
        return node == null ? null : node.getId();
    }
}
