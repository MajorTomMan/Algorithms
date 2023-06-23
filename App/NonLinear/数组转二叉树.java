package nonlinear;

import basic.structure.node.TreeNode;

public class 数组转二叉树 extends Common{
    public static void main(String[] args) {
        Integer[] nums={5,4,8,11,null,13,4,7,2,null,null,null,1};
        buildTree(nums);
        printTree();
    }
    public static TreeNode<Integer> transform(TreeNode<Integer> node,int data){
        if(node==null){
            return new TreeNode<Integer>(data,null,null);
        }
        if(node.left==null){
            node.left=transform(node.left, data);
        }
        else if(node.right==null){
            node.right=transform(node.right, data);
        }
        else{
            node.left=transform(node.left, data);
            node.right=transform(node.right, data);
        }
        return node;
    }
}
