package nonlinear;

import basic.structure.node.TreeNode;

public class 二叉树的镜像 extends Common {
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
        TreeNode<Integer> left=mirrorTree(root.left);
        TreeNode<Integer> right=mirrorTree(root.right);
        root.left=right;
        root.right=left;
        return root;
    }
}
