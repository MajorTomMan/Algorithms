package com.majortom.algorithms.library.structure;

public interface StringStructure {
    int length();
    char charAt(int index);
    void replace(String value);
    void insert(int index, String value);
    String remove(int index, int length);
    char update(int index, char value);
    String raw();
}
