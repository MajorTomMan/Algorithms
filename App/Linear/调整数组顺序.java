
import java.util.Scanner;

public class 调整数组顺序 {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        int[] nums=new int[999];
        int i=0;
        while(scanner.hasNextInt()){
            nums[i]=scanner.nextInt();
            i++;
        }
        reverse(nums);
        String s = "[";
        for (int data : nums) {
            if (data == 0) {
                continue;
            }
            s += data + ",";
        }
        s += "]";
        System.out.println(s);
        scanner.close();
    }

    public static void reverse(int[] nums) {
        for (int i = 0; i < nums.length; i++) {
            for (int j = i + 1; j < nums.length; j++) {
                if (nums[i] % 2 == 0 && nums[j] % 2 != 0) {
                    int temp = nums[i];
                    nums[i] = nums[j];
                    nums[j] = temp;
                }
            }
        }

    }
}
