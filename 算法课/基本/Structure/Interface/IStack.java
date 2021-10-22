package 基本.Structure.Interface;


public interface IStack<T>{
    T pop();
    void push(T var);
    boolean isEmpty();
    int getSize();
}