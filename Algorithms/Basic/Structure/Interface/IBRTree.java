package Basic.Structure.Interface;

public interface IBRTree<T> extends Example<T>{
    void Insert(T data);
    void Delete(T data);
    T Max();
    T Min();
}
