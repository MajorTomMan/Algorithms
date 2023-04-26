/*
 * @Date: 2023-04-26 16:51:29
 * @LastEditors: hujunhao hujunhao@rtczsz.com
 * @LastEditTime: 2023-04-26 17:58:32
 * @FilePath: /alg/App/NonLinear/平衡二叉树.java
 */
package NonLinear;

import basic.structure.BinaryTree;
import basic.structure.node.TreeNode;

public class 平衡二叉树 extends Example{
    public static void main(String[] args) {
        BinaryTree<Integer> Tree=new BinaryTree<>();
        Tree.setRoot(buildTreeByRandom(12));
        System.out.println(isBalanced(Tree.getRoot()));
    }
    public static boolean isBalanced(TreeNode<Integer> root) {
        return height(root)!=-1;
    }

    public static int height(TreeNode<Integer> node) {
        if(node==null){
            return 0;
        }
        int leftHeigh=height(node.Left);
        int rightHeigh=height(node.Right);
        if(leftHeigh==-1||rightHeigh==-1){
            return -1;
        }
        return Math.abs(leftHeigh-rightHeigh)<2?Math.max(leftHeigh, rightHeigh)+1:-1;
    }
}
