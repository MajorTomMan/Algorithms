/*
 * @Date: 2023-12-09 14:41:38
 * @LastEditors: MajorTomMan 765719516@qq.com
 * @LastEditTime: 2024-07-13 20:28:12
 * @FilePath: \ALG\app\nonlinear\二叉树的前序遍历.java
 * @Description: MajorTomMan @版权声明 保留文件所有权利
 */
package nonlinear;

import java.util.ArrayList;
import java.util.List;

import basic.structure.node.TreeNode;
import utils.AlgorithmsUtils;

public class 二叉树的前序遍历 {
    public static void main(String[] args) {
        Integer[] nums = { 1, null, 2, 3 };
        TreeNode<Integer, Integer> root = AlgorithmsUtils.buildTree(nums, nums);
        System.out.println();
        preorderTraversal(root).stream().forEach(System.out::println);
    }

    public static List<Integer> preorderTraversal(TreeNode<Integer, Integer> root) {
        List<Integer> list = new ArrayList<>();
        preorderTraversal(root, list);
        return list;
    }

    public static void preorderTraversal(TreeNode<Integer, Integer> root, List<Integer> list) {
        if (root == null) {
            return;
        }
        list.add((Integer) root.value);
        preorderTraversal(root.left, list);
        preorderTraversal(root.right, list);
    }
}
