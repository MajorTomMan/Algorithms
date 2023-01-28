package NonLinear;

import Basic.Structure.BinaryTree;
import Basic.Structure.Node.Treenode;

public class 平衡二叉树 extends Example{
    public static void main(String[] args) {
        BinaryTree<Integer> Tree=new BinaryTree<>();
        Tree.setRoot(buildTreeByRandom(12));
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
