/*
 * @Date: 2024-07-13 16:22:53
 * @LastEditors: MajorTomMan 765719516@qq.com
 * @LastEditTime: 2024-07-13 19:31:00
 * @FilePath: \ALG\Algorithms\basic\structure\node\TreeNode.java
 * @Description: MajorTomMan @版权声明 保留文件所有权利
 */
package basic.structure.node;

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
