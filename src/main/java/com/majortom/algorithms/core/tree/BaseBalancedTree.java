package com.majortom.algorithms.core.tree;

import com.majortom.algorithms.core.tree.node.BinaryTreeNode;
import com.majortom.algorithms.core.tree.node.TreeNode;

/**
 * 平衡树算法基类
 * 职责：提供旋转等底层工具，不持有数据，通过参数操作 BaseTree。
 */
public abstract class BaseBalancedTree<T extends Comparable<T>> extends BaseTreeAlgorithms<T> {

    // 算法执行入口
    public abstract void run(BaseTree<T> tree);

    protected abstract TreeNode<T> createNode(T data);

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
     * 左旋转
     */
    protected TreeNode<T> leftRotation(BaseTree<T> tree, TreeNode<T> node) {
        if (!(node instanceof BinaryTreeNode) || ((BinaryTreeNode<T>) node).right == null)
            return node;

        actionCount++;
        BinaryTreeNode<T> current = (BinaryTreeNode<T>) node;
        BinaryTreeNode<T> rightChild = (BinaryTreeNode<T>) current.right;

        current.right = rightChild.left;
        rightChild.left = current;

        updateMetrics(current);
        updateMetrics(rightChild);

        syncTree(tree, rightChild, current); // 同步旋转后的状态
        return rightChild;
    }

    /**
     * 右旋转
     */
    protected TreeNode<T> rightRotation(BaseTree<T> tree, TreeNode<T> node) {
        if (!(node instanceof BinaryTreeNode) || ((BinaryTreeNode<T>) node).left == null)
            return node;

        actionCount++;
        BinaryTreeNode<T> current = (BinaryTreeNode<T>) node;
        BinaryTreeNode<T> leftChild = (BinaryTreeNode<T>) current.left;

        current.left = leftChild.right;
        leftChild.right = current;

        updateMetrics(current);
        updateMetrics(leftChild);

        syncTree(tree, leftChild, current);
        return leftChild;
    }

    protected TreeNode<T> leftRightRotation(BaseTree<T> tree, TreeNode<T> node) {
        if (node instanceof BinaryTreeNode) {
            BinaryTreeNode<T> current = (BinaryTreeNode<T>) node;
            current.left = (BinaryTreeNode<T>) leftRotation(tree, current.left);
            return rightRotation(tree, current);
        }
        return node;
    }

    protected TreeNode<T> rightLeftRotation(BaseTree<T> tree, TreeNode<T> node) {
        if (node instanceof BinaryTreeNode) {
            BinaryTreeNode<T> current = (BinaryTreeNode<T>) node;
            current.right = (BinaryTreeNode<T>) rightRotation(tree, current.right);
            return leftRotation(tree, current);
        }
        return node;
    }

    public abstract void put(BaseTree<T> tree, T val);

    public abstract void remove(BaseTree<T> tree, T val);

    public abstract TreeNode<T> search(BaseTree<T> tree, T val);
}