package Structure.Interface;

public interface IDictionary<Key,Value>{
    void Put(Key key,Value val);
    Value Get(Key key);
    void Delete(Key key);
    boolean Contains(Key key);
    boolean IsEmpty();
    int Size();
    Key Min();
    Key Max();
    Key Floor(Key key);
    Key Ceiling(Key key);
    int Rank(Key key);
    Key Select(int k);
    void DeleteMin();
    void DeleteMax();
    int size(Key lo,Key hi);
    Iterable<Key> keys(Key lo,Key hi);
    Iterable<Key> keys();
}
