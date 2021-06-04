package Structure;

public interface ICyclelist<T> {
    public void Initial(T var);
    public void Delete(int index);
    public void Insert(T var);
    public void Show(Cyclelist<T> list);
}
