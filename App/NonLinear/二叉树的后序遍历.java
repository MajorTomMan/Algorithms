package NonLinear;

import java.util.ArrayList;
import java.util.List;

import Basic.Structure.Node.Treenode;

public class 二叉树的后序遍历 extends Example{
    public static void main(String[] args) {
        Integer[] nums={1,null,2,3};
        Treenode<Integer> root=buildTree(nums);
        System.out.println(postTraversal(root));
    }
    public static List<Integer> postTraversal(Treenode<Integer> root) {
        List<Integer> list=new ArrayList<>();
        postTraversal(root,list);
        return list;
    }
    public static void postTraversal(Treenode<Integer> root,List<Integer> list) {
        if(root==null){
            return;
        }
        postTraversal(root.Left,list);
        postTraversal(root.Right,list);
        list.add((Integer) root.data);
    }
}
