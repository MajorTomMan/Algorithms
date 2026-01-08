package com.majortom.algorithms.app.leetcode.linear;


import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

public class 多数元素 {
    private static Map<Integer,Integer> map=new HashMap<>();
    public static void main(String[] args) {
        Integer[] nums={2,2,1,1,1,2,2};
        System.out.println(majorityElement(nums));
    }
    public static int majorityElement(Integer[] nums) {
        int n=nums.length;
        int frequency=n/2;
        for(int i=0;i<n;i++){
            if(map.containsKey(nums[i])){
                map.put(nums[i],map.get(nums[i])+1);
            }
            else{
                map.put(nums[i],1);
            }
        }
        for(Entry<Integer, Integer> data:map.entrySet()){
            if(data.getValue()>frequency){
                return data.getKey();
            }
        }
        return -1;
    }
}
