/*
 * @Date: 2023-12-09 14:41:38
 * @LastEditors: MajorTomMan 765719516@qq.com
 * @LastEditTime: 2024-07-13 20:16:09
 * @FilePath: \ALG\app\nonlinear\平衡二叉树.java
 * @Description: MajorTomMan @版权声明 保留文件所有权利
 */
package com.majortom.algorithms.app.leetcode.nonlinear;

import com.majortom.algorithms.core.basic.node.TreeNode;
import com.majortom.algorithms.utils.AlgorithmsUtils;

public class 平衡二叉树 {
    public static void main(String[] args) {
        TreeNode<Integer, Integer> root = AlgorithmsUtils.buildTree(AlgorithmsUtils.randomArray(20, 30),
                AlgorithmsUtils.randomArray(20, 30));
        isBalanced(root);
    }

    public static boolean isBalanced(TreeNode<Integer, Integer> root) {
        return height(root) != -1;
    }

    public static int height(TreeNode<Integer, Integer> node) {
        if (node == null) {
            return 0;
        }
        int leftHeigh = height(node.left);
        int rightHeigh = height(node.right);
        if (leftHeigh == -1 || rightHeigh == -1) {
            return -1;
        }
        return Math.abs(leftHeigh - rightHeigh) < 2 ? Math.max(leftHeigh, rightHeigh) + 1 : -1;
    }
}
