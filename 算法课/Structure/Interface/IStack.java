package Structure.Interface;


public interface IStack<T>{
    T pop();
    void push(T var);
    boolean isEmpty();
    void Inital(T var);
    int getSize();
}