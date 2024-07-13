package linear;

import utils.AlgorithmsUtils;

public class 选择排序测试{
    public static void main(String[] args) {
        Integer[] arr=AlgorithmsUtils.randomArray(10,100);
        sort(arr);
        AlgorithmsUtils.display(arr);
    }
    /* 选择排序 */
    private static void sort(Integer[] arr){
        for(int i=0;i<=arr.length;i++){
            int minIndex=i;
            for (int j = i+1; j < arr.length; j++) {
                if(AlgorithmsUtils.less(arr[minIndex],arr[j])){
                    minIndex=j;
                }
            }
            AlgorithmsUtils.swap(arr, i, minIndex);
        }
    }
}
