package com.majortom.algorithms.visualization.impl.controller;

import static org.junit.jupiter.api.Assertions.*;

import com.majortom.algorithms.core.runtime.ExecutionStatus;
import com.majortom.algorithms.visualization.BaseController;
import com.majortom.algorithms.visualization.algorithm.AlgorithmCatalog;
import com.majortom.algorithms.visualization.execution.ClientExecutionRecord;
import com.majortom.algorithms.visualization.international.I18N;
import com.majortom.algorithms.visualization.module.AlgorithmSelectionSupport;
import com.majortom.algorithms.visualization.navigation.FamilyItemView;
import com.majortom.algorithms.visualization.navigation.FamilyNavigator;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * End-to-end editable-structure mutation + real algorithm execution sweep for all eight families.
 */
class WorkbenchCrudAlgorithmSweepTest {
  private static final java.util.Queue<Throwable> fxErrors = new ConcurrentLinkedQueue<>();
  private MainController controller;
  private Parent root;
  private Stage stage;

  @BeforeAll
  static void startFx() throws Exception {
    CountDownLatch started = new CountDownLatch(1);
    Platform.startup(() -> {
      Platform.setImplicitExit(false);
      Thread.currentThread().setUncaughtExceptionHandler((thread, error) -> fxErrors.add(error));
      javafx.application.Application.setUserAgentStylesheet(
          new atlantafx.base.theme.PrimerDark().getUserAgentStylesheet());
      started.countDown();
    });
    assertTrue(started.await(15, TimeUnit.SECONDS));
  }

  @BeforeEach
  void openWorkbench() throws Exception {
    fxErrors.clear();
    fx(() -> {
      I18N.setLocale(Locale.CHINESE);
      FXMLLoader loader =
          new FXMLLoader(getClass().getResource("/fxml/MainControls.fxml"), I18N.getBundle());
      root = loader.load();
      controller = loader.getController();
      stage = new Stage();
      stage.setScene(new Scene(root, 1600, 900));
      stage.show();
      layout();
      ((Slider) mainField("delaySlider")).setValue(0.0d);
      return null;
    });
    awaitRenderIdle();
  }

  @AfterEach
  void closeWorkbench() throws Exception {
    fx(() -> {
      invokeMain("detachCurrentController");
      stage.close();
      return null;
    });
    assertTrue(fxErrors.isEmpty(), () -> "Uncaught JavaFX errors: " + fxErrors);
  }

  @AfterAll
  static void stopFx() {
    Platform.exit();
  }

  @Test
  void allFamiliesCrudAndAlgorithmSweepInOneWorkbench() throws Exception {
    arrayCrudAndAlgorithms();
    linkedCrudAndAlgorithms();
    stackCrudAndAlgorithms();
    queueCrudAndAlgorithms();
    treeCrudAndAlgorithms();
    graphCrudAndAlgorithms();
    stringCrudAndAlgorithms();
    mazeGenerateSolveAlgorithmSweep();
  }

  void arrayCrudAndAlgorithms() throws Exception {
    ArrayController array = switchFamily(0, ArrayController.class);
    fx(() -> {
      array.applyBulkData("3, 1, 2");
      setText(array, "elementValueField", "4");
      setText(array, "elementIndexField", "1");
      invokeController(array, "handleAddElement");
      setText(array, "updateIndexField", "1");
      setText(array, "updateValueField", "5");
      invokeController(array, "handleUpdateElement");
      setText(array, "elementIndexField", "1");
      invokeController(array, "handleDeleteElement");
      return null;
    });
    awaitRendered(".array-cell", 3);
    runAlgorithms(array, AlgorithmCatalog.forWorkbenchModule("array", Integer.class));
  }

  void linkedCrudAndAlgorithms() throws Exception {
    LinkedListController linked = switchFamily(1, LinkedListController.class);
    fx(() -> {
      linked.applyBulkData("1, 2, 3");
      setText(linked, "valueField", "4");
      setText(linked, "indexField", "1");
      invokeController(linked, "handlePrimary");
      setText(linked, "valueField", "9");
      setText(linked, "indexField", "1");
      invokeController(linked, "update");
      setText(linked, "indexField", "1");
      invokeController(linked, "handleSecondary");
      return null;
    });
    awaitRendered(".visual-node", 3);
    runAlgorithms(linked, AlgorithmCatalog.forWorkbenchModule("linked-list", Integer.class));
  }

  void stackCrudAndAlgorithms() throws Exception {
    LinearStructureController stack = switchFamily(2, LinearStructureController.class);
    fx(() -> {
      stack.applyBulkData("1, 2, 3");
      setText(stack, "valueField", "4");
      invokeController(stack, "handlePrimary");
      invokeController(stack, "handleSecondary");
      return null;
    });
    awaitRendered(".visual-node", 3);
    runAlgorithms(stack, AlgorithmCatalog.forWorkbenchModule("stack", Integer.class));
  }

  void queueCrudAndAlgorithms() throws Exception {
    LinearStructureController queue = switchFamily(3, LinearStructureController.class);
    fx(() -> {
      queue.applyBulkData("1, 2, 3");
      setText(queue, "valueField", "4");
      invokeController(queue, "handlePrimary");
      invokeController(queue, "handleSecondary");
      return null;
    });
    awaitRendered(".visual-node", 3);
    runAlgorithms(queue, AlgorithmCatalog.forWorkbenchModule("queue", Integer.class));
  }

  void treeCrudAndAlgorithms() throws Exception {
    TreeController tree = switchFamily(4, TreeController.class);
    fx(() -> {
      tree.applyBulkData("10, 20, 30");
      setText(tree, "valueField", "40");
      invokeController(tree, "handleAddChild");
      setText(tree, "valueField", "41");
      invokeController(tree, "handleUpdate");
      invokeController(tree, "handleDelete");
      return null;
    });
    awaitRenderedAtLeast(".visual-node", 3);
    runAlgorithms(tree, AlgorithmCatalog.forWorkbenchModule("tree", Integer.class));
  }

  void graphCrudAndAlgorithms() throws Exception {
    GraphController graph = switchFamily(5, GraphController.class);
    fx(() -> {
      graph.applyBulkData("1, 2, 3 | 1-2:1 2-3:1");
      setText(graph, "nodeField", "4");
      invokeController(graph, "handleAddNode");
      setText(graph, "fromField", "3");
      setText(graph, "toField", "4");
      setText(graph, "weightField", "2");
      invokeController(graph, "handleAddEdge");
      setText(graph, "weightField", "3");
      invokeController(graph, "handleSetWeight");
      invokeController(graph, "handleDeleteEdge");
      setText(graph, "nodeField", "4");
      invokeController(graph, "handleDeleteNode");
      return null;
    });
    awaitRendered(".visual-node", 3);
    runAlgorithms(graph, AlgorithmCatalog.forWorkbenchModule("graph", Integer.class));
  }

  void stringCrudAndAlgorithms() throws Exception {
    StringController string = switchFamily(6, StringController.class);
    fx(() -> {
      string.applyBulkData("ABABCABABXABABCABAB");
      setText(string, "indexField", "1");
      setText(string, "valueField", "Z");
      invokeController(string, "handleInsert");
      setText(string, "indexField", "1");
      setText(string, "characterField", "Y");
      invokeController(string, "handleUpdate");
      setText(string, "indexField", "1");
      setText(string, "lengthField", "1");
      invokeController(string, "handleRemove");
      return null;
    });
    awaitRenderedAtLeast(".string-cell", 10);
    runAlgorithms(string, AlgorithmCatalog.forWorkbenchModule("string", String.class));
  }

  void mazeGenerateSolveAlgorithmSweep() throws Exception {
    MazeController maze = switchFamily(7, MazeController.class);
    fx(() -> {
      ((Slider) controllerField(maze, "sizeSlider")).setValue(11.0d);
      invokeController(maze, "handleApplySize");
      return null;
    });
    awaitRenderIdle();
    List<String> ids = fx(() -> List.copyOf(maze.algorithmIds()));
    assertFalse(ids.isEmpty(), "maze must expose generators/pathfinders");

    boolean appliedGeneratedMaze = false;
    for (String id : ids) {
      boolean selected = fx(() -> maze.selectAlgorithm(id));
      assertTrue(selected, () -> "Maze algorithm not selectable: " + id);
      fx(() -> {
        maze.handleAlgorithmStart();
        return null;
      });
      awaitAlgorithm(maze, "maze/" + id);
      if (!appliedGeneratedMaze && isMazeGenerator(maze, id)) {
        fx(() -> {
          invokeController(maze, "handleApplyAlgorithmResult");
          return null;
        });
        appliedGeneratedMaze = true;
        awaitRenderIdle();
      }
    }
    assertTrue(appliedGeneratedMaze, "maze generator result was never applied");
    var snapshot = fx(maze::captureStructureSnapshot).state();
    assertEquals(11, snapshot.rows());
    assertEquals(11, snapshot.columns());
    assertEquals(121, snapshot.openCells().size());
  }

  private boolean isMazeGenerator(MazeController maze, String algorithmId) {
    Object generators = controllerField(maze, "allGenerators");
    return generators instanceof List<?> list && list.contains(algorithmId);
  }

  private void runAlgorithms(BaseController<?> module, List<String> ids) throws Exception {
    assertFalse(ids.isEmpty(), () -> module.getClass().getSimpleName() + " exposes no algorithms");
    assertTrue(module instanceof AlgorithmSelectionSupport,
        () -> module.getClass().getSimpleName() + " must implement AlgorithmSelectionSupport");
    AlgorithmSelectionSupport selection = (AlgorithmSelectionSupport) module;
    for (String id : new ArrayList<>(ids)) {
      boolean selected = fx(() -> selection.selectAlgorithm(id));
      assertTrue(selected, () -> module.getClass().getSimpleName() + " cannot select " + id);
      fx(() -> {
        module.handleAlgorithmStart();
        return null;
      });
      awaitAlgorithm(module, module.getClass().getSimpleName() + "/" + id);
    }
  }

  private void awaitAlgorithm(BaseController<?> module, String label) throws Exception {
    long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(20);
    while (System.nanoTime() < deadline) {
      boolean running = fx(module::isRunning);
      if (!running) {
        ClientExecutionRecord record =
            (ClientExecutionRecord) baseControllerField(module, "lastExecution");
        assertNotNull(record, () -> label + " produced no execution record");
        assertEquals(ExecutionStatus.COMPLETED, record.result().status(),
            () -> label + " status=" + record.result().status());
        assertTrue(record.result().failure().isEmpty(),
            () -> label + " failure=" + record.result().failure());
        awaitRenderIdle();
        return;
      }
      Thread.sleep(10L);
    }
    fx(() -> {
      module.endAlgorithm();
      return null;
    });
    fail("Timed out waiting for algorithm: " + label);
  }

  private <T extends BaseController<?>> T switchFamily(int index, Class<T> type) throws Exception {
    fx(() -> {
      familyButton(index).fire();
      layout();
      return null;
    });
    awaitRenderIdle();
    BaseController<?> current = currentController();
    assertInstanceOf(type, current);
    return type.cast(current);
  }

  private void awaitRendered(String selector, int count) throws Exception {
    long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
    while (System.nanoTime() < deadline) {
      int rendered = fx(() -> {
        layout();
        return root.lookupAll(selector).size();
      });
      if (rendered == count)
        return;
      Thread.sleep(20L);
    }
    int actual = fx(() -> root.lookupAll(selector).size());
    fail("Expected " + count + " rendered " + selector + ", got " + actual);
  }

  private void awaitRenderedAtLeast(String selector, int count) throws Exception {
    long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
    while (System.nanoTime() < deadline) {
      int rendered = fx(() -> {
        layout();
        return root.lookupAll(selector).size();
      });
      if (rendered >= count)
        return;
      Thread.sleep(20L);
    }
    int actual = fx(() -> root.lookupAll(selector).size());
    fail("Expected at least " + count + " rendered " + selector + ", got " + actual);
  }

  /**
   * Two FX turns are enough to flush controller defer + RenderFramework commit for fixed-size
   * tests.
   */
  private void awaitRenderIdle() throws Exception {
    fx(() -> null);
    fx(() -> {
      layout();
      return null;
    });
    Thread.sleep(20L);
    fx(() -> {
      layout();
      return null;
    });
  }

  private BaseController<?> currentController() {
    return (BaseController<?>) mainField("currentSubController");
  }

  private FamilyItemView familyButton(int index) {
    return ((FamilyNavigator) mainField("familyNavigator")).items().get(index);
  }

  private Object mainField(String name) {
    return fieldValue(MainController.class, controller, name);
  }

  private static Object controllerField(Object target, String name) {
    Class<?> type = target.getClass();
    while (type != null) {
      try {
        return fieldValue(type, target, name);
      } catch (AssertionError ignored) {
        type = type.getSuperclass();
      }
    }
    throw new AssertionError("No field " + name + " on " + target.getClass());
  }

  private static Object baseControllerField(BaseController<?> target, String name) {
    return fieldValue(BaseController.class, target, name);
  }

  private static Object fieldValue(Class<?> owner, Object target, String name) {
    try {
      Field field = owner.getDeclaredField(name);
      field.setAccessible(true);
      return field.get(target);
    } catch (ReflectiveOperationException exception) {
      throw new AssertionError(exception);
    }
  }

  private static void setText(Object target, String fieldName, String value) {
    ((TextField) controllerField(target, fieldName)).setText(value);
  }

  private void invokeMain(String name, Object... args) throws Exception {
    invoke(controller, MainController.class, name, args);
  }

  private static void invokeController(Object target, String name, Object... args) {
    try {
      invoke(target, target.getClass(), name, args);
    } catch (Exception exception) {
      throw new AssertionError(exception);
    }
  }

  private static void invoke(Object target, Class<?> start, String name, Object... args)
      throws Exception {
    Class<?> type = start;
    while (type != null) {
      for (Method method : type.getDeclaredMethods()) {
        if (method.getName().equals(name) && method.getParameterCount() == args.length) {
          method.setAccessible(true);
          try {
            method.invoke(target, args);
            return;
          } catch (InvocationTargetException exception) {
            Throwable cause = exception.getCause();
            if (cause instanceof Exception checked)
              throw checked;
            if (cause instanceof Error error)
              throw error;
            throw new RuntimeException(cause);
          }
        }
      }
      type = type.getSuperclass();
    }
    throw new NoSuchMethodException(start.getName() + "." + name);
  }

  private void layout() {
    root.applyCss();
    root.layout();
  }

  private static <T> T fx(Callable<T> task) throws Exception {
    FutureTask<T> future = new FutureTask<>(task);
    Platform.runLater(future);
    return future.get(30, TimeUnit.SECONDS);
  }
}
