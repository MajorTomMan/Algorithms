package Basic.Structure.Interface;

public interface IQueue<T> extends Example<T>{
    T dequeue();
    void enqueue(T data);
}
