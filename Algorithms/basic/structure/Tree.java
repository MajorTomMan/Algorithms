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
            children = new ArrayList<>();
        }

        public Node(T father, T data) {
            this.data = data;
            Node childNode = new Node(data);
            Node fatherNode = new Node(father);
            childNode.parent = fatherNode;
            fatherNode.children.add(childNode);
        }

    }

    private Node root;
    private Integer size;

    /**
     * @param root
     */
    public Tree(T data) {
        this.root = new Node(data);
    }

    @Override
    public boolean isEmpty() {
        // TODO Auto-generated method stub
        if (root == null) {
            return true;
        }
        return false;
    }

    @Override
    public int size() {
        // TODO Auto-generated method stub
        return size;
    }

    /* 默认插入根节点 */
    @Override
    public void put(T data) {
        // TODO Auto-generated method stub
        if (isEmpty()) {
            root = new Node(data);
        }
        put(root, data);
    }

    private void put(Tree<T>.Node node, T data) {
        Node childNode = new Node(data);
        childNode.parent = node;
        node.children.add(childNode);
    }

    /* 根据父节点来插入子节点 */
    public void put(T father, T child) {
        // TODO Auto-generated method stub
        if (isEmpty()) {
            root = new Node(father, child);
        }
        put(father, child,root);
    }

    private Tree<T>.Node put(T father, T child, Node node) {
        if (node.data.compareTo(father) == 0) {
            node.children.add(new Node(child));
        }
        for (Tree<T>.Node childNode : node.children) {
            Node children = put(father, child, childNode);
            node.children.add(children);
        }
        return node;
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
