
import java.util.*;
import java.util.Map.Entry;

public class Test10 {
	private static Scanner sc;
	public static Map<Integer, String> InquireWorldCup(Map<Integer, String> map){
		int j=0;
		String[] footballTeam={"������","�����","�����","������","�¹�","����","����","Ӣ����","����","����","����͢","�����","����͢","�¹�","����","����","����","�¹�","������","�¹�","����"};
		sc = new Scanner(System.in);
		Integer input=sc.nextInt();
		//����ݺͶ��������ݴ��map
		for(int i=1930;i<2019;){
			String footballteam=footballTeam[j];
			map.put(i, footballteam);
			i=i+4;
			if(i==1942){
				i=1950;
			}
			else if(i>2019){
				break;
			}
			j++;
		}
		if(input>=2019){
			System.out.println("�����ݽ�������2019��,����������:");
			InquireWorldCup(map);
		}
		if(input<1930){
			System.out.println("��һ�����籭����������1930��ڵ���������Ҷ�,����������:");
			InquireWorldCup(map);
		}
		//�ж��Ƿ������籭���
		Boolean key=map.containsKey(input);
		if(key==true){
			System.out.println(map.get(input));
		}
		else{
			System.out.println("û�оٰ����籭");
			return map;
		}
		return map;
	}
	public static void output(Map<Integer, String> map){
		int flag=0;
		sc = new Scanner(System.in);
		String input=sc.nextLine();
		for(Entry<Integer, String> num:map.entrySet()){
			if(num.getValue().equals(input)){
				System.out.println(num.getKey()+".");
			}
			if(!num.getValue().equals(input)){
				System.out.println("û�л�ù����籭");
			}
		}
	}
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		/*
		 * һ������Map���������Ĺ��ܣ�
�������ж���һ���ַ�������ʾһ����ݣ������������籭�ھ�����֧��ӡ������ ��û�оٰ����籭���������û�оٰ����籭��
//tips:����Map�ӿ�containsKey(Object key)���� 
 
������ԭ�����籭Map �Ļ����ϣ��������¹��ܣ� ����һ֧��ӵ����֣��������Ӷ�ڵ�����б� ���磬���롰��������Ӧ����� 1958 1962 1970 1994 2002 ���롰��������Ӧ����� û�л�ù����籭 
//tips:����Map�ӿ�containsValue(Object value)����
ʾ����
		 */
		Map<Integer, String> map=new HashMap<>();
		System.out.println("������Ҫ��ѯ�����籭���:");
		map=InquireWorldCup(map);
		System.out.println("������Ҫ��ѯ�Ĺ��Ҷ�:");
		output(map);
	}

}
