/* ====================================================================
   Licensed to the Apache Software Foundation (ASF) under one or more
   contributor license agreements.  See the NOTICE file distributed with
   this work for additional information regarding copyright ownership.
   The ASF licenses this file to You under the Apache License, Version 2.0
   (the "License"); you may not use this file except in compliance with
   the License.  You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
==================================================================== */

package org.apache.poi.hssf.contrib.view;

import org.apache.poi.hssf.usermodel.HSSFCellStyle;
import org.apache.poi.hssf.contrib.view.brush.Brush;
import org.apache.poi.hssf.contrib.view.brush.BasicBrush;
import org.apache.poi.hssf.contrib.view.brush.DoubleStroke;
import org.apache.poi.hssf.contrib.view.brush.PendingPaintings;

import static javax.swing.SwingConstants.*;
import javax.swing.border.AbstractBorder;
import java.awt.*;
import java.awt.geom.Line2D;
import java.util.HashMap;

/**
 * This is an attempt to implement Excel style borders for the SheetViewer.
 * Mostly just overrides stuff so the javadoc won't appear here but will appear
 * in the generated stuff.
 *
 * @author Andrew C. Oliver (acoliver at apache dot org)
 * @author Jason Height
 */
public class SVBorder extends AbstractBorder {
  private static final Color NO_BORDER_COLOR = Color.LIGHT_GRAY;
  private static final Brush NO_BORDER_STROKE = new BasicBrush(1.0f);

  private static final HashMap<Short, Brush> BRUSHES;

  public SVBorder() {
  }

  static {
    float hairWidth = 0.5f;
    float thinWidth = 1;
    float mediumWidth = 2;
    float thickWidth = 3;

    BRUSHES = new HashMap<Short, Brush>();
    BRUSHES.put((short) -1, new BasicBrush(0.0f));
    BRUSHES.put(HSSFCellStyle.BORDER_DASH_DOT, buildDashDot(thinWidth));
    BRUSHES.put(HSSFCellStyle.BORDER_DASH_DOT_DOT, buildDashDotDot(thinWidth));
    BRUSHES.put(HSSFCellStyle.BORDER_DASHED, buildDashed(thinWidth));
    BRUSHES.put(HSSFCellStyle.BORDER_DOTTED, buildDotted(thinWidth));
    //!! Do this right (CompoundBorder?)
    BRUSHES.put(HSSFCellStyle.BORDER_DOUBLE, new DoubleStroke(thickWidth, thinWidth));
    BRUSHES.put(HSSFCellStyle.BORDER_HAIR, buildSolid(hairWidth));
    BRUSHES.put(HSSFCellStyle.BORDER_MEDIUM, buildSolid(hairWidth));
    BRUSHES.put(HSSFCellStyle.BORDER_MEDIUM_DASH_DOT, buildDashDot(
        mediumWidth));
    BRUSHES.put(HSSFCellStyle.BORDER_MEDIUM_DASH_DOT_DOT, buildDashDotDot(
        mediumWidth));
    BRUSHES.put(HSSFCellStyle.BORDER_MEDIUM_DASHED, buildDashed(mediumWidth));
    BRUSHES.put(HSSFCellStyle.BORDER_NONE, NO_BORDER_STROKE);
    //!! Do this right
    BRUSHES.put(HSSFCellStyle.BORDER_SLANTED_DASH_DOT, buildSolid(mediumWidth));
    BRUSHES.put(HSSFCellStyle.BORDER_THICK, buildSolid(thickWidth));
    BRUSHES.put(HSSFCellStyle.BORDER_THIN, buildSolid(thinWidth));
  }

  private static Brush buildDashDot(float width) {
    return new BasicBrush(width, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER,
        new float[]{5, 3, 2, 3, 2, 3}, 0);
  }

  
  private static Brush buildDashDotDot(float width) {
    return new BasicBrush(width, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER,
        new float[]{5, 3, 2, 3}, 0);
  }

  private static Brush buildDashed(float width) {
    return new BasicBrush(width, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER,
        new float[]{2, 2}, 0);
  }

  private static Brush buildDotted(float width) {
    return new BasicBrush(width, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER,
        new float[]{1, 1}, 0);
  }

  private static Brush buildSolid(float width) {
    return new BasicBrush(width, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER);
  }

  private static Color colorFor(Brush stroke, Color color) {
    if (stroke == NO_BORDER_STROKE)
      return NO_BORDER_COLOR;
    else
      return color;
  }

  public void paintBorder(Component c, Graphics g1, int x, int y, int width,
      int height) {

    Graphics2D g = (Graphics2D) g1;
    if (c instanceof SVTableCellRenderer) {
      SVTableCellRenderer cell = (SVTableCellRenderer) c;

//    if (c.cell != null && c.cell.getColumnIndex() == 1) {
//      if (c.cell.getRowIndex() == 1)
//        System.out.printf("%c%d: SOUTH = %d%n", (char) (c.cell.getColumnIndex() + 'A'), c.cell.getRowIndex() + 1, c.borderStyle(SOUTH));
//      if (c.cell.getRowIndex() == 2)
//        System.out.printf("%c%d: NORTH = %d%n", (char) (c.cell.getColumnIndex() + 'A'), c.cell.getRowIndex() + 1, c.borderStyle(NORTH));
//    }
      float x2 = x + width;
      float y2 = y + height;
      stroke(cell, g, NORTH, x, y, x2, y);
      stroke(cell, g, EAST, x2, y, x2, y2);
      stroke(cell, g, SOUTH, x, y2, x2, y2);
      stroke(cell, g, WEST, x, y, x, y2);
    }
  }

  private static void stroke(SVTableCellRenderer c, Graphics2D g, int side, float x1, float y1, float x2, float y2) {
    Color color;
    Brush brush;

    brush = brushForSide(c, side);
    boolean skip = c.borderStyle(side) == -1;
//    if (c.cell != null && c.cell.getColumnIndex() == 1) {
//      if (c.cell.getRowIndex() == 1 && side == SOUTH)
//        System.out.printf("%c%d: lineWidth = %g (%d): skip = %b%n", (char) (c.cell.getColumnIndex() + 'A'), c.cell.getRowIndex() + 1,
//            brush.getLineWidth(), c.borderStyle(side), skip);
//      if (c.cell.getRowIndex() == 2 && side == NORTH)
//        System.out.printf("%c%d: lineWidth = %g (%d): skip = %b%n", (char) (c.cell.getColumnIndex() + 'A'), c.cell.getRowIndex() + 1,
//            brush.getLineWidth(), c.borderStyle(side), skip);
//    }
    if (skip)
      return;
    if (brush.getLineWidth() > 0) {
      color = colorFor(brush, c.borderColor(side));
      Line2D.Float line = new Line2D.Float(x1, y1, x2, y2);
      PendingPaintings.add(c, g, brush, color, line);
    }
  }

  private static Brush brushForSide(SVTableCellRenderer c, int side) {
    return BRUSHES.get(c.borderStyle(side));
  }

  @Override
  public Insets getBorderInsets(Component c) {
    return getBorderInsets(c, new Insets(0, 0, 0, 0));
  }

  @Override
  public Insets getBorderInsets(Component raw, Insets insets) {
    SVTableCellRenderer c = (SVTableCellRenderer) raw;

    insets.top = Math.round(brushForSide(c, NORTH).getLineWidth());
    insets.right = Math.round(brushForSide(c, EAST).getLineWidth() + 2);
    insets.bottom = Math.round(brushForSide(c, SOUTH).getLineWidth());
    insets.left = Math.round(brushForSide(c, WEST).getLineWidth() + 2);

    insets.top++;
    insets.right++;
    insets.bottom++;
    insets.left++;

//    if (c.cell != null && c.cell.getColumnIndex() == 1) {
//      if (c.cell.getRowIndex() == 1)
//        System.out.printf("%c%d: insets = %s%n", (char) (c.cell.getColumnIndex() + 'A'), c.cell.getRowIndex() + 1,
//            insets);
//      if (c.cell.getRowIndex() == 2)
//        System.out.printf("%c%d: insets = %s%n", (char) (c.cell.getColumnIndex() + 'A'), c.cell.getRowIndex() + 1,
//            insets);
//    }
    return insets;
  }
}
