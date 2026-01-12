package com.majortom.algorithms.core.tree;

import com.majortom.algorithms.core.tree.node.TreeNode;

/**
 * 树的数据实体类
 * 职责：维护根节点、高亮状态以及基础的结构属性统计
 */
public class BaseTree<T> {
    protected TreeNode<T> root;

    // 当前操作焦点（属于数据状态，放在这里方便 Visualizer 读取）
    protected TreeNode<T> currentHighlight;

    public TreeNode<T> getRoot() {
        return root;
    }

    public void setRoot(TreeNode<T> root) {
        this.root = root;
    }

    public TreeNode<T> getCurrentHighlight() {
        return currentHighlight;
    }

    public void setCurrentHighlight(TreeNode<T> node) {
        this.currentHighlight = node;
    }

    public int size() {
        return root == null ? 0 : root.subTreeCount;
    }

    public int height() {
        return root == null ? 0 : root.height;
    }

    public boolean isEmpty() {
        return root == null;
    }

    public void clear() {
        root = null;
        currentHighlight = null;
    }
}