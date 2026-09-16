package com.majortom.algorithms.practice.exercises.princeton.others;

import edu.princeton.cs.algs4.RunLength;

import java.io.IOException;

public class 游程编码测试 {
    public static void main(String[] args) throws IOException {
        if (args[0].equals("-")) {
            RunLength.compress();
        } else if (args[0].equals("+")) {
            RunLength.expand();
        } else {
            throw new IllegalArgumentException("Illegal command line argument");
        }
    }
}
