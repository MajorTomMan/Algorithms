
package character.datacompression;

import sort.structure.pq.MinPQ;

public class Huffman {
    private final static int R=256;
    private Node root;
    private static String[] st;
    private static class Node implements Comparable<Node> {
        private final char ch;         //字符
        private final int freq;        //每个结点保存以该结点为根的子树中的字符数量
        private final Node left, right;
        private Node(char ch, int freq, Node left, Node right) {
            this.ch    = ch;
            this.freq  = freq;
            this.left  = left;
            this.right = right;
        }
        private boolean isLeaf() {
            return (left == null) && (right == null);
        }
        public int compareTo(Node that) {
            return this.freq - that.freq;
        }
    }
    public Huffman() {
    }
    public void createTree(char[] data){
        int[] freq=new int[R];
        for (int index = 0; index < data.length; index++) {
            freq[data[index]]++;
        }
        root=buildTree(freq);
        st=new String[R];
        buildCode(st,root,"");
    }
    private static Node buildTree(int[] freq){
        MinPQ<Node> pq=new MinPQ<>();
        for (char c=0;c<R; c++) {
            if(freq[c]>0){
                pq.insert(new Node(c,freq[c],null,null));
            }
        }
        while(pq.size()>1){
            Node x=pq.delMin();
            Node y=pq.delMin();
            Node parent=new Node('\0',x.freq+y.freq,x,y);
            pq.insert(parent);
        }
        return pq.delMin();
    }
    private static void buildCode(String[] data,Node x,String s) {
        if(x.isLeaf()){
            st[x.ch]=s;
            return;
        }
        buildCode(data, x.left, s+'0');
        buildCode(data, x.right, s+'1');
    }
    public void printTree() {
        printTree(root);
    }
    private void printTree(Node x){
        if(x.isLeaf()){
            System.out.println("data:"+x.ch+" freq:"+st[x.ch]);
            return;
        }
        printTree(x.left);
        printTree(x.right);
    }
}
