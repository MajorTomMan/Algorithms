package com.majortom.algorithms.library.basic.tree;

import java.util.concurrent.atomic.AtomicLong;

public class AVLTreeNode<T> extends BinaryTreeNode<T> {
    private static final AtomicLong FALLBACK_IDS = new AtomicLong(1L);

    private final long id;
    private int height = 1;
    private int subTreeCount = 1;

    public AVLTreeNode(T value) {
        this(FALLBACK_IDS.getAndIncrement(), value);
    }

    public AVLTreeNode(long id, T value) {
        super(value);
        if (id <= 0) {
            throw new IllegalArgumentException("node id must be positive");
        }
        this.id = id;
        FALLBACK_IDS.accumulateAndGet(id + 1L, Math::max);
    }

    public AVLTreeNode(long id, T value, int height, AVLTreeNode<T> left, AVLTreeNode<T> right) {
        this(id, value);
        initializeChildren(left, right);
        updateMetrics(height, 1 + count(left) + count(right));
    }

    @Override
    public long getId() {
        return id;
    }

    public int getHeight() {
        return height;
    }

    public int getSubTreeCount() {
        return subTreeCount;
    }

    void updateMetrics(int height, int subTreeCount) {
        if (height < 1 || subTreeCount < 1) {
            throw new IllegalArgumentException("AVL metrics must be positive");
        }
        this.height = height;
        this.subTreeCount = subTreeCount;
    }

    private int count(AVLTreeNode<T> node) {
        return node == null ? 0 : node.getSubTreeCount();
    }
}
