/*
 * @Date: 2024-07-13 15:50:03
 * @LastEditors: MajorTomMan 765719516@qq.com
 * @LastEditTime: 2024-07-13 20:24:09
 * @FilePath: \ALG\app\nonlinear\二叉树的层序遍历.java
 * @Description: MajorTomMan @版权声明 保留文件所有权利
 */
package com.majortom.algorithms.app.leetcode.ds.tree;

import com.majortom.algorithms.core.basic.LinkedList;
import com.majortom.algorithms.core.interfaces.Queue;
import com.majortom.algorithms.core.tree.node.TreeNode;

public class 二叉树的层序遍历{
    public static void main(String[] args) {

    }
    public static Integer[] levelOrder(TreeNode<Integer> root,int size){
        // root节点不为空的情况下将root加入队列,然后弹出root
        // 若其还有左右子树,则加入队列中,可一定保证其层次的完整遍历
        Queue<TreeNode<Integer>> queue = new LinkedList<>();
        if(root==null){
            return null;
        }
        else{
            queue.add(root);
        }
        Integer[] temp=new Integer[size+1];
        int i=0;
        while(!queue.isEmpty()){
            TreeNode<Integer> node=queue.poll();
            temp[i]=node.data.intValue();
            if(node.left!=null){
                queue.add(node.left);
            }
            if(node.right!=null){
                queue.add(node.right);
            }
            i++;
        }
        return temp;
    }
}
