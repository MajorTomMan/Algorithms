package com.majortom.algorithms.visualization.impl.visualizer.string;

import com.majortom.algorithms.visualization.render.api.DecorationGeometry;
import com.majortom.algorithms.visualization.render.api.DecorationLayoutResult;
import com.majortom.algorithms.visualization.render.api.DecorationSize;
import com.majortom.algorithms.visualization.render.api.ElementGeometry;
import java.util.ArrayList;
import java.util.List;

/** JavaFX-neutral placement of algorithm-only String/KMP decorations. */
public final class StringDecorationLayout {

  public DecorationLayoutResult layout(Input input) {
    if (input.first() == null) {
      return DecorationLayoutResult.empty();
    }

    double patternX = input.aligned() == null ? input.first().x() : input.aligned().x();
    double patternY = input.first().y() + input.first().height() + 24.0d;
    List<DecorationGeometry> result = new ArrayList<>();
    result.add(new DecorationGeometry(
        StringDecorationIds.PATTERN,
        patternX,
        patternY,
        input.patternSize().width(),
        input.patternSize().height()));
    result.add(new DecorationGeometry(
        StringDecorationIds.PATTERN_CAPTION,
        input.first().x() + 32.0d,
        patternY - 14.0d,
        input.captionSize().width(),
        input.captionSize().height()));

    if (input.observationVisible()) {
      result.add(new DecorationGeometry(
          StringDecorationIds.OBSERVATION,
          input.first().x(),
          patternY + input.patternSize().height() + 10.0d,
          input.observationSize().width(),
          input.observationSize().height()));
    }
    return DecorationLayoutResult.ofElements(result);
  }

  public record Input(
      ElementGeometry first,
      ElementGeometry aligned,
      DecorationSize patternSize,
      DecorationSize captionSize,
      DecorationSize observationSize,
      boolean observationVisible) {}
}
