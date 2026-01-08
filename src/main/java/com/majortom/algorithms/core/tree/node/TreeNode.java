
package com.majortom.algorithms.core.tree.node;

import java.util.ArrayList;
import java.util.List;

public class TreeNode<T> {
   public T data;
   public int height;
   public int subTreeCount;

   // 核心：二叉多叉共存
   public TreeNode<T> left; // 针对二叉树算法
   public TreeNode<T> right; // 针对二叉树算法
   public List<TreeNode<T>> children = new ArrayList<>(); // 针对多叉树算法

   public TreeNode(T data) {
      this.data = data;
      this.height = 1;
      this.subTreeCount = 1;
   }

   // 辅助方法：统一获取所有子节点（绘制时用）
   public List<TreeNode<T>> getAllChildren() {
      if (children != null && !children.isEmpty())
         return children;

      // 如果 children 为空，尝试组合 left 和 right
      List<TreeNode<T>> list = new ArrayList<>();
      if (left != null)
         list.add(left);
      if (right != null)
         list.add(right);
      return list;
   }
}