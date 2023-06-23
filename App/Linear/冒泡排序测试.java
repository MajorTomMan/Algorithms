package linear;



public class 冒泡排序测试 extends Common {
    public static void main(String[] args) {
        int[] arr = generatorRandomArray(10, 100);
        arr = sort(arr);
        show(arr);
    }

    /* 冒泡排序 */
    private static int[] sort(int[] arr) {
        for (int i = 0; i < arr.length; i++) {
            boolean swapped = false;
            for (int j = i + 1; j < arr.length; j++) {
                if (less(arr[i], arr[j])) {
                    swap(arr, i, j);
                    swapped = true;
                }
            }
            if (!swapped) {
                break;
            }
        }
        return arr;
    }
}
