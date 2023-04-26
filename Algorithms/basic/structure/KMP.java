package basic.structure;

import java.util.ArrayList;
import java.util.List;

public class KMP {
    private String target = ""; // 待查字符串
    private String pattern = ""; // 模式字符串
    private int[] next;

    /**
     * @param target  待查字符串
     * @param pattern 目标字符串
     */
    public KMP(String target, String pattern) {
        this.target = target;
        this.pattern = pattern;
        next=new int[pattern.length()];
    }

    // 构建Next数组
    private  void buildNext() {
        int postfix = 1; // 后缀指针
        int prefix = 0; // 前缀指针
        while (postfix < pattern.length()) { // 如果后缀指针小于目标字符串
            while ((prefix > 0) && (pattern.charAt(postfix) != pattern.charAt(prefix))) {
                prefix = next[prefix - 1];
            }
            if (pattern.charAt(postfix) == pattern.charAt(prefix)) {
                ++prefix;
            }
            next[postfix]=prefix;
            ++postfix;
        }
    }

    public void useKMP() {
        final List<Integer> match_positions = new ArrayList<>(); // 匹配位置
        int match_start_position = 0;
        int str = 0; // 字符串指针
        int patternPtr = 0; // 模式字符串指针
        buildNext();
        while (str < target.length()) {
            while ((patternPtr > 0) && (target.charAt(str) != pattern.charAt(patternPtr))) {
                patternPtr = next[patternPtr - 1];
            }
            if (target.charAt(str) == pattern.charAt(patternPtr)) {
                ++patternPtr;
                if (patternPtr == pattern.length()) {
                    match_start_position = str - pattern.length() + 1;
                    match_positions.add(match_start_position);
                    patternPtr = next[patternPtr - 1];
                }
            }
            ++str;
        }
        System.out.println(target);
        for (int i = 0; i < match_start_position; i++) {
            System.out.print(" ");
        }
        System.out.println(pattern);
    }
}
