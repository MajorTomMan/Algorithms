package com.majortom.algorithms.visualization.logging;

import com.majortom.algorithms.core.logging.LogEvent;
import com.majortom.algorithms.core.logging.LogLevel;
import javafx.geometry.Pos;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/** Virtualized log list with level/tag prefix styling. */
public final class LogView extends ListView<LogView.Line> {

    private static final DateTimeFormatter TIME_FORMAT =
            DateTimeFormatter.ofPattern("HH:mm:ss").withZone(ZoneId.systemDefault());

    public LogView() {
        getStyleClass().add("log-view");
        setCellFactory(ignored -> new LogCell());
    }

    public void append(LogEvent event, Instant timestamp) {
        append(timestamp, event.level(), event.tag(), event.message());
    }

    public void append(LogLevel level, String tag, String message) {
        append(Instant.now(), level, tag, message);
    }

    public void appendSystem(String message) {
        append(LogLevel.INFO, "SYSTEM", message);
    }

    private void append(Instant timestamp, LogLevel level, String tag, String message) {
        String normalizedTag = "";
        if (tag != null) {
            normalizedTag = tag;
        }
        getItems().add(new Line(TIME_FORMAT.format(timestamp), level, normalizedTag, message));
        scrollTo(getItems().size() - 1);
    }

    public record Line(String time, LogLevel level, String tag, String message) {
    }

    private static final class LogCell extends ListCell<Line> {

        @Override
        protected void updateItem(Line line, boolean empty) {
            super.updateItem(line, empty);
            if (empty || line == null) {
                setGraphic(null);
                setText(null);
                return;
            }

            String tag = line.tag().isBlank() ? "" : "/" + line.tag();
            Text prefix = new Text("[" + line.time() + "][" + line.level().name() + tag + "] ");
            prefix.getStyleClass().addAll("log-prefix", "log-prefix-" + line.level().name().toLowerCase());
            Text message = new Text(line.message());
            message.getStyleClass().add("log-message");
            TextFlow flow = new TextFlow(prefix, message);
            flow.getStyleClass().add("log-line");
            setGraphic(flow);
            setAlignment(Pos.TOP_LEFT);
        }
    }
}
