package com.majortom.algorithms.core.interfaces;

import java.util.function.BiConsumer;

public interface Tree<Key extends Comparable<Key>, Value extends Comparable<Value>> {
    public boolean isEmpty();

    public int size();

    public void put(Key key, Value value);

    public Value get(Key key);

    public void remove(Key key);

    public void replace(Key key, Value value);

    public void foreach(BiConsumer<Key, Value> action);

    public boolean containsKey(Key key);

    public boolean containsValue(Value value);
}