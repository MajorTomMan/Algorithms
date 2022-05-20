package NonLinear;

import java.util.ArrayList;
import java.util.List;

import Basic.Structure.Node.Treenode;

public class 二叉树的前序遍历 extends Example {
    public static void main(String[] args) {
        Integer[] nums={1,null,2,3};
        Treenode<Integer> root=buildTree(nums);
        printTree();
        System.out.println();
        preorderTraversal(root).stream().forEach(System.out::println);
    }
    public static List<Integer> preorderTraversal(Treenode<Integer> root) {
        List<Integer> list=new ArrayList<>();
        preorderTraversal(root,list);
        return list;
    }
    public static void preorderTraversal(Treenode<Integer> root,List<Integer> list) {
        if(root==null){
            return;
        }
        list.add((Integer) root.data);
        preorderTraversal(root.Left,list);
        preorderTraversal(root.Right,list);
    }
}
