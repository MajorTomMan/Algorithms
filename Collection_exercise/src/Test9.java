import java.util.Iterator;
import java.util.LinkedList;

public class Test9 {
	public static void Count(LinkedList<String> list){
		int i = 0,j = 0,k = 0,z = 0;
		Iterator<String> iter=list.iterator();
		while(iter.hasNext()){  //�����鿴�Ƿ���ʣ��Ԫ��
		   String charset=iter.next(); //��ȡ��������һ��Ԫ��
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
		 * һ������һ������ΪString���͵�List���ϣ�
		 * ͳ�Ƹü�����ÿ���ַ���ע�⣬�����ַ��������ֵĴ�����
		 * ���磺�������С�abc������bcd������Ԫ�أ���������������Ϊ����a = 1,b = 2,c = 2,d = 1����
		 */
		LinkedList<String> list=new LinkedList<String>();
		list.add("abc");
		list.add("bcd");
		Count(list);
	}

}
