package com.majortom.algorithms.visualization.render.runtime;

import com.majortom.algorithms.visualization.impl.controller.LinearStructureViewState;
import com.majortom.algorithms.visualization.render.api.PresentationRenderIntent;
import com.majortom.algorithms.visualization.render.api.RenderIntent;
import com.majortom.algorithms.visualization.render.api.RenderSessionId;
import com.majortom.algorithms.visualization.render.api.StructuralRenderIntent;
import com.majortom.algorithms.visualization.render.viewport.CameraPolicy;
import com.majortom.algorithms.visualization.runtime.array.ArrayViewState;
import com.majortom.algorithms.visualization.runtime.graph.GraphViewState;
import com.majortom.algorithms.visualization.runtime.linked.LinkedListViewState;
import com.majortom.algorithms.visualization.runtime.maze.MazeViewState;
import com.majortom.algorithms.visualization.runtime.string.StringViewState;
import com.majortom.algorithms.visualization.runtime.tree.TreeViewState;

/** Shared JavaFX-neutral render intent policies for structure families. */
public final class StructurePresenters {
  private StructurePresenters() {}

  public static StructurePresenter<ArrayViewState> array() {
    return (sessionId, previous, current) -> {
      boolean cold = previous == null;
      boolean replacement = previous != null
          && current.mutation().type() == ArrayViewState.Type.NONE
          && !current.completed()
          && !current.values().equals(previous.values());
      boolean structural = cold || replacement || arrayStructuralChange(previous, current);
      if (!structural) return new PresentationRenderIntent<>(sessionId, current);
      CameraPolicy camera = replacement ? CameraPolicy.FIT_CONTENT
          : (cold ? CameraPolicy.RESTORE : CameraPolicy.ENSURE_VISIBLE);
      return new StructuralRenderIntent<>(sessionId, current, camera, cold || replacement);
    };
  }

  public static StructurePresenter<StringViewState> string() {
    return (sessionId, previous, current) -> {
      boolean cold = previous == null;
      boolean replacement = previous != null
          && current.mutation().type() == StringViewState.Type.NONE
          && !current.completed()
          && !current.value().equals(previous.value());
      boolean structural = cold || replacement || !previous.value().equals(current.value());
      if (!structural) return new PresentationRenderIntent<>(sessionId, current);
      CameraPolicy camera = replacement ? CameraPolicy.FIT_CONTENT
          : (cold ? CameraPolicy.RESTORE : CameraPolicy.ENSURE_VISIBLE);
      return new StructuralRenderIntent<>(sessionId, current, camera, cold || replacement);
    };
  }

  public static StructurePresenter<GraphViewState> graph() {
    return (sessionId, previous, current) -> structuralOrPresentation(
        sessionId, current, previous == null || graphStructuralChange(previous, current), previous == null);
  }

  public static StructurePresenter<TreeViewState> tree() {
    return (sessionId, previous, current) -> structuralOrPresentation(
        sessionId, current, previous == null || treeStructuralChange(previous, current), previous == null);
  }

  public static StructurePresenter<LinearStructureViewState> linear() {
    return (sessionId, previous, current) -> structuralOrPresentation(
        sessionId, current, previous == null || !previous.values().equals(current.values()), previous == null);
  }

  public static StructurePresenter<LinkedListViewState> linkedList() {
    return (sessionId, previous, current) -> structuralOrPresentation(
        sessionId, current, previous == null || !previous.nodes().equals(current.nodes()), previous == null);
  }

  public static StructurePresenter<MazeViewState> maze() {
    return (sessionId, previous, current) -> structuralOrPresentation(
        sessionId, current, previous == null || previous.rows() != current.rows()
            || previous.columns() != current.columns(), previous == null);
  }

  private static <S> RenderIntent structuralOrPresentation(
      RenderSessionId sessionId, S current, boolean structural, boolean initial) {
    if (!structural) return new PresentationRenderIntent<>(sessionId, current);
    return new StructuralRenderIntent<>(sessionId, current,
        initial ? CameraPolicy.RESTORE : CameraPolicy.ENSURE_VISIBLE, initial);
  }

  private static boolean arrayStructuralChange(ArrayViewState previous, ArrayViewState current) {
    if (current.values().size() != previous.values().size()) return true;
    return switch (current.mutation().type()) {
      case INSERTED, REMOVED, UPDATED -> true;
      case SWAPPED -> false;
      case NONE -> !current.values().equals(previous.values());
    };
  }

  private static boolean graphStructuralChange(GraphViewState previous, GraphViewState current) {
    if (previous.directed() != current.directed() || !previous.nodes().equals(current.nodes())) return true;
    if (previous.edges().size() != current.edges().size()) return true;
    for (int index = 0; index < previous.edges().size(); index++) {
      GraphViewState.Edge left = previous.edges().get(index);
      GraphViewState.Edge right = current.edges().get(index);
      if (left.id() != right.id() || left.fromId() != right.fromId() || left.toId() != right.toId()) return true;
    }
    return false;
  }

  private static boolean treeStructuralChange(TreeViewState previous, TreeViewState current) {
    return previous.kind() != current.kind()
        || !java.util.Objects.equals(previous.rootId(), current.rootId())
        || !previous.nodes().equals(current.nodes());
  }
}
