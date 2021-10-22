package 查找.Structure;

public class LinearProbingHashST<Key,Value> extends SymbolTable<Key,Value>{
    private int N;
    private int M=16;
    private Key[] keys;
    private Value[] vals;
    public LinearProbingHashST(int cap){
        keys=(Key[])new Object[cap];
    }
    public LinearProbingHashST(){
        keys=(Key[])new Object[M];
    }
    @Override
    public void put(Key key, Value val) {
        // TODO Auto-generated method stub
        if(N>=M/2){
            resize(2*M);
        }
        int i;
        for(i=hash(key);keys[i]!=null;i=(i+1)%M){
            if(keys[i].equals(key)){
                vals[i]=val;
                return;
            }
        }
        keys[i]=key;
        vals[i]=val;
        N++;
    }
    @Override
    public Value get(Key key) {
        // TODO Auto-generated method stub
        for(int i=hash(key);keys[i]!=null;i=(i+1)%M){
            if(keys[i].equals(key)){
                return vals[i];
            }
        }
        return null;
    }
    @Override
    public int size() {
        // TODO Auto-generated method stub
        return 0;
    }
    @Override
    public Iterable<Key> keys() {
        // TODO Auto-generated method stub
        return null;
    }
    private int hash(Key key){
        return ((key.hashCode()%0x7fffffff)%M);
    }
    public void delete(Key key){
        if(!contains(key)){
            return;
        }
        int i=hash(key);
        while(!key.equals(keys[i])){
            i=(i+1)%M;
        }
        keys[i]=null;
        vals[i]=null;
        i=(i+1)%M;
        while(keys[i]!=null){
            Key keyToRedo=keys[i];
            Value valToRedo=vals[i];
            keys[i]=null;
            vals[i]=null;
            N--;
            put(keyToRedo,valToRedo);
            i=(i+1)%M;
        }
        N--;
        if(N>0&&N==M/8){
            resize(M/2);
        }
    }
    private void resize(int cap){
        LinearProbingHashST<Key,Value> t;
        t=new LinearProbingHashST<Key,Value>(cap);
        for(int i=0;i<M;i++){
            if(keys[i]!=null){
                t.put(keys[i], vals[i]);
            }
        }
        keys=t.keys;
        vals=t.vals;
        M=t.M;
    }
}
