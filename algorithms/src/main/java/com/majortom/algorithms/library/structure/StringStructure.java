package com.majortom.algorithms.library.structure;

public interface StringStructure {
    int length();

    default boolean isEmpty() {
        return length() == 0;
    }

    char charAt(int index);
    char set(int index, char value);
    void insert(int index, CharSequence value);
    String remove(int index, int length);
    String replace(int index, int length, CharSequence value);

    default void append(CharSequence value) {
        insert(length(), value);
    }

    String value();
}
