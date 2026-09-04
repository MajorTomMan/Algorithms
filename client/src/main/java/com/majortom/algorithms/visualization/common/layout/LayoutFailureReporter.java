package com.majortom.algorithms.visualization.common.layout;

/** Reports recoverable presentation-layout failures without escalating them to the JavaFX thread. */
public final class LayoutFailureReporter {
    private static final System.Logger LOGGER = System.getLogger(LayoutFailureReporter.class.getName());

    private LayoutFailureReporter() {
    }

    public static void report(String family, Throwable failure) {
        String message = family + " ELK layout failed; retaining last-good geometry until the next layout change";
        LOGGER.log(System.Logger.Level.ERROR, message, failure);
    }
}
