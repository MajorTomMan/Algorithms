import java.util.*;

public class Test3 {

	public static int listTest(ArrayList<Integer> al,int s) {
		int i=0;
		for(int j=0;j<al.size();j++) {
			if(s==al.get(j)) {
				i=al.indexOf(j);
				return i;
			}
		}
		return -1;
	}
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		// Test code 2 
		/*
		2.定义一个方法listTest(ArrayList<Integer> al, Integer s)，
		要求返回s在al里面第一次出现的索引，如果s没出现过返回-1。
		 */
		ArrayList<Integer> list=new ArrayList<Integer>();
		Random rn=new Random();
		int j=(rn.nextInt(12)+1);
		int k=(rn.nextInt(20)+1);
		System.out.println(j);
		System.out.println(k);
		for(int i =0;i<10;i++) {
			list.add(i);
			}
		int i=listTest(list,k);
		System.out.println(i);
	}

}
