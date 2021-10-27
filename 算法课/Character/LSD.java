package Character;

public class LSD { //低位优先的基数排序法
    public static void sort(String[] a,int w) {
        int N=a.length;
        int R=256;
        String[] aux=new String[N];
        for (int d = w-1; d>=0;d--) {
            int[] count=new int[R+1]; //计算出现频率
            for (int i = 0; i < N; i++) {
                count[a[i].charAt(d)+1]++;
            }
            for (int r = 0; r < R; r++) { //将频率转换成索引
                count[r+1]+=count[r];
            }
            for (int i = 0; i < N; i++) {
                aux[count[a[i].charAt(d)]++]=a[i];
            }
            for (int i = 0; i < N; i++) {
                a[i]=aux[i];
            }
        }
    }
}
