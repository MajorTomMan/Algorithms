
public class 插入排序测试 extends Common {
    public static void main(String[] args) {
        int[] arr = generatorRandomArray(10, 100);
        sort(arr);
        show(arr);
    }
    /* 插入排序 */
    private static void sort(int[] arr) {
        for (int i = 1; i < arr.length; i++) {
            for (int j = i; j >= 1; j--) {
                if (less(arr[j-1], arr[j])) {
                    swap(arr, j, j-1);
                }
            }
        }
    }
}
