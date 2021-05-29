import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class Test7 {

	public static void printList(Map<Integer,String> map){
		Set<Integer> i=map.keySet();
		Object[] k=i.toArray();
		Collection<String> Name=map.values();
		Object[] name=Name.toArray();
		for(int j=0;j<map.size();j++){
			System.out.println("序号为:"+k[j]);
			System.out.println("姓名为:"+name[j]);
			
		}
	}
	public static void AddName(Map<Integer,String> map){
		map.put(5, "李晓红");
		for(String Name:map.values()){
			System.out.println(Name);
		}
	}
	public static void deleteinformation(Map<Integer,String> map){
		Iterator<Integer> iter=map.keySet().iterator();
		while(iter.hasNext()){  //用hasNext来判断有没有元素的存在
			Integer key=iter.next(); //使用next()获得序列中的下一个元素。
			if(key==1){
				iter.remove(); //将上一次返回的元素从迭代器中移除
				System.out.println(map.values());
			}
		}
		
	}
	public static void updateinformation(Map<Integer,String> map){
		for(Integer key:map.keySet()){
			if(key==2){
				map.replace(2, "周林");
			}
		}
		System.out.println(map.values());
	}
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		//This is Test 8 Code
		/*
		 要求：
		1.遍历集合，-序号与对应人名打印。
		2.向该map集合中插入一个编码为5姓名为李晓红的信息
       	3.移除该map中的编号为1的信息
       	4.将map集合中编号为2的姓名信息修改为"周林"
		 */
		Map<Integer,String> map = new HashMap<Integer, String>();
        map.put(1, "张三丰");
        map.put(2, "周芷若");
        map.put(3, "汪峰");
        map.put(4, "灭绝师太");
    //    printList(map);
      //  AddName(map);
      //  deleteinformation(map);
        updateinformation(map);
	}

}
