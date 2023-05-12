

package basic.structure.iface;


public interface ILoopqueue<T>{
    T dequeue();
    void enqueue(T data);
}
