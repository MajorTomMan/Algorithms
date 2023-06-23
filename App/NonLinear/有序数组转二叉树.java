package nonlinear;

import basic.structure.node.TreeNode;

public class 有序数组转二叉树 {
    public static void main(String[] args) {
        int[] nums={-10,-3,0,5,9};
        System.out.println(sortedArrayToBST(nums));
    }
    public static TreeNode<Integer> sortedArrayToBST(int[] nums) {
        TreeNode<Integer> root=null;
        for(int low=0,high=nums.length-1;low<=high;low++,high--){
            int middle=low+(high-low)/2;
            if(root==null){
                root=new TreeNode<Integer>(nums[middle],null,null);
            }else{
                rebuild(root,nums[middle]);
            }
        }
        return root;
    }
    public static TreeNode<Integer> rebuild(TreeNode<Integer> node,int data){
        if(node==null){
            return new TreeNode<Integer>(data,null,null);
        }
        if(data>node.data){
            node.right=rebuild(node.right, data);
        }else if(data<node.data){
            node.left=rebuild(node.left, data);
        }
        return node;
    }
}
