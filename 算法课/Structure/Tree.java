package Structure;

import java.util.HashSet;
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
        // TODO Auto-generated method stu
        if(father!=null&&!father.data.saveData.equals(root.data.saveData)){
            father.child.remove(delchild);
            System.out.println("节点已经删除");
        }
        else if(father!=null&&father.data.saveData.equals(root.data.saveData)){
            for (TRnode<T> child:root.child) {
                if(child.data.saveData.equals(delchild.data.saveData)){
                    List<TRnode<T>> temp=child.child;
                    root.child.remove(child);
                    for (TRnode<T> tRnode : temp) {
                        tRnode.depth--;
                        root.child.add(tRnode);
                    }
                    System.out.println("节点已经删除");
                    return;
                }
            }
        }
        else{
            TRnode<T> node=root.child.listIterator().next();
            node.depth=0;
            for(TRnode<T> child:root.child){
                child.father=node;
                child.depth++;
                node.child.add(child);
            }
            root=node;
            System.out.println("节点已经删除");
        }

    }

    @Override
    public void Show(TRnode<T> node) {
        // TODO Auto-generated method stub
        if (node == null) {
            return;
        }
        Stack<TRnode<T>> stack = new Stack<>();
        HashSet<TRnode<T>> set = new HashSet<>();
        stack.push(node);
        set.add(node);
        System.out.println(node.data.saveData);
        
        while (!stack.isEmpty()) {
            //弹栈获得一个节点
            TRnode<T> cur = stack.pop();
            //查看这个节点的所有孩子
            for (TRnode<T> next : cur.child) {
                //如果有孩子是之前没有遍历到的，说明这个节点没有深度遍历完
                if (!set.contains(next)) {
                    //此节点与其孩子加入栈与Set中
                    stack.push(cur);
                    stack.push(next);
                    set.add(next);
                    System.out.println(next.data.saveData);
                    break;
                }
            }
        }
    }
    private TRnode<T> show(T data){
        if (root== null) {
            return null;
        }
        Stack<TRnode<T>> stack = new Stack<>();
        HashSet<TRnode<T>> set = new HashSet<>();
        stack.push(root);
        set.add(root);
        if(root.data.saveData.equals(data)){
            return root;
        } 
        while (!stack.isEmpty()) {
            //弹栈获得一个节点
            TRnode<T> cur = stack.pop();
            //查看这个节点的所有孩子
            for (TRnode<T> next : cur.child) {
                if(next.data.saveData.equals(data)){
                    return next;
                }
                //如果有孩子是之前没有遍历到的，说明这个节点没有深度遍历完
                else if (!set.contains(next)) {
                    //此节点与其孩子加入栈与Set中
                    stack.push(cur);
                    stack.push(next);
                    set.add(next);
                    break;
                }
            }
        }
        return null;
    }
    public void getFather(TRnode<T> node){
        System.out.println(node.father.data.saveData);
    }
    public void getchild(TRnode<T> father){
        for (TRnode<T> child: father.child) {
            System.out.println("--"+child.data.saveData);
        }
    }
    public TRnode<T> Search(T data) {
        System.out.println("要删除的节点是:"+data);
        TRnode<T> result=show(data);
        return result;
    }
    public TRnode<T> getRoot() {
        return root;
    }
    public void setRoot(TRnode<T> root) {
        this.root = root;
    }
}
