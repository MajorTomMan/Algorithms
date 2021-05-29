package Structure;

public interface ILinkedlist<T> {
    public void Initial(Node<T> head);
    public void Delete(int index);
    public void Insert(Node<T> node);
    public void InsertMiddle(int index, Node<T> node);
    public void InsertHead(Node<T> node);
    public void Reverse(Linkedlist<T> list);
    public void Show(Linkedlist<T> list);
}
