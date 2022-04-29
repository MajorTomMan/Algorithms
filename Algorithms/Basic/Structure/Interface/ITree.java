package Basic.Structure.Interface;

import Basic.Structure.Node.TRnode;

public interface ITree<T> extends Example<T>{
    void Insert(TRnode<T> child,TRnode<T> father);
    void Delete(TRnode<T> delchild,TRnode<T> father);
}