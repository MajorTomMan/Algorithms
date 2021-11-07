package Sort.Structure.Example;

public abstract class Example{
    public abstract void sort(Comparable[] a);
    protected static boolean less(Comparable v,Comparable w){
        return v.compareTo(w)<0; //小于返回-1
    }
    protected static void exch(Comparable[] a,int i,int j){
        Comparable t=a[i];
        a[i]=a[j];
        a[j]=t;
    }
    protected static void show(Comparable[] a){
        for(int i=0;i<a.length;i++){
            System.out.print(a[i]+" ");
        }
        System.out.println();
    }
    protected static boolean isSorted(Comparable[] a){
        for(int i=1;i<a.length;i++){
            if(less(a[i],a[i-1])){
                return false;
            }
        }
        return true;
    }
}
