
package NonLinear;

import basic.structure.node.TreeNode;

/**
 * 路径总和
 */
public class 路径总和 extends Common {
    public static void main(String[] args) {
        Integer[] nums = {1,2};
        TreeNode<Integer> root = buildTree(nums);
        printTree(root);
        System.out.println(hasPathSum(root, 1));
    }

    public static boolean hasPathSum(TreeNode<Integer> root, int targetSum) {
        if(root==null){
            return false;
        }
        targetSum-=root.data;
        if(root.left==null&&root.right==null){
            return targetSum==0;
        }
        return hasPathSum(root.left,targetSum)||hasPathSum(root.right,targetSum);
    }
}