package com.majortom.algorithms.visualization.settings;

import javafx.beans.value.ChangeListener;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Labeled;
import javafx.scene.control.TextInputControl;
import javafx.scene.layout.Region;
import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Applies separate Chinese/Latin font families without taking ownership of
 * font size, weight, color or Workbench geometry.
 *
 * <p>Pure-script controls keep their normal JavaFX Labeled rendering and only
 * receive an inline family override. Mixed Chinese/Latin Labeled text is
 * rendered through a TextFlow so each script run can use its own family while
 * the original text property remains available for bindings/accessibility.</p>
 */
final class ScriptFontSupport {

    private static final String INSTALLED_KEY = ScriptFontSupport.class.getName() + ".installed";
    private static final String SETTINGS_KEY = ScriptFontSupport.class.getName() + ".settings";
    private static final String FLOW_KEY = ScriptFontSupport.class.getName() + ".flow";
    private static final String ORIGINAL_GRAPHIC_KEY = ScriptFontSupport.class.getName() + ".originalGraphic";
    private static final String ORIGINAL_CONTENT_DISPLAY_KEY = ScriptFontSupport.class.getName() + ".originalContentDisplay";
    private static final String SCRIPT_FLOW_STYLE = "script-font-flow";

    private ScriptFontSupport() {
    }

    static void apply(Parent root, FontSettings settings) {
        if (root == null || settings == null) {
            return;
        }
        // Empty family deliberately means "do not override": JavaFX keeps the
        // current CSS/system family. Both selectors expose the same installed
        // family list; only the Unicode-script routing differs.
        applyNode(root, new ResolvedSettings(
                settings.chineseFamily(),
                settings.englishFamily()));
    }

    private static void applyNode(Node node, ResolvedSettings settings) {
        if (node == null || node.getStyleClass().contains(SCRIPT_FLOW_STYLE)) {
            return;
        }
        node.getProperties().put(SETTINGS_KEY, settings);
        if (node instanceof Labeled labeled) {
            install(labeled);
            update(labeled);
        } else if (node instanceof TextInputControl input) {
            install(input);
            update(input);
        } else if (node instanceof Text text) {
            install(text);
            update(text);
        }
        if (node instanceof Parent parent) {
            for (Node child : parent.getChildrenUnmodifiable()) {
                applyNode(child, settings);
            }
        }
    }

    private static void install(Labeled labeled) {
        if (Boolean.TRUE.equals(labeled.getProperties().get(INSTALLED_KEY))) {
            return;
        }
        labeled.getProperties().put(INSTALLED_KEY, Boolean.TRUE);
        ChangeListener<String> textListener = (observable, oldValue, newValue) -> update(labeled);
        ChangeListener<Font> fontListener = (observable, oldValue, newValue) -> update(labeled);
        labeled.textProperty().addListener(textListener);
        labeled.fontProperty().addListener(fontListener);
    }

    private static void install(TextInputControl input) {
        if (Boolean.TRUE.equals(input.getProperties().get(INSTALLED_KEY))) {
            return;
        }
        input.getProperties().put(INSTALLED_KEY, Boolean.TRUE);
        input.textProperty().addListener((observable, oldValue, newValue) -> update(input));
        input.promptTextProperty().addListener((observable, oldValue, newValue) -> update(input));
    }

    private static void install(Text text) {
        if (Boolean.TRUE.equals(text.getProperties().get(INSTALLED_KEY))) {
            return;
        }
        text.getProperties().put(INSTALLED_KEY, Boolean.TRUE);
        text.textProperty().addListener((observable, oldValue, newValue) -> update(text));
    }

    private static void update(Labeled labeled) {
        ResolvedSettings settings = settings(labeled);
        if (settings == null) {
            return;
        }
        List<Run> runs = runs(labeled.getText());
        boolean mixed = containsBothScripts(runs);
        if (!mixed || hasForeignGraphic(labeled)) {
            restoreNormalRendering(labeled);
            Script script = dominantScript(runs);
            applyFamilyStyle(labeled, family(settings, script));
            return;
        }

        clearFamilyStyle(labeled);
        TextFlow flow = textFlow(labeled);
        flow.getChildren().clear();
        Font base = labeled.getFont() == null ? Font.getDefault() : labeled.getFont();
        for (Run run : runs) {
            Text text = new Text(run.text());
            text.setFont(fontWithFamily(base, family(settings, run.script())));
            text.fillProperty().bind(labeled.textFillProperty());
            flow.getChildren().add(text);
        }
        labeled.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        if (labeled.getGraphic() != flow) {
            labeled.setGraphic(flow);
        }
    }

    private static void update(TextInputControl input) {
        ResolvedSettings settings = settings(input);
        if (settings == null) {
            return;
        }
        String value = input.getText();
        if (value == null || value.isBlank()) {
            value = input.getPromptText();
        }
        applyFamilyStyle(input, family(settings, dominantScript(runs(value))));
    }

    private static void update(Text text) {
        if (text.getParent() != null && text.getParent().getStyleClass().contains(SCRIPT_FLOW_STYLE)) {
            return;
        }
        ResolvedSettings settings = settings(text);
        if (settings == null) {
            return;
        }
        applyFamilyStyle(text, family(settings, dominantScript(runs(text.getText()))));
    }

    private static TextFlow textFlow(Labeled labeled) {
        Object existing = labeled.getProperties().get(FLOW_KEY);
        if (existing instanceof TextFlow flow) {
            return flow;
        }
        if (labeled.getGraphic() != null) {
            labeled.getProperties().put(ORIGINAL_GRAPHIC_KEY, labeled.getGraphic());
        }
        labeled.getProperties().put(ORIGINAL_CONTENT_DISPLAY_KEY, labeled.getContentDisplay());
        TextFlow flow = new TextFlow();
        flow.setMouseTransparent(true);
        flow.getStyleClass().add(SCRIPT_FLOW_STYLE);
        flow.setMinWidth(0.0d);
        flow.setPrefWidth(Region.USE_COMPUTED_SIZE);
        flow.setMaxWidth(Double.MAX_VALUE);
        labeled.getProperties().put(FLOW_KEY, flow);
        return flow;
    }

    private static boolean hasForeignGraphic(Labeled labeled) {
        Object flow = labeled.getProperties().get(FLOW_KEY);
        return labeled.getGraphic() != null && labeled.getGraphic() != flow;
    }

    private static void restoreNormalRendering(Labeled labeled) {
        Object flow = labeled.getProperties().get(FLOW_KEY);
        if (!(flow instanceof TextFlow) || labeled.getGraphic() != flow) {
            return;
        }
        Object originalGraphic = labeled.getProperties().get(ORIGINAL_GRAPHIC_KEY);
        labeled.setGraphic(originalGraphic instanceof Node node ? node : null);
        Object originalDisplay = labeled.getProperties().get(ORIGINAL_CONTENT_DISPLAY_KEY);
        labeled.setContentDisplay(originalDisplay instanceof ContentDisplay display
                ? display
                : ContentDisplay.LEFT);
    }

    private static Font fontWithFamily(Font base, String family) {
        if (family == null || family.isBlank()) {
            return base;
        }
        String style = base.getStyle() == null ? "" : base.getStyle().toLowerCase(Locale.ROOT);
        FontWeight weight = FontWeight.NORMAL;
        if (style.contains("black")) {
            weight = FontWeight.BLACK;
        } else if (style.contains("extra bold") || style.contains("extrabold")) {
            weight = FontWeight.EXTRA_BOLD;
        } else if (style.contains("semi bold") || style.contains("semibold") || style.contains("demi")) {
            weight = FontWeight.SEMI_BOLD;
        } else if (style.contains("bold")) {
            weight = FontWeight.BOLD;
        } else if (style.contains("medium")) {
            weight = FontWeight.MEDIUM;
        } else if (style.contains("light")) {
            weight = FontWeight.LIGHT;
        }
        FontPosture posture = style.contains("italic") || style.contains("oblique")
                ? FontPosture.ITALIC
                : FontPosture.REGULAR;
        return Font.font(family, weight, posture, base.getSize());
    }

    private static List<Run> runs(String value) {
        if (value == null || value.isEmpty()) {
            return List.of();
        }
        List<CodePointUnit> units = new ArrayList<>();
        for (int index = 0; index < value.length();) {
            int codePoint = value.codePointAt(index);
            units.add(new CodePointUnit(new String(Character.toChars(codePoint)), rawScript(codePoint)));
            index += Character.charCount(codePoint);
        }
        resolveNeutralScripts(units);
        List<Run> result = new ArrayList<>();
        StringBuilder text = new StringBuilder();
        Script current = null;
        for (CodePointUnit unit : units) {
            if (current != null && unit.script() != current) {
                result.add(new Run(text.toString(), current));
                text.setLength(0);
            }
            current = unit.script();
            text.append(unit.text());
        }
        if (!text.isEmpty()) {
            result.add(new Run(text.toString(), current == null ? Script.ENGLISH : current));
        }
        return List.copyOf(result);
    }

    private static void resolveNeutralScripts(List<CodePointUnit> units) {
        Script previous = null;
        for (int i = 0; i < units.size(); i++) {
            CodePointUnit unit = units.get(i);
            if (unit.script() != Script.NEUTRAL) {
                previous = unit.script();
                continue;
            }
            Script next = nextScript(units, i + 1);
            Script resolved = previous != null ? previous : (next != null ? next : Script.ENGLISH);
            units.set(i, new CodePointUnit(unit.text(), resolved));
        }
    }

    private static Script nextScript(List<CodePointUnit> units, int start) {
        for (int i = start; i < units.size(); i++) {
            if (units.get(i).script() != Script.NEUTRAL) {
                return units.get(i).script();
            }
        }
        return null;
    }

    private static Script rawScript(int codePoint) {
        Character.UnicodeScript unicodeScript = Character.UnicodeScript.of(codePoint);
        if (unicodeScript == Character.UnicodeScript.HAN) {
            return Script.CHINESE;
        }
        if (unicodeScript == Character.UnicodeScript.LATIN || Character.isDigit(codePoint)) {
            return Script.ENGLISH;
        }
        Character.UnicodeBlock block = Character.UnicodeBlock.of(codePoint);
        if (block == Character.UnicodeBlock.CJK_SYMBOLS_AND_PUNCTUATION
                || block == Character.UnicodeBlock.HALFWIDTH_AND_FULLWIDTH_FORMS) {
            return Script.CHINESE;
        }
        return Script.NEUTRAL;
    }

    private static Script dominantScript(List<Run> runs) {
        for (Run run : runs) {
            if (run.script() == Script.CHINESE) {
                return Script.CHINESE;
            }
        }
        return Script.ENGLISH;
    }

    private static boolean containsBothScripts(List<Run> runs) {
        boolean chinese = false;
        boolean english = false;
        for (Run run : runs) {
            chinese |= run.script() == Script.CHINESE;
            english |= run.script() == Script.ENGLISH;
        }
        return chinese && english;
    }

    private static String family(ResolvedSettings settings, Script script) {
        return script == Script.CHINESE ? settings.chineseFamily() : settings.englishFamily();
    }

    private static void applyFamilyStyle(Node node, String family) {
        String style = withoutFamilyDeclaration(node.getStyle());
        if (family != null && !family.isBlank()) {
            style += "-fx-font-family: \"" + escapeCssString(family) + "\";";
        }
        node.setStyle(style);
    }

    private static void clearFamilyStyle(Node node) {
        node.setStyle(withoutFamilyDeclaration(node.getStyle()));
    }

    private static String withoutFamilyDeclaration(String existing) {
        StringBuilder result = new StringBuilder();
        if (existing == null || existing.isBlank()) {
            return "";
        }
        for (String declaration : existing.split(";")) {
            String trimmed = declaration.trim();
            if (trimmed.isBlank() || trimmed.startsWith("-fx-font-family:")) {
                continue;
            }
            result.append(trimmed).append(';');
        }
        return result.toString();
    }

    private static String escapeCssString(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static ResolvedSettings settings(Node node) {
        Object value = node.getProperties().get(SETTINGS_KEY);
        return value instanceof ResolvedSettings settings ? settings : null;
    }

    private enum Script {
        CHINESE,
        ENGLISH,
        NEUTRAL
    }

    private record ResolvedSettings(String chineseFamily, String englishFamily) {
    }

    private record Run(String text, Script script) {
    }

    private record CodePointUnit(String text, Script script) {
    }
}
