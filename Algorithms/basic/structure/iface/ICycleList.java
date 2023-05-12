

package basic.structure.iface;

public interface ICycleList<T> extends ICommon<T> {
    public void Initial(T data);
    public void Delete(int index);
    public void Insert(T data);
}
