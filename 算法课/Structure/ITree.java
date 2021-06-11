package Structure;

public interface ITree<T>{
    void InitializeTree(TRnode<T> node);
    boolean TreeIsEmpty();
    boolean TreeIsFull();
    int TreeItemCount();
    void Insert(TRnode<T> node);
    void Delete(T data);
    void Show(TRnode<T> node);
}