/*
 * @Date: 2023-12-09 14:41:38
 * @LastEditors: MajorTomMan 765719516@qq.com
 * @LastEditTime: 2024-07-13 20:16:09
 * @FilePath: \ALG\app\nonlinear\平衡二叉树.java
 * @Description: MajorTomMan @版权声明 保留文件所有权利
 */
package com.majortom.algorithms.leetcode.ds.tree;

import com.majortom.algorithms.structure.tree.BinaryTreeNode;
import com.majortom.algorithms.leetcode.support.AlgorithmsUtils;

public class 平衡二叉树 {
    public static void main(String[] args) {
        BinaryTreeNode<Integer> root = AlgorithmsUtils.buildBST(AlgorithmsUtils.randomArray(20, 30));
        isBalanced(root);
    }

    public static boolean isBalanced(BinaryTreeNode<Integer> root) {
        return height(root) != -1;
    }

    public static int height(BinaryTreeNode<Integer> node) {
        if (node == null) {
            return 0;
        }
        int leftHeigh = height(node.getLeft());
        int rightHeigh = height(node.getRight());
        if (leftHeigh == -1 || rightHeigh == -1) {
            return -1;
        }
        if (Math.abs(leftHeigh - rightHeigh) < 2) {
            return Math.max(leftHeigh, rightHeigh) + 1;
        } else {
            return -1;
        }
    }
}
