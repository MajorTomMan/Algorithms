package linear;

public class 算术平方根 {
    public static void main(String[] args) {
        System.out.println(mySqrt(8));
    }
    // 将根号x写成以e为底的自然对数进行换底,即可得到其算术平方根
    public static int mySqrt(int x) {
        if (x == 0) {
            return 0;
        }
        int ans = (int) Math.exp(0.5 * Math.log(x));
        return (long) (ans + 1) * (ans + 1) <= x ? ans + 1 : ans;
    }
}
