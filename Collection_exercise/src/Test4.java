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
		while(it.hasNext()) { //�������������
			System.out.println(it.next());
		}
		/*---------------------------------------------*/
		System.out.println("--------------------------------");
		for(String str:LinkedList) { //��ǿforѭ�����
			System.out.println(str);
		}
	}

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		// Test code 3
		/*
		 
	3.��֪������һ��QQ���룬QQ�����Ϊ11λ�����Ϊ5λ,
	String[] strs = {"12345","67891","12347809933","98765432102","67891","12347809933"}��
	�����������������qq�Ŷ������LinkedList�У���list���ظ�Ԫ��ɾ������list������Ԫ�طֱ��õ���������ǿforѭ����ӡ������
		 */
		String[] strs = {"12345","67891","12347809933","98765432102","67891","12347809933"};
		Method(strs);
	}

}