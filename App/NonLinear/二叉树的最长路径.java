package NonLinear;


import Basic.Structure.BRTree;
import Basic.Structure.Node.Treenode;

public class 二叉树的最长路径 {
    public static void main(String[] args) {
        BRTree<Integer> Tree=new BRTree<Integer>();
        Tree.put(3);
        Tree.put(9);
        Tree.put(20);
        Tree.put(15);
        Tree.put(7);
        System.out.println(maxDepth(Tree.getRoot()));
    }
    public static int maxDepth(Treenode<Integer> root) {
        return longestPath(root);
    }
    public static int longestPath(Treenode<Integer> node){
        if(node==null){
            return 0;
        }
        else{
            int leftHeight = longestPath(node.Left);
            int rightHeight = longestPath(node.Right);
            return Math.max(leftHeight, rightHeight) + 1;
        }
    }
}
