

package basic.structure.iface;

public interface ICycleList<T> extends ICommon<T> {
    public void delete(int index);
    public void insert(T data);
}
