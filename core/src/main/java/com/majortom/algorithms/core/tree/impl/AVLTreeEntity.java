package com.majortom.algorithms.core.tree.impl;

import java.util.List;

import com.majortom.algorithms.core.tree.BaseTree;
import com.majortom.algorithms.core.tree.node.AVLTreeNode;
import com.majortom.algorithms.core.tree.node.BinaryTreeNode;
import com.majortom.algorithms.core.tree.node.TreeNode;

/**
 * AVL 树结构实体。
 *
 * <p>它为 {@link BaseTree} 的模板方法提供 AVL 专用节点创建和子节点挂载逻辑，
 * 使执行层复制快照时仍能得到正确的 AVL 节点类型。</p>
 *
 * @param <T> 节点数据类型
 */
public class AVLTreeEntity<T extends Comparable<T>> extends BaseTree<T> {
    /**
     * 创建同类型空 AVL 树。
     */
    @Override
    protected BaseTree<T> createEmptyTree() {
        return new AVLTreeEntity<>();
    }

    /**
     * 创建 AVL 节点实例。
     */
    @Override
    protected TreeNode<T> createNodeInstance(T data) {
        return new AVLTreeNode<>(data);
    }

    /**
     * 按二叉树约定挂载左右子节点。
     */
    @Override
    protected void linkChildren(TreeNode<T> parent, List<TreeNode<T>> children) {
        if (parent instanceof BinaryTreeNode) {
            BinaryTreeNode<T> node = (BinaryTreeNode<T>) parent;
            if (children.size() > 0)
                node.left = (BinaryTreeNode<T>) children.get(0);
            if (children.size() > 1)
                node.right = (BinaryTreeNode<T>) children.get(1);
        }
    }
}
