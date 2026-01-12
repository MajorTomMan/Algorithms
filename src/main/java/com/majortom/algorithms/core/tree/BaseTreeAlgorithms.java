package com.majortom.algorithms.core.tree;

import com.majortom.algorithms.core.base.BaseAlgorithms;
import com.majortom.algorithms.core.tree.node.TreeNode;

/**
 * 树算法逻辑基类
 * 职责：定义操作接口，并实现算法步骤与 UI 的同步逻辑
 */
public abstract class BaseTreeAlgorithms<T> extends BaseAlgorithms<BaseTree<T>> {

    /**
     * 同步树的状态到 UI
     * 注意：此时 data 是 BaseTree 对象
     */
    protected void syncTree(BaseTree<T> tree, TreeNode<T> activeNode, TreeNode<T> secondaryNode) {
        // 更新数据实体中的高亮状态
        TreeNode<T> oldHighlight = tree.getCurrentHighlight();
        if (oldHighlight != null) {
            oldHighlight.isHighlighted = false;
        }

        tree.setCurrentHighlight(activeNode);
        if (activeNode != null) {
            activeNode.isHighlighted = true;
        }

        // 调用顶层 sync，将整个 tree 状态推送到 UI 线程
        sync(tree, activeNode, secondaryNode);
    }

    // --- 抽象业务接口：算法子类去实现 ---

    public abstract void put(BaseTree<T> tree, T val);

    public abstract void remove(BaseTree<T> tree, T val);

    public abstract TreeNode<T> search(BaseTree<T> tree, T val);

    public abstract void traverse(BaseTree<T> tree);
}