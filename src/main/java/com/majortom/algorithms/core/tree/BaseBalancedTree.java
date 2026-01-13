package com.majortom.algorithms.core.tree;

import com.majortom.algorithms.core.tree.node.BinaryTreeNode;
import com.majortom.algorithms.core.tree.node.TreeNode;

/**
 * 平衡树算法基类
 * 职责：提供旋转等底层工具。
 * 适配说明：通过 BaseTree 的原子操作触发统计，减少显式强转。
 */
public abstract class BaseBalancedTree<T extends Comparable<T>> extends BaseTreeAlgorithms<T> {

    
    protected int height(TreeNode<T> node) {
        return node == null ? 0 : node.height;
    }

    /**
     * 更新节点的元数据（高度、子树规模）
     */
    protected void updateMetrics(TreeNode<T> node) {
        if (node == null)
            return;

        int h = 0;
        int count = 1;
        // 这里的 getChildren() 保证了通用性，无论是二叉树还是多叉树都能自适应
        for (TreeNode<T> child : node.getChildren()) {
            if (child != null) {
                h = Math.max(h, child.height);
                count += child.subTreeCount;
            }
        }
        node.height = 1 + h;
        node.subTreeCount = count;
    }

    /**
     * 左旋转 (Left Rotation)
     */
    protected BinaryTreeNode<T> leftRotation(BaseTree<T> tree, BinaryTreeNode<T> node) {
        if (node == null || node.right == null)
            return node;

        // 触发一次结构变更统计
        tree.modifyStructure(tree.getRoot());

        BinaryTreeNode<T> rightChild = (BinaryTreeNode<T>) node.right;
        node.right = rightChild.left;
        rightChild.left = node;

        updateMetrics(node);
        updateMetrics(rightChild);

        // 同步状态：让 Visualizer 知道这两个节点的位置发生了突变
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

    protected BinaryTreeNode<T> leftRightRotation(BaseTree<T> tree, BinaryTreeNode<T> node) {
        node.left = leftRotation(tree, (BinaryTreeNode<T>) node.left);
        return rightRotation(tree, node);
    }

    protected BinaryTreeNode<T> rightLeftRotation(BaseTree<T> tree, BinaryTreeNode<T> node) {
        node.right = rightRotation(tree, (BinaryTreeNode<T>) node.right);
        return leftRotation(tree, node);
    }

    // --- 抽象接口 ---

    protected abstract TreeNode<T> createNode(T data);

    public abstract void put(BaseTree<T> tree, T val);

    public abstract void remove(BaseTree<T> tree, T val);

    public abstract TreeNode<T> search(BaseTree<T> tree, T val);
}