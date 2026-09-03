package com.majortom.algorithms.library.string;

import com.majortom.algorithms.library.structure.StringStructure;

/** Domain contract for algorithms that search a project-owned StringStructure. */
public interface StringSearch<R> {
    R search(StringStructure target, java.lang.String pattern);
}
