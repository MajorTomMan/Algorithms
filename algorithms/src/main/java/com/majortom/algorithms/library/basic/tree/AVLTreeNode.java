package com.majortom.algorithms.library.basic.tree;

import java.util.concurrent.atomic.AtomicLong;

/** AVL node with stable structural identity. */
public class AVLTreeNode<T> extends BinaryTreeNode<T> {
    private static final AtomicLong FALLBACK_IDS = new AtomicLong(1L);

    public final long id;

    public AVLTreeNode(T data) {
        this(FALLBACK_IDS.getAndIncrement(), data);
    }

    public AVLTreeNode(long id, T data) {
        super(data);
        if (id <= 0) {
            throw new IllegalArgumentException("node id must be positive");
        }
        this.id = id;
    }
}
