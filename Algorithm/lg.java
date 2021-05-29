public class lg {
    public static void main(String[] args) {
        System.out.println(Lg_N(900));
    }
    public static int Lg(int n) { //参考做法
        int log_N=0;
        while(n>0){ //既然乘法是相乘,那我们可以反过来用除法做,直接用结果倒推出N为何
            log_N++; //相当于Log2N中的N
            n/=2; // 2的幂
        }
        return log_N-1; //因为不能大于Log2N的值,所以要减一
    } 
    public static int Lg_N(int n){ //受到启发以后写的
        int N=0;
        int temp=1;
        while(temp<=n){
            temp*=2; 
            N++;
        }
        return N-1;
    }
}
