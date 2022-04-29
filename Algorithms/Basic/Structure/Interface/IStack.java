package Basic.Structure.Interface;


public interface IStack<T> extends Example<T>{
    T pop();
    void push(T data);
}