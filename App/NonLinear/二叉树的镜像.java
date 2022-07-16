package NonLinear;

import Basic.Structure.Node.Treenode;

public class 二叉树的镜像 extends Example {
    public static void main(String[] args) {
        Integer[] a={4,2,7,1,3,6,9};
        Treenode<Integer> root=buildTree(a);
        printTree(root);
        mirrorTree(root);
        printTree(root);
    }
    public static Treenode<Integer> mirrorTree(Treenode<Integer> root) {
        if(root==null){
            return null;
        }
        Treenode<Integer> left=mirrorTree(root.Left);
        Treenode<Integer> right=mirrorTree(root.Right);
        root.Left=right;
        root.Right=left;
        return root;
    }
}
