
package basic.structure.iface;

public interface ICommonTree<T>{
    void put(T data);
    void get(T key);
    void delete(T key);
}
