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
        actionCount++;
        TreeNode<T> rightChild = node.right;
        node.right = rightChild.left;
        rightChild.left = node;

        updateMetrics(node);
        updateMetrics(rightChild);
        onStep();
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
        onStep();
        return leftChild;
    }

    protected TreeNode<T> leftRightRotation(TreeNode<T> node) {
        node.left = leftRotation(node.left);
        return rightRotation(node);
    }

    protected TreeNode<T> rightLeftRotation(TreeNode<T> node) {
        node.right = rightRotation(node.right);
        return leftRotation(node);
    }

    // 留给子类实现的业务方法
    public abstract void put(T val);

    public abstract void remove(T val);
}