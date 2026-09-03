package com.majortom.algorithms.library.basic.tree;

import com.majortom.algorithms.core.event.structure.TreeStructureEvent;
import com.majortom.algorithms.core.runtime.ExecutionEvents;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public final class GeneralTreeNode<T> extends TreeNode<T> {
    private final List<GeneralTreeNode<T>> children = new ArrayList<>();

    public GeneralTreeNode(T value) {
        super(Objects.requireNonNull(value, "value"));
    }

    public GeneralTreeNode(long id, T value) {
        this(id, value, List.of());
    }

    public GeneralTreeNode(long id, T value, List<GeneralTreeNode<T>> children) {
        super(id, Objects.requireNonNull(value, "value"));
        this.children.addAll(Objects.requireNonNull(children, "children"));
    }

    public List<GeneralTreeNode<T>> getChildren() {
        return Collections.unmodifiableList(children);
    }

    void addChild(int index, GeneralTreeNode<T> child) {
        children.add(index, child);
        ExecutionEvents.emit(new TreeStructureEvent.ChildInserted(getId(), index, child.getId(), child.getValue()));
    }

    boolean removeChild(GeneralTreeNode<T> child) {
        int index = children.indexOf(child);
        if (index < 0) {
            return false;
        }
        children.remove(index);
        ExecutionEvents.emit(new TreeStructureEvent.ChildRemoved(getId(), index, child.getId(), child.getValue()));
        return true;
    }
}
