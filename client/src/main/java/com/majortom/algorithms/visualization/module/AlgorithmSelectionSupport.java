package com.majortom.algorithms.visualization.module;

/**
 * Optional capability for a module whose algorithm can be selected by the
 * workspace navigation sidebar.
 */
public interface AlgorithmSelectionSupport {

    /**
     * Selects an algorithm by its stable catalog identifier.
     *
     * @return {@code true} when the identifier is supported by this module
     */
    boolean selectAlgorithm(String algorithmId);
}
