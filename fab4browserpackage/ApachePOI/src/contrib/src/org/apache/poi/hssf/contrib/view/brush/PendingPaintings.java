package org.apache.poi.hssf.contrib.view.brush;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.*;
import java.util.ArrayList;
import java.util.List;

public class PendingPaintings {
  public static final String PENDING_PAINTINGS =
      PendingPaintings.class.getSimpleName();

  private final List<Painting> paintings;

  public static class Painting {
    final Stroke stroke;
    final Color color;
    final Shape shape;
    final AffineTransform transform;

    public Painting(Stroke stroke, Color color, Shape shape,
        AffineTransform transform) {

      this.color = color;
      this.shape = shape;
      this.stroke = stroke;
      this.transform = transform;
    }

    public void draw(Graphics2D g) {
      g.setTransform(transform);
      g.setStroke(stroke);
      g.setColor(color);
      g.draw(shape);
    }
  }

  public PendingPaintings(JComponent parent) {
    paintings = new ArrayList<Painting>();
    parent.putClientProperty(PENDING_PAINTINGS, this);
  }

  public void clear() {
    paintings.clear();
  }

  public void paint(Graphics2D g) {
    g.setBackground(Color.CYAN);
    AffineTransform origTransform = g.getTransform();
    for (Painting c : paintings) {
      c.draw(g);
    }
    g.setTransform(origTransform);

    clear();
  }

  public static void add(JComponent c, Graphics2D g, Stroke stroke, Color color,
      Shape shape) {

    add(c, new Painting(stroke, color, shape, g.getTransform()));
  }

  public static void add(JComponent c, Painting newPainting) {
    PendingPaintings pending = pendingDrawsFor(c);
    if (pending != null) {
      pending.paintings.add(newPainting);
    }
  }

  static PendingPaintings pendingDrawsFor(JComponent c) {
    for (Component parent = c; parent != null; parent = parent.getParent()) {
      if (parent instanceof JComponent) {
        JComponent jc = (JComponent) parent;
        Object pd = jc.getClientProperty(PENDING_PAINTINGS);
        if (pd != null)
          return (PendingPaintings) pd;
      }
    }
    return null;
  }
}