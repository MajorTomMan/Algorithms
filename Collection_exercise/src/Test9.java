import java.util.Iterator;
import java.util.LinkedList;

public class Test9 {
	public static void Count(LinkedList<String> list){
		int i = 0,j = 0,k = 0,z = 0;
		Iterator<String> iter=list.iterator();
		while(iter.hasNext()){  //用来查看是否有剩余元素
		   String charset=iter.next(); //获取集合中下一个元素
		   char[] charactset=charset.toCharArray();
		   for(int num=0;num<charactset.length;num++){
			   char charact=charactset[num];
			   switch(charact){
			   case 'a':
				  i++;
				  break;
			   case 'b':
				   j++;
				   break;
			   case 'c':
				   k++;
				   break;
			   case 'd':
				   z++;
			       break;
			   }   
		   }
		}
		System.out.printf("a=%d,b=%d,c=%d,d=%d",i,j,k,z);
	}
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		/*
		 * 一、定义一个泛型为String类型的List集合，
		 * 统计该集合中每个字符（注意，不是字符串）出现的次数。
		 * 例如：集合中有”abc”、”bcd”两个元素，程序最终输出结果为：“a = 1,b = 2,c = 2,d = 1”。
		 */
		LinkedList<String> list=new LinkedList<String>();
		list.add("abc");
		list.add("bcd");
		Count(list);
	}

}
