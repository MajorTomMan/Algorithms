package com.majortom.algorithms.visualization.animation.fx;

import com.majortom.algorithms.visualization.animation.api.AnimationPlan;
import com.majortom.algorithms.visualization.common.view.EdgeView;
import com.majortom.algorithms.visualization.common.view.NodeView;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import javafx.geometry.Point2D;
import javafx.scene.Node;

/**
 * FX scene boundary consumed by the generic animation player.
 *
 * <p>The Player never reaches into Visualizer maps. Structure-specific adapters expose only
 * logical targets, current visual continuity snapshots and cleanup/stabilization hooks.</p>
 */
public interface AnimationSceneAdapter {
    Optional<NodeTarget> node(String logicalId);
    Optional<EdgeTarget> edge(String logicalId);
    Optional<Point2D> capturedNodeCenter(String logicalId);
    Optional<List<Point2D>> capturedEdgeRoute(String logicalId);
    Collection<NodeTarget> activeNodes();
    Collection<EdgeTarget> activeEdges();

    /** Remove only factual-exit visuals left behind by an interrupted transition. */
    void discardExitedVisuals();

    /** Force the authoritative scene to its clean, final state and remove all transient visuals. */
    void stabilize(AnimationPlan plan);

    record NodeTarget(String logicalId, Node node, Point2D finalCenter, List<Node> companions) {
        public NodeTarget {
            Objects.requireNonNull(logicalId, "logicalId");
            Objects.requireNonNull(node, "node");
            Objects.requireNonNull(finalCenter, "finalCenter");
            companions = List.copyOf(Objects.requireNonNull(companions, "companions"));
        }

        /** Convenience constructor for the shared NodeView primitive. */
        public NodeTarget(String logicalId, NodeView node, List<Node> companions) {
            this(logicalId, node, node.center(), companions);
        }
    }

    record EdgeTarget(String logicalId, EdgeView edge) {
        public EdgeTarget {
            Objects.requireNonNull(logicalId, "logicalId");
            Objects.requireNonNull(edge, "edge");
        }
    }
}
