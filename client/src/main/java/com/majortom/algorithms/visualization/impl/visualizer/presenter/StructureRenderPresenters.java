package com.majortom.algorithms.visualization.impl.visualizer.presenter;

import com.majortom.algorithms.visualization.impl.controller.LinearStructureViewState;
import com.majortom.algorithms.visualization.render.api.PresentationRenderIntent;
import com.majortom.algorithms.visualization.render.api.RenderIntent;
import com.majortom.algorithms.visualization.render.api.RenderSessionId;
import com.majortom.algorithms.visualization.render.api.StructuralRenderIntent;
import com.majortom.algorithms.visualization.render.runtime.StructureRenderPresenter;
import com.majortom.algorithms.visualization.render.viewport.CameraPolicy;
import com.majortom.algorithms.visualization.runtime.array.ArrayViewState;
import com.majortom.algorithms.visualization.runtime.graph.GraphViewState;
import com.majortom.algorithms.visualization.runtime.linked.LinkedListViewState;
import com.majortom.algorithms.visualization.runtime.maze.MazeViewState;
import com.majortom.algorithms.visualization.runtime.string.StringViewState;
import com.majortom.algorithms.visualization.runtime.tree.TreeViewState;
import java.util.Objects;

/** Stateless, JavaFX-neutral render intent policies for each visualization family. */
public final class StructureRenderPresenters {
  private static final StructureRenderPresenter<ArrayViewState> ARRAY = StructureRenderPresenters::arrayIntent;
  private static final StructureRenderPresenter<GraphViewState> GRAPH = StructureRenderPresenters::graphIntent;
  private static final StructureRenderPresenter<LinearStructureViewState> LINEAR = StructureRenderPresenters::linearIntent;
  private static final StructureRenderPresenter<LinkedListViewState> LINKED_LIST = StructureRenderPresenters::linkedListIntent;
  private static final StructureRenderPresenter<MazeViewState> MAZE = StructureRenderPresenters::mazeIntent;
  private static final StructureRenderPresenter<StringViewState> STRING = StructureRenderPresenters::stringIntent;
  private static final StructureRenderPresenter<TreeViewState> TREE = StructureRenderPresenters::treeIntent;

  private StructureRenderPresenters() {}

  public static StructureRenderPresenter<ArrayViewState> array() { return ARRAY; }
  public static StructureRenderPresenter<GraphViewState> graph() { return GRAPH; }
  public static StructureRenderPresenter<LinearStructureViewState> linear() { return LINEAR; }
  public static StructureRenderPresenter<LinkedListViewState> linkedList() { return LINKED_LIST; }
  public static StructureRenderPresenter<MazeViewState> maze() { return MAZE; }
  public static StructureRenderPresenter<StringViewState> string() { return STRING; }
  public static StructureRenderPresenter<TreeViewState> tree() { return TREE; }

  private static RenderIntent arrayIntent(
      RenderSessionId sessionId, ArrayViewState previous, ArrayViewState current) {
    require(sessionId, current);
    boolean coldOrRevisit = previous == null;
    boolean replacement = previous != null
        && current.mutation().type() == ArrayViewState.Type.NONE
        && !current.completed()
        && !current.values().equals(previous.values());
    boolean initial = coldOrRevisit || replacement;
    boolean structural = initial || arrayRequiresLayout(previous, current);
    if (!structural) return new PresentationRenderIntent<>(sessionId, current);
    CameraPolicy cameraPolicy = replacement
        ? CameraPolicy.FIT_CONTENT
        : (coldOrRevisit ? CameraPolicy.RESTORE : CameraPolicy.ENSURE_VISIBLE);
    return new StructuralRenderIntent<>(sessionId, current, cameraPolicy, initial);
  }

  private static boolean arrayRequiresLayout(ArrayViewState previous, ArrayViewState current) {
    if (previous == null) return true;
    if (current.values().size() != previous.values().size()) return true;
    return switch (current.mutation().type()) {
      case INSERTED, REMOVED, UPDATED -> true;
      case SWAPPED -> false;
      case NONE -> !current.values().equals(previous.values());
    };
  }

  private static RenderIntent graphIntent(
      RenderSessionId sessionId, GraphViewState previous, GraphViewState current) {
    require(sessionId, current);
    boolean initial = previous == null;
    if (!initial && !graphRequiresLayout(previous, current)) {
      return new PresentationRenderIntent<>(sessionId, current);
    }
    return new StructuralRenderIntent<>(sessionId, current,
        initial ? CameraPolicy.RESTORE : CameraPolicy.ENSURE_VISIBLE, initial);
  }

  private static boolean graphRequiresLayout(GraphViewState previous, GraphViewState current) {
    if (previous == null || previous.directed() != current.directed()
        || !previous.nodes().equals(current.nodes())) return true;
    if (previous.edges().size() != current.edges().size()) return true;
    for (int index = 0; index < previous.edges().size(); index++) {
      GraphViewState.Edge left = previous.edges().get(index);
      GraphViewState.Edge right = current.edges().get(index);
      if (left.id() != right.id() || left.fromId() != right.fromId() || left.toId() != right.toId()) {
        return true;
      }
    }
    return false;
  }

  private static RenderIntent linearIntent(
      RenderSessionId sessionId, LinearStructureViewState previous, LinearStructureViewState current) {
    require(sessionId, current);
    boolean initial = previous == null;
    boolean structural = initial || !previous.values().equals(current.values());
    if (!structural) return new PresentationRenderIntent<>(sessionId, current);
    return new StructuralRenderIntent<>(sessionId, current,
        initial ? CameraPolicy.RESTORE : CameraPolicy.ENSURE_VISIBLE, initial);
  }

  private static RenderIntent linkedListIntent(
      RenderSessionId sessionId, LinkedListViewState previous, LinkedListViewState current) {
    require(sessionId, current);
    boolean initial = previous == null;
    boolean structural = initial || !previous.nodes().equals(current.nodes());
    if (!structural) return new PresentationRenderIntent<>(sessionId, current);
    return new StructuralRenderIntent<>(sessionId, current,
        initial ? CameraPolicy.RESTORE : CameraPolicy.ENSURE_VISIBLE, initial);
  }

  private static RenderIntent mazeIntent(
      RenderSessionId sessionId, MazeViewState previous, MazeViewState current) {
    require(sessionId, current);
    boolean initial = previous == null;
    boolean structural = initial || previous.rows() != current.rows() || previous.columns() != current.columns();
    if (!structural) return new PresentationRenderIntent<>(sessionId, current);
    return new StructuralRenderIntent<>(sessionId, current,
        initial ? CameraPolicy.RESTORE : CameraPolicy.ENSURE_VISIBLE, initial);
  }

  private static RenderIntent stringIntent(
      RenderSessionId sessionId, StringViewState previous, StringViewState current) {
    require(sessionId, current);
    boolean coldOrRevisit = previous == null;
    boolean replacement = previous != null
        && current.mutation().type() == StringViewState.Type.NONE
        && !current.completed()
        && !current.value().equals(previous.value());
    boolean initial = coldOrRevisit || replacement;
    boolean structural = initial || !previous.value().equals(current.value());
    if (!structural) return new PresentationRenderIntent<>(sessionId, current);
    CameraPolicy cameraPolicy = replacement
        ? CameraPolicy.FIT_CONTENT
        : (coldOrRevisit ? CameraPolicy.RESTORE : CameraPolicy.ENSURE_VISIBLE);
    return new StructuralRenderIntent<>(sessionId, current, cameraPolicy, initial);
  }

  private static RenderIntent treeIntent(
      RenderSessionId sessionId, TreeViewState previous, TreeViewState current) {
    require(sessionId, current);
    boolean initial = previous == null;
    boolean structural = initial || previous.kind() != current.kind()
        || !Objects.equals(previous.rootId(), current.rootId())
        || !previous.nodes().equals(current.nodes());
    if (!structural) return new PresentationRenderIntent<>(sessionId, current);
    return new StructuralRenderIntent<>(sessionId, current,
        initial ? CameraPolicy.RESTORE : CameraPolicy.ENSURE_VISIBLE, initial);
  }

  private static void require(RenderSessionId sessionId, Object current) {
    Objects.requireNonNull(sessionId, "sessionId");
    Objects.requireNonNull(current, "current");
  }
}
