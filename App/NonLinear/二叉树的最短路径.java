package NonLinear;

import java.util.Random;

import Basic.Structure.BRTree;
import Basic.Structure.Node.Treenode;

public class 二叉树的最短路径 {
    public static void main(String[] args) {
        BRTree<Integer> tree=new BRTree<Integer>();
        System.out.println("-----------------------raw data--------------------");
        tree.put(2);
        tree.put(3);
        tree.put(4);
        tree.put(5);
        tree.put(6);
        System.out.println();
        System.out.println(minDepth(tree.getRoot()));
    }
    public static int minDepth(Treenode<Integer> root) {
        return ShortestPath(root);
    }
    public static int ShortestPath(Treenode<Integer> node){
        if(node==null){
            return 0;
        }
        int leftHeigh=ShortestPath(node.Left);
        int rightHeigh=ShortestPath(node.Right);
        if(leftHeigh==-1||rightHeigh==-1){
            return -1;
        }
        return Math.min(leftHeigh, rightHeigh)<2?Math.min(leftHeigh, rightHeigh)+1:-1;
    }
}
