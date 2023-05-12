

package basic.structure.iface;
public interface IList<T> extends ICommon<T>{
    public void Delete(int index);
    public void Insert(T data);
    public void Reverse();
}
