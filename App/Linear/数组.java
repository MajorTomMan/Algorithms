package linear;

public class 数组 {
    public static void main(String[] args) {
        int i = 0;
        Integer[] array = new Integer[100];
        while (i != array.length) {
            array[i] = i;
            i++;
        }
        System.out.println("----------------删除前-----------------");
        array = insert(3, 111, array);
        display(array);
        System.out.println("----------------删除后-----------------");
        array = delete(5, array);
        display(array);
    }

    private static Integer[] insert(int index, int data, Integer[] array) {
        int i = 0;
        Integer[] temp = new Integer[array.length + 1];
        while (i != array.length) {
            temp[i] = array[i];
            i++;
        }
        i = index;
        int tempdata = 0, nextdata = 0, j = i + 1;
        tempdata = temp[i];
        temp[i] = data;
        while (j != temp.length - 1) { // 使用tempdata和nextdata来保证数组内数据不会丢失,即nextdata为tempdata下一索引的数据
            nextdata = temp[j];
            temp[j] = tempdata;
            j++;
            tempdata = nextdata;
        }
        temp[j++] = nextdata;
        return temp;
    }

    private static Integer[] delete(int index, Integer[] array) { // 删除和增加相反就行了
        int i = array.length - 1;
        Integer[] temp = new Integer[array.length - 1];
        int tempdata = 0, nextdata = 0, j = i - 1;
        tempdata = array[i];
        while (i != index + 1) {
            nextdata = array[j];
            array[--i] = tempdata;
            tempdata = nextdata;
            j--;
        }
        i = 0;
        while (i != array.length - 1) {
            temp[i] = array[i];
            i++;
        }
        return temp;
    }

    private static void display(Integer[] array) {
        for (int i = 0; i < array.length; i++) {
            System.out.print(array[i] + " ");
        }
        System.out.println("长度是:" + array.length);
    }
}
