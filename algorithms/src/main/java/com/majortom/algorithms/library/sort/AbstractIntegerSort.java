package com.majortom.algorithms.library.sort;

import com.majortom.algorithms.core.api.Algorithm;
import com.majortom.algorithms.core.api.AlgorithmContext;
import com.majortom.algorithms.library.sort.model.IntegerSortInput;
import com.majortom.algorithms.library.sort.model.IntegerSortOutput;

/** Owns the common lifecycle for production integer sorting algorithms. */
public abstract class AbstractIntegerSort implements Algorithm<IntegerSortInput, IntegerSortOutput> {

    @Override
    public final IntegerSortOutput run(IntegerSortInput input, AlgorithmContext context)
            throws InterruptedException {
        IntegerSortSupport sort = new IntegerSortSupport(input.values(), context);
        sort.initialize();
        sort(sort);
        return new IntegerSortOutput(sort.complete());
    }

    protected abstract void sort(IntegerSortSupport sort) throws InterruptedException;
}
