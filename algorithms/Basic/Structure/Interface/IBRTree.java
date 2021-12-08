package Basic.Structure.Interface;

public interface IBRTree<T>{
    boolean IsEmpty();
    int Count();
    void Insert(T data);
    void Delete(T data);
    T Max();
    T Min();
    void Show();
}
