package com.majortom.algorithms.app.leetcode.linear;

import java.util.Scanner;

import edu.princeton.cs.algs4.Heap;
import edu.princeton.cs.algs4.Insertion;
import edu.princeton.cs.algs4.Merge;
import edu.princeton.cs.algs4.Quick;
import edu.princeton.cs.algs4.Selection;
import edu.princeton.cs.algs4.Shell;
import edu.princeton.cs.algs4.StdOut;
import edu.princeton.cs.algs4.StdRandom;
import edu.princeton.cs.algs4.Stopwatch;

public class 排序测试 {

    public static void main(String[] args) {
        // 1. 验证性测试（静态数据）
        runValidationTests();

        // 2. 动态对比实验（性能分析）
        runComparisonExperiment();
    }

    private static void runValidationTests() {
        String str = "E A S Y Q U E S T I O N";
        String[] template = str.split(" ");
        System.out.println("=== 算法正确性验证 ===");

        testAlgorithm("Selection", template.clone());
        testAlgorithm("Insertion", template.clone());
        testAlgorithm("Shell", template.clone());
        testAlgorithm("Merge", template.clone());
        testAlgorithm("Quick", template.clone());
        testAlgorithm("Heap", template.clone());
    }

    private static void testAlgorithm(String algName, Comparable[] data) {
        // 调用 algs4 库中的静态排序方法
        switch (algName) {
            case "Selection" -> Selection.sort(data);
            case "Insertion" -> Insertion.sort(data);
            case "Shell" -> Shell.sort(data);
            case "Merge" -> Merge.sort(data);
            case "Quick" -> Quick.sort(data);
            case "Heap" -> Heap.sort(data);
        }
        StdOut.printf("%-10s: ", algName);
        for (Comparable c : data)
            System.out.print(c + " ");
        System.out.println();
    }

    private static void runComparisonExperiment() {
        System.out.println("\n=== 算法性能对比分析 ===");
        System.out.print("请输入 (算法1 算法2 数组规模 试验次数): ");

        try (Scanner scanner = new Scanner(System.in)) {
            if (!scanner.hasNextLine())
                return;
            String[] params = scanner.nextLine().split("\\s+");
            if (params.length < 4)
                return;

            String alg1 = params[0];
            String alg2 = params[1];
            int n = Integer.parseInt(params[2]);
            int t = Integer.parseInt(params[3]);

            double t1 = timeRandomInput(alg1, n, t);
            double t2 = timeRandomInput(alg2, n, t);

            StdOut.printf("对于 %d 个随机 Double 元素:\n", n);
            StdOut.printf("  %s 耗时: %.4fs\n", alg1, t1);
            StdOut.printf("  %s 耗时: %.4fs\n", alg2, t2);
            StdOut.printf("结果: %s 比 %s 快 %.2f 倍\n", alg1, alg2, t2 / t1);
        }
    }

    public static double timeRandomInput(String alg, int n, int t) {
        double total = 0.0;
        Double[] a = new Double[n];
        for (int i = 0; i < t; i++) {
            // 构造随机数组
            for (int j = 0; j < n; j++)
                a[j] = StdRandom.uniformDouble();
            total += time(alg, a);
        }
        return total;
    }

    private static double time(String alg, Double[] a) {
        Stopwatch timer = new Stopwatch(); // 使用 algs4 里的计时器
        if (alg.equals("Selection"))
            Selection.sort(a);
        if (alg.equals("Insertion"))
            Insertion.sort(a);
        if (alg.equals("Shell"))
            Shell.sort(a);
        if (alg.equals("Merge"))
            Merge.sort(a);
        if (alg.equals("Quick"))
            Quick.sort(a);
        if (alg.equals("Heap"))
            Heap.sort(a);
        return timer.elapsedTime();
    }
}