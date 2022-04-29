package Basic.Structure.Interface;
public interface ICyclelist<T> extends Example<T> {
    public void Initial(T data);
    public void Delete(int index);
    public void Insert(T data);
    public void Show();
}
