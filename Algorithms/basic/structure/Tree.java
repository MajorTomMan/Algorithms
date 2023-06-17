package basic.structure;


import java.util.ArrayList;
import java.util.List;

import basic.structure.iface.ITree;

public class Tree<T extends Comparable<T>> implements ITree<T> {
    /**
     * Treenode
     */
    private class Node {
        private T data;
        private Node parent;
        private List<Node> children;
        /**
         * @param data
         * @param parent
         * @param children
         */
        public Node(T data) {
            this.data = data;
        }

    }
    private Node root;
    private Integer size;

    /**
     * @param root
     */
    public Tree(T data) {
        this.root = new Node(data);
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
    public int size() {
        // TODO Auto-generated method stub
    }

    @Override
    public void put(T data) {
        // TODO Auto-generated method stub
    }

    @Override
    public void get(T key) {
        // TODO Auto-generated method stub
    }

    @Override
    public void delete(T key) {
        // TODO Auto-generated method stub
    }
}
