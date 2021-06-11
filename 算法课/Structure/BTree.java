package Structure;

public class BTree<T> implements IBTree<T>{
    private BTnode<T> root;
    @Override
    public void InitializeBTree(BTnode<T> node) {
        // TODO Auto-generated method stub
        root=node;
    }

    @Override
    public boolean BTreeIsEmpty() {
        // TODO Auto-generated method stub
        if(root==null){
            return true;
        }
        return false;
    }

    @Override
    public int BTreeItemCount() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public void Insert(BTnode<T> node,String choose) {
        // TODO Auto-generated method stub
        if(BTreeIsEmpty()){
            InitializeBTree(node);
            return;
        }
        BTnode<T> temp=root;
        while(true){
            if(choose.equals("left")&&temp.Left==null){
                temp.Left=node;
                break;
            }
            else if(choose.equals("left")&&temp.Left!=null){
                temp=temp.Left;
                continue;
            }
            else if(choose.equals("right")&&temp.Right==null){
                temp.Right=node;
                break;
            }
            else{
                temp=temp.Right;
                continue;
            }
        }
    }

    @Override
    public void Delete(T data) {
        // TODO Auto-generated method stub
        return;
    }

    @Override
    public void SearchAll_M(BTnode<T> node) {
        // TODO Auto-generated method stub
        if(node==null){
            return;
        }
        SearchAll_M(node.Left);
        System.out.println(node.item.saveData);
        SearchAll_M(node.Right);
    }
    public BTnode<T> getRoot() {
        return root;
    }

    public void setRoot(BTnode<T> root) {
        this.root = root;
    }
}
