package 基本.Structure.Interface;

import 基本.Structure.Node.BTnode;

public interface IBRTree<T>{
    void InitializeBRTree(BTnode<T> node);
    boolean BRTreeIsEmpty();
    boolean BRTreeIsFull();
    int BRTreeItemCount();
    boolean Insert(BTnode<T> node);
    boolean Delete(T data);
    //void BRTree_Traverse(Method method);
    BTnode<T> FindMax(BTnode<T> node);
    BTnode<T> FindMin(BTnode<T> node);
    void SearchAll_M(BTnode<T> node);
    void SearchAll_P(BTnode<T> node);
    void SearchAll_R(BTnode<T> node);
}
