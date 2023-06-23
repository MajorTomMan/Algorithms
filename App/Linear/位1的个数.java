package linear;

import basic.structure.Stack;

public class 位1的个数 {
    public static void main(String[] args) {
        System.out.println(hammingWeight(0b00000000000000000000000000001011));
    }
    public static int hammingWeight(int n) {
        Stack<Integer> stack=new Stack<>();
        int[] bits;
        int i=0;
        while(n!=0){
            stack.push(n%2);
            n/=2;
        }
        int limit=32-stack.size();
        if(stack.size()!=32){
            while(i!=limit){
                stack.push(0);
                i++;
            }
        }
        stack.Show();
        i=0;
        bits=new int[stack.size()];
        while(!stack.isEmpty()){
            bits[i]=stack.pop();
            i++;
        }
        int sum=0;
        for(i=0;i<32;i++){
            if(bits[i]==1){
                sum++;
            }
        }
        return sum;
    }
}
