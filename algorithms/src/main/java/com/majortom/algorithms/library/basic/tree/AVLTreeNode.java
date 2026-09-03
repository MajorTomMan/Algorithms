package com.majortom.algorithms.library.basic.tree;

public class AVLTreeNode<T> extends BinaryTreeNode<T> {
    private int height = 1;
    private int subTreeCount = 1;

    public AVLTreeNode(T value) {
        super(value);
    }

    public AVLTreeNode(long id, T value) {
        super(id, value);
    }

    public AVLTreeNode(long id, T value, int height, AVLTreeNode<T> left, AVLTreeNode<T> right) {
        this(id, value);
        initializeChildren(left, right);
        updateMetrics(height, 1 + count(left) + count(right));
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
