package com.majortom.algorithms.visualization.module;

import java.util.List;
import java.util.function.Consumer;

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

    /** Algorithms compatible with the module's currently active structure variant. */
    List<String> algorithmIds();

    /** Current algorithm selection, or {@code null} when no compatible algorithm is selected. */
    String selectedAlgorithmId();

    /**
     * Receives selection changes caused by the module-local selector or a
     * structure-variant switch. Passing {@code null} clears the listener.
     */
    void setAlgorithmSelectionListener(Consumer<String> listener);
}
