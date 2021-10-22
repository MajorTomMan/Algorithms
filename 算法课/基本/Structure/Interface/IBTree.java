package 基本.Structure.Interface;

import 基本.Structure.Node.BTnode;

public interface IBTree<T>{
    void InitializeBTree(BTnode<T> node);
    boolean BTreeIsEmpty();
    int BTreeItemCount();
    void Insert(BTnode<T> node,String choose);
    void Delete(T data);
    void SearchAll_M(BTnode<T> node);
}
