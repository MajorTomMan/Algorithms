package com.majortom.algorithms.structure.linked;

import com.majortom.algorithms.core.annotation.Structure;
import com.majortom.algorithms.core.metadata.StructureModule;

@Structure(id = "stack", name = "Stack", module = StructureModule.STACK, implementation = LinkedList.class)
public interface StackStructure<T> extends Iterable<T> {
    int size();

    default boolean isEmpty() {
        return size() == 0;
    }

    void push(T value);
    T pop();
    T peek();
}
