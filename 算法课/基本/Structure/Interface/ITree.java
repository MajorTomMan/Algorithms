package 基本.Structure.Interface;

import 基本.Structure.Node.TRnode;

public interface ITree<T>{
    boolean TreeIsEmpty();
    int TreeItemCount();
    void Insert(TRnode<T> child,TRnode<T> father);
    void Delete(TRnode<T> delchild,TRnode<T> father);
    void Show(TRnode<T> node);
}