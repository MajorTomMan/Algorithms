package basic.structure;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public class HuffmanTree<T extends Comparable<T>> {
    private Node<T> root;
    private class Node<T> {
        T data;
        int weight;
        int code;
        Node<T> left;
        Node<T> right;

        public Node(T data, int weight, int code, Node<T> left, Node<T> right) {
            this.data = data;
            this.weight = weight;
            this.code = code;
            this.left = left;
            this.right = right;
        }
        public void setCode(int code) {
            this.code = code;
        }
        public int getWeight() {
            return weight;
        }
        @Override
        public String toString() {
            return "Node [code=" + code + ", data=" + data + ", left=" + left + ", right=" + right + ", weight="
                    + weight + "]";
        }
        
    }

    public void buildTree(Map<T, Integer> map) {
        List<Node<T>> list = new ArrayList<>();
        for (T data : map.keySet()) {
            list.add(new Node<T>(data, map.get(data), 0, null, null));
        }
        Collections.sort(list,new Comparator<Node<T>>() {
            @Override
            public int compare(Node<T> o1,Node<T> o2) {
                // TODO Auto-generated method stub
                return o1.getWeight()-o2.getWeight();
            }
        });
        while (list.size() != 1) {
            Node<T> left = list.get(0);
            Node<T> right = list.get(1);
            right.setCode(1);
            Node<T> root = new Node<T>(null, left.getWeight() + right.getWeight(), 0, left, right);
            list.remove(left);
            list.remove(right);
            list.add(root);
        }
        root=list.get(0);
    }

    public void printTree(){
        String code="";
        printNode(root,code);
    }
    private void printNode(Node<T> node,String code){
        if(node==null){ 
            return;
        }
        printNode(node.left,code+node.code);
        if(node.data==null){
            ;
        }else{
            System.out.print("data:"+node.data+" code:"+code);
            System.out.println();
        }
        printNode(node.right,code+node.code);
    }
}
