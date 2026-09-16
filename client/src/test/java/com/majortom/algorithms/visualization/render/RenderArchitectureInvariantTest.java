package com.majortom.algorithms.visualization.render;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class RenderArchitectureInvariantTest {
  @Test
  void arrayReferenceVisualizerDoesNotOwnRenderInfrastructure() throws IOException {
    Path array = Path.of(
        "src/main/java/com/majortom/algorithms/visualization/impl/visualizer/ArrayVisualizer.java");
    String source = Files.readString(array);
    assertFalse(source.contains("Platform.runLater"));
    assertFalse(source.contains("ExecutorService"));
    assertFalse(source.contains("Executors."));
    assertFalse(source.contains("requestLayout("));
    assertFalse(source.contains("fitWithMinimumScale("));
    assertFalse(source.contains("surface.fit("));
  }

  @Test
  void renderPackageHasSinglePlatformRunLaterOwner() throws IOException {
    Path render = Path.of("src/main/java/com/majortom/algorithms/visualization/render");
    try (var files = Files.walk(render)) {
      files.filter(path -> path.toString().endsWith(".java")).forEach(path -> {
        try {
          String source = Files.readString(path);
          if (source.contains("Platform.runLater")) {
            assertTrue(path.endsWith("FxExecutorImpl.java"), () -> "runLater leaked into " + path);
          }
        } catch (IOException failure) {
          throw new RuntimeException(failure);
        }
      });
    }
  }

  @Test
  void renderClockIsTheOnlyClientScheduledExecutorOwner() throws IOException {
    Path clientMain = Path.of("src/main/java/com/majortom/algorithms/visualization");
    try (var files = Files.walk(clientMain)) {
      files.filter(path -> path.toString().endsWith(".java")).forEach(path -> {
        try {
          String source = Files.readString(path);
          if (source.contains("ScheduledExecutorService")
              || source.contains("newSingleThreadScheduledExecutor")
              || source.contains("newScheduledThreadPool")) {
            assertTrue(path.endsWith("render/timing/RenderClock.java"),
                () -> "scheduled timing leaked outside RenderClock: " + path);
          }
        } catch (IOException failure) {
          throw new RuntimeException(failure);
        }
      });
    }
  }

  @Test
  void layoutPackageIsJavaFxNeutral() throws IOException {
    Path layout = Path.of("src/main/java/com/majortom/algorithms/visualization/render/layout");
    try (var files = Files.walk(layout)) {
      files.filter(path -> path.toString().endsWith(".java")).forEach(path -> {
        try {
          assertFalse(
              Files.readString(path).contains("javafx."), () -> "JavaFX leaked into " + path);
        } catch (IOException failure) {
          throw new RuntimeException(failure);
        }
      });
    }
  }

  @Test
  void structureVisualizersDoNotOwnRenderInfrastructure() throws IOException {
    Path visualizers =
        Path.of("src/main/java/com/majortom/algorithms/visualization/impl/visualizer");
    try (var files = Files.walk(visualizers)) {
      files.filter(path -> path.getFileName().toString().endsWith("Visualizer.java"))
          .forEach(path -> {
            try {
              String source = Files.readString(path);
              assertFalse(source.contains("Platform.runLater"), () -> path + " owns FX scheduling");
              assertFalse(
                  source.contains("ExecutorService"), () -> path + " owns a render executor");
              assertFalse(source.contains("Executors."), () -> path + " owns a render executor");
              assertFalse(
                  source.contains("ScheduledExecutor"), () -> path + " owns a render clock");
              assertFalse(source.contains("fitWithMinimumScale("), () -> path + " owns Camera/Fit");
              assertFalse(source.contains("surface.fit("), () -> path + " owns Camera/Fit");
              assertFalse(
                  source.contains("requestLayout("), () -> path + " owns JavaFX layout scheduling");
              assertFalse(
                  source.contains("layoutVersion"), () -> path + " owns stale-result versioning");
              assertFalse(source.contains("layoutQueue"), () -> path + " owns a layout queue");
            } catch (IOException failure) {
              throw new RuntimeException(failure);
            }
          });
    }
  }

  @Test
  void structureLayoutEnginesAreJavaFxNeutral() throws IOException {
    for (String relative :
        java.util.List.of("graph/GraphElkLayout.java", "graph/GraphTopologyLayout.java",
            "tree/TreeElkLayout.java", "linked/LinkedListElkLayout.java")) {
      Path engine =
          Path.of("src/main/java/com/majortom/algorithms/visualization/impl/visualizer", relative);
      assertFalse(
          Files.readString(engine).contains("javafx."), () -> "JavaFX leaked into " + relative);
    }
  }

  @Test
  void retiredPerVisualizerLayoutAdaptersStayDeleted() {
    assertFalse(Files.exists(Path.of("src/main/java/com/majortom/algorithms/visualization/impl/"
                                     + "visualizer/linear/StackQueueElkLayout.java")));
    assertFalse(Files.exists(Path.of("src/main/java/com/majortom/algorithms/visualization/impl/"
                                     + "visualizer/string/StringElkLayout.java")));
  }

  @Test
  void structureVisualizerCaptureIsPureDescription() throws IOException {
    Path visualizers =
        Path.of("src/main/java/com/majortom/algorithms/visualization/impl/visualizer");
    try (var files = Files.walk(visualizers)) {
      files.filter(path -> path.getFileName().toString().endsWith("Visualizer.java"))
          .forEach(path -> {
            try {
              String source = Files.readString(path);
              String capture = methodBody(source, "public LayoutRequest captureLayout");
              assertFalse(
                  capture.contains("surface."), () -> path + " mutates Surface during capture");
              assertFalse(capture.contains("nodeViews"),
                  () -> path + " reads live node views during capture");
              assertFalse(capture.contains("edgeViews"),
                  () -> path + " reads live edge views during capture");
              assertFalse(capture.contains("getChildren()"),
                  () -> path + " mutates SceneGraph during capture");
              assertFalse(capture.contains("new NodeView"),
                  () -> path + " creates JavaFX nodes during capture");
              assertFalse(capture.contains("new EdgeView"),
                  () -> path + " creates JavaFX edges during capture");
              assertFalse(capture.contains("javafx.animation"),
                  () -> path + " owns animation during capture");
            } catch (IOException failure) {
              throw new RuntimeException(failure);
            }
          });
    }
  }

  @Test
  void retiredVisualizerTimingAndGeometryFeedbackStayDeleted() throws IOException {
    Path visualizers =
        Path.of("src/main/java/com/majortom/algorithms/visualization/impl/visualizer");
    try (var files = Files.walk(visualizers)) {
      files.filter(path -> path.toString().endsWith(".java")).forEach(path -> {
        try {
          String source = Files.readString(path);
          assertFalse(source.contains("AnimationCoordinator"),
              () -> path + " restored local animation ownership");
          assertFalse(source.contains("requestGeometryRefresh("),
              () -> path + " restored geometry feedback");
          assertFalse(source.contains("layoutBoundsProperty().addListener"),
              () -> path + " restored live layoutBounds feedback");
        } catch (IOException failure) {
          throw new RuntimeException(failure);
        }
      });
    }
    assertFalse(Files.exists(Path.of(
        "src/main/java/com/majortom/algorithms/visualization/common/AnimationCoordinator.java")));
    assertFalse(Files
            .readString(
                Path.of("src/main/java/com/majortom/algorithms/visualization/BaseVisualizer.java"))
            .contains("PauseTransition"));
    assertFalse(Files
            .readString(Path.of("src/main/java/com/majortom/algorithms/visualization/common/"
                                + "VisualizationSurface.java"))
            .contains("PauseTransition"));
  }

  @Test
  void cameraWorldExtentNeverFallsBackToLiveSceneGraphBounds() throws IOException {
    String source = Files.readString(Path.of(
        "src/main/java/com/majortom/algorithms/visualization/common/VisualizationSurface.java"));
    assertTrue(source.contains("setPrimaryContentBounds(BoundsSnapshot bounds)"));
    assertFalse(source.contains("worldPane.getLayoutBounds()"),
        "Camera/world extent must come from LayoutPatch primaryContentBounds only");
  }

  @Test
  void retiredLegacyLayoutAdaptersStayDeleted() {
    assertFalse(Files.exists(Path.of("src/main/java/com/majortom/algorithms/visualization/impl/"
                                     + "visualizer/array/ArrayElkLayout.java")));
    assertFalse(
        Files.exists(Path.of("src/main/java/com/majortom/algorithms/visualization/common/layout")));
  }

  private static String methodBody(String source, String signature) {
    int signatureIndex = source.indexOf(signature);
    assertTrue(signatureIndex >= 0, () -> "missing method: " + signature);
    int start = source.indexOf('{', signatureIndex);
    int depth = 0;
    for (int index = start; index < source.length(); index++) {
      char current = source.charAt(index);
      if (current == '{')
        depth++;
      else if (current == '}' && --depth == 0)
        return source.substring(start, index + 1);
    }
    throw new IllegalStateException("unterminated method: " + signature);
  }
}
