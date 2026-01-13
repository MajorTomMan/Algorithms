package com.majortom.algorithms.core.tree.node;

import java.util.List;

/**
 * 树节点抽象基类
 * 职责：封装核心数据与结构元数据（高度、规模），提供统一的子节点访问接口。
 * 
 * @param <T> 存储的数据类型
 */
public abstract class TreeNode<T> {
   // 核心数据
   public T data;

   // --- 算法结构元数据 ---
   // 默认高度与计数由基类维护，保证所有树算法的通用统计能力
   public int height = 1;
   public int subTreeCount = 1;

   /**
    * 状态位：
    * 用于存储算法特定的值（如 AVL 的平衡因子、红黑树的颜色位、B树的键数等）。
    */
   public int status = 0;

   public TreeNode(T data) {
      this.data = data;
   }

   /**
    * 获取当前节点的所有子节点
    * 职责：实现此方法以适配通用的树遍历、高度计算及 UI 渲染逻辑。
    * 
    * @return 子节点列表
    */
   public abstract List<? extends TreeNode<T>> getChildren();

   /**
    * 辅助方法：判断是否为叶子节点
    */
   public boolean isLeaf() {
      List<? extends TreeNode<T>> children = getChildren();
      if (children == null || children.isEmpty())
         return true;
      for (TreeNode<T> child : children) {
         if (child != null)
            return false;
      }
      return true;
   }
}