
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

import javax.swing.*;
import javax.swing.table.TableCellRenderer;
import javax.swing.border.*;

import java.awt.*;

import java.io.Serializable;

import org.apache.poi.hssf.usermodel.*;
import org.apache.poi.ss.format.CellFormat;
import org.apache.poi.ss.usermodel.Row;

/**
 * Sheet Viewer Table Cell Render -- not commented via javadoc as it
 * nearly completely consists of overridden methods.
 *
 * @author Andrew C. Oliver
 */
public class SVTableCellRenderer extends JLabel
    implements TableCellRenderer, Serializable
{
    protected static Border noFocusBorder = new EmptyBorder(1, 1, 1, 1);

    private static Border cellBorder = new SVBorder();

    private boolean hasFocus;
    private HSSFCell cell;
    private HSSFWorkbook wb = null;

    public SVTableCellRenderer(HSSFWorkbook wb) {
	super();
	setOpaque(true);
        setBorder(noFocusBorder);
        this.wb = wb;
    }

    public boolean hasFocus() {
      return hasFocus;
    }

    public Color borderColor(int which) {
      HSSFCellStyle s = getStyle();
      if (s == null)
        return null;
      Color defColor = SVTableUtils.black;
      switch (which) {
      case NORTH:
        return SVTableUtils.getAWTColor(s.getTopBorderColor(), defColor);
      case EAST:
        return SVTableUtils.getAWTColor(s.getRightBorderColor(), defColor);
      case SOUTH:
        return SVTableUtils.getAWTColor(s.getBottomBorderColor(), defColor);
      case WEST:
        return SVTableUtils.getAWTColor(s.getLeftBorderColor(), defColor);
      default:
        throw new IllegalArgumentException("Unknown side: " + which);
      }
    }

    public short borderStyle(int which) {
      HSSFCellStyle s = getStyle();
      short border = HSSFCellStyle.BORDER_NONE;
      if (s != null) {
        switch (which) {
        case NORTH:
          border = s.getBorderTop();
          break;
        case EAST:
          border = s.getBorderRight();
          break;
        case SOUTH:
          border = s.getBorderBottom();
          break;
        case WEST:
          border = s.getBorderLeft();
          break;
        }
      }
      if (border != HSSFCellStyle.BORDER_NONE)
        return border;

      switch (which) {
      case NORTH:
        return -1;
      case EAST:
        if (cell == null || hasBorderAt(cell.getRowIndex(),
            cell.getColumnIndex() + 1, WEST))
          return -1;
        else
          return HSSFCellStyle.BORDER_NONE;
      case SOUTH:
        if (cell == null || hasBorderAt(cell.getRowIndex() + 1,
            cell.getColumnIndex(), NORTH))
          return -1;
        else
          return HSSFCellStyle.BORDER_NONE;
      case WEST:
        return -1;
      default:
        throw new IllegalArgumentException("Unknown side: " + which);
      }
    }

    private boolean hasBorderAt(int rowIndex, int colIndex, int side) {
      HSSFSheet sheet = cell.getSheet();
      if (rowIndex >= sheet.getLastRowNum())
        return false;
      HSSFRow row = sheet.getRow(rowIndex);
      HSSFCell cell = row.getCell(colIndex, Row.RETURN_NULL_AND_BLANK);
      if (cell == null)
        return false;
      HSSFCellStyle style = cell.getCellStyle();
      if (style == null)
        return false;
      short border = HSSFCellStyle.BORDER_NONE;
      switch (side) {
      case NORTH:
        border = style.getBorderTop();
        break;
      case EAST:
        border = style.getBorderRight();
        break;
      case SOUTH:
        border = style.getBorderBottom();
        break;
      case WEST:
        border = style.getBorderLeft();
        break;
      }
      return border != HSSFCellStyle.BORDER_NONE;
    }

    private HSSFCellStyle getStyle() {
      return cell == null ? null : cell.getCellStyle();
    }

    public Component getTableCellRendererComponent(JTable table, Object value,
                          boolean isSelected, boolean hasFocus, int row, int column) {

        //If the JTables default cell renderer has been setup correctly the
        //value will be the HSSFCell that we are trying to render
        HSSFCell c = (HSSFCell)value;
        cell = c;

        if (c != null) {
          HSSFCellStyle s = c.getCellStyle();
          HSSFFont f = wb.getFontAt(s.getFontIndex());
          setFont(SVTableUtils.makeFont(f));

          if (s.getFillPattern() == HSSFCellStyle.SOLID_FOREGROUND) {
            setBackground(SVTableUtils.getAWTColor(s.getFillForegroundColor(), SVTableUtils.white));
          } else setBackground(SVTableUtils.white);

          setForeground(SVTableUtils.getAWTColor(f.getColor(), SVTableUtils.black));

          this.hasFocus = hasFocus;
          setBorder(cellBorder);

          //Set the value that is rendered for the cell
          //also applies the format
          CellFormat cf = CellFormat.getInstance(s.getDataFormatString());
          cf.apply(this, c);

          short verticalAlign = s.getVerticalAlignment();
          if (getStyle().getWrapText()) {
            setText("<html>" + getText() + "</html>");
            // This is a workaround: JLabel doesn't know the preferred height
            // of HTML text, so it can't get the vertical right -- this at
            // least makes it always visible.
            //!! Do this right (Use JTextPanel?)
            verticalAlign = HSSFCellStyle.VERTICAL_TOP;
          }

          //Set the text alignment of the cell
          switch (s.getAlignment()) {
            case HSSFCellStyle.ALIGN_LEFT:
            case HSSFCellStyle.ALIGN_JUSTIFY:
            case HSSFCellStyle.ALIGN_FILL:
              setHorizontalAlignment(SwingConstants.LEFT);
              break;
            case HSSFCellStyle.ALIGN_CENTER:
            case HSSFCellStyle.ALIGN_CENTER_SELECTION:
              setHorizontalAlignment(SwingConstants.CENTER);
              break;
            case HSSFCellStyle.ALIGN_RIGHT:
              setHorizontalAlignment(SwingConstants.RIGHT);
              break;
            case HSSFCellStyle.ALIGN_GENERAL:
            default:
              switch (ultimateCellType(c)) {
                case HSSFCell.CELL_TYPE_BOOLEAN:
                case HSSFCell.CELL_TYPE_ERROR:
                  setHorizontalAlignment(SwingConstants.CENTER);
                  break;
                case HSSFCell.CELL_TYPE_NUMERIC:
                  setHorizontalAlignment(SwingConstants.RIGHT);
                  break;
                case HSSFCell.CELL_TYPE_STRING:
                  setHorizontalAlignment(SwingConstants.LEFT);
                  break;
                default:
                  setHorizontalAlignment(SwingConstants.RIGHT);
                  break;
              }
              break;
          }
          switch (verticalAlign) {
            case HSSFCellStyle.VERTICAL_BOTTOM:
              setVerticalAlignment(SwingConstants.BOTTOM);
              break;
            case HSSFCellStyle.VERTICAL_CENTER:
            case HSSFCellStyle.VERTICAL_JUSTIFY:
              setVerticalAlignment(SwingConstants.CENTER);
              break;
            case HSSFCellStyle.VERTICAL_TOP:
              setVerticalAlignment(SwingConstants.TOP);
              break;
            default:
              setVerticalAlignment(SwingConstants.BOTTOM);
              break;
            }
        } else {
          setText("");
          setBackground(SVTableUtils.white);
        }


	if (hasFocus) {
          setForeground(Color.BLACK);
          setBackground(Color.YELLOW);
	}

	// ---- begin optimization to avoid painting background ----
	Color back = getBackground();
	boolean colorMatch = (back != null) && ( back.equals(table.getBackground()) ) && table.isOpaque();
        setOpaque(!colorMatch);
	// ---- end optimization to avoid painting background ----
	return this;
    }

  private static int ultimateCellType(HSSFCell c) {
    int type = c.getCellType();
    if (type == HSSFCell.CELL_TYPE_FORMULA)
      type = c.getCachedFormulaResultType();
    return type;
  }

  public void validate() {}

    public void revalidate() {}

    public void repaint(long tm, int x, int y, int width, int height) {}

    public void repaint(Rectangle r) { }

    protected void firePropertyChange(String propertyName, Object oldValue, Object newValue) {
	// Strings get interned...
	if (propertyName=="text") {
	    super.firePropertyChange(propertyName, oldValue, newValue);
	}
    }

    public void firePropertyChange(String propertyName, boolean oldValue, boolean newValue) { }
}
