package com.majortom.algorithms.core.tree;

import com.majortom.algorithms.core.base.BaseStructure;
import com.majortom.algorithms.core.tree.node.TreeNode;

import java.util.ArrayList;
import java.util.List;

/**
 * 树结构数据基类
 * 职责：
 * 1. 维护树的根节点引用。
 * 2. 提供通用的深拷贝（Copy）模板逻辑，支持算法状态回滚。
 * 3. 抽象节点创建与挂载钩子，适配不同分支数的树种。
 * * @param <T> 存储的数据类型（需具备可比性）
 */
public abstract class BaseTree<T extends Comparable<T>> extends BaseStructure<TreeNode<T>> {

    protected TreeNode<T> root;
    // 当前操作焦点：算法当前扫描或修改的节点
    protected TreeNode<T> currentHighlight;

    public BaseTree() {
        this.root = null;
    }

    public TreeNode<T> getRoot() {
        return root;
    }

    public void setRoot(TreeNode<T> root) {
        this.root = root;
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

    /**
     * 实现 BaseStructure 的深拷贝接口
     * 策略：递归拓扑拷贝 + 元数据同步
     */
    @Override
    public BaseStructure<TreeNode<T>> copy() {
        // 1. 调用子类钩子创建具体的树容器实例（如 AVLTree）
        BaseTree<T> newTree = createEmptyTree();

        // 2. 递归执行深度拓扑拷贝
        if (this.root != null) {
            newTree.setRoot(recursiveCopy(this.root));
        }

        // 3. 同步算法统计信息
        newTree.actionCount = this.actionCount;
        newTree.compareCount = this.compareCount;

        return newTree;
    }

    /**
     * 泛用型递归拷贝逻辑
     * 职责：利用 TreeNode 的 getChildren 接口，不依赖具体的 left/right 字段
     */
    private TreeNode<T> recursiveCopy(TreeNode<T> source) {
        if (source == null)
            return null;

        // A. 创建新节点实例并同步通用元数据
        TreeNode<T> target = createNodeInstance(source.data);
        target.height = source.height;
        target.status = source.status;
        target.subTreeCount = source.subTreeCount;

        // B. 获取所有子节点并递归拷贝
        List<? extends TreeNode<T>> sourceChildren = source.getChildren();
        if (sourceChildren != null && !sourceChildren.isEmpty()) {
            List<TreeNode<T>> clonedChildren = new ArrayList<>();
            for (TreeNode<T> child : sourceChildren) {
                clonedChildren.add(recursiveCopy(child));
            }

            // C. 调用子类钩子完成子节点挂载
            linkChildren(target, clonedChildren);
        }

        return target;
    }

    // --- 供子类实现的抽象钩子 (Template Methods) ---

    /**
     * 创建一个同类型的空树容器
     */
    protected abstract BaseTree<T> createEmptyTree();

    /**
     * 创建一个具体的节点实例（如 AVLTreeNode）
     */
    protected abstract TreeNode<T> createNodeInstance(T data);

    /**
     * 将克隆出的子节点列表挂载到父节点上
     * 对于二叉树：约定 children.get(0) 是左，get(1) 是右
     */
    protected abstract void linkChildren(TreeNode<T> parent, List<TreeNode<T>> children);

    // --- 基础维护方法 ---

    @Override
    public TreeNode<T> getData() {
        return root;
    }

    @Override
    public void resetStatistics() {
        super.resetStatistics();
        this.currentHighlight = null;
    }

    @Override
    public void clear() {
        this.root = null;
        resetStatistics();
        this.currentHighlight = null;
    }

    public TreeNode<T> getCurrentHighlight() {
        return currentHighlight;
    }

    /**
     * 获取树的总高度
     */
    public int height() {
        return root == null ? 0 : root.height;
    }

    /**
     * 获取树的节点总数
     */
    public int size() {
        return root == null ? 0 : root.subTreeCount;
    }

    public boolean isEmpty() {
        return root == null;
    }
}