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
			System.out.println("���Ϊ:"+k[j]);
			System.out.println("����Ϊ:"+name[j]);
			
		}
	}
	public static void AddName(Map<Integer,String> map){
		map.put(5, "������");
		for(String Name:map.values()){
			System.out.println(Name);
		}
	}
	public static void deleteinformation(Map<Integer,String> map){
		Iterator<Integer> iter=map.keySet().iterator();
		while(iter.hasNext()){  //��hasNext���ж���û��Ԫ�صĴ���
			Integer key=iter.next(); //ʹ��next()��������е���һ��Ԫ�ء�
			if(key==1){
				iter.remove(); //����һ�η��ص�Ԫ�شӵ��������Ƴ�
				System.out.println(map.values());
			}
		}
		
	}
	public static void updateinformation(Map<Integer,String> map){
		for(Integer key:map.keySet()){
			if(key==2){
				map.replace(2, "����");
			}
		}
		System.out.println(map.values());
	}
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		//This is Test 8 Code
		/*
		 Ҫ��
		1.�������ϣ�-������Ӧ������ӡ��
		2.���map�����в���һ������Ϊ5����Ϊ���������Ϣ
       	3.�Ƴ���map�еı��Ϊ1����Ϣ
       	4.��map�����б��Ϊ2��������Ϣ�޸�Ϊ"����"
		 */
		Map<Integer,String> map = new HashMap<Integer, String>();
        map.put(1, "������");
        map.put(2, "������");
        map.put(3, "����");
        map.put(4, "���ʦ̫");
    //    printList(map);
      //  AddName(map);
      //  deleteinformation(map);
        updateinformation(map);
	}

}
