
package com.majortom.algorithms.core.basic.node;

import java.util.List;

public class TreeNode<Key, Value> {
   public Key key;
   public Value value;
   public int height;
   public int subTreeCount;
   public TreeNode<Key, Value> left;
   public TreeNode<Key, Value> right;
   public List<TreeNode<Key, Value>> children;

   public TreeNode(Key key, Value value, TreeNode<Key, Value> left, TreeNode<Key, Value> right) {
      this.key = key;
      this.value = value;
      this.left = left;
      this.right = right;
      this.children = null;
   }

   public TreeNode(Key key, Value value, List<TreeNode<Key, Value>> children) {
      this.key = key;
      this.value = value;
      this.left = null;
      this.right = null;
      this.children = children;
   }

   public TreeNode(Key key, Value value) {
      this.key = key;
      this.value = value;
      this.left = null;
      this.right = null;
      this.children = null;
   }
}
