package nonlinear;

import basic.structure.node.TreeNode;
import utils.AlgorithmsUtils;

public class 二叉树的镜像{
    public static void main(String[] args) {
        Integer[] a={4,2,7,1,3,6,9};
        TreeNode<Integer, Integer> root=AlgorithmsUtils.buildTree(a,a);
        mirrorTree(root);
    }
    public static TreeNode<Integer, Integer> mirrorTree(TreeNode<Integer, Integer> root) {
        if(root==null){
            return null;
        }
        TreeNode<Integer, Integer> left=mirrorTree(root.left);
        TreeNode<Integer, Integer> right=mirrorTree(root.right);
        root.left=right;
        root.right=left;
        return root;
    }
}
