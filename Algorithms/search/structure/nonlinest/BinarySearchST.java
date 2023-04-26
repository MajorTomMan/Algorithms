package search.structure.nonlinest;

import basic.structure.Queue;
import search.structure.example.OrderSymbolTable;

public class BinarySearchST<Key extends Comparable<Key>,Value extends Comparable<Value>> extends OrderSymbolTable<Key,Value>{
    private Key[] keys;
    private Value[] vals;
    private int N;
    public BinarySearchST(int capacity) {
        keys=(Key[])new Comparable[capacity];
        vals=(Value[])new Comparable[capacity];
    }
    @Override
    public Key min() {
        // TODO Auto-generated method stub
        return keys[0];
    }
    @Override
    public Key max() {
        // TODO Auto-generated method stub
        return keys[N-1];
    }
    @Override
    public Key floor(Key key) {
        // TODO Auto-generated method stub
        int lo=rank(key);
        return keys[lo-1];
    }
    @Override
    public Key ceiling(Key key) {
        // TODO Auto-generated method stub
        int i=rank(key);
        return keys[i];
    }
    @Override
    public int rank(Key key) {
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
                lo=mid+1;
            }
            else{
                return mid;
            }
        }
        return lo;
    }
    @Override
    public Key select(int k) {
        // TODO Auto-generated method stub
        return keys[k];
    }
    @Override
    public Iterable<Key> keys(Key lo, Key hi) {
        // TODO Auto-generated method stub
        Queue<Key> q=new Queue<Key>();
        for(int i=rank(lo);i<rank(hi);i++){
            q.enqueue(keys[i]);
        }
        if(contains(hi)){
            q.enqueue(keys[rank(hi)]);
        }
        return q;
    }
    @Override
    public int size() {
        // TODO Auto-generated method stub
        return N;
    }
    @Override
    public Value get(Key key) {
        // TODO Auto-generated method stub
        if(isEmpty()){
            return null;
        }
        int i=rank(key);
        if(i<N&&keys[i].compareTo(key)==0){
            return vals[i];
        }
        else{
            return null;
        }
    }
    @Override
    public void put(Key key,Value val) {
        // TODO Auto-generated method stub
        int i=rank(key);
        if(i<N&&keys[i].compareTo((Key) key)==0){
            vals[i]=(Value) val;
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
    public void delete(Key key) {
        // TODO Auto-generated method stub
        int i=rank(key);
        vals[i]=null;
        keys[i]=null;
    }
}
