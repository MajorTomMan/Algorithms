package com.majortom.algorithms.visualization.logging;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import javafx.collections.FXCollections;
import javafx.geometry.Pos;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

/** Virtualized view over one independently retained log channel. */
public final class LogView extends ListView<LogEntry> {
  private static final DateTimeFormatter TIME_FORMAT =
      DateTimeFormatter.ofPattern("HH:mm:ss").withZone(ZoneId.systemDefault());

  public LogView() {
    getStyleClass().add("log-view");
    setCellFactory(ignored -> new LogCell());
    setItems(FXCollections.observableArrayList());
  }

  /** Switches the visible list without copying or merging another channel's history. */
  public void showChannel(LogChannel channel) {
    if (channel == null) {
      setItems(FXCollections.observableArrayList());
      return;
    }
    setItems(channel.entries());
    if (!getItems().isEmpty()) scrollTo(getItems().size() - 1);
  }

  private static final class LogCell extends ListCell<LogEntry> {
    @Override
    protected void updateItem(LogEntry line, boolean empty) {
      super.updateItem(line, empty);
      if (empty || line == null) {
        setGraphic(null);
        setText(null);
        return;
      }

      Region bullet = new Region();
      bullet.getStyleClass().addAll(
          "log-bullet", "log-bullet-" + line.level().name().toLowerCase());

      Text time = new Text(TIME_FORMAT.format(line.timestamp()) + "  ");
      time.getStyleClass().add("log-time");
      String tag = line.tag().isBlank() ? "" : line.tag() + ": ";
      Text message = new Text(tag + line.message());
      message.getStyleClass().add("log-message");
      TextFlow flow = new TextFlow(time, message);
      flow.getStyleClass().add("log-line");

      HBox row = new HBox(6, bullet, flow);
      row.setAlignment(Pos.TOP_LEFT);
      row.getStyleClass().add("log-row");
      setGraphic(row);
      setAlignment(Pos.TOP_LEFT);
    }
  }
}
