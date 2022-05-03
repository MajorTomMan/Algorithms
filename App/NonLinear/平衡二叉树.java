package NonLinear;

import java.util.Random;

import Basic.Structure.BRTree;
import Basic.Structure.Tree;
import Basic.Structure.Node.Treenode;

public class 平衡二叉树 {
    public static void main(String[] args) {
        BRTree<Integer> Tree=new BRTree<Integer>(50);
        int i=0;
        Random random=new Random();
        System.out.println("-----------------------raw data--------------------");
        while(i!=12){
            int ran=random.nextInt(100);
            Tree.put(ran);
            System.out.print(ran+" ");
            i++;
        }
        System.out.println();
        System.out.println(isBalanced(Tree.getRoot()));
    }
    public static boolean isBalanced(Treenode<Integer> root) {
        return height(root)!=-1;
    }

    public static int height(Treenode<Integer> node) {
        if(node==null){
            return 0;
        }
        int leftHeigh=height(node.Left);
        int rightHeigh=height(node.Right);
        if(leftHeigh==-1||rightHeigh==-1){
            return -1;
        }
        return Math.abs(leftHeigh-rightHeigh)<2?Math.max(leftHeigh, rightHeigh)+1:-1;
    }
}
