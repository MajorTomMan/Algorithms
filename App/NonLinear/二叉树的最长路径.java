/*
 * @Date: 2023-04-26 16:51:29
 * @LastEditors: hujunhao hujunhao@rtczsz.com
 * @LastEditTime: 2023-04-26 17:58:42
 * @FilePath: /alg/App/NonLinear/二叉树的最长路径.java
 */
package NonLinear;

import basic.structure.BinaryTree;
import basic.structure.node.TreeNode;

public class 二叉树的最长路径 extends Example{
    public static void main(String[] args) {
        BinaryTree<Integer> Tree=new BinaryTree<Integer>();
        Tree.put(3);
        Tree.put(9);
        Tree.put(20);
        Tree.put(15);
        Tree.put(7);
        printTree(Tree.getRoot());
        System.out.println(maxDepth(Tree.getRoot()));
    }
    public static int maxDepth(TreeNode<Integer> root) {
        return longestPath(root);
    }
    public static int longestPath(TreeNode<Integer> node){
        if(node==null){
            return 0;
        }
        else{
            int leftHeight = longestPath(node.Left);
            int rightHeight = longestPath(node.Right);
            return Math.max(leftHeight, rightHeight) + 1;
        }
    }
}
