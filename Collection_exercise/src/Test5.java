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
			System.out.println("红球第"+k+"个号码:"+i);
			if(j==5) {
				System.out.println("蓝球第1个号码:"+i);
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
		双色球规则：双色球每注投注号码由6个红色球号码和1个蓝色球号码组成。红色球号码从1—33中选择；蓝色球号码从1—16中选择；请随机生成一注双色球号码。（要求同色号码不重复）
		 */
		Method();
	}

}
