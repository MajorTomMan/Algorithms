package Structure;

public class Dictionary<Key extends Comparable<Key>,Value> implements IDictionary<Key,Value>{
    private Key[] keys;
    private Value[] vals;
    private int N;
    public Dictionary(int capacity){
        keys=(Key[])new Comparable[capacity];
        vals=(Value[])new Object[capacity];
    }
    @Override
    public void Put(Key key, Value val) {
        // TODO Auto-generated method stub
        int i=Rank(key);
        if(i<N&&keys[i].compareTo(key)==0){
            vals[i]=val;
            return;
        }
        for(int j=N;j>i;j--){
            keys[j]=keys[j-1];
            vals[j]=vals[j-1];
        }
        keys[i]=key;
        vals[i]=val;
        N++;
    }

    @Override
    public Value Get(Key key) {
        // TODO Auto-generated method stub
        if(IsEmpty()){
            return null;
        }
        int i=Rank(key);
        if(i<N&&keys[i].compareTo(key)==0){
            return vals[i];
        }
        else[
            return null;
        ]
    }

    @Override
    public void Delete(Key key) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public boolean Contains(Key key) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean IsEmpty() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public int Size() {
        // TODO Auto-generated method stub
        return N;
    }

    @Override
    public Key Min() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Key Max() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Key Floor(Key key) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Key Ceiling(Key key) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public int Rank(Key key) {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public Key Select(int k) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void DeleteMin() {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void DeleteMax() {
        // TODO Auto-generated method stub
        
    }

    @Override
    public int size(Key lo, Key hi) {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public Iterable<Key> keys(Key lo, Key hi) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Iterable<Key> keys() {
        // TODO Auto-generated method stub
        return null;
    }

}
