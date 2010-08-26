package org.apache.poi.hssf.contrib.view.brush;

import java.awt.*;

/**
   * This Stroke implementation applies a BasicStroke to a shape twice. If you
 * draw with this Stroke, then instead of outlining the shape, you're
 * outlining the outline of the shape.
 */
public class DoubleStroke implements Brush {
  BasicStroke stroke1, stroke2; // the two strokes to use

  public DoubleStroke(float width1, float width2) {
    stroke1 = new BasicStroke(width1); // Constructor arguments specify
    stroke2 = new BasicStroke(width2); // the line widths for the strokes
  }

  public Shape createStrokedShape(Shape s) {
    // Use the first stroke to create an outline of the shape
    Shape outline = stroke1.createStrokedShape(s);
    // Use the second stroke to create an outline of that outline.
    // It is this outline of the outline that will be filled in
    return stroke2.createStrokedShape(outline);
  }

  public float getLineWidth() {
    return stroke1.getLineWidth() + 2 * stroke2.getLineWidth();
  }
}