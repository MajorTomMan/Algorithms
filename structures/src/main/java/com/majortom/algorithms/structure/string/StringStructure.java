package com.majortom.algorithms.structure.string;

public interface StringStructure {
    int length();

    default boolean isEmpty() {
        return length() == 0;
    }

    char charAt(int index);
    char set(int index, char value);
    void insert(int index, CharSequence value);
    java.lang.String remove(int index, int length);
    java.lang.String replace(int index, int length, CharSequence value);

    default void append(CharSequence value) {
        insert(length(), value);
    }

    java.lang.String value();
}
