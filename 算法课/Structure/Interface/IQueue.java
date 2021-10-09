package Structure.Interface;

import Structure.Node.Node;

public interface IQueue<T>{
    T dequeue();
    void enqueue(T var);
    boolean isEmpty();
    void Inital(T var);
    int getSize();
    void show(Node<T> node);
}
