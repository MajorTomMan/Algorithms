package com.majortom.algorithms.library.basic.tree;

import com.majortom.algorithms.core.event.structure.TreeStructureEvent;
import com.majortom.algorithms.core.runtime.ExecutionEvents;

import java.util.List;
import java.util.Objects;

public abstract class TreeNode<T> {
    private T value;
    private int height = 1;
    private int subTreeCount = 1;
    private int status;

    protected TreeNode(T value) {
        this.value = value;
    }

    public abstract long getId();

    public T getValue() {
        return value;
    }

    public void setValue(T value) {
        if (Objects.equals(this.value, value)) {
            return;
        }
        T previous = this.value;
        this.value = value;
        ExecutionEvents.emit(new TreeStructureEvent.ValueChanged(getId(), previous, value));
    }

    public int getHeight() {
        return height;
    }

    public int getSubTreeCount() {
        return subTreeCount;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public void updateMetrics(int height, int subTreeCount) {
        if (height < 1 || subTreeCount < 1) {
            throw new IllegalArgumentException("tree metrics must be positive");
        }
        this.height = height;
        this.subTreeCount = subTreeCount;
    }

    public abstract List<? extends TreeNode<T>> getChildren();

    public boolean isLeaf() {
        List<? extends TreeNode<T>> children = getChildren();
        if (children == null || children.isEmpty()) {
            return true;
        }
        for (TreeNode<T> child : children) {
            if (child != null) {
                return false;
            }
        }
        return true;
    }
}
