

package basic.structure.iface;

public interface IQueue<T> extends ICommon<T>{
    T dequeue();
    void enqueue(T data);
}
