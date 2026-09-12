package com.majortom.algorithms.structure.tree;

import java.util.List;
import java.util.Objects;

public interface GeneralTreeStructure<T> extends TreeStructure<T> {
    /** Lightweight tree-specific input used only by the trusted bulk-load path. */
    record NodeInput<T>(T value, List<NodeInput<T>> children) {
        public NodeInput {
            value = Objects.requireNonNull(value, "value");
            children = List.copyOf(Objects.requireNonNull(children, "children"));
        }

        public static <T> NodeInput<T> leaf(T value) {
            return new NodeInput<>(value, List.of());
        }
    }

    @Override
    GeneralTreeNode<T> root();

    /** Replaces the complete ordered tree through the trusted bulk-load path. Null means empty. */
    void initialize(NodeInput<T> root);

    GeneralTreeNode<T> addRoot(T value);

    GeneralTreeNode<T> addChild(GeneralTreeNode<T> parent, T value);

    GeneralTreeNode<T> addChild(GeneralTreeNode<T> parent, int index, T value);

    GeneralTreeNode<T> addParent(GeneralTreeNode<T> node, T value);

    T set(GeneralTreeNode<T> node, T value);

    GeneralTreeNode<T> findById(long id);

    GeneralTreeNode<T> findFirstByValue(T value);

    boolean remove(GeneralTreeNode<T> node);

    void move(GeneralTreeNode<T> node, GeneralTreeNode<T> newParent);

    void move(GeneralTreeNode<T> node, GeneralTreeNode<T> newParent, int index);
}
