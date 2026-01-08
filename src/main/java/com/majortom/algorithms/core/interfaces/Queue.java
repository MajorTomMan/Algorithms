package com.majortom.algorithms.core.interfaces;

public interface Queue<T> extends List<T> {
    public T peak();

    public T poll();
}