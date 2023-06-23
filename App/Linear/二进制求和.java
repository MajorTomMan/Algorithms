package linear;



import java.util.Stack;

public class 二进制求和 {
    public static void main(String[] args) {
        System.out.println(addBinary(
                "10100000100100110110010000010101111011011001101110111111111101000000101111001110001111100001101",
                "110101001011101110001111100110001010100001101011101010000011011011001011101111001100000011011110011"));
    }

    public static String addBinary(String a, String b) {
        // 解决不了超大整型的数据,但一般的数据还可以解决
        // return transForm(calculateBinary(a, b));
        return anotherWay(a, b);
    }

    // 能处理超大数据量的方法
    public static String anotherWay(String a, String b) {
        StringBuffer ans = new StringBuffer();

        int n = Math.max(a.length(), b.length()), carry = 0;
        for (int i = 0; i < n; ++i) {
            carry += i < a.length() ? (a.charAt(a.length() - 1 - i) - '0') : 0;
            carry += i < b.length() ? (b.charAt(b.length() - 1 - i) - '0') : 0;
            ans.append((char) (carry % 2 + '0'));
            carry /= 2;
        }

        if (carry > 0) {
            ans.append('1');
        }
        ans.reverse();
        return ans.toString();
    }

    // 自己写的
    private static Long calculateBinary(String a, String b) {
        String s = "";
        if (a.length() != b.length()) {
            if (Math.min(a.length(), b.length()) == a.length()) {
                for (int k = 0; k < b.length() - a.length(); k++) {
                    s += "0";
                }
                s += a;
                a = s;
            } else {
                for (int k = 0; k < a.length() - b.length(); k++) {
                    s += "0";
                }
                s += b;
                b = s;
            }
        }
        int i = a.length() - 1, j = b.length() - 1;
        Long data_a = 0L, data_b = 0L;
        int temp = 1;
        while (i >= 0) {
            char c = a.charAt(i);
            data_a += Long.parseLong(String.valueOf(c)) * temp;
            i--;
            temp *= 2;
        }
        temp = 1;
        while (j >= 0) {
            char c = b.charAt(j);
            data_b += Long.parseLong(String.valueOf(c)) * temp;
            j--;
            temp *= 2;
        }
        return data_a + data_b;
    }

    private static String transForm(Long data) {
        if (data == 0) {
            return "0";
        }
        Stack<Long> stack = new Stack<>();
        String s = "";
        while (data != 0) {
            stack.push(data % 2);
            data = data / 2;
        }
        while (!stack.isEmpty()) {
            s += stack.pop();
        }
        return s;
    }
}
