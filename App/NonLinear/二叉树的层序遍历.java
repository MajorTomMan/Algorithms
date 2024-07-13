/*
 * @Date: 2024-07-13 15:50:03
 * @LastEditors: MajorTomMan 765719516@qq.com
 * @LastEditTime: 2024-07-13 20:24:09
 * @FilePath: \ALG\app\nonlinear\二叉树的层序遍历.java
 * @Description: MajorTomMan @版权声明 保留文件所有权利
 */
package nonlinear;

import basic.structure.LinkedList;
import basic.structure.interfaces.Queue;
import basic.structure.node.TreeNode;

public class 二叉树的层序遍历{
    public static void main(String[] args) {

    }
    public static Integer[] levelOrder(TreeNode<Integer, Integer> root,int size){
        // root节点不为空的情况下将root加入队列,然后弹出root
        // 若其还有左右子树,则加入队列中,可一定保证其层次的完整遍历
        Queue<TreeNode<Integer, Integer>> queue = new LinkedList<>();
        if(root==null){
            return null;
        }
        else{
            queue.add(root);
        }
        Integer[] temp=new Integer[size+1];
        int i=0;
        while(!queue.isEmpty()){
            TreeNode<Integer, Integer> node=queue.poll();
            temp[i]=node.value.intValue();
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
