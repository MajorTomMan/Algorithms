public class 加一 {
    public static void main(String[] args) {
        int[] nums={1,2,9,9};
        System.out.println(plusOne(nums));
    }
    public static int[] plusOne(int[] digits) {
        int n = digits.length;
        for (int i = n - 1; i >= 0; --i) {
            //先找出九有几个,然后当当前数组元素不等于9时意味着后面全是九,将其改成9即可
            if (digits[i] != 9) {
                ++digits[i];
                for (int j = i + 1; j < n; ++j) {
                    digits[j] = 0;
                }
                return digits;
            }
        }

        // digits 中所有的元素均为 9
        int[] ans = new int[n + 1];
        ans[0] = 1;
        return ans;
    }
}
