package NonLinear;

import Basic.Structure.Node.Treenode;

public class 数组转二叉树 extends Example{
    public static void main(String[] args) {
        Integer[] nums={5,4,8,11,null,13,4,7,2,null,null,null,1};
        buildTree(nums);
        printTree();
    }
    public static Treenode<Integer> transform(Treenode<Integer> node,int data){
        if(node==null){
            return new Treenode<Integer>(data,null,null);
        }
        if(node.Left==null){
            node.Left=transform(node.Left, data);
            node.SubTreeNum++;
        }
        else if(node.Right==null){
            node.Right=transform(node.Right, data);
            node.SubTreeNum++;
        }
        else{
            node.Left=transform(node.Left, data);
            node.Right=transform(node.Right, data);
            node.SubTreeNum++;
        }
        return node;
    }
}
