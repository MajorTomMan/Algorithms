package com.majortom.algorithms.library.basic;

import com.majortom.algorithms.core.runtime.StructureEvents;
import com.majortom.algorithms.library.structure.StringStructure;

import java.util.Objects;

public final class String implements StringStructure {
    private static final int DEFAULT_CAPACITY = 16;

    private char[] characters;
    private int length;

    public String() {
        this("");
    }

    public String(java.lang.String value) {
        Objects.requireNonNull(value, "value");
        characters = new char[Math.max(DEFAULT_CAPACITY, value.length())];
        value.getChars(0, value.length(), characters, 0);
        length = value.length();
    }

    @Override
    public int length() {
        return length;
    }

    @Override
    public char charAt(int index) {
        checkIndex(index);
        return characters[index];
    }

    @Override
    public char set(int index, char value) {
        checkIndex(index);
        char previous = characters[index];
        if (previous == value) {
            return previous;
        }
        characters[index] = value;
        StructureEvents.stringUpdated(index, previous, value);
        return previous;
    }

    @Override
    public void insert(int index, CharSequence value) {
        Objects.requireNonNull(value, "value");
        checkInsertIndex(index);
        java.lang.String text = value.toString();
        if (text.isEmpty()) {
            return;
        }
        ensureCapacity(length + text.length());
        System.arraycopy(characters, index, characters, index + text.length(), length - index);
        text.getChars(0, text.length(), characters, index);
        length += text.length();
        StructureEvents.stringInserted(index, text);
    }

    @Override
    public java.lang.String remove(int index, int removeLength) {
        checkRange(index, removeLength);
        if (removeLength == 0) {
            return "";
        }
        java.lang.String removed = new java.lang.String(characters, index, removeLength);
        int moved = length - index - removeLength;
        if (moved > 0) {
            System.arraycopy(characters, index + removeLength, characters, index, moved);
        }
        length -= removeLength;
        StructureEvents.stringRemoved(index, removed);
        return removed;
    }

    @Override
    public java.lang.String replace(int index, int replaceLength, CharSequence value) {
        Objects.requireNonNull(value, "value");
        checkRange(index, replaceLength);
        java.lang.String previous = new java.lang.String(characters, index, replaceLength);
        java.lang.String replacement = value.toString();
        if (previous.contentEquals(replacement)) {
            return previous;
        }
        int newLength = length - replaceLength + replacement.length();
        ensureCapacity(newLength);
        int tailLength = length - index - replaceLength;
        if (replacement.length() != replaceLength && tailLength > 0) {
            System.arraycopy(characters, index + replaceLength, characters, index + replacement.length(), tailLength);
        }
        replacement.getChars(0, replacement.length(), characters, index);
        length = newLength;
        StructureEvents.stringReplaced(index, previous, replacement);
        return previous;
    }

    @Override
    public java.lang.String value() {
        return new java.lang.String(characters, 0, length);
    }

    private void ensureCapacity(int required) {
        if (required <= characters.length) {
            return;
        }
        int capacity = Math.max(required, characters.length + (characters.length >> 1) + 1);
        char[] expanded = new char[capacity];
        System.arraycopy(characters, 0, expanded, 0, length);
        characters = expanded;
    }

    private void checkIndex(int index) {
        if (index < 0 || index >= length) {
            throw new IndexOutOfBoundsException("index=" + index + ", length=" + length);
        }
    }

    private void checkInsertIndex(int index) {
        if (index < 0 || index > length) {
            throw new IndexOutOfBoundsException("index=" + index + ", length=" + length);
        }
    }

    private void checkRange(int index, int rangeLength) {
        if (rangeLength < 0 || index < 0 || index + rangeLength > length) {
            throw new IndexOutOfBoundsException("index=" + index + ", length=" + rangeLength + ", valueLength=" + length);
        }
    }
}
