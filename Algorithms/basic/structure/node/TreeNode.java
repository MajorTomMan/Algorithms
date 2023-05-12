
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
