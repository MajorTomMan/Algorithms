package Basic.Structure.Node;

public class Treenode<T> {
   public T data;
   public int SubTreeNum;
   public Treenode<T> Left;
   public Treenode<T> Right;

   public Treenode(T data, Treenode<T> left, Treenode<T> right) {
      this.data = data;
      this.Left = left;
      this.Right = right;
   }

   public Treenode(T data) {
      this.data = data;
      this.Left = null;
      this.Right = null;
   }
}
