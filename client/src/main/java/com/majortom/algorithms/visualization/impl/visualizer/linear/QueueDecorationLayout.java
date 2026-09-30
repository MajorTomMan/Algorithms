package com.majortom.algorithms.visualization.impl.visualizer.linear;

import com.majortom.algorithms.visualization.render.api.DecorationGeometry;
import com.majortom.algorithms.visualization.render.api.DecorationLayoutResult;
import com.majortom.algorithms.visualization.render.api.DecorationSize;
import com.majortom.algorithms.visualization.render.api.ElementGeometry;
import java.util.ArrayList;
import java.util.List;

/** JavaFX-neutral placement of queue role and flow decorations around resolved item geometry. */
public final class QueueDecorationLayout {

  public DecorationLayoutResult layout(Input input) {
    if (input.front() == null || input.rear() == null || input.itemCount() <= 0) {
      return DecorationLayoutResult.ofElements(List.of(
          geometry(QueueDecorationIds.FRONT, 40.0d, 30.0d, input.frontSize()),
          geometry(QueueDecorationIds.REAR, 130.0d, 30.0d, input.rearSize()),
          geometry(QueueDecorationIds.DEQUEUE, 40.0d, 86.0d, input.dequeueSize()),
          geometry(QueueDecorationIds.ENQUEUE, 130.0d, 86.0d, input.enqueueSize())));
    }

    ElementGeometry front = input.front();
    ElementGeometry rear = input.rear();
    double top = Math.min(front.y(), rear.y());
    double bottom = Math.max(front.y() + front.height(), rear.y() + rear.height());
    List<DecorationGeometry> result = new ArrayList<>();

    if (input.itemCount() == 1) {
      result.add(centered(QueueDecorationIds.FRONT,
          front.x() + front.width() / 2.0d,
          Math.max(2.0d, top - 28.0d),
          input.frontRearSize()));
      result.add(geometry(QueueDecorationIds.DEQUEUE,
          front.x(), bottom + 14.0d, input.dequeueSize()));
      result.add(geometry(QueueDecorationIds.ENQUEUE,
          Math.max(front.x(), front.x() + front.width() - input.enqueueSize().width()),
          bottom + 32.0d, input.enqueueSize()));
      return DecorationLayoutResult.ofElements(result);
    }

    result.add(centered(QueueDecorationIds.FRONT,
        front.x() + front.width() / 2.0d,
        Math.max(2.0d, top - 28.0d),
        input.frontSize()));
    result.add(centered(QueueDecorationIds.REAR,
        rear.x() + rear.width() / 2.0d,
        Math.max(2.0d, top - 28.0d),
        input.rearSize()));
    result.add(geometry(QueueDecorationIds.DEQUEUE,
        front.x(), bottom + 14.0d, input.dequeueSize()));
    result.add(geometry(QueueDecorationIds.ENQUEUE,
        Math.max(rear.x(), rear.x() + rear.width() - input.enqueueSize().width()),
        bottom + 14.0d, input.enqueueSize()));
    return DecorationLayoutResult.ofElements(result);
  }

  private static DecorationGeometry centered(
      String id, double centerX, double y, DecorationSize size) {
    return geometry(id, centerX - size.width() / 2.0d, y, size);
  }

  private static DecorationGeometry geometry(
      String id, double x, double y, DecorationSize size) {
    return new DecorationGeometry(id, x, y, size.width(), size.height());
  }

  public record Input(
      ElementGeometry front,
      ElementGeometry rear,
      int itemCount,
      DecorationSize frontSize,
      DecorationSize rearSize,
      DecorationSize frontRearSize,
      DecorationSize dequeueSize,
      DecorationSize enqueueSize) {}
}
