/*
 * @Date: 2023-04-26 16:51:29
 * @LastEditors: hujunhao hujunhao@rtczsz.com
 * @LastEditTime: 2023-04-26 17:57:07
 * @FilePath: /alg/Algorithms/search/structure/linearst/ArrayST.java
 */
package search.structure.linearst;

import basic.structure.Queue;
import search.structure.example.SymbolTable;

/**
 * ArrayST
 * @param <Value>
 */
public class ArrayST<Key,Value> extends SymbolTable<Key,Value>{
    private Key[] keys;
    private Value[] vals;
    private int N;
    public ArrayST(int capacity) {
        keys=(Key[])new Comparable[capacity];
        vals=(Value[])new Comparable[capacity];
    }
    @Override
    public void put(Key key, Value val) {
        // TODO Auto-generated method stub
        if(contains(key)&&val==null||val.equals(null)){
            int i=search(key);
            keys[i]=keys[i+1];
            vals[i]=vals[i+1];
            return;
        }
        keys[N]=key;
        vals[N]=val;
        N++;
    }
    @Override
    public Value get(Key key) {
        // TODO Auto-generated method stub
        if(isEmpty()){
            return null;
        }
        int i=search(key);
        if(i<N&&keys[i].equals(key)){
            return vals[i];
        }
        else{
            return null;
        }
    }
    @Override
    public int size() {
        // TODO Auto-generated method stub
        return N;
    }
    @Override
    public Iterable<Key> keys() {
        // TODO Auto-generated method stub
        Queue<Key> que=new Queue<>();
        for(int i=0;i<keys.length;i++){
            que.enqueue(keys[i]);
        }
        return (Iterable<Key>) que;
    }
    private int search(Key key){
        int i;
        for(i=0;i!=keys.length;i++){
            if(keys[i]==key){
                break;
            }
        }
        return i;
    }
}