package NonLinear;

import java.util.Random;

import Basic.Structure.BRTree;
import Basic.Structure.Node.Treenode;

/**
 * 路径总和
 */
public class 路径总和 {
    public static void main(String[] args) {
        BRTree<Integer> Tree = new BRTree<Integer>();
        System.out.println("-----------------------raw data--------------------");
        Tree.put(5);
        Tree.put(4);
        Tree.put(11);
        Tree.put(7);
        Tree.put(2);
        Tree.put(8);
        Tree.put(13);
        Tree.put(4);
        Tree.put(1);
        System.out.println();
        System.out.println(hasPathSum(Tree.getRoot(), 22));
    }

    public static boolean hasPathSum(Treenode<Integer> root, int targetSum) {
        return hasPathSum(root, targetSum, 0);
    }

    public static boolean hasPathSum(Treenode<Integer> root, int targetSum, int sum) {
        if (root == null) {
            return false;
        }
        if (targetSum == sum) {
            return true;
        }
        hasPathSum(root.Left, targetSum, sum += root.data);
        hasPathSum(root.Right, targetSum, sum += root.data);
        return false;
    }
}