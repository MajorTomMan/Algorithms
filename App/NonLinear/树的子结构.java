package nonlinear;

import basic.structure.node.TreeNode;

public class 树的子结构 extends Common {
    public static void main(String[] args) {
        Integer[] a = { 1, 0, 1, -4, -3 }, b = { 1, -4 };
        TreeNode<Integer> tree_a = buildTree(a);
        TreeNode<Integer> tree_b = buildTree(b);
        System.out.println(tree_a);
        System.out.println("-------------------------- test b tree----------------------");
        System.out.println(tree_b);
        System.out.println("-----------------------------------------------------------");
        printTree(tree_a);
        System.out.println("--------------------------b tree----------------------");
        printTree(tree_b);
        System.out.println(isSubStructure(tree_a, tree_b));
    }

    // 先检查A和B共同的子树根节点,然后递归判断子树结构
    public static boolean isSubStructure(TreeNode<Integer> A, TreeNode<Integer> B) {
        if (A == null || B == null) {
            return false;
        }
        if (A.data == B.data && checkSubTree(A, B)) {
            return true;
        }
        return isSubStructure(A.left, B) || isSubStructure(A.right, B);
    }

    // 先序遍历判断子树结构,当B子树为null 代表B树已经遍历完,即可返回true,若A为null 则代表该子树不是A中子树,返回false
    public static boolean checkSubTree(TreeNode<Integer> a, TreeNode<Integer> b) {
        if (b == null) {
            return true;
        }
        if (a == null) {
            return false;
        }
        return a.data == b.data && checkSubTree(a.left, b.left) && checkSubTree(a.right, b.right);
    }
}
