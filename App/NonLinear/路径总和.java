package NonLinear;

import Basic.Structure.Node.Treenode;

/**
 * 路径总和
 */
public class 路径总和 extends Example {
    public static void main(String[] args) {
        Integer[] nums = {1,2};
        Treenode<Integer> root = buildTree(nums);
        printTree(root);
        System.out.println(hasPathSum(root, 1));
    }

    public static boolean hasPathSum(Treenode<Integer> root, int targetSum) {
        if(root==null){
            return false;
        }
        targetSum-=root.data;
        if(root.Left==null&&root.Right==null){
            return targetSum==0;
        }
        return hasPathSum(root.Left,targetSum)||hasPathSum(root.Right,targetSum);
    }
}