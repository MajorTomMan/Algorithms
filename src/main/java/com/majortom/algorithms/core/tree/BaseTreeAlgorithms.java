package com.majortom.algorithms.core.tree;

import com.majortom.algorithms.core.base.BaseAlgorithms;
import com.majortom.algorithms.core.tree.node.TreeNode;

/**
 * 树算法逻辑基类
 * 职责：定义操作接口，并统筹算法步骤与 UI 的同步逻辑。
 * 
 * @param <T> 树节点存储的数据类型
 */
public abstract class BaseTreeAlgorithms<T extends Comparable<T>> extends BaseAlgorithms<BaseTree<T>> {

    /**
     * 同步树的状态到 UI
     * 职责：将算法当前的焦点节点更新到实体，并触发 UI 渲染信号。
     */
    protected void syncTree(BaseTree<T> tree, TreeNode<T> activeNode, TreeNode<T> secondaryNode) {
        // 1. 利用实体的 focusNode 自动处理高亮更新和 compareCount 计数
        if (activeNode != null) {
            tree.focusNode(activeNode);
        }

        // 2. 调用顶层 sync，将整个 tree 实体推送到 UI 线程
        // 参数说明：实体对象, 活跃节点, 次要节点（如旋转时的另一个节点）
        sync(tree, activeNode, secondaryNode);
    }

    /**
     * 辅助方法：快速触发一次结构变更后的同步
     */
    protected void syncStructure(BaseTree<T> tree) {
        tree.modifyStructure(tree.getRoot());
        sync(tree, null, null);
    }

    // --- 抽象业务接口 ---

    /**
     * 插入/更新
     */
    public abstract void put(BaseTree<T> tree, T val);

    /**
     * 删除
     */
    public abstract void remove(BaseTree<T> tree, T val);

    /**
     * 搜索
     */
    public abstract TreeNode<T> search(BaseTree<T> tree, T val);

    /**
     * 遍历（前/中/后序）
     */
    public abstract void traverse(BaseTree<T> tree);

    @Override
    public void run(BaseTree<T> structure) {
        // TODO Auto-generated method stub

    }
}