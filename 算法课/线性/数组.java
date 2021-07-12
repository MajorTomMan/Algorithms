package 线性;

public class 数组{
    public static void main(String[] args) {
        int i=0;
        int[] array=new int[100];
        while(i!=array.length){
            array[i]=i;
            i++;
        }
        System.out.println("----------------删除前-----------------");
        array=insert(3,111, array);
        show(array);
        System.out.println("----------------删除后-----------------");
        array=delete(5,array);
        show(array);
    }
    private static int[] insert(int index,int data,int[] array){
        int i=0;
        int[] temp=new int[array.length+1];
        while(i!=array.length){
            temp[i]=array[i];
            i++;
        }
        i=index;
        int tempdata=0,nextdata=0,j=i+1;
        tempdata=temp[i];
        temp[i]=data;
        while(j!=temp.length-1){ //使用tempdata和nextdata来保证数组内数据不会丢失,即nextdata为tempdata下一索引的数据
            nextdata=temp[j];
            temp[j]=tempdata;
            j++;
            tempdata=nextdata;
        }
        temp[j++]=nextdata;
        return temp;
    }
    private static int[] delete(int index,int[] array){ //删除和增加相反就行了
        int i=array.length-1;
        int[] temp=new int[array.length-1];
        int tempdata=0,nextdata=0,j=i-1;
        tempdata=array[i];
        while(i!=index+1){
            nextdata=array[j];
            array[--i]=tempdata;
            tempdata=nextdata;
            j--;
        }
        i=0;
        while(i!=array.length-1){
            temp[i]=array[i];
            i++;
        }
        return temp;
    }
    private static void show(int[] array){
        for(int i=0;i<array.length;i++){
            System.out.print(array[i]+" ");
        }
        System.out.println("长度是:"+array.length);
    }
}
