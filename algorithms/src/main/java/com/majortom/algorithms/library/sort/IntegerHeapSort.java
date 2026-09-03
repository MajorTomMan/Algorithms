package com.majortom.algorithms.library.sort;

import com.majortom.algorithms.library.structure.ArrayStructure;

/** Max-heap sort over an ArrayStructure<Integer>. */
public final class IntegerHeapSort extends AbstractIntegerSort {

    @Override
    public int compare(Integer left, Integer right) {
        return Integer.compare(left, right);
    }

    @Override
    public void sort(ArrayStructure<Integer> array) {
        for (int root = array.size() / 2 - 1; root >= 0; root--) {
            siftDown(array, root, array.size());
        }
        for (int end = array.size() - 1; end > 0; end--) {
            swap(array, 0, end);
            siftDown(array, 0, end);
        }
        if (array.size() > 0) {
        }
    }

    private void siftDown(ArrayStructure<Integer> array, int root, int size) {
        int current = root;
        while (true) {
            int left = current * 2 + 1;
            if (left >= size) {
                return;
            }
            int largest = left;
            int right = left + 1;
            if (right < size && compareAt(array, right, left) > 0) {
                largest = right;
            }
            if (compareAt(array, largest, current) <= 0) {
                return;
            }
            swap(array, current, largest);
            current = largest;
        }
    }
}
