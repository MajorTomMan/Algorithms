package com.majortom.algorithms.core.tree.impl;

import com.majortom.algorithms.core.tree.BaseBalancedTree;
import com.majortom.algorithms.core.tree.BaseTree;
import com.majortom.algorithms.core.tree.node.AVLTreeNode;
import com.majortom.algorithms.core.tree.node.BinaryTreeNode;
import com.majortom.algorithms.core.tree.node.TreeNode;

/**
 * AVL 树具体实现
 * 职责：通过递归返回值维护树结构，并在平衡因子失衡时触发基类的旋转工具。
 */
public class AVLTree<T extends Comparable<T>> extends BaseBalancedTree<T> {

    @Override
    protected TreeNode<T> createNode(T data) {
        return new AVLTreeNode<>(data);
    }

    // --- 外部接口实现 ---

    @Override
    public void put(BaseTree<T> tree, T val) {
        // 核心：入口处将递归返回的新根节点（可能是旋转后的）更新回 tree 实体
        tree.setRoot(doPut(tree, tree.getRoot(), val));
        syncTree(tree, null, null); // 操作结束，清除 UI 高亮
    }

    @Override
    public void remove(BaseTree<T> tree, T val) {
        tree.setRoot(doRemove(tree, tree.getRoot(), val));
        syncTree(tree, null, null);
    }

    @Override
    public TreeNode<T> search(BaseTree<T> tree, T val) {
        return doSearch(tree, tree.getRoot(), val);
    }

    // --- 内部递归逻辑 ---

    private TreeNode<T> doPut(BaseTree<T> tree, TreeNode<T> node, T data) {
        if (node == null) {
            actionCount++;
            return createNode(data);
        }

        compareCount++;
        syncTree(tree, node, null); // 渲染当前扫描到的路径

        int cmp = data.compareTo(node.data);
        BinaryTreeNode<T> bNode = (BinaryTreeNode<T>) node;

        if (cmp < 0) {
            // 递归返回后，父节点重新接住可能已经变动（旋转）后的子节点
            bNode.left = (BinaryTreeNode<T>) doPut(tree, bNode.left, data);
        } else if (cmp > 0) {
            bNode.right = (BinaryTreeNode<T>) doPut(tree, bNode.right, data);
        } else {
            return node; // 不处理重复值
        }

        // 递归回溯时更新元数据并检查平衡
        updateMetrics(node);
        return rebalance(tree, node);
    }

    private TreeNode<T> doRemove(BaseTree<T> tree, TreeNode<T> node, T val) {
        if (node == null)
            return null;

        compareCount++;
        syncTree(tree, node, null);

        int cmp = val.compareTo(node.data);
        BinaryTreeNode<T> bNode = (BinaryTreeNode<T>) node;

        if (cmp < 0) {
            bNode.left = (BinaryTreeNode<T>) doRemove(tree, bNode.left, val);
        } else if (cmp > 0) {
            bNode.right = (BinaryTreeNode<T>) doRemove(tree, bNode.right, val);
        } else {
            actionCount++;
            // 找到目标：处理删除逻辑
            if (bNode.left == null || bNode.right == null) {
                node = (bNode.left != null) ? bNode.left : bNode.right;
            } else {
                // 有双子：找后继（右子树最小）
                TreeNode<T> successor = findMin(tree, bNode.right);
                node.data = successor.data;
                syncTree(tree, node, successor);
                bNode.right = (BinaryTreeNode<T>) doRemove(tree, bNode.right, successor.data);
            }
        }

        if (node == null)
            return null;
        updateMetrics(node);
        return rebalance(tree, node);
    }

    private TreeNode<T> doSearch(BaseTree<T> tree, TreeNode<T> node, T val) {
        if (node == null)
            return null;

        syncTree(tree, node, null);
        compareCount++;

        int cmp = val.compareTo(node.data);
        if (cmp == 0)
            return node;

        BinaryTreeNode<T> bNode = (BinaryTreeNode<T>) node;
        return cmp < 0 ? doSearch(tree, bNode.left, val) : doSearch(tree, bNode.right, val);
    }

    // --- 平衡判定 ---

    /**
     * 根据平衡因子决定触发哪种旋转
     * 返回值：新的子树根节点（供上层父节点接住）
     */
    private TreeNode<T> rebalance(BaseTree<T> tree, TreeNode<T> node) {
        int balance = getBalance(node);
        BinaryTreeNode<T> bNode = (BinaryTreeNode<T>) node;

        // LL Case -> 右旋
        if (balance > 1 && getBalance(bNode.left) >= 0) {
            return rightRotation(tree, node);
        }
        // RR Case -> 左旋
        if (balance < -1 && getBalance(bNode.right) <= 0) {
            return leftRotation(tree, node);
        }
        // LR Case -> 先左后右
        if (balance > 1 && getBalance(bNode.left) < 0) {
            return leftRightRotation(tree, node);
        }
        // RL Case -> 先右后左
        if (balance < -1 && getBalance(bNode.right) > 0) {
            return rightLeftRotation(tree, node);
        }

        return node; // 已经平衡，原样返回
    }

    private int getBalance(TreeNode<T> node) {
        if (node == null)
            return 0;
        BinaryTreeNode<T> bn = (BinaryTreeNode<T>) node;
        return height(bn.left) - height(bn.right);
    }

    private TreeNode<T> findMin(BaseTree<T> tree, TreeNode<T> node) {
        BinaryTreeNode<T> current = (BinaryTreeNode<T>) node;
        while (current.left != null) {
            syncTree(tree, current, null);
            current = (BinaryTreeNode<T>) current.left;
        }
        return current;
    }

    @Override
    public void traverse(BaseTree<T> tree) {
        // 可实现层序遍历以供 Debug
    }

    @Override
    public void run(BaseTree<T> tree) {
        // TODO Auto-generated method stub

    }
}