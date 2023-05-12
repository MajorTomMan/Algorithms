/*
 * @Date: 2023-04-26 16:51:29
 * @LastEditors: hujunhao hujunhao@rtczsz.com
 * @LastEditTime: 2023-04-27 09:21:41
 * @FilePath: /alg/App/NonLinear/二叉树的最短路径.java
 */

package NonLinear;

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
        System.out.println(minDepth(tree.getRoot()));
    }
    public static int minDepth(TreeNode<Integer> root) {
        return ShortestPath(root);
    }
    public static int ShortestPath(TreeNode<Integer> node){
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
