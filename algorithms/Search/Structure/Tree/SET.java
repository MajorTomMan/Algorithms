package Search.Structure.Tree;

import java.util.TreeSet;

public class SET<Key extends Comparable<Key>>{
    private TreeSet<Key> set;
    public SET(){
        set=new TreeSet<>();
    }
    public void add(Key key){
        set.add(key);
    }
    public void delete(Key key){
        if(set.contains(key)){
            set.remove(key);
        }
    }
    public boolean contains(Key key){
        return set.contains(key);
    }
    public boolean isEmpty(){
        return set.isEmpty();
    }
    public int size(){
        return set.size();
    }
    public String toString(){
        return set.toString();
    }
}
