package Structure;

import java.util.List;

public class Tree<T> implements ITree<T>{
    private TRnode<T> root;
    private int depth;
    @Override
    public boolean TreeIsEmpty() {
        // TODO Auto-generated method stub
        if(root==null){
            return true;
        }
        return false;
    }

    @Override
    public int TreeItemCount() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public void Insert(TRnode<T> child,TRnode<T> father) {
        // TODO Auto-generated method stub
        if(TreeIsEmpty()){
            setRoot(child);
            return;
        }
        if(father.child.size()==0){
            depth++;
        }
        child.depth=depth;
        child.father=father;
        father.child.add(child);
    }

    @Override
    public void Delete(TRnode<T> delchild,TRnode<T> father) {
        // TODO Auto-generated method stub
        father.child.remove(delchild.data.saveData);
        System.out.println("节点已经删除");
    }

    @Override
    public void Show(TRnode<T> node) {
        // TODO Auto-generated method stub
        if(node==null){
            return;
        }
    }
    public void getFather(TRnode<T> node){
        System.out.println(node.father.data.saveData);
    }
    public void getchild(TRnode<T> father){
        for (TRnode<T> child: father.child) {
            System.out.println("--"+child.data.saveData);
        }
    }
    public TRnode<T> Search(T data){
        TRnode<T> temp=root;
        TRnode<T> result=null;
        List<TRnode<T>> childs=temp.child;
        for (TRnode<T> child : childs) {
            result=childSearch(child, data);
            if(result!=null){
                return result;
            }
        }
        return result;
    }
    private TRnode<T> childSearch(TRnode<T> childnTRnode,T data){
        TRnode<T> result=null;
        if(childnTRnode.child.size()==0){
            return null;
        }
        childSearch(childnTRnode.child.iterator().next(),data);
        if(childnTRnode.data.saveData.equals(data)){
            return result;
        }
        return null;
    }
    public TRnode<T> getRoot() {
        return root;
    }
    public void setRoot(TRnode<T> root) {
        this.root = root;
    }
}
