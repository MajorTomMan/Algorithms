package com.majortom.algorithms.library.structure;

import com.majortom.algorithms.library.basic.tree.GeneralTreeNode;

public interface GeneralTreeStructure<T> extends TreeStructure<T> {
    @Override
    GeneralTreeNode<T> root();

    GeneralTreeNode<T> addRoot(T value);

    GeneralTreeNode<T> addChild(GeneralTreeNode<T> parent, T value);

    GeneralTreeNode<T> addChild(GeneralTreeNode<T> parent, int index, T value);

    T set(GeneralTreeNode<T> node, T value);

    boolean remove(GeneralTreeNode<T> node);

    void move(GeneralTreeNode<T> node, GeneralTreeNode<T> newParent);

    void move(GeneralTreeNode<T> node, GeneralTreeNode<T> newParent, int index);
}
