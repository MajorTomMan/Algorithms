import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

 class Student implements Comparable<Student>{
	private String Name;
	private int Age;
	private float Score;
	Student(String Name,int Age,float score) {
		this.Name=Name;
		this.Age=Age;
		this.Score=score;
	}
	public String GetName() {
		return Name;
	}
	public int GetAge() {
		return Age;
	}
	public float GetScore() {
		return Score;
	}
	public void SetAge(int Age) {
		this.Age=Age;
	}
	public void SetName(String Name) {
		this.Name=Name;
	}
	public void SetScore(float Score)  {
		this.Score=Score;
        }
	@Override
	public String toString() {
		return 
				"Name='" + Name + '\'' +
				", Age=" + Age +
				", Score=" + Score +
				"\n";
	}
	public int compareTo(Student o) {
		// TODO Auto-generated method stub
		//����һ���м�����жϳɼ��Ĵ�С ����ɼ���� �Ƚ�����
		Object output=((o.GetScore())-this.GetScore());
		System.out.println(output);
		int result=(int)((o.GetScore())-this.GetScore());
		if(result==0) {
			result=o.GetAge()-this.GetAge();
		}
		return result;
	}
}

public class Test6{
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		// Test Code 6
		/*
		 �ֱ���Comparable��Comparator�����ӿڶ�������λͬѧ�ĳɼ���������������ɼ�һ�������ڳɼ�����Ļ����ϰ���������С��������
		 ������String��

���䣨int��

������float��

liusan

20

90.0F

lisi

22

90.0F

wangwu

20

99.0F

sunliu

22

100.0F

��дһ��Student������ʵ��Comparable<Student>�ӿ�,����������дCompareTo(Student o)��������
		 */
		Student stu1 = new Student("liusan",20,90.0f);
		Student stu2 = new Student("lisi",22,90.0f);
		Student stu3 = new Student("wangwu",20,90.0f);
		Student stu4 = new Student("sunliu",20,100.0f);
		System.out.println("��������");
		ArrayList<Student> arr=new ArrayList<>();
		arr.add(stu1);
		arr.add(stu2);
		arr.add(stu3);
		arr.add(stu4);
		Collections.sort(arr);
		System.out.println(arr);
		Collections.shuffle(arr); //���ڴ��Ҽ����е�Ԫ��
		Collections.sort(arr,new Comparator<Student>() {

			@Override
			public int compare(Student o1, Student o2) {
				// TODO Auto-generated method stub
				int result=(int)((o2.GetScore())-o1.GetScore());
				System.out.println(result);
				System.out.println("o2����Ϊ:"+o2.GetScore());
				System.out.println("o1����Ϊ:"+o1.GetScore());
				if(result==0) {
					result=o2.GetAge()-o1.GetAge();
				}
				
				System.out.println(result);
				return result;
			}
			
		});
		System.out.println("��������");
		System.out.println(arr);
	}
}