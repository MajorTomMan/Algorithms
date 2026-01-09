package com.majortom.algorithms.core.interfaces;

import com.majortom.algorithms.core.tree.BaseTree;
import com.majortom.algorithms.core.tree.node.TreeNode;
import java.util.List;

public abstract class BalancedTree<T extends Comparable<T>> extends BaseTree<T> {

    protected int height(TreeNode<T> node) {
        return node == null ? 0 : node.height;
    }

    // 泛用更新：不管是二叉旋转还是多叉变动，都用此方法刷新元数据
    protected void updateMetrics(TreeNode<T> node) {
        if (node == null)
            return;
        int h = 0;
        int count = 1;
        List<TreeNode<T>> allChildren = node.getAllChildren();
        for (TreeNode<T> child : allChildren) {
            h = Math.max(h, height(child));
            count += (child == null ? 0 : child.subTreeCount);
        }
        node.height = 1 + h;
        node.subTreeCount = count;
    }

    protected TreeNode<T> leftRotation(TreeNode<T> node) {
        if (node == null || node.right == null)
            return node;

        actionCount++; // 旋转属于关键操作
        TreeNode<T> rightChild = node.right;
        node.right = rightChild.left;
        rightChild.left = node;

        updateMetrics(node);
        updateMetrics(rightChild);

        // 核心改动：使用 sync 同步到 UI。焦点 a 为新根节点，b 为旧根节点
        sync(this, rightChild, node);
        return rightChild;
    }

    protected TreeNode<T> rightRotation(TreeNode<T> node) {
        if (node == null || node.left == null)
            return node;

        actionCount++;
        TreeNode<T> leftChild = node.left;
        node.left = leftChild.right;
        leftChild.right = node;

        updateMetrics(node);
        updateMetrics(leftChild);

        sync(this, leftChild, node);
        return leftChild;
    }

    protected TreeNode<T> leftRightRotation(TreeNode<T> node) {
        // 这里不需要额外 sync，因为内部的 leftRotation 和 rightRotation 已经 sync 过了
        node.left = leftRotation(node.left);
        return rightRotation(node);
    }

    protected TreeNode<T> rightLeftRotation(TreeNode<T> node) {
        node.right = rightRotation(node.right);
        return leftRotation(node);
    }

    public abstract void put(T val);

    public abstract void remove(T val);
}