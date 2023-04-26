/*
 * @Date: 2023-04-26 16:51:29
 * @LastEditors: hujunhao hujunhao@rtczsz.com
 * @LastEditTime: 2023-04-26 17:58:25
 * @FilePath: /alg/App/NonLinear/轴对称树.java
 */
package NonLinear;

import java.util.Random;

import basic.structure.BinaryTree;
import basic.structure.node.TreeNode;

public class 轴对称树 {
    public static void main(String[] args) {
        int i=0;
        BinaryTree<Integer> Tree=new BinaryTree<Integer>(1);
        Random random=new Random();
        Tree.put(2);
        Tree.put(2);
        Tree.put(3);
        Tree.put(4);
        Tree.put(3);
        Tree.put(4);
        System.out.println();
        System.out.println(isSymmetric(Tree.getRoot()));
    }
    public static boolean isSymmetric(TreeNode<Integer> root) {
        return isSymmetric(root.Left, root.Right);
    }
    private static boolean isSymmetric(TreeNode<Integer> left,TreeNode<Integer> right) {
        if(left==null||right==null){
            return false;
        }
        if(left.data==right.data||left==null&&right==null){
            return true;
        }
        return isSymmetric(left.Left, right.Right)&&isSymmetric(left.Right, right.Left);
    }
}
