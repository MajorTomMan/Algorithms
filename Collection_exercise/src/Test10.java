
import java.util.*;
import java.util.Map.Entry;

public class Test10 {
	private static Scanner sc;
	public static Map<Integer, String> InquireWorldCup(Map<Integer, String> map){
		int j=0;
		String[] footballTeam={"乌拉圭","意大利","意大利","乌拉圭","德国","巴西","巴西","英格兰","巴西","西德","阿根廷","意大利","阿根廷","德国","巴西","法国","巴西","德国","西班牙","德国","法国"};
		sc = new Scanner(System.in);
		Integer input=sc.nextInt();
		//将年份和夺冠球队数据存进map
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
			System.out.println("该数据仅更新至2019年,请重新输入:");
			InquireWorldCup(map);
		}
		if(input<1930){
			System.out.println("第一个世界杯夺冠球队是在1930夺冠的乌拉圭国家队,请重新输入:");
			InquireWorldCup(map);
		}
		//判断是否是世界杯年份
		Boolean key=map.containsKey(input);
		if(key==true){
			System.out.println(map.get(input));
		}
		else{
			System.out.println("没有举办世界杯");
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
				System.out.println("没有获得过世界杯");
			}
		}
	}
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		/*
		 * 一、利用Map，完成下面的功能：
从命令行读入一个字符串，表示一个年份，输出该年的世界杯冠军是哪支球队。如果该 年没有举办世界杯，则输出：没有举办世界杯。
//tips:参阅Map接口containsKey(Object key)方法 
 
二、在原有世界杯Map 的基础上，增加如下功能： 读入一支球队的名字，输出该球队夺冠的年份列表。 例如，读入“巴西”，应当输出 1958 1962 1970 1994 2002 读入“荷兰”，应当输出 没有获得过世界杯 
//tips:参阅Map接口containsValue(Object value)方法
示例：
		 */
		Map<Integer, String> map=new HashMap<>();
		System.out.println("请输入要查询的世界杯年份:");
		map=InquireWorldCup(map);
		System.out.println("请输入要查询的国家队:");
		output(map);
	}

}
