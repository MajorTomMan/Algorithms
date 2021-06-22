package Structure;

import java.util.List;

public class TRnode<T>{
    public Data<T> data;
    public int depth;
    public TRnode<T> father;
    public List<TRnode<T>> child;
}
