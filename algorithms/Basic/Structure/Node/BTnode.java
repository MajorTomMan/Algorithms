package Basic.Structure.Node;


public class BTnode<T>{
   public Data<T> item;
   public int SubTreeNum;
   public BTnode<T> Left;
   public BTnode<T> Right; 
   public BTnode(Data<T> item, BTnode<T> left, BTnode<T> right) {
      this.item = item;
      this.Left = left;
      this.Right = right;
   }
}
