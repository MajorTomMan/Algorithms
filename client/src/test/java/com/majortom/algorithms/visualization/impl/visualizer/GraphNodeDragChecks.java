package com.majortom.algorithms.visualization.impl.visualizer;

import com.majortom.algorithms.core.metadata.GraphDirection;
import com.majortom.algorithms.core.snapshot.GraphSnapshot;
import com.majortom.algorithms.visualization.common.VisualizationSurface;
import com.majortom.algorithms.visualization.common.view.EdgeView;
import com.majortom.algorithms.visualization.common.view.NodeView;
import com.majortom.algorithms.visualization.impl.visualizer.graph.GraphElkLayout;
import com.majortom.algorithms.visualization.render.api.*;
import com.majortom.algorithms.visualization.runtime.graph.GraphViewState;
import javafx.application.Platform;
import javafx.geometry.Point2D;
import javafx.scene.Scene;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/** End-to-end JavaFX smoke check for dragging parallel edges at normal and zoomed viewports. */
public final class GraphNodeDragChecks {
  public static void main(String[] args) throws Exception {
    CountDownLatch ready = new CountDownLatch(1);
    Platform.startup(() -> {});
    Platform.runLater(() -> {
      try { exercise(); }
      catch (Throwable failure) { failure.printStackTrace(); System.exit(1); }
      finally { ready.countDown(); Platform.exit(); }
    });
    if (!ready.await(25, TimeUnit.SECONDS)) throw new AssertionError("JavaFX drag test timed out");
    System.out.println("GraphNodeDragChecks passed");
  }

  private static void exercise() throws Exception {
    GraphVisualizer visualizer = new GraphVisualizer();
    VisualizationSurface surface = (VisualizationSurface) visualizer.fxSurfaceAdapter();
    Stage stage = new Stage();
    stage.setScene(new Scene(new StackPane(surface), 840, 540));
    stage.show();
    GraphViewState graph = GraphViewState.initial(new GraphSnapshot<>(GraphDirection.DIRECTED,
        List.of(new GraphSnapshot.Vertex<>(1, "深圳"),
                new GraphSnapshot.Vertex<>(2, "广州"),
                new GraphSnapshot.Vertex<>(3, "东莞")),
        List.of(new GraphSnapshot.Edge(11, 1, 2, "rail", 102),
                new GraphSnapshot.Edge(12, 1, 2, "road", 125))));
    var patch = new LayoutResult(new LayoutRequestId(1), 1,
        Map.of(GraphElkLayout.nodeId(1), new ElementGeometry(GraphElkLayout.nodeId(1), 80, 100, 56, 56),
               GraphElkLayout.nodeId(2), new ElementGeometry(GraphElkLayout.nodeId(2), 360, 110, 56, 56),
               GraphElkLayout.nodeId(3), new ElementGeometry(GraphElkLayout.nodeId(3), 250, 320, 56, 56)),
        List.of(new EdgeGeometry(GraphElkLayout.edgeId(11), List.of(
                    new EdgeGeometry.Point(132, 128), new EdgeGeometry.Point(250, 93), new EdgeGeometry.Point(360, 138))),
                new EdgeGeometry(GraphElkLayout.edgeId(12), List.of(
                    new EdgeGeometry.Point(132, 128), new EdgeGeometry.Point(250, 173), new EdgeGeometry.Point(360, 138)))),
        new BoundsSnapshot(0, 0, 520, 480));
    visualizer.commitLayout(graph, patch,
        RenderCommitContext.structural(1, visualizer.sessionId(), 1, 1, 1, 1,
            StructuralChange.MODEL, true, PresentationProgressSink.NONE)).toCompletableFuture().join();
    @SuppressWarnings("unchecked")
    var vertices = (Map<Long,NodeView>) read(visualizer, "nodeViews");
    @SuppressWarnings("unchecked")
    var edges = (Map<Long,EdgeView>) read(visualizer, "edgeViews");
    NodeView sz = vertices.get(1L), gz = vertices.get(2L), dg = vertices.get(3L);
    EdgeView rail = edges.get(11L), road = edges.get(12L);
    check(sz != null && gz != null && dg != null && rail != null && road != null, "View creation");
    Point2D firstMid = rail.routeSnapshot().get(1);
    Point2D secondMid = road.routeSnapshot().get(1);
    check(firstMid.distance(secondMid) > 20, "Initial parallel edge separation");
    drag(surface, sz, 80, 40);
    check(sz.visualCenter().distance(new Point2D(188, 168)) < 0.1, "Dragging at 100% should move node");
    Point2D shifted = rail.routeSnapshot().get(1);
    check(shifted.distance(firstMid.add(40,20)) < 0.1, "Control point follows one endpoint halfway");
    check(road.routeSnapshot().get(1).distance(secondMid.add(40,20)) < 0.1,
        "Parallel road route follows independently");
    check(gz.visualCenter().distance(new Point2D(388, 138)) < 0.1, "Unrelated node moved");
    check(dg.visualCenter().distance(new Point2D(278, 348)) < 0.1, "Isolated node moved");

    // A structural/layout recalculation must not discard the manually positioned vertex.
    visualizer.commitLayout(graph, patch,
        RenderCommitContext.structural(2, visualizer.sessionId(), 1, 1, 2, 1,
            StructuralChange.GEOMETRY, false, PresentationProgressSink.NONE)).toCompletableFuture().join();
    check(sz.visualCenter().distance(new Point2D(188,168)) < 0.1, "Manual position lost after layout");
    check(rail.routeSnapshot().get(1).distance(firstMid.add(40,20)) < 0.1,
        "Layout reset multigraph route");

    surface.zoomIn();
    Point2D before = sz.visualCenter();
    Point2D initialScene = surface.nodeLayer().localToScene(before);
    Point2D target = surface.nodeLayer().sceneToLocal(initialScene.add(60, 30));
    drag(surface, sz, 60, 30);
    check(sz.visualCenter().distance(target) < 0.2, "Zoom-aware dragging incorrect");
    check(rail.routeSnapshot().get(1).distance(
        shifted.add(sz.visualCenter().subtract(before).multiply(0.5))) < 0.2,
        "Zoom-aware routed edge incorrect");
    stage.close();
    visualizer.dispose();
  }

  private static void drag(VisualizationSurface surface, NodeView view, double dx, double dy) {
    Point2D point = surface.nodeLayer().localToScene(view.visualCenter());
    view.getOnMousePressed().handle(mouse(MouseEvent.MOUSE_PRESSED, point, true));
    view.getOnMouseDragged().handle(mouse(MouseEvent.MOUSE_DRAGGED, point.add(dx,dy), true));
    view.getOnMouseReleased().handle(mouse(MouseEvent.MOUSE_RELEASED, point.add(dx,dy), false));
  }
  private static MouseEvent mouse(javafx.event.EventType<MouseEvent> type, Point2D p, boolean down) {
    return new MouseEvent(type, p.getX(),p.getY(),p.getX(),p.getY(), MouseButton.PRIMARY, 1,
        false,false,false,false, down,false,false,false,false,false,null);
  }
  private static Object read(Object receiver, String name) throws Exception {
    Field field = GraphVisualizer.class.getDeclaredField(name);
    field.setAccessible(true);
    return field.get(receiver);
  }
  private static void check(boolean success, String description) {
    if (!success) throw new AssertionError(description);
  }
}
