public class 二车竞赛 {
    public static void main(String[] args) {
        int j=0;
        int k=0;
        int row=1;
        int A_front=1;
        int B_front=13;
        Map map=new Map();
        map.create(7, 7);
        map.Map[0][0]=3;
        map.Map[13][6]=4;
        map.Show(7, 7);
        while(row!=12){
            if(map.Map[A_front][j]==0&&A_front<14&&map.Map[A_front+1][j]!=4){
                map.Map[A_front][j]=3;
                map.Map[A_front-1][j]=0;
                A_front++;
            }
            else if(A_front==13){
                map.Map[A_front][row]=3;
                map.Map[A_front][row-1]=0;
                row++;
            }
        }
    }
}


class Map{
    private int  Matrix[][];
    private int Copy[][];
    public int Map[][];
    public void create(int width,int length){
        Matrix=new int[width][length];
        Copy=new int[width][length];
        addBarrier(length, width);
        Combination(Matrix, Copy, width, length);
    }
    public void addBarrier(int length,int width){
        for(int i=1;i<width-1;i++){
            for(int j=1;j<length-1;j++){
                Matrix[i][j]=1;
                Copy[i][j]=1;
            }
        }
    }
    public void Show(int length,int width){
        for(int i=0;i<width*2;i++){
            for(int j=0;j<length;j++){
                System.out.print(Map[i][j]);
            }
            System.out.println();
        }
    }
    public void Combination(int[][] Matrix,int[][] Copy,int width,int length){
        Map=new int[width*2][length];
        int z=0,k=0;
        for(int i=0;i<width;i++){
            for(int j=0;j<length;j++){
                Map[i][j]=Matrix[i][j];
            }
        }
        int result=width*2;
        for(int i=width;i<result;i++){
            for(int j=0;j<length;j++){
                Map[i][j]=Copy[z][k];
                k++;
            }
            k=0;
            z++;
        }
        Map[7][3]=2;
    }
}



