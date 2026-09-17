package com.majortom.algorithms.visualization.impl.visualizer.tree.animation;

import com.majortom.algorithms.visualization.runtime.tree.TreeViewState;
import java.util.LinkedHashSet;
import java.util.Set;

/** Stable logical ids shared by the tree planner and FX scene adapter. */
public final class TreeAnimationIds {
    private TreeAnimationIds() {}

    public static String node(long id) {
        return "tree:" + id;
    }

    public static String childEdge(int index, long sourceId, long targetId) {
        return edge("child", index, sourceId, targetId);
    }

    public static String leftEdge(long sourceId, long targetId) {
        return edge("left", 0, sourceId, targetId);
    }

    public static String rightEdge(long sourceId, long targetId) {
        return edge("right", 1, sourceId, targetId);
    }

    public static Set<String> edgeIds(TreeViewState state) {
        Set<String> ids = new LinkedHashSet<>();
        for (TreeViewState.Node node : state.nodes().values()) {
            if (state.kind() == TreeViewState.Kind.GENERAL) {
                for (int index = 0; index < node.childIds().size(); index++) {
                    Long targetId = node.childIds().get(index);
                    if (targetId != null && state.nodes().containsKey(targetId)) {
                        ids.add(childEdge(index, node.id(), targetId));
                    }
                }
            } else {
                if (node.leftId() != null && state.nodes().containsKey(node.leftId())) {
                    ids.add(leftEdge(node.id(), node.leftId()));
                }
                if (node.rightId() != null && state.nodes().containsKey(node.rightId())) {
                    ids.add(rightEdge(node.id(), node.rightId()));
                }
            }
        }
        return ids;
    }

    private static String edge(String relation, int index, long sourceId, long targetId) {
        return "tree:" + relation + ":" + index + ":" + sourceId + ":" + targetId;
    }
}
