package com.majortom.algorithms.library.basic.tree;

import com.majortom.algorithms.core.runtime.StructureEvents;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

public abstract class TreeNode<T> {
    private static final AtomicLong IDS = new AtomicLong(1L);

    private final long id;
    private T value;

    protected TreeNode(T value) {
        this(IDS.getAndIncrement(), value);
    }

    protected TreeNode(long id, T value) {
        if (id <= 0) {
            throw new IllegalArgumentException("node id must be positive");
        }
        this.id = id;
        this.value = value;
        IDS.accumulateAndGet(id + 1L, Math::max);
    }

    public final long getId() {
        return id;
    }

    public T getValue() {
        return value;
    }

    public void setValue(T value) {
        if (Objects.equals(this.value, value)) {
            return;
        }
        T previous = this.value;
        this.value = value;
        StructureEvents.treeValueChanged(id, previous, value);
    }
}
