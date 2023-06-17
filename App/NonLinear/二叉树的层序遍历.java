
package NonLinear;

import basic.structure.BinaryTree;
import basic.structure.Queue;
import basic.structure.node.TreeNode;

public class 二叉树的层序遍历 extends Common{
    private static BinaryTree<Integer> tree=new BinaryTree<>();
    public static void main(String[] args) {

    }
    public static int[] levelOrder(TreeNode<Integer> root,int size){
        // root节点不为空的情况下将root加入队列,然后弹出root
        // 若其还有左右子树,则加入队列中,可一定保证其层次的完整遍历
        Queue<TreeNode<Integer>> queue=new Queue<>();
        if(root==null){
            return null;
        }
        else{
            queue.enqueue(root);
        }
        int[] temp=new int[size+1];
        int i=0;
        while(!queue.isEmpty()){
            TreeNode<Integer> node=queue.dequeue();
            temp[i]=node.data.intValue();
            if(node.left!=null){
                queue.enqueue(node.left);
            }
            if(node.right!=null){
                queue.enqueue(node.right);
            }
            i++;
        }
        return temp;
    }
}
