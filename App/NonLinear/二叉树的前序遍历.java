/*
 * @Date: 2023-04-26 16:51:29
 * @LastEditors: hujunhao hujunhao@rtczsz.com
 * @LastEditTime: 2023-04-26 17:58:39
 * @FilePath: /alg/App/NonLinear/二叉树的前序遍历.java
 */
package NonLinear;

import java.util.ArrayList;
import java.util.List;

import basic.structure.node.TreeNode;

public class 二叉树的前序遍历 extends Example {
    public static void main(String[] args) {
        Integer[] nums={1,null,2,3};
        TreeNode<Integer> root=buildTree(nums);
        printTree();
        System.out.println();
        preorderTraversal(root).stream().forEach(System.out::println);
    }
    public static List<Integer> preorderTraversal(TreeNode<Integer> root) {
        List<Integer> list=new ArrayList<>();
        preorderTraversal(root,list);
        return list;
    }
    public static void preorderTraversal(TreeNode<Integer> root,List<Integer> list) {
        if(root==null){
            return;
        }
        list.add((Integer) root.data);
        preorderTraversal(root.Left,list);
        preorderTraversal(root.Right,list);
    }
}
