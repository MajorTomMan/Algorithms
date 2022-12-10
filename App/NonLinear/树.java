package NonLinear;

import Basic.Structure.Tree;

public class 树 {
    public static void main(String[] args) {
        Tree<Integer> tree=new Tree<>(1);
        tree.put(2);
        tree.put(3);
        tree.put(4);
        tree.put(6, 2);
        tree.put(8, 3);
    }
}
