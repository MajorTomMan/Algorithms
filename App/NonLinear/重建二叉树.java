package NonLinear;

import java.util.HashMap;

import Search.Structure.Hash.SeparateChainingHashST;

public class 重建二叉树 {
    private static HashMap<Integer, Integer> indexMap;

    private static class TreeNode {
        int val;
        TreeNode left;
        TreeNode right;

        TreeNode(int x) {
            val = x;
        }

        @Override
        public String toString() {
            return "TreeNode [left=" + left + ", right=" + right + ", val=" + val + "]";
        }

    }

    public static void main(String[] args) {
        int[] preorder = { 3, 9, 20, 15, 7 }, inorder = { 9, 3, 15, 20, 7 };
        System.out.println(rebuild(preorder, inorder));
    }

    public static TreeNode rebuild(int[] preorder, int[] inorder) {
        int n = preorder.length;
        // 构造哈希映射，帮助我们快速定位根节点
        indexMap = new HashMap<>();
        for (int i = 0; i < n; i++) {
            indexMap.put(inorder[i], i); //将中序遍历数组散列
        }
        return rebuild(preorder, inorder, 0, n - 1, 0, n - 1);
    }

    private static TreeNode rebuild(int[] preorder, int[] inorder,
            int preorder_left, int preorder_right,
            int inorder_left, int inorder_right) {
        if (preorder_left > preorder_right) {
            return null;
        }
        int pre_root = preorder_left;
        int in_root = indexMap.get(preorder[pre_root]); //利用先序遍历的根节点当作键来获得中序的根节点索引
        int child_tree_num = in_root - inorder_left;
        TreeNode root = new TreeNode(preorder[pre_root]); //根据先序遍历中的值新建根节点
        root.left = rebuild(preorder, inorder, preorder_left + 1, preorder_left + child_tree_num, inorder_left,
                in_root - 1);
        root.right = rebuild(preorder, inorder, preorder_left + child_tree_num + 1, preorder_right, in_root + 1,
                inorder_right);
        return root;
    }
}
