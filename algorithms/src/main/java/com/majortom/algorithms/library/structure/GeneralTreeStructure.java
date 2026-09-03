package com.majortom.algorithms.library.structure;

import com.majortom.algorithms.library.basic.tree.TreeNode;

public interface GeneralTreeStructure<T> extends TreeStructure<T> {
    TreeNode<T> addRoot(T value);
    TreeNode<T> addChild(TreeNode<T> parent, T value);
    TreeNode<T> addChild(TreeNode<T> parent, int index, T value);
    boolean remove(TreeNode<T> node);
}
