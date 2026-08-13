package com.majortom.algorithms.visualization;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VisualThemeResourceTest {

    private static final List<String> FXML_RESOURCES = List.of(
            "/fxml/MainControls.fxml",
            "/fxml/SortControls.fxml",
            "/fxml/MazeControls.fxml",
            "/fxml/TreeControls.fxml",
            "/fxml/GraphControls.fxml");

    @Test
    void everyFxmlUsesTheSingleThemeResource() throws IOException {
        assertNotNull(getClass().getResource("/style/theme.css"));
        for (String resource : FXML_RESOURCES) {
            String source = read(resource);
            assertTrue(source.contains("@../style/theme.css"), resource);
            assertFalse(source.contains("ui_theme.css"), resource);
        }
    }

    @Test
    void themeRetainsDynamicDialogSemanticClasses() throws IOException {
        String theme = read("/style/theme.css");
        for (String selector : List.of(
                ".operation-dialog-pane",
                ".operation-dialog-content",
                ".dialog-form-section",
                ".dialog-section-title",
                ".dialog-input",
                ".dialog-action-row")) {
            assertTrue(theme.contains(selector), selector);
        }
    }

    private String read(String resource) throws IOException {
        try (InputStream input = getClass().getResourceAsStream(resource)) {
            assertNotNull(input, resource);
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
