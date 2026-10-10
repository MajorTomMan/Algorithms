package com.majortom.algorithms.visualization.impl.controller;

import com.majortom.algorithms.core.snapshot.BinaryTreeSnapshot;
import com.majortom.algorithms.core.snapshot.GeneralTreeSnapshot;
import com.majortom.algorithms.visualization.runtime.value.ValueAdapters;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;

/** Pure input preparation for editable tree structures; no controller or rendering state. */
final class TreeInputFactory {
    private TreeInputFactory() {}

    static GeneralTreeSnapshot<Object> generalSnapshot(List<Object> values) {
        return new GeneralTreeSnapshot<>(generalNode(values, 0), values.size());
    }

    private static GeneralTreeSnapshot.Node<Object> generalNode(List<Object> values, int index) {
        if (index >= values.size()) {
            return null;
        }
        List<GeneralTreeSnapshot.Node<Object>> children = new ArrayList<>(3);
        for (int offset = 1; offset <= 3; offset++) {
            GeneralTreeSnapshot.Node<Object> child = generalNode(values, index * 3 + offset);
            if (child != null) {
                children.add(child);
            }
        }
        return new GeneralTreeSnapshot.Node<>(index + 1L, values.get(index), children);
    }

    static BinaryTreeSnapshot<Object> balancedAvlSnapshot(List<Object> sortedValues) {
        return new BinaryTreeSnapshot<>(
                balancedAvlNode(sortedValues, 0, sortedValues.size()), sortedValues.size());
    }

    private static BinaryTreeSnapshot.Node<Object> balancedAvlNode(
            List<Object> values, int fromInclusive, int toExclusive) {
        if (fromInclusive >= toExclusive) {
            return null;
        }
        int middle = fromInclusive + (toExclusive - fromInclusive) / 2;
        return new BinaryTreeSnapshot.Node<>(
                middle + 1L, values.get(middle),
                balancedAvlNode(values, fromInclusive, middle),
                balancedAvlNode(values, middle + 1, toExclusive));
    }

    static List<Object> sortedComparableValues(List<Object> values) {
        List<Object> sorted = new ArrayList<>(values);
        sorted.sort(TreeInputFactory::compareComparableValues);
        for (int index = 1; index < sorted.size(); index++) {
            if (compareComparableValues(sorted.get(index - 1), sorted.get(index)) == 0) {
                throw new IllegalArgumentException("AVL bulk values must be unique");
            }
        }
        return List.copyOf(sorted);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    static int compareComparableValues(Object left, Object right) {
        return requireComparable(left).compareTo(right);
    }

    @SuppressWarnings("rawtypes")
    static Comparable requireComparable(Object value) {
        if (!(value instanceof Comparable<?> comparable)) {
            throw new IllegalArgumentException("AVL value type must implement Comparable: " + value.getClass().getName());
        }
        return (Comparable) comparable;
    }

    static List<Object> sampleGeneralValues(Class<?> valueType) {
        if (valueType == String.class) {
            return List.of("root", "alpha", "beta", "gamma", "delta", "epsilon", "zeta", "eta");
        }
        if (valueType == Integer.class) return List.of(50, 30, 70, 90, 10, 40, 60, 80);
        return sampleTypedValues(valueType, 8);
    }

    static List<Object> sampleAvlValues(Class<?> valueType) {
        if (valueType == String.class) {
            return List.of("alpha", "beta", "delta", "epsilon", "gamma", "theta", "zeta");
        }
        if (valueType == Integer.class) return List.of(10, 30, 40, 50, 60, 70, 90);
        return sampleTypedValues(valueType, 7);
    }

    private static List<Object> sampleTypedValues(Class<?> valueType, int requested) {
        int count = Math.min(requested, ValueAdapters.maxDistinctSamples(valueType));
        List<Object> result = new ArrayList<>(count);
        for (int index = 0; index < count; index++) {
            result.add(ValueAdapters.distinctValue(valueType, index));
        }
        return List.copyOf(result);
    }

    static List<Object> randomValues(Class<?> valueType, Random random) {
        LinkedHashSet<Object> unique = new LinkedHashSet<>();
        int count = Math.min(10, ValueAdapters.maxDistinctSamples(valueType));
        int attempts = 0;
        while (unique.size() < count && attempts++ < 1000) {
            unique.add(ValueAdapters.randomValue(valueType, random));
        }
        for (int index = 0; unique.size() < count; index++) {
            unique.add(ValueAdapters.distinctValue(valueType, index));
        }
        return new ArrayList<>(unique);
    }
}
