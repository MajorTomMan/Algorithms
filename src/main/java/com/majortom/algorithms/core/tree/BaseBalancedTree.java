package com.majortom.algorithms.core.tree;

import com.majortom.algorithms.core.tree.node.BinaryTreeNode;
import com.majortom.algorithms.core.tree.node.TreeNode;
/**
 * 平衡树抽象基类
 * 适配通用的 TreeNode 架构，统一处理旋转与元数据更新逻辑
 */
public abstract class BaseBalancedTree<T extends Comparable<T>> extends BaseTree<T> {

    /**
     * 工厂方法：由子类实现，用于创建特定类型的节点（如 AVLTreeNode）
     */
    protected abstract TreeNode<T> createNode(T data);

    protected int height(TreeNode<T> node) {
        return node == null ? 0 : node.height;
    }

    /**
     * 泛用元数据更新
     * 基于 getChildren() 遍历，兼容二叉与多叉结构的属性计算
     */
    protected void updateMetrics(TreeNode<T> node) {
        if (node == null)
            return;

        int h = 0;
        int count = 1;
        // 使用通用的迭代接口
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
     * 左旋转适配
     * 强制要求操作对象为 BinaryTreeNode 以确保 left/right 指针可用
     */
    protected TreeNode<T> leftRotation(TreeNode<T> node) {
        if (!(node instanceof BinaryTreeNode) || ((BinaryTreeNode<T>) node).right == null)
            return node;

        actionCount++;
        BinaryTreeNode<T> current = (BinaryTreeNode<T>) node;
        BinaryTreeNode<T> rightChild = (BinaryTreeNode<T>) current.right;

        // 执行旋转交换
        current.right = rightChild.left;
        rightChild.left = current;

        // 更新受影响节点的元数据
        updateMetrics(current);
        updateMetrics(rightChild);

        sync(this, rightChild, current);
        return rightChild;
    }

    /**
     * 右旋转适配
     */
    protected TreeNode<T> rightRotation(TreeNode<T> node) {
        if (!(node instanceof BinaryTreeNode) || ((BinaryTreeNode<T>) node).left == null)
            return node;

        actionCount++;
        BinaryTreeNode<T> current = (BinaryTreeNode<T>) node;
        BinaryTreeNode<T> leftChild = (BinaryTreeNode<T>) current.left;

        // 执行旋转交换
        current.left = leftChild.right;
        leftChild.right = current;

        updateMetrics(current);
        updateMetrics(leftChild);

        sync(this, leftChild, current);
        return leftChild;
    }

    protected TreeNode<T> leftRightRotation(TreeNode<T> node) {
        if (node instanceof BinaryTreeNode) {
            BinaryTreeNode<T> current = (BinaryTreeNode<T>) node;
            current.left = (BinaryTreeNode<T>) leftRotation(current.left);
            return rightRotation(current);
        }
        return node;
    }

    protected TreeNode<T> rightLeftRotation(TreeNode<T> node) {
        if (node instanceof BinaryTreeNode) {
            BinaryTreeNode<T> current = (BinaryTreeNode<T>) node;
            current.right = (BinaryTreeNode<T>) rightRotation(current.right);
            return leftRotation(current);
        }
        return node;
    }

    public abstract void put(T val);

    public abstract void remove(T val);
}