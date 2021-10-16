package Structure.Interface;

public interface IQueue<T>{
    T dequeue();
    void enqueue(T var);
    boolean isEmpty();
    int getSize();
}
