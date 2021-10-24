package Basic.Structure.Node;


public class BTnode<T>{
   public Data<T> item;
   public BTnode<T> Left;
   public BTnode<T> Right; 
   public BTnode(Data<T> item, BTnode<T> left, BTnode<T> right) {
      this.item = item;
      Left = left;
      Right = right;
   }
}
