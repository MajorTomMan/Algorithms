package nonlinear;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import basic.structure.BinaryTree;
import basic.structure.node.TreeNode;

public class 二叉树的中序遍历 extends Common {
    public static void main(String[] args) {
        Integer[] nums = new Integer[]{1,2,3,4,5,6,7,8,9,10};
        TreeNode<Integer> tree = buildTree(nums);
        System.out.println(inorderTraversal(tree));

    }
    public static List<Integer> inorderTraversal(TreeNode<Integer> root) {
        List<Integer> list=new ArrayList<>();
        inorderTraversal(root,list);
        return list;
    }
    private static TreeNode<Integer> inorderTraversal(TreeNode<Integer> node,List<Integer> list) {
        if(node==null){
            return node;
        }
        inorderTraversal(node.left, list);
        list.add(node.data);
        inorderTraversal(node.right, list);
        return node;
    }
}
