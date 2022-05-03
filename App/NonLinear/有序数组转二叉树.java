package NonLinear;

import Basic.Structure.Node.Treenode;

public class 有序数组转二叉树 {
    public static void main(String[] args) {
        int[] nums={-10,-3,0,5,9};
        System.out.println(sortedArrayToBST(nums));
    }
    public static Treenode<Integer> sortedArrayToBST(int[] nums) {
        Treenode<Integer> root=null;
        for(int low=0,high=nums.length-1;low<=high;low++,high--){
            int middle=low+(high-low)/2;
            if(root==null){
                root=new Treenode<Integer>(nums[middle],null,null);
            }else{
                rebuild(root,nums[middle]);
            }
        }
        return root;
    }
    public static Treenode<Integer> rebuild(Treenode<Integer> node,int data){
        if(node==null){
            return new Treenode<Integer>(data,null,null);
        }
        if(data>node.data){
            node.Right=rebuild(node.Right, data);
        }else if(data<node.data){
            node.Left=rebuild(node.Left, data);
        }
        return node;
    }
}
