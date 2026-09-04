/*
 * @Date: 2023-12-09 14:41:38
 * @LastEditors: MajorTomMan 765719516@qq.com
 * @LastEditTime: 2024-07-13 20:11:56
 * @FilePath: \ALG\app\nonlinear\树的子结构.java
 * @Description: MajorTomMan @版权声明 保留文件所有权利
 */
package com.majortom.algorithms.app.leetcode.ds.tree;

import com.majortom.algorithms.library.basic.tree.BinaryTreeNode;
import com.majortom.algorithms.library.utils.AlgorithmsUtils;

public class 树的子结构 {
    public static void main(String[] args) {
        BinaryTreeNode<Integer> tree_a = AlgorithmsUtils.buildBST(AlgorithmsUtils.randomArray(20, 30));
        BinaryTreeNode<Integer> tree_b = AlgorithmsUtils.buildBST(AlgorithmsUtils.randomArray(20, 30));
        System.out.println(isSubStructure(tree_a, tree_b));
    }

    // 先检查A和B共同的子树根节点,然后递归判断子树结构
    public static boolean isSubStructure(BinaryTreeNode<Integer> A, BinaryTreeNode<Integer> B) {
        if (A == null || B == null) {
            return false;
        }
        if (A.getValue() == B.getValue() && checkSubTree(A, B)) {
            return true;
        }
        return isSubStructure(A.getLeft(), B) || isSubStructure(A.getRight(), B);
    }

    /*
     * 先序遍历判断子树结构,
     * 当B子树为null 代表B树已经遍历完,即可返回true,
     * 若A为null 则代表该子树不是A中子树,
     * 返回false
     */
    public static boolean checkSubTree(BinaryTreeNode<Integer> a, BinaryTreeNode<Integer> b) {
        if (b == null) {
            return true;
        }
        if (a == null) {
            return false;
        }
        return a.getValue() == b.getValue() && checkSubTree(a.getLeft(), b.getLeft()) && checkSubTree(a.getRight(), b.getRight());
    }
}
