package com.majortom.algorithms.core.tree.node;

import java.util.ArrayList;
import java.util.List;

/**
 * 二叉树节点基类。
 *
 * <p>它把 left/right 转换为 {@link #getChildren()} 返回的统一子节点列表，
 * 让 {@code BaseTree} 的深拷贝和可视化布局不需要关心具体字段名。</p>
 *
 * @param <T> 节点数据类型
 */
public abstract class BinaryTreeNode<T> extends TreeNode<T> {
    /**
     * 左子节点。
     */
    public BinaryTreeNode<T> left;

    /**
     * 右子节点。
     */
    public BinaryTreeNode<T> right;

    /**
     * 创建二叉树节点。
     *
     * @param data 节点数据
     */
    public BinaryTreeNode(T data) {
        super(data);
    }

    /**
     * 获取左右子节点列表。
     *
     * @return 固定包含 left 和 right 的列表
     */
    @Override
    public List<BinaryTreeNode<T>> getChildren() {
        List<BinaryTreeNode<T>> list = new ArrayList<>(2);
        list.add(left);
        list.add(right);
        return list;
    }
}
