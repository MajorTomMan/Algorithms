import java.util.*;


public class Test2{
	// Test code 1
	 /*
	 1.产生10个1-100的随机数，并放到一个数组中，
	  * 把数组中大于等于10的数字放到一个list集合中，并打印到控制台。
	 */
	public static void main(String[] args){
		int array[]=new int[10];
		ArrayList<Integer> list=new ArrayList<Integer>();
		Random rn=new Random();
		//随机
		for(int i=0;i<10;i++){
			int output=(rn.nextInt(100)+1);
			array[i]=output;
		}
		//添加
			for(int i=0;i<10;i++){
				if(array[i]>=10){
					list.add(array[i]);
				}
			}
		//输出
		for(int i=0;i<10;i++){
			if(list.get(i)==null) {
				break;
			}
			System.out.println(list.get(i));
		}
	}
}