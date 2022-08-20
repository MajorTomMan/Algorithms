package Basic.Structure.Interface;

public interface ExampleTree<T>{
    T get(T data);
    void put(T data,int depth);
}
