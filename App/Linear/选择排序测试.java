
public class 选择排序测试  extends Common{
    public static void main(String[] args) {
        int[] arr=generatorRandomArray(10,100);
        sort(arr);
        show(arr);
    }
    /* 选择排序 */
    private static void sort(int[] arr){
        for(int i=0;i<=arr.length;i++){
            int minIndex=i;
            for (int j = i+1; j < arr.length; j++) {
                if(less(arr[minIndex],arr[j])){
                    minIndex=j;
                }
            }
            swap(arr, i, minIndex);
        }
    }
}
