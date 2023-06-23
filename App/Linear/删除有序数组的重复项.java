package linear;

import java.util.ArrayList;
import java.util.List;

public class 删除有序数组的重复项 {
    private static List<Integer> list = new ArrayList<>();

    public static void main(String[] args) {
        int[] nums = { 0, 0, 1, 1, 1, 2, 2, 3, 3, 4 };
        System.out.println(removeDuplicates(nums));
    }

    public static int removeDuplicates(int[] nums) {
        if (nums == null || nums.length == 0) {
            return 0;
        }
        int i = 0, j = 0;
        list.add(nums[0]);
        while (i < nums.length) {
            while (j < nums.length && nums[j] == nums[i]) {
                j++;
            }
            if (j != nums.length) {
                nums[i + 1] = nums[j];
                list.add(nums[j]);
            }
            i++;
        }
        if (list.size() == 1) {
            nums[1] = 0;
        }
        for (int z : nums) {
            System.out.printf(z + " ");
        }
        return list.size();
    }
}
