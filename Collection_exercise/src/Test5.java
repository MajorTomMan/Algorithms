import java.util.*;

public class Test5{
	public static void Method() {
		ArrayList<Integer> List=new ArrayList<Integer>();
		Random rn=new Random();
		int Rnum1=(rn.nextInt(16)+1);
		for(int i=0;i<5;i++) {
			int Rnum=(rn.nextInt(33)+1);
			List.add(Rnum);
		}
		List.add(Rnum1);
		for(int i:List) {
			int j=List.indexOf(i);
			int k=j+1;
			System.out.println("�����"+k+"������:"+i);
			if(j==5) {
				System.out.println("�����1������:"+i);
			}
		}
		System.out.println("------------------------");
		HashSet<Integer> hashset=new HashSet<Integer>();
		hashset.addAll(List);
		for(int i:hashset) {
			System.out.println(i);
		}
	}
	public static void main(String[] args) {
		// TODO Auto-generated method stub
       // Test code 4
		/*
		˫ɫ�����˫ɫ��ÿעͶע������6����ɫ������1����ɫ�������ɡ���ɫ������1��33��ѡ����ɫ������1��16��ѡ�����������һע˫ɫ����롣��Ҫ��ͬɫ���벻�ظ���
		 */
		Method();
	}

}
