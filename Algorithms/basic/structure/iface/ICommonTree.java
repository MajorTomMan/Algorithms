
package basic.structure.iface;

public interface ICommonTree<T>{
    void put(T data);
    T get(T key);
    void delete(T key);
}
