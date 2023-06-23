package nonlinear;

import java.util.Random;

import basic.structure.BinaryTree;
import basic.structure.node.TreeNode;

public class 轴对称树 {
    public static void main(String[] args) {
        int i=0;
        BinaryTree<Integer> Tree=new BinaryTree<Integer>();
        Random random=new Random();
        Tree.put(2);
        Tree.put(2);
        Tree.put(3);
        Tree.put(4);
        Tree.put(3);
        Tree.put(4);
        System.out.println();
    }
    public static boolean isSymmetric(TreeNode<Integer> root) {
        return isSymmetric(root.left, root.right);
    }
    private static boolean isSymmetric(TreeNode<Integer> left,TreeNode<Integer> right) {
        if(left==null||right==null){
            return false;
        }
        if(left.data==right.data||left==null&&right==null){
            return true;
        }
        return isSymmetric(left.left, right.right)&&isSymmetric(left.right, right.left);
    }
}
