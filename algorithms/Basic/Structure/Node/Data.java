package Basic.Structure.Node;


public class Data<T>{
    public T saveData;
    public Data(T saveData) {
        this.saveData = saveData;
    }
    @Override
    public String toString() {
        return "Data [saveData=" + saveData + "]";
    }
}
