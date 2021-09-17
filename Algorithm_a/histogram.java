import java.util.Date;
import java.util.Random;

public class histogram {
    public static void main(String[] args) {
        int[] a=new int[10000];
        int[] b;
        int sum=0;
        a=ran(a, a.length);
        b=Histogram(a,a.length);
        Show(b,sum);
    }

    public static int[] Histogram(int[] a, int M) {
        int temp=0;
        int[] b=new int[M];
        Sort(a,a.length);
        for(int i=0;i<M;i++){
            temp=Count(a, i);
            b[i]=temp;
        }
        return b;
    }

    public static int[] ran(int[] a, int n) {
        Random random = new Random();
        random.setSeed(new Date().getTime());
        for (int i = 0; i < n; i++) {
            a[i] = random.nextInt(n);
        }
        return a;
    }

    public static void Sort(int[] a, int n) {
        int temp;
        for (int i = 0; i < n; i++) {
            for (int j = i + 1; j < n; j++) {
                if (a[i] > a[j]) {
                    temp = a[i];
                    a[i] = a[j];
                    a[j] = temp;
                }
            }
        }
    }

    public static int Count(int[] a, int i) {
        int count=0;
        for(int j=0;j<a.length;j++){
            if(a[j]==i){
                count++;
            }
            else if(a[j]!=i&&a[j]<i){
                continue;
            }
            else{
                break;
            }
        }
        return count;
    }
    public static void Show(int[] b,int sum){
        for(int i=0;i<b.length;i++){
            sum+=b[i];
            System.out.println(b[i]);
        }
        System.out.println("B的长度:"+b.length);
        System.out.println("B的数组元素总和:"+sum);
    }
}
