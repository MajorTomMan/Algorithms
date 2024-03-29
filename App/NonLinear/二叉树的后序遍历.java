package nonlinear;

import java.util.ArrayList;
import java.util.List;

import basic.structure.node.TreeNode;

public class 二叉树的后序遍历 extends Common{
    public static void main(String[] args) {
        Integer[] nums={1,null,2,3};
        TreeNode<Integer> root=buildTree(nums);
        System.out.println(postTraversal(root));
    }
    public static List<Integer> postTraversal(TreeNode<Integer> root) {
        List<Integer> list=new ArrayList<>();
        postTraversal(root,list);
        return list;
    }
    public static void postTraversal(TreeNode<Integer> root,List<Integer> list) {
        if(root==null){
            return;
        }
        postTraversal(root.left,list);
        postTraversal(root.right,list);
        list.add((Integer) root.data);
    }
}
