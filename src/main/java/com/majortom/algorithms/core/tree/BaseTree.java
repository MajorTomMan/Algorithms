package com.majortom.algorithms.core.tree;

import com.majortom.algorithms.core.base.BaseAlgorithm;
import com.majortom.algorithms.core.tree.node.TreeNode;

import java.util.List;

/**
 * 树算法的抽象基类
 * 继承自 BaseAlgorithm，将数据快照类型定义为 BaseTree<T> 自身
 */
public abstract class BaseTree<T> extends BaseAlgorithm<BaseTree<T>> {
    protected TreeNode<T> root; // 树的根节点
    protected TreeNode<T> currentHighlight; // 当前正在操作/遍历的节点（焦点）

    // --- 核心同步适配 ---

    /**
     * 子类调用此方法同步树的状态到 UI
     * 
     * @param activeNode    当前高亮的节点 (对应焦点 a)
     * @param secondaryNode 辅助高亮节点（如旋转时的对比点，对应焦点 b）
     */
    protected void syncTree(TreeNode<T> activeNode, TreeNode<T> secondaryNode) {
        this.currentHighlight = activeNode;
        sync(this, activeNode, secondaryNode);
    }

    // --- 基础属性访问 ---

    public TreeNode<T> getRoot() {
        return root;
    }

    public TreeNode<T> getCurrentHighlight() {
        return currentHighlight;
    }

    // --- 抽象业务接口保持不变 ---

    public abstract void put(T val);

    public abstract void remove(T val);

    public abstract TreeNode<T> search(T val);

    public abstract void traverse();

    public abstract int size();

    public abstract int height();

    public abstract boolean isEmpty();

    public abstract void clear();

    public abstract List<T> toList();
}