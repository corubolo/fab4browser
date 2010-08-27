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

import org.apache.poi.hssf.usermodel.HSSFSheet;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import java.awt.*;

/**
 * This class presents the row header to the table.
 *
 * @author Jason Height
 */
public class SVRowHeader extends JTable {
  private JTable table;

  /**
   * This model simply returns an integer number up to the number of rows that
   * are present in the sheet.
   */
  private static class SVRowHeaderModel extends AbstractTableModel {
    private HSSFSheet sheet;

    public SVRowHeaderModel(HSSFSheet sheet) {
      this.sheet = sheet;
    }

    public int getRowCount() {
      return sheet.getLastRowNum();
    }

    public int getColumnCount() {
      return 1;
    }

    public Object getValueAt(int rowIndex, int columnIndex) {
      return rowIndex + 1;
    }
  }

  @Override
  public Rectangle getCellRect(int row, int column, boolean includeSpacing) {
    Rectangle r = super.getCellRect(row, column, includeSpacing);
    int height = table.getRowHeight(row);
    if (getRowHeight(row) != height)
      setRowHeight(row, height);
    r.height = height;
    return r;
  }

  /** Renders the row number */
  private static class RowHeaderRenderer
      implements TableCellRenderer {

    private final JTable table;

    RowHeaderRenderer(JTable table) {
      this.table = table;
    }

    public Component getTableCellRendererComponent(JTable headerTable,
        Object value, boolean isSelected, boolean hasFocus, int row,
        int column) {

      TableCellRenderer r = table.getTableHeader().getDefaultRenderer();

      JLabel c = (JLabel) r.getTableCellRendererComponent(headerTable, value,
          isSelected, hasFocus, row, 0);
      c.setHorizontalAlignment(SwingConstants.RIGHT);
      c.setVerticalAlignment(SwingConstants.TOP);
      return c;
    }
  }

  public SVRowHeader(HSSFSheet sheet, JTable table) {
    super(new SVRowHeaderModel(sheet));
    Dimension d = getPreferredScrollableViewportSize();
    this.table = table;
    d.width = getPreferredSize().width;
    setPreferredScrollableViewportSize(d);
    setDefaultRenderer(Object.class, new RowHeaderRenderer(table));
  }
}
