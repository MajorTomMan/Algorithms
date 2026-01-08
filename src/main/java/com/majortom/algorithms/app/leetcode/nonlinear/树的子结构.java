/*
 * @Date: 2023-12-09 14:41:38
 * @LastEditors: MajorTomMan 765719516@qq.com
 * @LastEditTime: 2024-07-13 20:11:56
 * @FilePath: \ALG\app\nonlinear\树的子结构.java
 * @Description: MajorTomMan @版权声明 保留文件所有权利
 */
package com.majortom.algorithms.app.leetcode.nonlinear;

import com.majortom.algorithms.core.tree.node.TreeNode;
import com.majortom.algorithms.utils.AlgorithmsUtils;

public class 树的子结构 {
    public static void main(String[] args) {
        TreeNode<Integer> tree_a = AlgorithmsUtils.buildBST(AlgorithmsUtils.randomArray(20, 30));
        TreeNode<Integer> tree_b = AlgorithmsUtils.buildBST(AlgorithmsUtils.randomArray(20, 30));
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

    /*
     * 先序遍历判断子树结构,
     * 当B子树为null 代表B树已经遍历完,即可返回true,
     * 若A为null 则代表该子树不是A中子树,
     * 返回false
     */
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
