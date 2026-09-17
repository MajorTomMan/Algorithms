package com.majortom.algorithms.visualization.impl.visualizer.linear.animation;

/** Stable logical ids shared by Stack/Queue planners and their FX scene adapters. */
public final class LinearAnimationIds {
    private LinearAnimationIds() {}

    public static String node(String kind, int index) {
        return kind + ":" + index;
    }

    public static String exit(String kind, int previousIndex) {
        return kind + ":exit:" + previousIndex;
    }

    public static int activeIndex(String kind, String logicalId) {
        String prefix = kind + ":";
        if (logicalId == null || !logicalId.startsWith(prefix) || logicalId.startsWith(prefix + "exit:")) {
            return -1;
        }
        return parse(logicalId.substring(prefix.length()));
    }

    public static int exitIndex(String kind, String logicalId) {
        String prefix = kind + ":exit:";
        if (logicalId == null || !logicalId.startsWith(prefix)) return -1;
        return parse(logicalId.substring(prefix.length()));
    }

    private static int parse(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ignored) {
            return -1;
        }
    }
}
