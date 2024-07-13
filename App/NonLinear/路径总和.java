package nonlinear;

import basic.structure.node.TreeNode;
import sort.structure.common.Common;
import utils.AlgorithmsUtils;

/**
 * 路径总和
 */
public class 路径总和 {
    public static void main(String[] args) {
        TreeNode<Integer, Integer> root = AlgorithmsUtils.buildTree(AlgorithmsUtils.randomArray(20, 30),
                AlgorithmsUtils.randomArray(20, 30));
        System.out.println(hasPathSum(root, 1));
    }

    public static boolean hasPathSum(TreeNode<Integer, Integer> root, int targetSum) {
        if (root == null) {
            return false;
        }
        targetSum -= root.value;
        if (root.left == null && root.right == null) {
            return targetSum == 0;
        }
        return hasPathSum(root.left, targetSum) || hasPathSum(root.right, targetSum);
    }
}