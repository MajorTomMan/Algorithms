package com.majortom.algorithms.core.tree.node;

import java.util.List;

/**
 * 树节点抽象基类
 * 封装了算法运行所需的通用元数据及可视化状态
 */
public abstract class TreeNode<T> {
   // 节点存储的核心数据
   public T data;

   // --- 算法元数据 ---
   public int height = 1; // 节点高度
   public int subTreeCount = 1; // 以当前节点为根的子树节点总数
   public int status = 0; // 状态位：用于存储平衡因子、颜色(红/黑)等算法特定值

   // --- 可视化状态 ---
   public double x, y; // UI 坐标位置
   public boolean isHighlighted; // 高亮标记，用于展示当前操作路径或焦点

   public TreeNode(T data) {
      this.data = data;
   }

   /**
    * 获取当前节点的所有子节点
    * 用于统一树的遍历、坐标计算及 UI 渲染逻辑
    * * @return 子节点列表
    */
   public abstract List<? extends TreeNode<T>> getChildren();
}