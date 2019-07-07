import java.util.ArrayList;
import java.util.Iterator;

public class Test4{
	public static void Method(String[] array) {
		ArrayList<String> LinkedList=new ArrayList<String>();
		String QQnumber;
		int num=0;
		for(int i=0;i<array.length;i++) {
			LinkedList.add(array[i]);
		}
		while(true) {
			for(int j=0;j<LinkedList.size();j++) {
				QQnumber=LinkedList.get(j);
				for(int k=j+1;k<LinkedList.size();k++) {
					if(QQnumber.equals(LinkedList.get(k))) {
						LinkedList.remove(LinkedList.get(k));
					}
				}
			}
			num++;
			if(num==2) {
				break;
			}
		}
		Iterator<String> it=LinkedList.iterator();
		while(it.hasNext()) { //迭代器遍历输出
			System.out.println(it.next());
		}
		/*---------------------------------------------*/
		System.out.println("--------------------------------");
		for(String str:LinkedList) { //增强for循环输出
			System.out.println(str);
		}
	}

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		// Test code 3
		/*
		 
	3.已知数组存放一批QQ号码，QQ号码最长为11位，最短为5位,
	String[] strs = {"12345","67891","12347809933","98765432102","67891","12347809933"}。
	将该数组里面的所有qq号都存放在LinkedList中，将list中重复元素删除，将list中所有元素分别用迭代器和增强for循环打印出来。
		 */
		String[] strs = {"12345","67891","12347809933","98765432102","67891","12347809933"};
		Method(strs);
	}

}