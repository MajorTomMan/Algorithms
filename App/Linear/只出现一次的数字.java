package linear;

public class 只出现一次的数字 {
    public static void main(String[] args) {
        int[] nums={4,1,2,1,2};
        System.out.println(singleNumber(nums));
    }
    public static int singleNumber(int[] nums) {
        int single=0;
        for(int num:nums){
            single^=num;
        }
        return single;
    }
}
