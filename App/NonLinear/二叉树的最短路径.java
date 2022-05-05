package NonLinear;

import java.util.ArrayList;
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
        if(node.Left==null && node.Right==null){
            return 1;
        }
        if(node.Left!=null){
            return ShortestPath(node.Left)+1;
        }
        if(node.Right!=null){
            return ShortestPath(node.Right)+1;
        }
        return Math.min(ShortestPath(node.Left),ShortestPath(node.Right))+1;
    }
}
