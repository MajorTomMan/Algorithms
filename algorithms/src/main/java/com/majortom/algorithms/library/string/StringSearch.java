package com.majortom.algorithms.library.string;

import com.majortom.algorithms.library.structure.StringStructure;

import java.util.List;

/** Domain contract for algorithms that search a project-owned StringStructure. */
public interface StringSearch {
    List<Integer> search(StringStructure target, java.lang.String pattern);
}
