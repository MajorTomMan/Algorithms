package Basic.Structure.Interface;
import Basic.Structure.Node.Node;

public interface ILinkedlist<T> extends Example<T>{
   // public void Initial(T data);
    public void Delete(int index);
    public void Insert(T data);
   // public void InsertMiddle(int index,T data);
   // public void InsertHead(T data);
    public void Reverse();
    public void Show();
}
