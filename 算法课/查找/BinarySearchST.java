package 查找;

import Structure.Queue;

public class BinarySearchST<Key extends Comparable<Key>,Value extends Comparable<Value>> extends OrderSymbolTable{
    private Key[] keys;
    private Value[] vals;
    private int N;
    public BinarySearchST(int capacity) {
        keys=(Key[])new Comparable[capacity];
        vals=(Value[])new Object[capacity];
    }
    @Override
    protected Comparable min() {
        // TODO Auto-generated method stub
        return keys[0];
    }
    @Override
    protected Comparable max() {
        // TODO Auto-generated method stub
        return keys[N-1];
    }
    @Override
    protected Comparable floor(Comparable key) {
        // TODO Auto-generated method stub
        return null;
    }
    @Override
    protected Comparable ceiling(Comparable key) {
        // TODO Auto-generated method stub
        int i=rank(key);
        return keys[i];
    }
    @Override
    protected int rank(Comparable key) {
        // TODO Auto-generated method stub
        int lo=0;
        int hi=N-1;
        while(lo<=hi){
            int mid=lo+(hi-lo)/2;
            int cmp=key.compareTo(keys[mid]);
            if(cmp<0){
                hi=mid-1;
            }
            else if(cmp>0){
                lo=mid-1;
            }
            else{
                return mid;
            }
        }
        return lo;
    }
    @Override
    protected Comparable Select(int k) {
        // TODO Auto-generated method stub
        return keys[k];
    }
    @Override
    protected Iterable keys(Comparable lo, Comparable hi) {
        // TODO Auto-generated method stub
        Queue<Key> q=new Queue<Key>();
        for(int i=rank(lo);i<rank(hi);i++){
            q.enqueue(keys[i]);
        }
        if(contains(hi)){
            q.enqueue(keys[rank(hi)]);
        }
        return (Iterable) q;
    }
    @Override
    protected int size() {
        // TODO Auto-generated method stub
        return N;
    }
    @Override
    protected Object get(Object key) {
        // TODO Auto-generated method stub
        if(isEmpty()){
            return null;
        }
        int i=rank((Comparable) key);
        if(i<N&&keys[i].compareTo((Key) key)==0){
            return vals[i];
        }
        else{
            return null;
        }
    }
    @Override
    protected void put(Object key, Object val) {
        // TODO Auto-generated method stub
        int i=rank((Comparable) key);
        if(i<N&&keys[i].compareTo((Key) key)==0){
            vals[i]=(Value) val;
            return;
        }
        for(int j=N;j>i;j--){
            keys[j]=keys[j-1];
            vals[j]=vals[j-1];
        }
        keys[i]=(Key) key;
        vals[i]=(Value) val;
        N++;
    }
}
