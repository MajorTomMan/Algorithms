package com.majortom.algorithms.core.tree;

import com.majortom.algorithms.core.tree.node.BinaryTreeNode;
import com.majortom.algorithms.core.tree.node.TreeNode;

/**
 * 平衡树算法基类
 * 职责：提供旋转等底层平衡工具，并确保元数据（高度、规模）在变更后能被准确记录。
 * 适配说明：旋转后的元数据更新直接影响 BaseTree.copy() 快照的准确性。
 */
public abstract class BaseBalancedTree<T extends Comparable<T>> extends BaseTreeAlgorithms<T> {

    protected int height(TreeNode<T> node) {
        return node == null ? 0 : node.height;
    }

    /**
     * 更新节点的元数据（高度、子树规模）
     * 职责：这是深拷贝准确性的基石。利用 getChildren() 确保逻辑对二叉/多叉树通用。
     */
    protected void updateMetrics(TreeNode<T> node) {
        if (node == null)
            return;

        int maxHeight = 0;
        int totalCount = 1;

        // 遍历子节点以计算当前节点的高度和规模
        for (TreeNode<T> child : node.getChildren()) {
            if (child != null) {
                maxHeight = Math.max(maxHeight, child.height);
                totalCount += child.subTreeCount;
            }
        }
        node.height = 1 + maxHeight;
        node.subTreeCount = totalCount;
    }

    /**
     * 左旋转 (Left Rotation)
     */
    protected BinaryTreeNode<T> leftRotation(BaseTree<T> tree, BinaryTreeNode<T> node) {
        if (node == null || node.right == null)
            return node;

        // 标记结构变更：触发动画频率控制
        tree.modifyStructure(tree.getRoot());

        BinaryTreeNode<T> rightChild = (BinaryTreeNode<T>) node.right;
        node.right = rightChild.left;
        rightChild.left = node;

        // 必须由下至上更新：先更新变成子节点的 node，再更新变成父节点的 rightChild
        updateMetrics(node);
        updateMetrics(rightChild);

        // 同步状态：告知 Visualizer 节点关系已重组
        sync(tree, rightChild, node);
        return rightChild;
    }

    /**
     * 右旋转 (Right Rotation)
     */
    protected BinaryTreeNode<T> rightRotation(BaseTree<T> tree, BinaryTreeNode<T> node) {
        if (node == null || node.left == null)
            return node;

        tree.modifyStructure(tree.getRoot());

        BinaryTreeNode<T> leftChild = (BinaryTreeNode<T>) node.left;
        node.left = leftChild.right;
        leftChild.right = node;

        updateMetrics(node);
        updateMetrics(leftChild);

        sync(tree, leftChild, node);
        return leftChild;
    }

    /**
     * 左右双旋 (LR Rotation)
     */
    protected BinaryTreeNode<T> leftRightRotation(BaseTree<T> tree, BinaryTreeNode<T> node) {
        if (node == null)
            return null;
        node.left = leftRotation(tree, (BinaryTreeNode<T>) node.left);
        return rightRotation(tree, node);
    }

    /**
     * 右左双旋 (RL Rotation)
     */
    protected BinaryTreeNode<T> rightLeftRotation(BaseTree<T> tree, BinaryTreeNode<T> node) {
        if (node == null)
            return null;
        node.right = rightRotation(tree, (BinaryTreeNode<T>) node.right);
        return leftRotation(tree, node);
    }

    // --- 算法核心接口 ---

    protected abstract TreeNode<T> createNode(T data);

    public abstract void put(BaseTree<T> tree, T val);

    public abstract void remove(BaseTree<T> tree, T val);

    public abstract TreeNode<T> search(BaseTree<T> tree, T val);
}