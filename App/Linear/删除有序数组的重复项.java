
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

public class 删除有序数组的重复项 {
    private static Set<Integer> set=new LinkedHashSet<>();
    public static void main(String[] args) {
        int[] nums={1,1,2};
        System.out.println(removeDuplicates(nums));
    }
    public static int removeDuplicates(int[] nums) {
        for (int data : nums) {
            set.add(data);
        }
        nums=new int[set.size()];
        Integer[] ttt=new Integer[set.size()];
        Integer[] temp=(Integer[])set.toArray(ttt);
        for (int index = 0; index < temp.length; index++) {
            nums[index]=temp[index].intValue();
        }
        return nums.length;
    }
}
