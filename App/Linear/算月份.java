public class 算月份 {
    private static int[] m_Days = {
            31, 29, 31, 30, 31, 30, 31, 31, 30, 30, 31
    };

    public static void main(String[] args) {
        judge(1896, 2);
    }

    public static void judge(int years, int mouth) {
        boolean isLeapYears = false;
        if (years% 400==0 || years %4==0 && years%100!=0) {
            isLeapYears = true;
        } else {
        }
        if (isLeapYears == false) {
            System.out.println("该月天数为:" + m_Days[mouth-1]);
        } else {
            m_Days[1]=28;
            System.out.println("该月天数为:" + m_Days[mouth-1]);
        }
    }
}
