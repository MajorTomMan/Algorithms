
package basic.structure.interfaces;


public interface Stack<T> extends List<T>{
    T pop();
    void push(T data);
    void push(T[] data);
}