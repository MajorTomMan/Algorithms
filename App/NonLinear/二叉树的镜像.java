/*
 * @Date: 2023-04-26 16:51:29
 * @LastEditors: hujunhao hujunhao@rtczsz.com
 * @LastEditTime: 2023-04-26 17:58:45
 * @FilePath: /alg/App/NonLinear/二叉树的镜像.java
 */
package NonLinear;

import basic.structure.node.TreeNode;

public class 二叉树的镜像 extends Example {
    public static void main(String[] args) {
        Integer[] a={4,2,7,1,3,6,9};
        TreeNode<Integer> root=buildTree(a);
        printTree(root);
        mirrorTree(root);
        printTree(root);
    }
    public static TreeNode<Integer> mirrorTree(TreeNode<Integer> root) {
        if(root==null){
            return null;
        }
        TreeNode<Integer> left=mirrorTree(root.Left);
        TreeNode<Integer> right=mirrorTree(root.Right);
        root.Left=right;
        root.Right=left;
        return root;
    }
}
