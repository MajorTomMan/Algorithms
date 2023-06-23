package linear;

import java.util.HashMap;
import java.util.Map;

public class 跳台阶 {
    // 使用哈希map，充当备忘录的作用
    Map<Integer, Integer> tempMap = new HashMap<>();

    public int numWays(int n) {
        // n = 0 也算1种
        if (n == 0) {
            return 1;
        }
        if (n <= 2) {
            return n;
        }
        // 先判断有没计算过，即看看备忘录有没有
        if (tempMap.containsKey(n)) {
            // 备忘录有，即计算过，直接返回
            return tempMap.get(n);
        } else {
            // 备忘录没有，即没有计算过，执行递归计算,并且把结果保存到备忘录map中，对1000000007取余（这个是leetcode题目规定的）
            tempMap.put(n, (numWays(n - 1) + numWays(n - 2)) % 1000000007);
            return tempMap.get(n);
        }
    }
}
