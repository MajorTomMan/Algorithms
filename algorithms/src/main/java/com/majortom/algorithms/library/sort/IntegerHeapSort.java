package com.majortom.algorithms.library.sort;

/** Max-heap sort over immutable integer input. */
public final class IntegerHeapSort extends AbstractIntegerSort {

    @Override
    protected void sort(IntegerSortSupport sort) throws InterruptedException {
        for (int root = sort.size() / 2 - 1; root >= 0; root--) {
            siftDown(sort, root, sort.size());
        }
        for (int end = sort.size() - 1; end > 0; end--) {
            sort.selectRange(0, end);
            sort.swap(0, end);
            sort.settle(end);
            siftDown(sort, 0, end);
        }
        if (sort.size() > 0) {
            sort.settle(0);
        }
    }

    private void siftDown(IntegerSortSupport sort, int root, int size) throws InterruptedException {
        int current = root;
        while (true) {
            int left = current * 2 + 1;
            if (left >= size) {
                return;
            }
            int largest = left;
            int right = left + 1;
            if (right < size && sort.compare(right, left) > 0) {
                largest = right;
            }
            if (sort.compare(largest, current) <= 0) {
                return;
            }
            sort.swap(current, largest);
            current = largest;
        }
    }
}
