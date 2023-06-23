package nonlinear;

import basic.structure.BinaryTree;
import basic.structure.node.TreeNode;

public class 二叉树的最短路径 extends Common {
    public static void main(String[] args) {
        BinaryTree<Integer> tree=new BinaryTree<Integer>();
        System.out.println("-----------------------raw data--------------------");
        tree.put(2);
        tree.put(3);
        tree.put(4);
        tree.put(5);
        tree.put(6);
        System.out.println();
    }
    public static int minDepth(TreeNode<Integer> root) {
        return ShortestPath(root);
    }
    public static int ShortestPath(TreeNode<Integer> node){
        if(node==null){
            return 0;
        }
        if(node.left==null && node.right==null){
            return 1;
        }
        if(node.left!=null){
            return ShortestPath(node.left)+1;
        }
        if(node.right!=null){
            return ShortestPath(node.right)+1;
        }
        return Math.min(ShortestPath(node.left),ShortestPath(node.right))+1;
    }
}
