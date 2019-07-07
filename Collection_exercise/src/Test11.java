
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Scanner;

public class Test11 {
	//�洢��վ�б�
	private static Scanner sc;
	public static Map<Integer, String> Read(){
		Map<Integer, String> map=new HashMap<>();
		String[] Name={"����ׯ","��֪·","ƽ����","�����۶����","��Ӫ","����","��С��","��̩ׯ","������","ɭ�ֹ�԰����","����ƥ�˹�԰","��������","������"};
		Integer[] num={1,2,3,4,5,6,7,8,9,10,11,12,13};
		for(int i=0;i<13;i++){
			map.put(num[i], Name[i]);
		}
		return map;
	}
	//�����վ�б�
	public static void print(Map<Integer, String> map){
		for(Entry<Integer, String> i:map.entrySet()){
			System.out.println("��"+i.getKey()+"վ:"+" "+i.getValue());
		}
	}
	//����������վ�ۼƼ۸�
	public static void PrintPriceAndStationAndCountTime(Map<Integer, String> map,String start,String over){
		int num1 = 0,num2 = 0,price;
		for(Entry<Integer,String> i:map.entrySet()){
			if(i.getValue().equals(start)){
				num1=i.getKey();
			}
			else if(i.getValue().equals(over)){
				num2=i.getKey();
			}
	}
		int Count=num2-num1;
		int CountTime=num2*2;
		switch(Count){
		case 0:
		case 1:
		case 2:
			price=3;
			System.out.println("���γ�����������վ�ۼ��շ�Ϊ:"+price+"Ԫ"+" "+"һ����Ҫ"+CountTime+"����");
			break;
		case 3:
		case 4:
		case 5:
			price=4;
			System.out.println("���γ�����������վ�ۼ��շ�Ϊ:"+price+"Ԫ"+" "+"һ����Ҫ"+CountTime+"����");
			break;
		case 6:
			price=6;
			System.out.println("���γ�����������վ�ۼ��շ�Ϊ:"+price+"Ԫ"+" "+"һ����Ҫ"+CountTime+"����");
			break;
		case 7:
			price=8;
			System.out.println("���γ�����������վ�ۼ��շ�Ϊ:"+price+"Ԫ"+" "+"һ����Ҫ"+CountTime+"����");
			break;
		default:
			price=10;
			System.out.println("���γ�����������վ�ۼ��շ�Ϊ:"+price+"Ԫ"+" "+"һ����Ҫ"+CountTime+"����");
			break;
		}
	}
    public static void check(Map<Integer, String> map,String start,String over) {
			Boolean startkey=map.containsValue(start);
			Boolean overkey=map.containsValue(over);
			if(startkey!=true||overkey!=true){
				System.out.println("��������ϳ�վ:"+start+"���³�վ:"+over+"������,����������");
				System.out.println("���������վ:");
				sc=new Scanner(System.in);
				String start1=sc.nextLine();
				System.out.println("���������վ:");
				String over1=sc.nextLine();
				check(map, start1, over1);
			}
			else{
				PrintPriceAndStationAndCountTime(map, start, over);
			}
			
	}
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		/*
		 * 1.վ��ź�վ����Ӧ��ϵ���£�
            1=����ׯ
            2=��֪·
            3=ƽ����
            4=�����۶����
            5=��Ӫ
       //....
�����϶�Ӧ��ϵ�����ݴ洢��map�����У�key����ʾվ��ţ�value����ʾվ������������ӡ(���Բ���˳���ӡ)��
       ��10վ: ɭ�ֹ�԰����
       ��6վ: ����
       ��12վ: ��������
       ��13վ: ������
       //...
2.�������Ʊ�۹���
       ���г� 3վ�ڣ�����3վ���շ�3Ԫ��
            3վ���ϵ�������5վ������5վ�����շ�4Ԫ��
            5վ���ϵģ���4Ԫ�Ļ����ϣ�ÿ��1վ����2Ԫ��
            10Ԫ�ⶥ��
3.��ӡ��ʽ����Ҫ�Լ���¼����ϳ�վ�͵���վ�����жϣ����û�и�վ����ʾ�������룬ֱ��վ������Ϊֹ����
       ע�⣺ÿվ��Ҫ2����
       �������ϳ�վ��
       ɳ��
       ��������ϳ�վ��ɳ�Ӳ����ڣ������������ϳ�վ��
       �ϵ�
       ��������ϳ�վ���ϵز����ڣ������������ϳ�վ��
       ����ׯ
       �����뵽��վ��
       ɳ��
       ������ĵ���վ��ɳ�Ӳ����ڣ����������뵽��վ��
       ������
       ������ĵ���վ�������첻���ڣ����������뵽��վ��
       ��С��
       ������ׯ����С�ڹ�����6վ�շ�6Ԫ����Լ��Ҫ 12����
		 */
		Map<Integer, String> map=Read();
		System.out.println("���г�վ�б�:");
		print(map);
		System.out.println("���������վ:");
		sc=new Scanner(System.in);
		String start=sc.nextLine();
		System.out.println("���������վ:");
		String over=sc.nextLine();
		check(map, start, over);
	}

}
