package com.majortom.algorithms.structure.tree;

import com.majortom.algorithms.core.annotation.Structure;
import com.majortom.algorithms.core.metadata.StructureModule;

@Structure(module = StructureModule.TREE)
public interface BinaryTreeStructure<T> extends TreeStructure<T> {
    @Override
    BinaryTreeNode<T> root();
}
