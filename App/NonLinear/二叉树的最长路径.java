
package NonLinear;

import basic.structure.BinaryTree;
import basic.structure.node.TreeNode;

public class 二叉树的最长路径 extends Common{
    public static void main(String[] args) {
        BinaryTree<Integer> Tree=new BinaryTree<Integer>();
        Tree.put(3);
        Tree.put(9);
        Tree.put(20);
        Tree.put(15);
        Tree.put(7);
    }
    public static int maxDepth(TreeNode<Integer> root) {
        return longestPath(root);
    }
    public static int longestPath(TreeNode<Integer> node){
        if(node==null){
            return 0;
        }
        else{
            int leftHeight = longestPath(node.left);
            int rightHeight = longestPath(node.right);
            return Math.max(leftHeight, rightHeight) + 1;
        }
    }
}
