import java.util.HashMap;

public class Test8 {
	public static void ReadList(String[] arr,String[] arr1){
		HashMap<String, String> map=new HashMap<String, String>();
		String provincial_capital;
		String city;
		//循环输入至map集合
		for(int i=0;i<arr.length;i++){
			provincial_capital=arr[i];
			city=arr1[i];
			map.put(provincial_capital, city);
		}
		//输出
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
		 * 一、有2个数组，第一个数组内容为：[黑龙江省,浙江省,江西省,广东省,福建省]，
		 * 第二个数组为：[哈尔滨,杭州,南昌,广州,福州]，将第一个数组元素作为key，
		 * 第二个数组元素作为value存储到Map集合中。如{黑龙江省=哈尔滨, 浙江省=杭州, …}。
		 */
		String[] arr={"黑龙江省","浙江省","江西省","广东省","福建省"};
		String[] arr1={"哈尔滨","杭州","南昌","广州","福州"};
		ReadList(arr,arr1);
	}

}
