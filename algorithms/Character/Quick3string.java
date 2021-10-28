package Character;

import Func.Random.StdRandom;

public class Quick3string extends MSD{ //三向字符串快速排序
    public static void sort(String[] a){
        StdRandom.shuffle(a);
        sort(a, 0, a.length-1, 0);
        assert isSorted(a);
    }
    private static void sort(String[] a,int lo,int hi,int d) {
        if(hi<=lo){
            return;
        }
        int lt=lo,gt=hi;
        int v=charAt(a[lo],d);
        int i=lo+1;
        while(i<=gt){
            int t=charAt(a[i],d);
            if(t<v){
                exch(a,lt++,i++);
            }else if(t>v){
                exch(a,i,gt--);
            }
            else{
                i++;
            }
        }
        sort(a,lo,lt-1,d);
        if(v>=0){
            sort(a,lt,gt,d+1); 
        }
        sort(a,gt+1,hi,d);
    }
    private static int charAt(String s,int d){
        if(d<s.length()){
            return s.charAt(d);
        }
        else{
            return -1;
        }
    }
    private static boolean isSorted(String[] a) {
        for (int i = 1; i < a.length; i++){
            if (a[i].compareTo(a[i-1]) < 0){
                return false;
            }
        }
        return true;
    }

}
