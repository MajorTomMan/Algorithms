package com.majortom.algorithms.core.interfaces;

import com.majortom.algorithms.core.basic.node.TreeNode;

@SuppressWarnings("hiding")
public interface BalancedTree<Key extends Comparable<Key>, Value extends Comparable<Value>> extends Tree<Key, Value> {
    // 一般用不到
    // 左-右旋-解决右子树过高
    default TreeNode<Key, Value> leftRightRotation(TreeNode<Key, Value> node) {
        node.right = rightRotation(node.right); // 对右子树进行右旋
        return leftRotation(node); // 对当前节点进行左旋
    }

    // 一般用不到
    // 右-左旋-解决左子树过高
    default TreeNode<Key, Value> rightLeftRotation(TreeNode<Key, Value> node) {
        node.left = leftRotation(node.left); // 对左子树进行左旋
        return rightRotation(node); // 对当前节点进行右旋
    }

    // 左旋-解决右子树过高
    default TreeNode<Key, Value> leftRotation(TreeNode<Key, Value> node) {
        if (node == null || node.right == null) {
            return node;
        }
        TreeNode<Key, Value> right = node.right;
        node.right = right.left;
        right.left = node;
        // 更新节点高度
        node.height = 1 + Math.max(height(node.left), height(node.right));
        right.height = 1 + Math.max(height(right.left), height(right.right));
        // 返回新的根节点 right
        return right;
    }

    // 右旋-解决左子树过高
    default TreeNode<Key, Value> rightRotation(TreeNode<Key, Value> node) {
        if (node == null || node.left == null) {
            return node;
        }
        TreeNode<Key, Value> left = node.left;
        node.left = left.right;
        left.right = node;
        // 更新节点高度
        node.height = 1 + Math.max(height(node.left), height(node.right));
        left.height = 1 + Math.max(height(left.left), height(left.right)); // 更新左旋后的节点高度
        return left;
    }

    default int height(TreeNode<Key, Value> node) {
        return node == null ? -1 : node.height;
    }

}
