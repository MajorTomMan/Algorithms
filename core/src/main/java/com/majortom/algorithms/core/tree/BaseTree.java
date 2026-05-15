package com.majortom.algorithms.core.tree;

import com.majortom.algorithms.core.base.BaseStructure;
import com.majortom.algorithms.core.tree.node.TreeNode;

import java.util.ArrayList;
import java.util.List;

/**
 * 树结构数据基类。
 *
 * <p>它维护树根、当前高亮节点和通用深拷贝模板。具体树种只需要实现创建空树、
 * 创建节点和挂载子节点三个钩子，就能被执行层快照、树算法和树可视化复用。</p>
 *
 * @param <T> 存储的数据类型
 */
public abstract class BaseTree<T extends Comparable<T>> extends BaseStructure<TreeNode<T>> {

    /**
     * 树根节点。
     */
    protected TreeNode<T> root;

    /**
     * 当前操作焦点：算法当前扫描或修改的节点。
     */
    protected TreeNode<T> currentHighlight;

    /**
     * 创建空树。
     */
    public BaseTree() {
        this.root = null;
    }

    /**
     * 获取根节点。
     *
     * @return 根节点
     */
    public TreeNode<T> getRoot() {
        return root;
    }

    /**
     * 设置根节点。
     *
     * @param root 新根节点
     */
    public void setRoot(TreeNode<T> root) {
        this.root = root;
    }

    /**
     * 高亮并统计一次节点访问。
     *
     * @param node 当前访问节点
     */
    public void focusNode(TreeNode<T> node) {
        this.currentHighlight = node;
        incrementCompare();
    }

    /**
     * 标记一次结构变更。
     *
     * @param newRoot 变更后的根节点
     */
    public void modifyStructure(TreeNode<T> newRoot) {
        this.root = newRoot;
        incrementAction();
    }

    /**
     * 创建树快照。
     *
     * @return 当前树结构的深拷贝
     */
    @Override
    public BaseStructure<TreeNode<T>> copy() {
        // 1. 调用子类钩子创建具体的树容器实例（如 AVLTree）
        BaseTree<T> newTree = createEmptyTree();

        // 2. 递归执行深度拓扑拷贝
        if (this.root != null) {
            newTree.setRoot(recursiveCopy(this.root));
        }

        // 3. 同步算法统计信息
        newTree.actionCount = this.actionCount;
        newTree.compareCount = this.compareCount;

        return newTree;
    }

    /**
     * 递归拷贝节点及子树。
     *
     * @param source 源节点
     * @return 拷贝出的节点
     */
    private TreeNode<T> recursiveCopy(TreeNode<T> source) {
        if (source == null)
            return null;

        // A. 创建新节点实例并同步通用元数据
        TreeNode<T> target = createNodeInstance(source.data);
        target.height = source.height;
        target.status = source.status;
        target.subTreeCount = source.subTreeCount;

        // B. 获取所有子节点并递归拷贝
        List<? extends TreeNode<T>> sourceChildren = source.getChildren();
        if (sourceChildren != null && !sourceChildren.isEmpty()) {
            List<TreeNode<T>> clonedChildren = new ArrayList<>();
            for (TreeNode<T> child : sourceChildren) {
                clonedChildren.add(recursiveCopy(child));
            }

            // C. 调用子类钩子完成子节点挂载
            linkChildren(target, clonedChildren);
        }

        return target;
    }

    /**
     * 创建同类型空树容器。
     *
     * @return 空树
     */
    protected abstract BaseTree<T> createEmptyTree();

    /**
     * 创建具体节点实例。
     *
     * @param data 节点数据
     * @return 新节点
     */
    protected abstract TreeNode<T> createNodeInstance(T data);

    /**
     * 将克隆出的子节点列表挂载到父节点上。
     *
     * @param parent 父节点
     * @param children 子节点列表
     */
    protected abstract void linkChildren(TreeNode<T> parent, List<TreeNode<T>> children);

    /**
     * 获取树根作为底层数据。
     *
     * @return 根节点
     */
    @Override
    public TreeNode<T> getData() {
        return root;
    }

    /**
     * 重置统计和当前高亮。
     */
    @Override
    public void resetStatistics() {
        super.resetStatistics();
        this.currentHighlight = null;
    }

    /**
     * 清空树结构和统计。
     */
    @Override
    public void clear() {
        this.root = null;
        resetStatistics();
        this.currentHighlight = null;
    }

    /**
     * 获取当前高亮节点。
     *
     * @return 当前高亮节点
     */
    public TreeNode<T> getCurrentHighlight() {
        return currentHighlight;
    }

    /**
     * 获取树高度。
     *
     * @return 树高度
     */
    public int height() {
        return root == null ? 0 : root.height;
    }

    /**
     * 获取树节点总数。
     *
     * @return 节点总数
     */
    public int size() {
        return root == null ? 0 : root.subTreeCount;
    }

    /**
     * 判断树是否为空。
     *
     * @return 没有根节点时返回 true
     */
    public boolean isEmpty() {
        return root == null;
    }
}
