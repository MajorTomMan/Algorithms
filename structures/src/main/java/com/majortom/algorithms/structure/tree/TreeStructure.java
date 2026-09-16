package com.majortom.algorithms.structure.tree;

import com.majortom.algorithms.core.annotation.Structure;
import com.majortom.algorithms.core.metadata.StructureModule;

@Structure(module = StructureModule.TREE)
public interface TreeStructure<T> {
    int size();

    default boolean isEmpty() {
        return size() == 0;
    }

    TreeNode<T> root();
}
