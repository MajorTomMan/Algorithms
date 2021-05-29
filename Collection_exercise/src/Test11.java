
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Scanner;

public class Test11 {
	//存储车站列表
	private static Scanner sc;
	public static Map<Integer, String> Read(){
		Map<Integer, String> map=new HashMap<>();
		String[] Name={"朱辛庄","育知路","平西府","回龙观东大街","霍营","育新","西小口","永泰庄","林萃桥","森林公园南门","奥林匹克公园","奥体中心","北土城"};
		Integer[] num={1,2,3,4,5,6,7,8,9,10,11,12,13};
		for(int i=0;i<13;i++){
			map.put(num[i], Name[i]);
		}
		return map;
	}
	//输出车站列表
	public static void print(Map<Integer, String> map){
		for(Entry<Integer, String> i:map.entrySet()){
			System.out.println("第"+i.getKey()+"站:"+" "+i.getValue());
		}
	}
	//计算所经过站累计价格
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
			System.out.println("本次乘坐地铁所经站累计收费为:"+price+"元"+" "+"一共需要"+CountTime+"分钟");
			break;
		case 3:
		case 4:
		case 5:
			price=4;
			System.out.println("本次乘坐地铁所经站累计收费为:"+price+"元"+" "+"一共需要"+CountTime+"分钟");
			break;
		case 6:
			price=6;
			System.out.println("本次乘坐地铁所经站累计收费为:"+price+"元"+" "+"一共需要"+CountTime+"分钟");
			break;
		case 7:
			price=8;
			System.out.println("本次乘坐地铁所经站累计收费为:"+price+"元"+" "+"一共需要"+CountTime+"分钟");
			break;
		default:
			price=10;
			System.out.println("本次乘坐地铁所经站累计收费为:"+price+"元"+" "+"一共需要"+CountTime+"分钟");
			break;
		}
	}
    public static void check(Map<Integer, String> map,String start,String over) {
			Boolean startkey=map.containsValue(start);
			Boolean overkey=map.containsValue(over);
			if(startkey!=true||overkey!=true){
				System.out.println("您输入的上车站:"+start+"或下车站:"+over+"不存在,请重新输入");
				System.out.println("请输入出发站:");
				sc=new Scanner(System.in);
				String start1=sc.nextLine();
				System.out.println("请输入结束站:");
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
		 * 1.站编号和站名对应关系如下：
            1=朱辛庄
            2=育知路
            3=平西府
            4=回龙观东大街
            5=霍营
       //....
将以上对应关系的数据存储到map集合中，key：表示站编号，value：表示站名，并遍历打印(可以不按顺序打印)：
       第10站: 森林公园南门
       第6站: 育新
       第12站: 奥体中心
       第13站: 北土城
       //...
2.计算地铁票价规则：
       总行程 3站内（包含3站）收费3元，
            3站以上但不超过5站（包含5站）的收费4元，
            5站以上的，在4元的基础上，每多1站增加2元，
            10元封顶；
3.打印格式（需要对键盘录入的上车站和到达站进行判断，如果没有该站，提示重新输入，直到站名存在为止）：
       注意：每站需要2分钟
       请输入上车站：
       沙河
       您输入的上车站：沙河不存在，请重新输入上车站：
       上地
       您输入的上车站：上地不存在，请重新输入上车站：
       朱辛庄
       请输入到达站：
       沙河
       您输入的到达站：沙河不存在，请重新输入到达站：
       西二旗
       您输入的到达站：西二旗不存在，请重新输入到达站：
       西小口
       从朱辛庄到西小口共经过6站收费6元，大约需要 12分钟
		 */
		Map<Integer, String> map=Read();
		System.out.println("所有车站列表:");
		print(map);
		System.out.println("请输入出发站:");
		sc=new Scanner(System.in);
		String start=sc.nextLine();
		System.out.println("请输入结束站:");
		String over=sc.nextLine();
		check(map, start, over);
	}

}
