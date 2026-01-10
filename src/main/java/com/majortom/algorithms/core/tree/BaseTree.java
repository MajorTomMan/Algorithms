package com.majortom.algorithms.core.tree;

import com.majortom.algorithms.core.base.BaseAlgorithm;
import com.majortom.algorithms.core.tree.node.TreeNode;
import java.util.List;

/**
 * 树算法的抽象基类
 * 
 */
public abstract class BaseTree<T> extends BaseAlgorithm<BaseTree<T>> {
    // 根节点，现在支持任何继承自 TreeNode 的子类实例
    protected TreeNode<T> root;

    // 当前操作焦点（可视化标记）
    protected TreeNode<T> currentHighlight;

    // --- 核心同步适配 ---

    /**
     * 子类调用此方法同步树的状态到 UI
     * 利用新 TreeNode 的 isHighlighted 属性进行状态标记
     */
    protected void syncTree(TreeNode<T> activeNode, TreeNode<T> secondaryNode) {
        // 先清除旧的高亮状态（如果需要全局唯一高亮）
        if (this.currentHighlight != null) {
            this.currentHighlight.isHighlighted = false;
        }

        this.currentHighlight = activeNode;

        if (activeNode != null) {
            activeNode.isHighlighted = true;
        }

        // 执行 BaseAlgorithm 的快照同步
        sync(this, activeNode, secondaryNode);
    }

    // --- 基础属性访问与通用实现 ---

    public TreeNode<T> getRoot() {
        return root;
    }

    public TreeNode<T> getCurrentHighlight() {
        return currentHighlight;
    }

    /**
     * 利用 TreeNode 预留的 subTreeCount 属性，Size 的获取变得极其简单且统一
     */
    public int size() {
        return root == null ? 0 : root.subTreeCount;
    }

    /**
     * 利用 TreeNode 预留的 height 属性
     */
    public int height() {
        return root == null ? 0 : root.height;
    }

    public boolean isEmpty() {
        return root == null;
    }

    /**
     * 统一定义清理逻辑
     */
    public void clear() {
        root = null;
        currentHighlight = null;
        resetStatistics();
        sync(this, null, null);
    }

    // --- 抽象业务接口 ---

    public abstract void put(T val);

    public abstract void remove(T val);

    public abstract TreeNode<T> search(T val);

    public abstract void traverse();

    public abstract List<T> toList();
}