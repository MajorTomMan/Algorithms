package search.structure.linearst;

import java.util.LinkedList;
import java.util.Queue;

import search.structure.example.OrderSymbolTable;

public class OrderedSequentialSearchST<Key,Value extends Comparable<Value>> extends OrderSymbolTable<Key,Value>{
    private SequentialSearchST<Key,Value> Linked;

    public OrderedSequentialSearchST() {
        Linked=new SequentialSearchST<>();
    }
    @Override
    public Key min() {
        // TODO Auto-generated method stub
        Linked.sort();
        Key data=null;
        for(Key key:Linked.keys()){
            if(!Linked.isEmpty()){
                data=key;
            }
        }
        return data;
    }

    @Override
    public Key max() {
        // TODO Auto-generated method stub
        Linked.sort();
        Key data=null;
        for(Key key:Linked.keys()){
            data=key;
            break;
        }
        return data;
    }

    @Override
    public Key floor(Key key) {
        // TODO Auto-generated method stub
        Linked.sort();
        Key data=null;
        for (Key k:Linked.keys()) {
            if(k.equals(key)){
                break;
            }
            else{
                data=k;
                continue;
            }
        }
        return data;
    }

    @Override
    public Key ceiling(Key key) {
        // TODO Auto-generated method stub
        Linked.sort();
        Key data=null;
        int flag=0;
        for (Key k:Linked.keys()) {
            if(k.equals(key)){
                flag=1;
            }
            if(flag==1){
                data=k;
                break;
            }
        }
        return data;
    }

    @Override
    public int rank(Key key) {
        // TODO Auto-generated method stub
        Linked.sort();
        int counter=0;
        for (Key k : Linked.keys()) {
            if(k.equals(key)){
                break;
            }
            counter++;
        }
        return counter;
    }

    @Override
    public Key select(int k) {
        // TODO Auto-generated method stub
        Linked.sort();
        int i=0;
        Key data=null;
        for (Key key :Linked.keys()) {
            if(i==k){
                data=key;
                break;
            }
            i++;
        }
        return data;
    }

    @Override
    public Iterable<Key> keys(Key lo, Key hi) {
        // TODO Auto-generated method stub
        Queue<Key> queue = new LinkedList<>();
        for (Key key : Linked.keys()) {
            queue.add(key);
        }
        return queue;
    }

    @Override
    public int size() {
        // TODO Auto-generated method stub
        return Linked.size();
    }

    @Override
    public void delete(Key key) {
        // TODO Auto-generated method stub
        Linked.delete(key);
        Linked.sort();
    }

    @Override
    public void put(Key key, Value val) {
        // TODO Auto-generated method stub
        Linked.put(key, val);
    }

    @Override
    public Value get(Key key) {
        // TODO Auto-generated method stub
        Linked.sort();
        return Linked.get(key);
    }
    
}
