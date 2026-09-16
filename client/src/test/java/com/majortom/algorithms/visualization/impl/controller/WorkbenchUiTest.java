package com.majortom.algorithms.visualization.impl.controller;

import static org.junit.jupiter.api.Assertions.*;

import com.majortom.algorithms.visualization.BaseController;
import com.majortom.algorithms.visualization.common.OverlayPane;
import com.majortom.algorithms.visualization.common.VisualizationSurface;
import com.majortom.algorithms.visualization.international.I18N;
import com.majortom.algorithms.visualization.navigation.FamilyItemView;
import com.majortom.algorithms.visualization.navigation.FamilyNavigator;
import com.majortom.algorithms.visualization.runtime.VisualValue;
import com.majortom.algorithms.visualization.settings.FontSettings;
import com.majortom.algorithms.visualization.settings.FontSettingsService;
import com.majortom.algorithms.visualization.structure.RuntimeValueTypeSupport;
import com.majortom.algorithms.visualization.structure.StructureSnapshotSupport;

import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Region;
import javafx.stage.Stage;
import javafx.stage.Window;

import org.junit.jupiter.api.*;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.*;

/** Real FXML, JavaFX controls, event dispatch and layout; no OS input injection. */
class WorkbenchUiTest {
    private static final java.util.Queue<Throwable> fxErrors = new ConcurrentLinkedQueue<>();
    private MainController controller;
    private Parent root;
    private Stage stage;

    @BeforeAll
    static void startFx() throws Exception {
        CountDownLatch started = new CountDownLatch(1);
        Platform.startup(
                () -> {
                    Platform.setImplicitExit(false);
                    Thread.currentThread()
                            .setUncaughtExceptionHandler((thread, error) -> fxErrors.add(error));
                    javafx.application.Application.setUserAgentStylesheet(
                            new atlantafx.base.theme.PrimerDark().getUserAgentStylesheet());
                    started.countDown();
                });
        assertTrue(started.await(15, TimeUnit.SECONDS));
    }

    @BeforeEach
    void openWorkbench() throws Exception {
        fx(
                () -> {
                    I18N.setLocale(Locale.CHINESE);
                    FXMLLoader loader =
                            new FXMLLoader(
                                    getClass().getResource("/fxml/MainControls.fxml"),
                                    I18N.getBundle());
                    root = loader.load();
                    controller = loader.getController();
                    stage = new Stage();
                    stage.setScene(new Scene(root, 1600, 900));
                    stage.show();
                    layout();
                    return null;
                });
        awaitNode(".array-cell");
    }

    @AfterEach
    void closeWorkbench() throws Exception {
        fx(
                () -> {
                    invoke("detachCurrentController");
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
    void arrayClickShowsGenericDetailsAndLocaleDoesNotChangeData() throws Exception {
        for (Class<?> type : List.of(Integer.class, String.class)) {
            fx(
                    () -> {
                        ArrayController array = (ArrayController) subController();
                        array.setRuntimeValueType(type);
                        array.applyBulkData(type == String.class ? "alpha, beta" : "24, 36");
                        return null;
                    });
            awaitNode(".array-cell");
            fx(
                    () -> {
                        Object before = snapshotState();
                        click(root.lookup(".array-cell"));
                        String detail = label("structureInspectorBody").getText();
                        assertTrue(detail.contains(type == String.class ? "alpha" : "24"), detail);
                        assertTrue(node("structureSelectionOverlay").isVisible());
                        I18N.setLocale(Locale.ENGLISH);
                        layout();
                        assertEquals(before, snapshotState());
                        assertEquals(
                                type,
                                ((RuntimeValueTypeSupport) subController()).runtimeValueType());
                        assertTrue(label("structureInspectorBody").getText().contains("Value"));
                        I18N.setLocale(Locale.CHINESE);
                        assertEquals(before, snapshotState());
                        return null;
                    });
        }
    }

    @Test
    void linkedNodeClickReachesInspector() throws Exception {
        fx(
                () -> {
                    moduleButton(1).fire();
                    return null;
                });
        awaitVisibleNode(".visual-node");
        fx(
                () -> {
                    click(root.lookup(".visual-node"));
                    assertTrue(node("structureSelectionOverlay").isVisible());
                    assertTrue(label("structureInspectorBody").getText().contains("前驱"));
                    Object before = snapshotState();
                    I18N.setLocale(Locale.ENGLISH);
                    assertEquals(before, snapshotState());
                    assertTrue(label("structureInspectorBody").getText().contains("Previous"));
                    return null;
                });
    }

    @Test
    void everyGenericSelectionFormatsBeforePublishingInBothLanguages() throws Exception {
        fx(
                () -> {
                    String text = "用户值 %d / alpha";
                    VisualValue value = VisualValue.of(text);
                    for (Locale locale : List.of(Locale.CHINESE, Locale.ENGLISH)) {
                        I18N.setLocale(locale);
                        invoke(
                                "showArraySelection",
                                new ArrayController.IndexSelection(1, value, 3));
                        assertTrue(label("structureInspectorBody").getText().contains(text));
                        invoke(
                                "showTreeSelection",
                                new TreeController.NodeSelection(7, value, null, 2, 0));
                        assertTrue(label("structureInspectorBody").getText().contains(text));
                        invoke(
                                "showLinkedSelection",
                                new LinkedListController.NodeSelection(7, value, null, 8L, 0, 2));
                        assertTrue(label("structureInspectorBody").getText().contains(text));
                        invoke(
                                "showLinearSelection",
                                new LinearStructureController.ItemSelection(0, value, "TOP", 2));
                        assertTrue(label("structureInspectorBody").getText().contains(text));
                        invoke(
                                "showGraphSelection",
                                new GraphController.NodeSelection(7, value, 2));
                        assertTrue(label("structureInspectorBody").getText().contains(text));
                        invoke(
                                "showGraphSelection",
                                new GraphController.EdgeSelection(
                                        9, value, VisualValue.of("终点"), true));
                        assertTrue(label("structureInspectorBody").getText().contains("终点"));
                    }
                    return null;
                });
    }

    @Test
    void stackQueueTreeAndGraphNodesReachInspectorViaMouseEvents() throws Exception {
        for (int module : new int[] {2, 3, 4, 5}) {
            fx(
                    () -> {
                        moduleButton(module).fire();
                        return null;
                    });
            awaitVisibleNode(".visual-node");
            fx(
                    () -> {
                        click(root.lookup(".visual-node"));
                        assertTrue(node("structureSelectionOverlay").isVisible());
                        assertTrue(label("structureInspectorBody").getText().contains("值"));
                        return null;
                    });
        }
    }

    @Test
    void graphAutoFitRespectsSurfaceSafeInsets() throws Exception {
        fx(
                () -> {
                    moduleButton(5).fire();
                    return null;
                });
        awaitVisibleNode(".visual-node");
        fx(
                () -> {
                    layout();
                    VisualizationSurface surface =
                            (VisualizationSurface)
                                    subController()
                                            .getVisualizerView()
                                            .lookup(".visualization-surface");
                    assertNotNull(surface);
                    Bounds viewport = surface.localToScene(surface.getBoundsInLocal());
                    javafx.geometry.Insets insets = surface.safeInsets();
                    assertTrue(
                            insets.getTop() >= 56.0d,
                            () -> "Graph requires a dedicated top camera safe area, got " + insets);
                    var nodes = subController().getVisualizerView().lookupAll(".visual-node");
                    assertFalse(nodes.isEmpty());
                    for (Node visualNode : nodes) {
                        Bounds bounds = visualNode.localToScene(visualNode.getBoundsInLocal());
                        assertTrue(
                                bounds.getMinX() >= viewport.getMinX() + insets.getLeft() - 3.0d,
                                () -> "Graph node escaped left safe inset: " + bounds);
                        assertTrue(
                                bounds.getMaxX() <= viewport.getMaxX() - insets.getRight() + 3.0d,
                                () -> "Graph node escaped right safe inset: " + bounds);
                        assertTrue(
                                bounds.getMinY() >= viewport.getMinY() + insets.getTop() - 3.0d,
                                () -> "Graph node escaped top safe inset: " + bounds);
                        assertTrue(
                                bounds.getMaxY() <= viewport.getMaxY() - insets.getBottom() + 3.0d,
                                () -> "Graph node escaped bottom safe inset: " + bounds);
                    }
                    return null;
                });
    }

    @Test
    void firstFitRemainsCenteredAfterRealPulseAcrossFamilies() throws Exception {
        for (int module : new int[] {3, 4, 5}) {
            fx(
                    () -> {
                        moduleButton(module).fire();
                        return null;
                    });
            awaitVisibleNode(".visual-node");
            // Let JavaFX process at least one later frame after the render transaction completed.
            // GesturePane target normalization used to move the camera during this window.
            Thread.sleep(180L);
            fx(
                    () -> {
                        layout();
                        VisualizationSurface surface =
                                (VisualizationSurface)
                                        subController()
                                                .getVisualizerView()
                                                .lookup(".visualization-surface");
                        assertNotNull(surface);
                        var nodes = subController().getVisualizerView().lookupAll(".visual-node");
                        assertFalse(nodes.isEmpty());

                        double minX = Double.POSITIVE_INFINITY;
                        double minY = Double.POSITIVE_INFINITY;
                        double maxX = Double.NEGATIVE_INFINITY;
                        double maxY = Double.NEGATIVE_INFINITY;
                        for (Node visualNode : nodes) {
                            Bounds bounds = visualNode.localToScene(visualNode.getBoundsInLocal());
                            minX = Math.min(minX, bounds.getMinX());
                            minY = Math.min(minY, bounds.getMinY());
                            maxX = Math.max(maxX, bounds.getMaxX());
                            maxY = Math.max(maxY, bounds.getMaxY());
                        }

                        Bounds viewport = surface.localToScene(surface.getBoundsInLocal());
                        javafx.geometry.Insets insets = surface.safeInsets();
                        double safeMinX = viewport.getMinX() + insets.getLeft();
                        double safeMaxX = viewport.getMaxX() - insets.getRight();
                        double safeMinY = viewport.getMinY() + insets.getTop();
                        double safeMaxY = viewport.getMaxY() - insets.getBottom();
                        double expectedCenterX = (safeMinX + safeMaxX) / 2.0d;
                        double expectedCenterY = (safeMinY + safeMaxY) / 2.0d;
                        double actualCenterX = (minX + maxX) / 2.0d;
                        double actualCenterY = (minY + maxY) / 2.0d;

                        assertEquals(
                                expectedCenterX,
                                actualCenterX,
                                4.0d,
                                () ->
                                        "Family "
                                                + module
                                                + " camera drifted horizontally after pulse");
                        assertEquals(
                                expectedCenterY,
                                actualCenterY,
                                4.0d,
                                () ->
                                        "Family "
                                                + module
                                                + " camera drifted vertically after pulse");
                        return null;
                    });
        }
    }

    @Test
    void selectionAndLongValuesDoNotMoveInspectorBoundary() throws Exception {
        fx(
                () -> {
                    assertInstanceOf(
                            OverlayPane.class, node("structureSelectionOverlay").getParent());
                    assertTrue(node("structureSelectionOverlay").getParent().isMouseTransparent());
                    for (double width : new double[] {1600, 1280, 1100, 1920}) {
                        stage.setWidth(width);
                        layout();
                        Region panel = (Region) node("snapshotPanel");
                        double x = panel.getLayoutX(), w = panel.getWidth();
                        invoke(
                                "showArraySelection",
                                new ArrayController.IndexSelection(
                                        0, VisualValue.of("很长的字符串".repeat(200)), 1));
                        for (int pulse = 0; pulse < 12; pulse++) {
                            layout();
                            assertEquals(x, panel.getLayoutX(), 0.01);
                            assertEquals(w, panel.getWidth(), 0.01);
                        }
                        assertTrue(
                                label("structureInspectorBody")
                                        .getText()
                                        .contains("很长的字符串".repeat(200)));
                        invoke("clearStructureSelection");
                        layout();
                        assertEquals(x, panel.getLayoutX(), 0.01);
                    }
                    return null;
                });
    }

    @Test
    void typeControlStaysInStructureRailAndCancelPreservesData() throws Exception {
        fx(
                () -> {
                    assertTrue(isAncestor(node("structureControlRail"), node("valueTypeBox")));
                    assertFalse(isAncestor(node("topBar"), node("valueTypeBox")));
                    Object before = snapshotState();
                    answerNextTypeDialog(false);
                    ((ComboBox<?>) node("valueTypeSelector")).getSelectionModel().select(1);
                    assertEquals(before, snapshotState());
                    assertEquals(
                            Integer.class,
                            ((RuntimeValueTypeSupport) subController()).runtimeValueType());
                    assertEquals(
                            0,
                            ((ComboBox<?>) node("valueTypeSelector"))
                                    .getSelectionModel()
                                    .getSelectedIndex());
                    answerNextTypeDialog(true);
                    ((ComboBox<?>) node("valueTypeSelector")).getSelectionModel().select(1);
                    assertEquals(
                            String.class,
                            ((RuntimeValueTypeSupport) subController()).runtimeValueType());
                    assertFalse(((RuntimeValueTypeSupport) subController()).hasValues());
                    // Empty data switches immediately without a second confirmation.
                    ((ComboBox<?>) node("valueTypeSelector")).getSelectionModel().select(0);
                    assertEquals(
                            Integer.class,
                            ((RuntimeValueTypeSupport) subController()).runtimeValueType());
                    assertFalse(((RuntimeValueTypeSupport) subController()).hasValues());
                    moduleButton(1).fire();
                    assertTrue(isAncestor(node("structureControlRail"), node("valueTypeBox")));
                    return null;
                });
    }

    @Test
    void typesResetEmptyForAllGenericFamiliesAndSpecialFamiliesAreReadOnly() throws Exception {
        fx(
                () -> {
                    for (int module = 0; module < 6; module++) {
                        moduleButton(module).fire();
                        RuntimeValueTypeSupport support = (RuntimeValueTypeSupport) subController();
                        support.setRuntimeValueType(String.class);
                        assertFalse(support.hasValues());
                        Object before = snapshotState();
                        I18N.setLocale(Locale.ENGLISH);
                        I18N.setLocale(Locale.CHINESE);
                        assertEquals(before, snapshotState());
                        assertEquals(String.class, support.runtimeValueType());
                    }
                    moduleButton(6).fire();
                    assertTrue(node("valueTypeSelector").isDisabled());
                    moduleButton(7).fire();
                    assertFalse(node("valueTypeBox").isManaged());
                    return null;
                });
    }

    @Test
    void algorithmModeAndSnapshotPreviewCannotChangeType() throws Exception {
        fx(
                () -> {
                    Object before = snapshotState();
                    ((Button) node("algorithmWorkspaceBtn")).fire();
                    assertTrue(node("valueTypeSelector").isDisabled());
                    assertTrue(label("algorithmInputSourceLabel").getText().contains("整数"));
                    ((ComboBox<?>) node("valueTypeSelector")).getSelectionModel().select(1);
                    assertEquals(before, snapshotState());
                    assertEquals(
                            Integer.class,
                            ((RuntimeValueTypeSupport) subController()).runtimeValueType());
                    ((Button) node("structureWorkspaceBtn")).fire();
                    ((Button) node("saveSnapshotBtn")).fire();
                    // The preview flag is normally set by a saved-snapshot card.
                    Field preview =
                            MainController.class.getDeclaredField("structureSnapshotPreviewActive");
                    preview.setAccessible(true);
                    preview.setBoolean(controller, true);
                    invoke("updateWorkspaceInteractionState");
                    assertTrue(node("valueTypeBox").isDisabled());
                    return null;
                });
    }

    @Test
    void languageChangesKeepTreeAndGraphVariantsAndAlgorithmChoice() throws Exception {
        for (int module : new int[] {4, 5}) {
            fx(
                    () -> {
                        moduleButton(module).fire();
                        Field structureField =
                                subController().getClass().getDeclaredField("structureSelector");
                        structureField.setAccessible(true);
                        ComboBox<?> selector = (ComboBox<?>) structureField.get(subController());
                        selector.getSelectionModel().select(1);
                        Field algorithmField =
                                subController().getClass().getDeclaredField("algorithmSelector");
                        algorithmField.setAccessible(true);
                        ComboBox<?> algorithm = (ComboBox<?>) algorithmField.get(subController());
                        algorithm.getSelectionModel().selectLast();
                        int algorithmIndex = algorithm.getSelectionModel().getSelectedIndex();
                        Object before = snapshotState();
                        I18N.setLocale(Locale.ENGLISH);
                        I18N.setLocale(Locale.CHINESE);
                        assertEquals(1, selector.getSelectionModel().getSelectedIndex());
                        assertEquals(
                                algorithmIndex, algorithm.getSelectionModel().getSelectedIndex());
                        assertEquals(before, snapshotState());
                        return null;
                    });
        }
    }

    @Test
    void arrayRemainsVisibleAfterLeavingAndReturning() throws Exception {
        fx(
                () -> {
                    moduleButton(0).fire();
                    return null;
                });
        awaitVisibleNode(".array-cell");
        fx(
                () -> {
                    moduleButton(5).fire();
                    return null;
                });
        awaitVisibleNode(".visual-node");
        fx(
                () -> {
                    moduleButton(0).fire();
                    return null;
                });
        awaitVisibleNode(".array-cell");
    }

    @Test
    void genericModulesCanLeaveAndReturnAfterTypeChange() throws Exception {
        for (int module = 0; module < 6; module++) {
            final int selectedModule = module;
            fx(
                    () -> {
                        moduleButton(selectedModule).fire();
                        if (((RuntimeValueTypeSupport) subController()).hasValues())
                            answerNextTypeDialog(true);
                        ((ComboBox<?>) node("valueTypeSelector")).getSelectionModel().select(1);
                        assertEquals(
                                String.class,
                                ((RuntimeValueTypeSupport) subController()).runtimeValueType());
                        moduleButton(7).fire();
                        moduleButton(selectedModule).fire();
                        layout();
                        assertEquals(
                                String.class,
                                ((RuntimeValueTypeSupport) subController()).runtimeValueType());
                        assertEquals(
                                1,
                                ((ComboBox<?>) node("valueTypeSelector"))
                                        .getSelectionModel()
                                        .getSelectedIndex());
                        assertFalse(
                                ((javafx.scene.layout.VBox) node("structureKindHost"))
                                        .getChildren()
                                        .isEmpty());
                        assertFalse(
                                ((javafx.scene.layout.VBox) node("structureControlsHost"))
                                        .getChildren()
                                        .isEmpty());
                        if (selectedModule == 4) {
                            assertTrue(root.lookup("#addRootBtn").isVisible());
                            ((TextField) root.lookup("#valueField")).setText("root");
                            ((Button) root.lookup("#addRootBtn")).fire();
                            assertTrue(((RuntimeValueTypeSupport) subController()).hasValues());
                        }
                        assertEquals(
                                I18N.text(
                                        selectedModule == 4
                                                ? "label.value_type.node"
                                                : selectedModule == 5
                                                        ? "label.value_type.vertex"
                                                        : "label.value_type.element"),
                                label("valueTypeLabel").getText());
                        // Return to the Integer path too, not only a fresh String controller.
                        ((BaseModuleController<?>) subController())
                                .applyBulkData(
                                        selectedModule == 5
                                                ? "alpha,beta | alpha->beta:5"
                                                : "alpha,beta");
                        assertTrue(((RuntimeValueTypeSupport) subController()).hasValues());
                        answerNextTypeDialog(true);
                        ((ComboBox<?>) node("valueTypeSelector")).getSelectionModel().select(0);
                        moduleButton(7).fire();
                        moduleButton(selectedModule).fire();
                        assertEquals(
                                Integer.class,
                                ((RuntimeValueTypeSupport) subController()).runtimeValueType());
                        return null;
                    });
        }
    }

    @Test
    void familyNavigatorGeometryIsStableAcrossSelectionAndMode() throws Exception {
        fx(
                () -> {
                    FamilyNavigator navigator = (FamilyNavigator) node("familyNavigator");
                    List<FamilyGeometry> baseline = familyGeometry(navigator);

                    moduleButton(5).fire();
                    layout();
                    assertFamilyGeometryEquals(
                            baseline,
                            familyGeometry(navigator),
                            "Selecting Graph must not change family navigation geometry");

                    ((Button) node("algorithmWorkspaceBtn")).fire();
                    layout();
                    assertSame(
                            navigator,
                            node("familyNavigator"),
                            "Structure/Algorithm must use one physical FamilyNavigator");
                    assertFamilyGeometryEquals(
                            baseline,
                            familyGeometry(navigator),
                            "Mode switch must not change shared family navigation geometry");

                    navigator.item("tree").fire();
                    layout();
                    assertFamilyGeometryEquals(
                            baseline,
                            familyGeometry(navigator),
                            "Selecting Tree must be presentation-only");
                    assertEquals("tree", navigator.selectedFamily());
                    return null;
                });
    }

    @Test
    void familyNavigatorLeavesLayoutInPracticeModeAndReturnsUnchanged() throws Exception {
        fx(
                () -> {
                    FamilyNavigator navigator = (FamilyNavigator) node("familyNavigator");
                    List<FamilyGeometry> baseline = familyGeometry(navigator);
                    ((Button) node("practiceWorkspaceBtn")).fire();
                    layout();
                    assertFalse(navigator.isManaged());
                    assertFalse(navigator.isVisible());

                    ((Button) node("structureWorkspaceBtn")).fire();
                    layout();
                    assertTrue(navigator.isManaged());
                    assertTrue(navigator.isVisible());
                    assertFamilyGeometryEquals(
                            baseline,
                            familyGeometry(navigator),
                            "Practice round-trip must not rebuild family geometry");
                    return null;
                });
    }

    @Test
    void familyNavigatorScalesAsOneUnitAcrossFontAndViewportSweep() throws Exception {
        FontSettingsService fonts = new FontSettingsService();
        for (double width : new double[] {1600.0d, 1200.0d, 1000.0d}) {
            for (double size : new double[] {16.25d, 18.0d, 20.0d, 22.0d, 24.0d}) {
                fx(
                        () -> {
                            stage.setWidth(width);
                            stage.setHeight(width >= 1200.0d ? 900.0d : 720.0d);
                            fonts.apply(root, new FontSettings("", "", size, ""));
                            invoke("refreshUiFramework");
                            layout();

                            FamilyNavigator navigation = (FamilyNavigator) node("familyNavigator");
                            assertEquals(8, navigation.items().size());
                            double rowHeight = navigation.items().getFirst().getHeight();
                            double nameX =
                                    ((Label)
                                                    navigation
                                                            .items()
                                                            .getFirst()
                                                            .lookup(".family-item-name"))
                                            .getLayoutX();
                            double previousY = -1.0d;
                            for (FamilyItemView item : navigation.items()) {
                                assertEquals(
                                        rowHeight,
                                        item.getHeight(),
                                        0.5d,
                                        "all family rows must have identical height");
                                Label name = (Label) item.lookup(".family-item-name");
                                assertNotNull(name);
                                assertEquals(
                                        nameX,
                                        name.getLayoutX(),
                                        0.5d,
                                        "family name column must stay aligned");
                                assertTrue(
                                        name.getWidth() + 0.5d >= name.prefWidth(-1.0d),
                                        () ->
                                                "family label clipped at "
                                                        + size
                                                        + "px / "
                                                        + width
                                                        + "px: "
                                                        + name.getText()
                                                        + " width="
                                                        + name.getWidth()
                                                        + " pref="
                                                        + name.prefWidth(-1.0d));
                                assertTrue(
                                        item.getLayoutY() > previousY,
                                        "family rows must keep registry order");
                                previousY = item.getLayoutY();
                            }
                            return null;
                        });
            }
        }
    }

    private void assertFamilyGeometryEquals(
            List<FamilyGeometry> expected, List<FamilyGeometry> actual, String message) {
        assertEquals(expected.size(), actual.size(), message);
        for (int index = 0; index < expected.size(); index++) {
            FamilyGeometry a = expected.get(index);
            FamilyGeometry b = actual.get(index);
            assertEquals(a.id(), b.id(), message);
            assertEquals(a.x(), b.x(), 0.5d, message);
            assertEquals(a.y(), b.y(), 0.5d, message);
            assertEquals(a.width(), b.width(), 0.5d, message);
            assertEquals(a.height(), b.height(), 0.5d, message);
            assertEquals(a.nameX(), b.nameX(), 0.5d, message);
        }
    }

    private List<FamilyGeometry> familyGeometry(FamilyNavigator navigator) {
        return navigator.items().stream()
                .map(
                        item ->
                                new FamilyGeometry(
                                        item.familyId(),
                                        item.getLayoutX(),
                                        item.getLayoutY(),
                                        item.getWidth(),
                                        item.getHeight(),
                                        ((Label) item.lookup(".family-item-name")).getLayoutX()))
                .toList();
    }

    private record FamilyGeometry(
            String id, double x, double y, double width, double height, double nameX) {}

    @Test
    void failedModulePreparationPreservesCurrentControlsDataAndNavigation() throws Exception {
        fx(
                () -> {
                    BaseController<?> previous = subController();
                    Object before = snapshotState();
                    Object definition = field("activeDefinition");
                    String title = label("topContextLabel").getText();
                    var controls =
                            List.copyOf(
                                    ((javafx.scene.layout.VBox) node("structureControlsHost"))
                                            .getChildren());
                    var broken =
                            new com.majortom.algorithms.visualization.module
                                    .WorkbenchModuleDefinition(
                                    "graph",
                                    "Graph",
                                    () ->
                                            new BaseModuleController<Object>(
                                                    null, "/fxml/missing-test-panel.fxml") {
                                                @Override
                                                protected String moduleId() {
                                                    return "graph";
                                                }

                                                @Override
                                                protected String formatStatsMessage() {
                                                    return "";
                                                }

                                                @Override
                                                protected void setupI18n() {}

                                                @Override
                                                protected void onResetData() {}

                                                @Override
                                                public void handleAlgorithmStart() {}
                                            });
                    invoke("switchToModule", broken);
                    assertSame(previous, subController());
                    assertSame(definition, field("activeDefinition"));
                    assertEquals(before, snapshotState());
                    assertEquals(title, label("topContextLabel").getText());
                    assertEquals(
                            controls,
                            ((javafx.scene.layout.VBox) node("structureControlsHost"))
                                    .getChildren());
                    assertTrue(
                            moduleButton(0)
                                    .getPseudoClassStates()
                                    .contains(javafx.css.PseudoClass.getPseudoClass("selected")));
                    // Old controller is still usable, not disposed by the failed transition.
                    ((ArrayController) previous).applyBulkData("91, 92");
                    assertNotEquals(before, snapshotState());
                    return null;
                });
    }

    private void answerNextTypeDialog(boolean confirm) {
        Platform.runLater(
                () -> {
                    for (Window window : List.copyOf(Window.getWindows())) {
                        if (window == stage || window.getScene() == null) continue;
                        Node pane = window.getScene().getRoot();
                        if (pane instanceof DialogPane dialog) {
                            ButtonType answer =
                                    dialog.getButtonTypes().stream()
                                            .filter(
                                                    b ->
                                                            b.getButtonData().isCancelButton()
                                                                    != confirm)
                                            .findFirst()
                                            .orElseThrow();
                            ((Button) dialog.lookupButton(answer)).fire();
                            return;
                        }
                    }
                    throw new AssertionError("Expected a type-change confirmation dialog");
                });
    }

    private Object snapshotState() {
        return ((StructureSnapshotSupport<?>) subController()).captureStructureSnapshot().state();
    }

    private BaseController<?> subController() {
        return (BaseController<?>) field("currentSubController");
    }

    private FamilyItemView moduleButton(int index) {
        return ((FamilyNavigator) node("familyNavigator")).items().get(index);
    }

    private Node node(String id) {
        return (Node) field(id);
    }

    private Label label(String id) {
        return (Label) field(id);
    }

    private Object field(String name) {
        try {
            Field f = MainController.class.getDeclaredField(name);
            f.setAccessible(true);
            return f.get(controller);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
    }

    private void invoke(String name, Object... args) throws Exception {
        Method method =
                java.util.Arrays.stream(MainController.class.getDeclaredMethods())
                        .filter(m -> m.getName().equals(name))
                        .filter(m -> m.getParameterCount() == args.length)
                        .findFirst()
                        .orElseThrow();
        method.setAccessible(true);
        method.invoke(controller, args);
    }

    private void layout() {
        root.applyCss();
        root.layout();
    }

    private static double effectiveOpacity(Node node) {
        double opacity = 1.0d;
        for (Node current = node; current != null; current = current.getParent()) {
            opacity *= current.getOpacity();
        }
        return opacity;
    }

    private static boolean isAncestor(Node parent, Node child) {
        for (Node n = child; n != null; n = n.getParent()) if (n == parent) return true;
        return false;
    }

    private static void click(Node node) {
        assertNotNull(node);
        node.fireEvent(
                new MouseEvent(
                        MouseEvent.MOUSE_CLICKED,
                        10,
                        10,
                        10,
                        10,
                        MouseButton.PRIMARY,
                        1,
                        false,
                        false,
                        false,
                        false,
                        false,
                        false,
                        false,
                        false,
                        false,
                        true,
                        null));
    }

    private void awaitVisibleNode(String selector) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
        while (System.nanoTime() < deadline) {
            if (fx(
                    () -> {
                        layout();
                        Node candidate = root.lookup(selector);
                        return candidate != null && effectiveOpacity(candidate) > 0.99d;
                    })) return;
            Thread.sleep(20L);
        }
        fail("Timed out waiting for visible node " + selector);
    }

    private void awaitNode(String selector) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
        while (System.nanoTime() < deadline) {
            if (fx(
                    () -> {
                        layout();
                        return root.lookup(selector) != null;
                    })) return;
            Thread.sleep(40);
        }
        fail("No rendered node: " + selector);
    }

    private static <T> T fx(Callable<T> task) throws Exception {
        FutureTask<T> future = new FutureTask<>(task);
        Platform.runLater(future);
        return future.get(30, TimeUnit.SECONDS);
    }
}
