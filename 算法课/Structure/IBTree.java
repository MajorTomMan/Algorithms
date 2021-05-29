package Structure;

public interface IBTree<T>{
    void InitializeBTree(BTrnode<T> node);
    boolean BTreeIsEmpty();
    boolean BTreeIsFull();
    int BTreeItemCount();
    boolean EnBRTree(BTrnode<T> node);
    boolean DeBRTree(BTrnode<T> node);
    //void BTree_Traverse(Method method);
    BTrnode<T> FindMax(BTrnode<T> node);
    BTrnode<T> FindMin(BTrnode<T> node);
    void SearchAll_M(BTrnode<T> node);
    void SearchAll_P(BTrnode<T> node);
    void SearchAll_R(BTrnode<T> node);
    void SearchAll_M_noFeedBack(BTrnode<T> node);
    void SearchAll_P_noFeedBack(BTrnode<T> node);
    void SearchAll_R_noFeedBack(BTrnode<T> node);
    void SearchAll_L_noFeedBack(BTrnode<T> node);
}
