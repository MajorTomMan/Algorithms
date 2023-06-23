package linear;




public class 矩阵中的路径 {
    public static void main(String[] args) {
        char[][] board = { {'a','a'} };
        String word = "a";
        printMap(board);
        System.out.println("----------------------------------------------");
        System.out.println(exist(board, word));
    }

    public static boolean exist(char[][] board, String word) {
        char[] words = word.toCharArray();
        return backtracking(board, words, 0, 0, 0);
    }
    private static void printMap(char[][] board){
        for(int i=0;i<board.length;i++){
            for(int j=0;j<board[i].length;j++){
                System.out.print(board[i][j]+" ");
            }
            System.out.println();
        }
    }

    /**
     * 
     * @param board 字符串地图
     * @param x   起始点横坐标
     * @param y   起始点纵坐标
     * @param k   目标字符串索引
     * @return
     */
    // 处理不了只有一行的矩阵
    private static boolean backtracking(char[][] board,char[] word,int x,int y,int k){
        if(x==board.length||y==board[0].length){
            return false;
        }
        else if(x<0||y<0){
            return false;
        }
        else if(board[x][y]!=word[k]){
            return false;
        }
        if(k==word.length-1){
            return true;
        }
        else{
            board[x][y]=',';
            // 向下递归
            printMap(board);
            System.out.println("----------every step---------");
            if(backtracking(board, word, x+1,y, k+1)){
                board[x][y]=word[k];
                printMap(board);
                System.out.println("----------every step---------");
                return true;
            }
            // 向右递归
            if(backtracking(board, word, x, y+1, k+1)){
                board[x][y]=word[k];
                printMap(board);
                System.out.println("----------every step---------");
                return true;
            }
            // 向上递归
            if(backtracking(board, word, x-1, y, k+1)){
                board[x][y]=word[k];
                printMap(board);
                System.out.println("----------every step---------");
                return true;
            }
            // 向左递归
            if(backtracking(board, word, x, y-1, k+1)){
                board[x][y]=word[k];
                printMap(board);
                System.out.println("----------every step---------");
                return true;
            }
        }
        return false;
    }
/*  正确答案
    public boolean exist(char[][] board, String word) {
        for(int i = 0; i < board.length; i++)
            for(int j = 0; j < board[i].length; j++)
                if(dfs(board,word,0,i,j)) return true;
        return false;   
    }
    int[] dx = new int[]{-1,0,1,0}, dy = new int[]{0,1,0,-1};
    boolean dfs(char[][] board, String word,int u,int x,int y)
    {
        if(board[x][y] != word.charAt(u)) return false;
        if(u == word.length() - 1)   return true;
        char t = board[x][y];
        board[x][y] = '.';
        for(int i = 0; i < 4; i++)
        {
            int a = x + dx[i], b = y + dy[i];
            if(a < 0 || a >= board.length|| b < 0 || b >= board[0].length || board[a][b] == '.')  continue;
            if(dfs(board,word,u+1,a,b)) return true;
        }
        board[x][y] = t;
        return false;
    } */
}
