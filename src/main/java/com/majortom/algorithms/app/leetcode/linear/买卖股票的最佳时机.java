package com.majortom.algorithms.app.leetcode.linear;



public class 买卖股票的最佳时机 {
    public static void main(String[] args) {
        Integer prices[]={7,1,5,3,6,4};
        System.out.println(maxProfit(prices));
    }
    public static int maxProfit(Integer[] prices) {
        int minprice = Integer.MAX_VALUE;
        int maxprofit = 0;
        for (int i = 0; i < prices.length; i++) {
            if (prices[i] < minprice) {
                minprice = prices[i];
            } else if (prices[i] - minprice > maxprofit) {
                maxprofit = prices[i] - minprice;
            }
        }
        return maxprofit;
    }
}
