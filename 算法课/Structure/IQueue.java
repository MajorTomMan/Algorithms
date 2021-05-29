package Structure;


public interface IQueue<T>{
    T pop();
    void push(T var);
    boolean isEmpty();
    void Inital(T var);
    int getSize();
    void show(Queue<T> queue);
}
