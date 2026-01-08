package com.majortom.algorithms.core.tree;

import com.majortom.algorithms.core.maze.BaseMaze;
import com.majortom.algorithms.core.tree.listeners.TreeStepListener;
import com.majortom.algorithms.core.tree.node.TreeNode;

/**
 * 树算法的抽象基类，用于管理可视化统计数据和步进回调
 * 泛型 T 代表树的根节点类型 (例如 TreeNode<Key, Value>)
 */
public abstract class BaseTree<T> {
    protected TreeNode<T> root; // 树的根节点
    protected int compareCount = 0; // 比较次数
    protected int actionCount = 0; // 操作次数 (例如 AVL 树的旋转次数，BST 的插入次数)

    protected TreeStepListener stepListener;

    public void setStepListener(TreeStepListener listener) {
        this.stepListener = listener;
    }

    protected void onStep() {
        if (stepListener != null) {
            stepListener.onStep();
        }
    }

    public TreeNode<T> getRoot() {
        return root;
    }

    public int getCompareCount() {
        return compareCount;
    }

    public int getActionCount() {
        return actionCount;
    }

    public void resetStatistics() {
        this.compareCount = 0;
        this.actionCount = 0;
    }

    // 抽象方法，子类实现具体算法逻辑
    public abstract void put(T val);

    public abstract void remove(T val);

    public abstract TreeNode<T> search(T val);

    public abstract void traverse();

    public abstract int size();

    public abstract int height();

    public abstract boolean isEmpty();

    public abstract void clear();
}