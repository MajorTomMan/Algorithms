package com.majortom.algorithms.visualization.impl.visualizer.indexed;

import java.util.function.IntConsumer;

/** Minimal FX contract shared by indexed strip cells such as Array and String slots. */
public interface IndexedStripCell {
  void setIndex(int index);

  void setLayoutSize(double width, double height);

  void setSelectionHandler(IntConsumer handler);

  void setSelected(boolean selected);
}
