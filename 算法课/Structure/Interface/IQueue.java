package Structure.Interface;

import Structure.Node.Node;

public interface IQueue<T>{
    T pop();
    void push(T var);
    boolean isEmpty();
    void Inital(T var);
    int getSize();
    void show(Node<T> node);
}
