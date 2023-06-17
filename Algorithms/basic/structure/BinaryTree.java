package basic.structure;

import basic.structure.iface.ITree;
import basic.structure.node.TreeNode;

public class BinaryTree<T extends Comparable<T>> implements ITree<T> {
    private TreeNode<T> root;
}