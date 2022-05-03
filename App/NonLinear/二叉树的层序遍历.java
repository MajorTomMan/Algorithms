package NonLinear;

import java.util.Random;

import Basic.Structure.BRTree;
import Basic.Structure.Queue;
import Basic.Structure.Node.Treenode;

public class 二叉树的层序遍历 {
    public static void main(String[] args) {
        BRTree<Integer> Tree=new BRTree<Integer>(50);
        int i=0;
        Random random=new Random();
        System.out.println("-----------------------raw data--------------------");
        while(i!=12){
            int ran=random.nextInt(100);
            Tree.Insert(ran);
            System.out.print(ran+" ");
            i++;
        }
        System.out.println();
        for (int data: levelOrder(Tree.getRoot(),i)) {
            System.out.print(data+" ");
        }
    }
    public static int[] levelOrder(Treenode<Integer> root,int size){
        // root节点不为空的情况下将root加入队列,然后弹出root
        // 若其还有左右子树,则加入队列中,可一定保证其层次的完整遍历
        Queue<Treenode<Integer>> queue=new Queue<>();
        if(root==null){
            return null;
        }
        else{
            queue.enqueue(root);
        }
        int[] temp=new int[size+1];
        int i=0;
        while(!queue.isEmpty()){
            Treenode<Integer> node=queue.dequeue();
            temp[i]=node.data.intValue();
            if(node.Left!=null){
                queue.enqueue(node.Left);
            }
            if(node.Right!=null){
                queue.enqueue(node.Right);
            }
            i++;
        }
        return temp;
    }
}
