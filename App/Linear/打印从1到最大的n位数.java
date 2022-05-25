public class 打印从1到最大的n位数 {
    public static void main(String[] args) {
        int[] nums=printNumbers(3);
        for(int data:nums){
            System.out.print(data+" ");
        }
    }

    public static int[] printNumbers(int n) {
        if(n==0){
            return new int[1];
        }
        String s="";
        int i=0;
        while(i!=n){
            s+=""+9;
            i++;
        }
        int times=Integer.parseInt(s);
        int[] result=new int[times];
        for(i=1;i<=times;i++){
            result[i-1]=i;
        }
        return result;
    }
}
