package Basic.Structure.Node;

import java.util.List;

public class TRnode<T> {
    public T data;
    public int depth;
    public int nodenumber;
    public TRnode<T> father;
    public List<TRnode<T>> child;

    public TRnode(T data, TRnode<T> father, List<TRnode<T>> child) {
        this.data = data;
        this.father = father;
        this.child = child;
    }

    @Override
    public String toString() {
        return "TRnode [child=" + child + ", data=" + data + ", depth=" + depth + ", father=" + father + ", nodenumber="
                + nodenumber + "]";
    }
    
}
