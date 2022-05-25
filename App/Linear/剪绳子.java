import java.util.ArrayList;
import java.util.List;

public class 剪绳子 {
    public static void main(String[] args) {
        System.out.println(cuttingRope(6));
    }
    public static int cuttingRope(int n) {
        int[] dp = new int[n + 1];
        dp[2] = 1;
        for(int i = 3; i < n + 1; i++){
            for(int j = 2; j < i; j++){
                dp[i] = Math.max(dp[i], Math.max(j * (i - j), j * dp[i - j]));
            }
        }
        return dp[n];
    }
}
