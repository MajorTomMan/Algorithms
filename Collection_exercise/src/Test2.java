import java.util.*;


public class Test2{
	// Test code 1
	 /*
	 1.����10��1-100������������ŵ�һ�������У�
	  * �������д��ڵ���10�����ַŵ�һ��list�����У�����ӡ������̨��
	 */
	public static void main(String[] args){
		int array[]=new int[10];
		ArrayList<Integer> list=new ArrayList<Integer>();
		Random rn=new Random();
		//���
		for(int i=0;i<10;i++){
			int output=(rn.nextInt(100)+1);
			array[i]=output;
		}
		//���
			for(int i=0;i<10;i++){
				if(array[i]>=10){
					list.add(array[i]);
				}
			}
		//���
		for(int i=0;i<10;i++){
			if(list.get(i)==null) {
				break;
			}
			System.out.println(list.get(i));
		}
	}
}