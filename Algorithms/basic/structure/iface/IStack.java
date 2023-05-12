
package basic.structure.iface;


public interface IStack<T> extends ICommon<T>{
    T pop();
    void push(T data);
}