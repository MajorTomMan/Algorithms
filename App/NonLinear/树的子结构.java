/*
 * @Date: 2023-12-09 14:41:38
 * @LastEditors: MajorTomMan 765719516@qq.com
 * @LastEditTime: 2024-07-13 20:11:56
 * @FilePath: \ALG\app\nonlinear\树的子结构.java
 * @Description: MajorTomMan @版权声明 保留文件所有权利
 */
package nonlinear;

import basic.structure.node.TreeNode;
import utils.AlgorithmsUtils;

public class 树的子结构 {
    public static void main(String[] args) {
        TreeNode<Integer, Integer> tree_a = AlgorithmsUtils.buildTree(AlgorithmsUtils.randomArray(20, 30),
                AlgorithmsUtils.randomArray(20, 30));
        TreeNode<Integer, Integer> tree_b = AlgorithmsUtils.buildTree(AlgorithmsUtils.randomArray(20, 30),
                AlgorithmsUtils.randomArray(20, 30));
        System.out.println(isSubStructure(tree_a, tree_b));
    }

    // 先检查A和B共同的子树根节点,然后递归判断子树结构
    public static boolean isSubStructure(TreeNode<Integer, Integer> A, TreeNode<Integer, Integer> B) {
        if (A == null || B == null) {
            return false;
        }
        if (A.value == B.value && checkSubTree(A, B)) {
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
    public static boolean checkSubTree(TreeNode<Integer, Integer> a, TreeNode<Integer, Integer> b) {
        if (b == null) {
            return true;
        }
        if (a == null) {
            return false;
        }
        return a.value == b.value && checkSubTree(a.left, b.left) && checkSubTree(a.right, b.right);
    }
}
