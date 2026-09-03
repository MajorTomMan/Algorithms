/*
 * @Date: 2023-12-09 14:41:38
 * @LastEditors: MajorTomMan 765719516@qq.com
 * @LastEditTime: 2024-07-13 20:23:04
 * @FilePath: \ALG\app\nonlinear\对称二叉树.java
 * @Description: MajorTomMan @版权声明 保留文件所有权利
 */
package com.majortom.algorithms.app.leetcode.ds.tree;

import com.majortom.algorithms.library.basic.LinkedList;
import com.majortom.algorithms.library.structure.QueueStructure;
import com.majortom.algorithms.library.basic.tree.BinaryTreeNode;
import com.majortom.algorithms.library.basic.utils.AlgorithmsUtils;

public class 对称二叉树{
    public static void main(String[] args) {
        Integer[] nums = { 1, 2, 2, 3, 4, 4, 3 };
        BinaryTreeNode<Integer> root = AlgorithmsUtils.buildBST(nums);
        System.out.println(isSymmetric(root));
    }
    /*
     * 如果同时满足下面的条件，两个树互为镜像：
     * 它们的两个根结点具有相同的值
     * 每个树的右子树都与另一个树的左子树镜像对称
     */
    public static boolean isSymmetric(BinaryTreeNode<Integer> root) {
        return check(root);

    }

    public static boolean check(BinaryTreeNode<Integer> p, BinaryTreeNode<Integer> q) {
        if (p == null && q == null) {
            return true;
        }
        if (p == null || q == null) {
            return false;
        }
        return p.getValue() == q.getValue() && check(p.getLeft(), q.getRight()) && check(p.getRight(), q.getLeft());
    }

    /*
     * 首先我们引入一个队列，这是把递归程序改写成迭代程序的常用方法。初始化时我们把根节点入队两次。
     * 每次提取两个结点并比较它们的值（队列中每两个连续的结点应该是相等的，而且它们的子树互为镜像），
     * 然后将两个结点的左右子结点按相反的顺序插入队列中。
     * 当队列为空时，或者我们检测到树不对称（即从队列中取出两个不相等的连续结点）时，该算法结束。
     */
    public static boolean check(BinaryTreeNode<Integer> root) {
        QueueStructure<BinaryTreeNode<Integer>> queue = new LinkedList<>();
        queue.enqueue(root);
        queue.enqueue(root);
        while (!queue.isEmpty()) {
            BinaryTreeNode<Integer> node_l = queue.dequeue();
            BinaryTreeNode<Integer> node_r = queue.dequeue();
            if (node_l == null && node_r == null) {
                continue;
            }
            if (node_l == null || node_r == null || node_l.getValue() != node_r.getValue()) {
                return false;
            }
            queue.enqueue(node_l.getLeft());
            queue.enqueue(node_r.getRight());
            queue.enqueue(node_l.getRight());
            queue.enqueue(node_r.getLeft());
        }
        return true;
    }
}
