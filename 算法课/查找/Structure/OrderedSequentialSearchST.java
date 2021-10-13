package 查找.Structure;

import Structure.Node.SequentialSearchST;

public class OrderedSequentialSearchST<Key,Value extends Comparable<Value>> extends OrderSymbolTable<Key,Value>{
    private SequentialSearchST<Key,Value> Linked;

    public OrderedSequentialSearchST() {
        Linked=new SequentialSearchST<>();
    }
    @Override
    public Key min() {
        // TODO Auto-generated method stub
        Linked.sort(Linked.getFirst());
        return Linked.getFirst().getKey();
    }

    @Override
    public Key max() {
        // TODO Auto-generated method stub
        SequentialSearchST<Key,Value> t=Linked;
        t.sort(t.getFirst());
        SequentialSearchST<Key,Value>.Node temp=t.getFirst();
        while(temp.getNext()!=null){
            temp=temp.getNext();
        }
        return temp.getKey();
    }

    @Override
    public Key floor(Key key) {
        // TODO Auto-generated method stub
        SequentialSearchST<Key,Value> t=Linked;
        t.sort(t.getFirst());
        SequentialSearchST<Key,Value>.Node temp=t.getFirst();
        SequentialSearchST<Key,Value>.Node result=null;
        while(!temp.getKey().equals(key)&&temp!=null){
            result=temp;
            temp=temp.getNext();
        }
        return result.getKey();
    }

    @Override
    public Key ceiling(Key key) {
        // TODO Auto-generated method stub
        SequentialSearchST<Key,Value> t=Linked;
        t.sort(t.getFirst());
        SequentialSearchST<Key,Value>.Node temp=t.getFirst();
        while(!temp.getKey().equals(key)&&temp!=null){
            temp=temp.getNext();
        }
        return temp.getNext().getKey();
    }

    @Override
    public int rank(Key key) {
        // TODO Auto-generated method stub
        Linked.sort(Linked.getFirst());
        int counter=0;
        SequentialSearchST<Key,Value>.Node temp=Linked.getFirst();
        while(!temp.getKey().equals(key)&&temp!=null){
            counter++;
            temp=temp.getNext();
        }
        return counter;
    }

    @Override
    public Key select(int k) {
        // TODO Auto-generated method stub
        Linked.sort(Linked.getFirst());
        int counter=0;
        SequentialSearchST<Key,Value>.Node temp=Linked.getFirst();
        while(counter!=k){
            counter++;
            temp=temp.getNext();
        }
        return temp.getKey();
    }

    @Override
    public Iterable<Key> keys(Key lo, Key hi) {
        // TODO Auto-generated method stub
        Linked.keys(Linked.getFirst());
        return null;
    }

    @Override
    public int size() {
        // TODO Auto-generated method stub
        return Linked.size();
    }

    @Override
    public void delete(Key key) {
        // TODO Auto-generated method stub
        SequentialSearchST<Key,Value> t=Linked;
        t.sort(t.getFirst());
        SequentialSearchST<Key,Value>.Node temp=t.getFirst();
        SequentialSearchST<Key,Value>.Node result=null;
        while(!temp.getKey().equals(key)&&temp!=null){
            result=temp;
            temp=temp.getNext();
        }
        result.setNext(temp.getNext());
        temp=null;
    }

    @Override
    public void put(Key key, Value val) {
        // TODO Auto-generated method stub
        Linked.sort(Linked.getFirst());
        Linked.put(key, val);
    }

    @Override
    public Value get(Key key) {
        // TODO Auto-generated method stub
        Linked.sort(Linked.getFirst());
        return Linked.get(key);
    }
    
}
