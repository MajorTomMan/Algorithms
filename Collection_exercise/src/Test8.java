import java.util.HashMap;

public class Test8 {
	public static void ReadList(String[] arr,String[] arr1){
		HashMap<String, String> map=new HashMap<String, String>();
		String provincial_capital;
		String city;
		//ѭ��������map����
		for(int i=0;i<arr.length;i++){
			provincial_capital=arr[i];
			city=arr1[i];
			map.put(provincial_capital, city);
		}
		//���
		for(String provincial_capitalName:map.keySet()){
			System.out.println(provincial_capitalName);
		}
		for(String cityName:map.values()){
			System.out.println(cityName);
		}
	}
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		/*
		 * һ����2�����飬��һ����������Ϊ��[������ʡ,�㽭ʡ,����ʡ,�㶫ʡ,����ʡ]��
		 * �ڶ�������Ϊ��[������,����,�ϲ�,����,����]������һ������Ԫ����Ϊkey��
		 * �ڶ�������Ԫ����Ϊvalue�洢��Map�����С���{������ʡ=������, �㽭ʡ=����, ��}��
		 */
		String[] arr={"������ʡ","�㽭ʡ","����ʡ","�㶫ʡ","����ʡ"};
		String[] arr1={"������","����","�ϲ�","����","����"};
		ReadList(arr,arr1);
	}

}
