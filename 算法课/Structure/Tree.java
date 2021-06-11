package Structure;

public class Tree<T> implements ITree<T>{
    private TRnode<T> root;
    @Override
    public void InitializeTree(TRnode<T> node) {
        // TODO Auto-generated method stub
        root=node;

    }

    @Override
    public boolean TreeIsEmpty() {
        // TODO Auto-generated method stub
        if(root==null){
            return true;
        }
        return false;
    }

    @Override
    public boolean TreeIsFull() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public int TreeItemCount() {
        // TODO Auto-generated method stub
        int count=0;
        TRnode<T> node=root;
        while(node!=null){
            count++;
            node=node.child;
        }
        return count;
    }

    @Override
    public void Insert(TRnode<T> node) {
        // TODO Auto-generated method stub
        TRnode<T> temp=root;
        if(TreeIsEmpty()){
            InitializeTree(node);
            return;
        }
        while(temp.child!=null){
            temp=temp.child;
        }
        node.father=temp;
        temp.child=node;
    }

    @Override
    public void Delete(T data) {
        // TODO Auto-generated method stub

    }

    @Override
    public void Show(TRnode<T> node) {
        // TODO Auto-generated method stub
        if(node==null){
            return;
        }
        Show(node.child);
        System.out.println(node.data.saveData);
    }
    public TRnode<T> getRoot() {
        return root;
    }

    public void setRoot(TRnode<T> root) {
        this.root = root;
    }

}
