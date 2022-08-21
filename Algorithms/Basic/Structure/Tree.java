package Basic.Structure;


import Basic.Structure.Interface.ITree;
import Basic.Structure.Node.TRnode;

public class Tree<T> implements ITree<T> {
    private TRnode<T> root;

    @Override
    public boolean isEmpty() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public int Size() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public T get(T data) {
        // TODO Auto-generated method stub
        return null;
    }

    public void put(T data, int depth) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void put(T data) {
        // TODO Auto-generated method stub
        
    }
}
