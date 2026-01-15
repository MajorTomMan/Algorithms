package com.majortom.algorithms.core.tree;

import com.majortom.algorithms.core.base.BaseStructure;
import com.majortom.algorithms.core.tree.node.TreeNode;

/**
 * 树的数据实体类
 * 职责：维护根节点引用，并记录当前高亮状态。
 * 
 * @param <T> 树节点中存储的数据类型
 */
public class BaseTree<T> extends BaseStructure<TreeNode<T>> {

    protected TreeNode<T> root;

    // 当前操作焦点：算法当前扫描或修改的节点
    protected TreeNode<T> currentHighlight;

    public BaseTree() {
        this.root = null;
    }

    /**
     * 实现 BaseStructure 契约：返回根节点作为核心数据模型
     */
    @Override
    public TreeNode<T> getData() {
        return root;
    }

    @Override
    public void resetStatistics() {
        // TODO Auto-generated method stub
        super.resetStatistics();
        this.currentHighlight = null;
    }

    /**
     * 原子操作：高亮并统计一次节点访问（比较）
     */
    public void focusNode(TreeNode<T> node) {
        this.currentHighlight = node;
        incrementCompare();
    }

    /**
     * 原子操作：标记一次结构变更（插入、删除、旋转）
     */
    public void modifyStructure(TreeNode<T> newRoot) {
        this.root = newRoot;
        incrementAction();
    }

    // --- 树结构属性 ---

    public TreeNode<T> getRoot() {
        return root;
    }

    public void setRoot(TreeNode<T> root) {
        this.root = root;
    }

    public TreeNode<T> getCurrentHighlight() {
        return currentHighlight;
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

    @Override
    public void clear() {
        root = null;
        currentHighlight = null;
        resetStatistics();
    }
}