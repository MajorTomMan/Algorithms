/*
 * @Date: 2023-04-26 16:51:29
 * @LastEditors: hujunhao hujunhao@rtczsz.com
 * @LastEditTime: 2023-04-26 17:58:20
 * @FilePath: /alg/Algorithms/basic/structure/node/TreeNode.java
 */
package basic.structure.node;

public class TreeNode<T> {
   public T data;
   public int SubTreeNum;
   public TreeNode<T> Left;
   public TreeNode<T> Right;

   public TreeNode(T data, TreeNode<T> left, TreeNode<T> right) {
      this.data = data;
      this.Left = left;
      this.Right = right;
   }

   public TreeNode(T data) {
      this.data = data;
      this.Left = null;
      this.Right = null;
   }
}
