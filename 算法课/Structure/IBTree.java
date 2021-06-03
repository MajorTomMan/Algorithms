package Structure;

public interface IBTree<T>{
    void InitializeBTree(BTRnode<T> node);
    boolean BTreeIsEmpty();
    boolean BTreeIsFull();
    int BTreeItemCount();
    boolean EnBRTree(BTRnode<T> node);
    boolean DeBRTree(T data);
    //void BTree_Traverse(Method method);
    BTRnode<T> FindMax(BTRnode<T> node);
    BTRnode<T> FindMin(BTRnode<T> node);
    void SearchAll_M(BTRnode<T> node);
    void SearchAll_P(BTRnode<T> node);
    void SearchAll_R(BTRnode<T> node);
}
