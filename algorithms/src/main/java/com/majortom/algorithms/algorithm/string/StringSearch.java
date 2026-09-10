package com.majortom.algorithms.algorithm.string;

import com.majortom.algorithms.structure.string.StringStructure;

import java.util.List;

/** Domain contract for algorithms that search a project-owned StringStructure. */
public interface StringSearch extends StringAlgorithm {
    List<Integer> search(StringStructure target, java.lang.String pattern);
}
