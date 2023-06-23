package linear;



public class 调整数组顺序 {
    public static void main(String[] args) {
        int[] nums = { 2, 16, 3, 5, 13, 1, 16, 1, 12, 18, 11, 8, 11, 11, 5, 1 };
        nums = reverse(nums);
        for (int i : nums) {
            System.out.print(i + " ");
        }
    }
    // 快慢指针法
    public static int[] reverse(int[] nums) {
        if (nums.length == 0) {
            return nums;
        }
        int left = 0, right = nums.length - 1;
        while (left < right) {
            while (left <= right && nums[left] % 2 == 1) {
                left++;
            }
            while (left <= right && nums[right] % 2 == 0) {
                right--;
            }
            if (left > right) {
                break;
            }
            int tmp = nums[left];
            nums[left] = nums[right];
            nums[right] = tmp;
        }
        return nums;
    }
}
