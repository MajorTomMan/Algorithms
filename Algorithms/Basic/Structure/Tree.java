package Basic.Structure;


import java.util.ArrayList;
import java.util.List;

import Basic.Structure.Interface.ITree;

public class Tree<T extends Comparable<T>> implements ITree<T> {
    /**
     * Treenode
     */
    private class Treenode<T> {
        private T data;
        private Treenode<T> parent;
        private List<Treenode<T>> children;
        /**
         * @param data
         */
        public Treenode(T data) {
            this.data = data;
            children=new ArrayList<>();
        }
        /**
         * @return the data
         */
        public T getData() {
            return data;
        }
        /**
         * @return the parent
         */
        public Treenode<T> getParent() {
            return parent;
        }
        /**
         * @param parent the parent to set
         */
        public void setParent(Treenode<T> parent) {
            this.parent = parent;
        }
        /**
         * @param data the data to set
         */
        public void setData(T data) {
            this.data = data;
        }
        /**
         * @return the children
         */
        public List<Treenode<T>> getChildren() {
            return children;
        }
        /**
         * @param children the children to set
         */
        public void setChildren(List<Treenode<T>> children) {
            this.children = children;
        }
    }
    private Treenode<T> root;
    private Integer size;

    /**
     * @param root
     */
    public Tree(T data) {
        this.root = new Treenode<>(data);
        root.children=new ArrayList<>();
    }

    @Override
    public boolean isEmpty() {
        // TODO Auto-generated method stub
        if(root==null){
            return true;
        }
        return false;
    }

    @Override
    public int Size() {
        // TODO Auto-generated method stub
        return Size(0,root);
    }
    private int Size(int size,Treenode<T> node){
        if(node==null){
            return 0;
        }
        for (Treenode<T> children: node.getChildren()) {
           Size(size,children);
           ++size;
        }
        return size;
    }
    public Treenode<T> get(T data) {
        // TODO Auto-generated method stub
        return search(data);
    }

    @Override
    public void put(T data) {
        // TODO Auto-generated method stub
        if(isEmpty()){
            root=new Treenode<T>(data);
            return;
        }
        Treenode<T> node=new Treenode<T>(data);
        root.getChildren().add(node);
        node.setParent(root);
    }
    public void put(T data,T parent) {
        // TODO Auto-generated method stub
        if(isEmpty()){
            return;
        }
        Treenode<T> node=search(parent);
        // 如果没有找到已经插入的父节点,则插入根节点上
        if(node==null){
            Treenode<T> datanode=new Treenode<T>(data);
            Treenode<T> parentnode=new Treenode<T>(parent);
            parentnode.getChildren().add(datanode);
            datanode.setParent(parentnode);
            root.getChildren().add(parentnode);
        }
        else{
            //如果有父节点,则插入父节点的子节点中
            Treenode<T> datanode=new Treenode<T>(data);
            datanode.setParent(node);
            node.getChildren().add(datanode);
        }
    }
    
    private Treenode<T> search(T data){
        Queue<Treenode<T>> queue=new Queue<>();
        queue.enqueue(root);
        while(!queue.isEmpty()){
            Treenode<T> node=queue.dequeue();
            for (Treenode<T> children : node.getChildren()) {
                if(children.data.compareTo(data)==0){
                    return children;
                }
                
            }
        }
        return null;
    }
}
