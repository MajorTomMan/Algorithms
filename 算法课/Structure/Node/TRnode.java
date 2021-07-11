package Structure.Node;

import java.util.List;

public class TRnode<T>{
    public Data<T> data;
    public int depth;
    public int nodenumber;
    public TRnode<T> father;
    public List<TRnode<T>> child;
    public TRnode(Data<T> data,TRnode<T> father, List<TRnode<T>> child) {
        this.data = data;
        this.father = father;
        this.child = child;
    }
}
