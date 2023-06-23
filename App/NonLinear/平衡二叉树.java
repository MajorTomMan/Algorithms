package nonlinear;

import basic.structure.BinaryTree;
import basic.structure.node.TreeNode;

public class 平衡二叉树 extends Common{
    public static void main(String[] args) {
        BinaryTree<Integer> Tree=new BinaryTree<>();
    }
    public static boolean isBalanced(TreeNode<Integer> root) {
        return height(root)!=-1;
    }

    public static int height(TreeNode<Integer> node) {
        if(node==null){
            return 0;
        }
        int leftHeigh=height(node.left);
        int rightHeigh=height(node.right);
        if(leftHeigh==-1||rightHeigh==-1){
            return -1;
        }
        return Math.abs(leftHeigh-rightHeigh)<2?Math.max(leftHeigh, rightHeigh)+1:-1;
    }
}
