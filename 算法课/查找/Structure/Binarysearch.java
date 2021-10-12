package 查找.Structure;
public class Binarysearch{
    public static int rank(int key,int[] a) {
        // Arrays must be sorted before using this methods
        int lo=0;
        int hi=a.length-1;
        while(lo<=hi){
            int mid=lo+(hi-lo)/2;
            if(key<a[mid]){
                hi=mid-1;
            }
            else if(key>a[mid]){
                lo=mid+1;
            }
            else{
                return mid;
            }
        }
        return -1;
    }
}
