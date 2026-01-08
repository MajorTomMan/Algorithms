/*
 * @Date: 2023-12-09 14:41:38
 * @LastEditors: MajorTomMan 765719516@qq.com
 * @LastEditTime: 2024-07-13 20:29:32
 * @FilePath: \ALG\app\nonlinear\二叉树的最短路径.java
 * @Description: MajorTomMan @版权声明 保留文件所有权利
 */
package com.majortom.algorithms.app.leetcode.nonlinear;

import com.majortom.algorithms.core.basic.node.TreeNode;
import com.majortom.algorithms.utils.AlgorithmsUtils;

public class 二叉树的最短路径 {
    public static void main(String[] args) {
        TreeNode<Integer, Integer> root = AlgorithmsUtils.buildTree(AlgorithmsUtils.randomArray(20, 30),
                AlgorithmsUtils.randomArray(20, 30));
        minDepth(root);

    }

    public static int minDepth(TreeNode<Integer, Integer> root) {
        return ShortestPath(root);
    }

    public static int ShortestPath(TreeNode<Integer, Integer> node) {
        if (node == null) {
            return 0;
        }
        if (node.left == null && node.right == null) {
            return 1;
        }
        if (node.left != null) {
            return ShortestPath(node.left) + 1;
        }
        if (node.right != null) {
            return ShortestPath(node.right) + 1;
        }
        return Math.min(ShortestPath(node.left), ShortestPath(node.right)) + 1;
    }
}
