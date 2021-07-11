package Structure;

import java.util.HashSet;
import java.util.List;

import Structure.Interface.ITree;
import Structure.Node.TRnode;

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
        father.nodenumber=father.child.size();
    }

    @Override
    public void Delete(TRnode<T> delchild,TRnode<T> father) {
        // TODO Auto-generated method stu
        if(father!=null&&!father.data.saveData.equals(root.data.saveData)){ //如果是非根节点的子节点要被删除,执行这句
            father.child.remove(delchild);
            father.nodenumber=father.child.size();
            System.out.println("节点已经删除");
        }
        else if(father!=null&&father.data.saveData.equals(root.data.saveData)){ //如果是根节点中的子节点,将根节点中的子节点中的节点复制给根节点
            for (TRnode<T> child:root.child) {
                if(child.data.saveData.equals(delchild.data.saveData)){
                    List<TRnode<T>> temp=child.child;
                    root.child.remove(child);
                    for (TRnode<T> tRnode : temp) {
                        tRnode.depth--;
                        root.child.add(tRnode);
                    }
                    father.nodenumber=father.child.size();
                    System.out.println("节点已经删除");
                    return;
                }
            }
        }
        else{ //如果是根节点被删除,则取根节点中第一个节点为根节点并将除第一个节点以为的节点复制成为第一个节点的子树
            TRnode<T> node=root.child.listIterator().next();
            node.depth=0;
            for(TRnode<T> child:root.child){
                child.father=node;
                child.depth++;
                node.child.add(child);
            }
            root=node;
            root.nodenumber=root.child.size();
            System.out.println("节点已经删除");
        }

    }

    @Override
    public void Show(TRnode<T> node) { //深度优先搜索
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
    private TRnode<T> show(T data){ //用于查找树中节点的深度优先搜索,返回查找到的节点,若无,则返回null
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
    public TRnode<T> Search(T data) { //查找节点
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
