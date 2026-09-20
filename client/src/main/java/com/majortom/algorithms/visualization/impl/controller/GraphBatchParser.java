package com.majortom.algorithms.visualization.impl.controller;

import com.majortom.algorithms.visualization.runtime.value.ValueAdapter;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

/** Parses graph bulk text into immutable graph input; the UI supplies localized error reporting. */
final class GraphBatchParser {
    private GraphBatchParser() {}

    @FunctionalInterface
    interface ErrorReporter {
        void report(String key, Object... arguments);
    }

    record GraphBatch(List<Object> nodes, List<GraphBatchEdge> edges) {}
    record GraphBatchEdge(Object from, Object to, double weight) {}

    static GraphBatch parse(String input, ValueAdapter<Object> adapter, Class<?> valueType,
            Function<String, List<Object>> parseNodes, ErrorReporter errors) {
        return new Parser(adapter, valueType, parseNodes, errors).parseGraphBatch(input);
    }

    private static final class Parser {
        private final ValueAdapter<Object> valueAdapter;
        private final Class<?> runtimeValueType;
        private final Function<String, List<Object>> parseNodes;
        private final ErrorReporter errors;

        private Parser(ValueAdapter<Object> valueAdapter, Class<?> runtimeValueType,
                Function<String, List<Object>> parseNodes, ErrorReporter errors) {
            this.valueAdapter = valueAdapter;
            this.runtimeValueType = runtimeValueType;
            this.parseNodes = parseNodes;
            this.errors = errors;
        }

    GraphBatch parseGraphBatch(String input) {
        if (input == null || input.isBlank()) {
            errors.report("message.error.bulk_input_empty");
            return null;
        }
        String[] sections = input.split("\\|", -1);
        if (sections.length > 2) {
            errors.report("message.error.bulk_input_invalid");
            return null;
        }
        List<Object> nodes = parseNodes.apply(sections[0]);
        if (nodes == null) {
            return null;
        }
        Set<Object> nodeSet = new LinkedHashSet<>(nodes);
        if (nodeSet.size() != nodes.size()) {
            errors.report("message.error.bulk_duplicates");
            return null;
        }
        List<GraphBatchEdge> edges = new ArrayList<>();
        if (sections.length == 2 && !sections[1].isBlank()) {
            String[] edgeTokens = sections[1].trim().split("[,;\\s]+");
            for (String token : edgeTokens) {
                if (token.isBlank()) {
                    continue;
                }
                GraphBatchEdge edge = parseGraphEdge(token);
                if (edge == null) {
                    return null;
                }
                if (!nodeSet.contains(edge.from()) || !nodeSet.contains(edge.to())) {
                    errors.report("message.error.graph_bulk_endpoint", token);
                    return null;
                }
                edges.add(edge);
            }
        }
        return new GraphBatch(List.copyOf(nodes), List.copyOf(edges));
    }

    GraphBatchEdge parseGraphEdge(String token) {
        String relation = token;
        double weight = 1.0d;
        int weightSeparator = token.lastIndexOf(':');
        if (weightSeparator >= 0) {
            relation = token.substring(0, weightSeparator);
            try {
                weight = Double.parseDouble(token.substring(weightSeparator + 1));
            } catch (RuntimeException exception) {
                errors.report("message.error.bulk_input_invalid");
                return null;
            }
        }
        if (!Double.isFinite(weight)) {
            errors.report("message.error.invalid_graph_weight");
            return null;
        }

        String left;
        String right;
        int separator = relation.indexOf("->");
        int separatorLength = 2;
        if (separator < 0) {
            separator = relation.indexOf('>');
            separatorLength = 1;
        }
        if (separator >= 0) {
            left = relation.substring(0, separator);
            right = relation.substring(separator + separatorLength);
        } else if (runtimeValueType == Integer.class) {
            java.util.regex.Matcher matcher = java.util.regex.Pattern
                    .compile("^(-?\\d+)\\s*-\\s*(-?\\d+)$")
                    .matcher(relation);
            if (!matcher.matches()) {
                errors.report("message.error.bulk_input_invalid");
                return null;
            }
            left = matcher.group(1);
            right = matcher.group(2);
        } else {
            errors.report("message.error.bulk_input_invalid");
            return null;
        }

        try {
            return new GraphBatchEdge(valueAdapter.parse(left), valueAdapter.parse(right), weight);
        } catch (RuntimeException exception) {
            errors.report("message.error.bulk_input_invalid");
            return null;
        }
    }

    }
}
