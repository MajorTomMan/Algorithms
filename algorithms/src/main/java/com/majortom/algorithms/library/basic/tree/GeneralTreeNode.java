package com.majortom.algorithms.library.basic.tree;

import com.majortom.algorithms.core.event.structure.TreeStructureEvent;
import com.majortom.algorithms.core.runtime.ExecutionEvents;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

public final class GeneralTreeNode<T> extends TreeNode<T> {
    private static final AtomicLong IDS = new AtomicLong(1L);

    private final long id;
    private final List<GeneralTreeNode<T>> children = new ArrayList<>();

    public GeneralTreeNode(T value) {
        this(IDS.getAndIncrement(), value);
    }

    public GeneralTreeNode(long id, T value) {
        this(id, value, List.of());
    }

    public GeneralTreeNode(long id, T value, List<GeneralTreeNode<T>> children) {
        super(Objects.requireNonNull(value, "value"));
        if (id <= 0) {
            throw new IllegalArgumentException("node id must be positive");
        }
        this.id = id;
        this.children.addAll(Objects.requireNonNull(children, "children"));
        IDS.accumulateAndGet(id + 1L, Math::max);
    }

    @Override
    public long getId() {
        return id;
    }

    @Override
    public List<GeneralTreeNode<T>> getChildren() {
        return Collections.unmodifiableList(children);
    }

    void addChild(int index, GeneralTreeNode<T> child) {
        children.add(index, child);
        ExecutionEvents.emit(new TreeStructureEvent.ChildInserted(id, index, child.getId(), child.getValue()));
    }

    boolean removeChild(GeneralTreeNode<T> child) {
        int index = children.indexOf(child);
        if (index < 0) {
            return false;
        }
        children.remove(index);
        ExecutionEvents.emit(new TreeStructureEvent.ChildRemoved(id, index, child.getId(), child.getValue()));
        return true;
    }
}
