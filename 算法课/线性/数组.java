/*
public class 数组 {
    public static void main(String[] args){
        int length=16;
        DataStructure dataStructure=new DataStructure();
        dataStructure.stulist(length);
        Student[] stuList= DataStructure.getStuList();
        for (int i=0;i<length;i++) {
            Student student=stuList[i];
            student.setId(i);
            stuList[i]=student;
        }
        Student student=new Student();
        student.setId(7);
        ListOperator listOperator=new ListOperator(stuList, length);
        listOperator.Insert(4, student);
        listOperator.Show();
        listOperator.Delete(4);
        listOperator.Show();
    }
}

class Student {
    private int id;
    private int age;
    private String name;
    private String nativeplace;
    private char genger;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getNativeplace() {
        return nativeplace;
    }

    public void setNativeplace(String nativeplace) {
        this.nativeplace = nativeplace;
    }

    public char getGenger() {
        return genger;
    }

    public void setGenger(char genger) {
        this.genger = genger;
    }
}

class DataStructure {
    private static Student[] stuList;
    private int maxlength;
    private int length;

    public void stulist(int maxlength) {
        Init(maxlength);
    }

    public void Init(int maxlength) {
        stuList = new Student[maxlength];
        for (int i = 0; i < maxlength; i++) {
            Student student = new Student();
            student.setName("");
            student.setNativeplace("");
            student.setGenger(' ');
            stuList[i] = student;
        }
        this.maxlength = maxlength;
    }

    public int getLength() {
        return length;
    }

    public void setLength(int length) {
        this.length = length;
    }

    public static Student[] getStuList() {
        return stuList;
    }

    public static void setStuList(Student[] stuList) {
        DataStructure.stuList = stuList;
    }

    public int getMaxlength() {
        return maxlength;
    }

    public void setMaxlength(int maxlength) {
        this.maxlength = maxlength;
    }

    public void setId(int i) {
    }
}

class ListOperator {
    private static Student[] stuList;
    private static int length;

    public ListOperator(Student[] stuList, int length) {
        ListOperator.stuList = stuList;
        ListOperator.length = length;
    }

    public void Show() {
        for (Student student : stuList) {
            System.out.println(student.getId());
        }
        System.out.println("-----------------------");
    }

    public boolean Insert(int index, Student student) {
        boolean success = false;
        for (; index < length; index++) {
            Student Stunext = stuList[index];
            stuList[index] = student;
            student = Stunext;
            success = true;
        }
        return success;
    }

    public boolean Delete(int index) {
        boolean success = false;
        while(true) {
                if(index+1<length){
                    Student Stunext = stuList[index+1];
                    stuList[index]=Stunext;
                    success=true;
                }
                else{
                    Student Stunext=new Student();
                    Stunext.setId(0);
                    stuList[index]=Stunext;
                    break;
                }
                index++;
        }
        return success;
    }
    public static void Update(){
    
    }
    
}


*/