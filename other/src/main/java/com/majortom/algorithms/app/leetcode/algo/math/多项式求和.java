package com.majortom.algorithms.app.leetcode.algo.math;

import java.util.ArrayList;
import java.util.List;

public class 多项式求和 {
    public static void main(String[] args) {
        Polynomial left = new Polynomial();
        Polynomial right = new Polynomial();
        left.add(7, 0);
        left.add(3, 1);
        left.add(9, 8);
        left.add(5, 17);
        right.add(8, 1);
        right.add(22, 7);
        right.add(-9, 8);
        add(left, right).display();
    }

    public static Polynomial add(Polynomial left, Polynomial right) {
        Polynomial result = new Polynomial();
        int leftIndex = 0;
        int rightIndex = 0;
        while (leftIndex < left.terms.size() && rightIndex < right.terms.size()) {
            Term leftTerm = left.terms.get(leftIndex);
            Term rightTerm = right.terms.get(rightIndex);
            if (leftTerm.exponent < rightTerm.exponent) {
                result.add(leftTerm.coefficient, leftTerm.exponent);
                leftIndex++;
            } else if (leftTerm.exponent > rightTerm.exponent) {
                result.add(rightTerm.coefficient, rightTerm.exponent);
                rightIndex++;
            } else {
                int coefficient = leftTerm.coefficient + rightTerm.coefficient;
                if (coefficient != 0) {
                    result.add(coefficient, leftTerm.exponent);
                }
                leftIndex++;
                rightIndex++;
            }
        }
        while (leftIndex < left.terms.size()) {
            Term term = left.terms.get(leftIndex++);
            result.add(term.coefficient, term.exponent);
        }
        while (rightIndex < right.terms.size()) {
            Term term = right.terms.get(rightIndex++);
            result.add(term.coefficient, term.exponent);
        }
        return result;
    }

    private record Term(int coefficient, int exponent) {
    }

    public static final class Polynomial {
        private final List<Term> terms = new ArrayList<>();

        public void add(int coefficient, int exponent) {
            terms.add(new Term(coefficient, exponent));
            terms.sort(java.util.Comparator.comparingInt(Term::exponent));
        }

        public void display() {
            terms.forEach(System.out::println);
        }
    }
}
