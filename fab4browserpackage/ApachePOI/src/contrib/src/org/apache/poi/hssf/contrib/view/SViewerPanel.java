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

import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;

import javax.swing.*;
import javax.swing.table.*;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.*;
import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

/**
 * This class presents the sheets to the user.
 *
 *
 * @author Andrew C. Oliver
 * @author Jason Height
 */
public class SViewerPanel extends JPanel {
  /** Reference to the wookbook that is being displayed*/
  /* package */ HSSFWorkbook wb;
  /** Reference to the tabs component*/
  /* package */ JTabbedPane sheetPane;
  /** Reference to the cell renderer that is used to render all cells*/
  private SVTableCellRenderer cellRenderer;
  /** Reference to the cell editor that is used to edit all cells.
   *  Only constructed if editing is allowed
   */
  private SVTableCellEditor cellEditor;
  /** Flag indicating if editing is allowed. Otherwise the viewer is in
   *  view only mode.
   */
  private boolean allowEdits;

  private JTextComponent formulaDisplay;
  private SVSheetTable sheetTable;

  /**Construct the representation of the workbook*/
  public SViewerPanel(HSSFWorkbook wb, boolean allowEdits) {
    this.wb = wb;
    this.allowEdits = allowEdits;

    initialiseGui();
  }

    public SViewerPanel(URL url, boolean allowEdits) throws IOException {
        this(getXLSFromURL(url), allowEdits);
    }

    public SViewerPanel(InputStream in, boolean allowEdits) throws IOException {
        this(constructWorkbook(in), allowEdits);
    }

    public SViewerPanel(String path, boolean allowEdits) throws IOException {
        this(constructWorkbook(new FileInputStream(path)), allowEdits);
    }

    private static HSSFWorkbook constructWorkbook(InputStream in)
            throws IOException {

        try {
            return new HSSFWorkbook(in);
        } finally {
            in.close();
        }
    }

    /** opens a url and returns an inputstream */
    private static InputStream getXLSFromURL(URL url) throws IOException {
        URLConnection uc = url.openConnection();
        String field = uc.getHeaderField(0);
        for (int i = 0; field != null; i++) {
            System.out.println(field);
            field = uc.getHeaderField(i);
        }
        return new BufferedInputStream(uc.getInputStream());
    }

  private void initialiseGui() {
    cellRenderer = new SVTableCellRenderer(this.wb);
    if (allowEdits)
      cellEditor = new SVTableCellEditor(this.wb);

    //Initialise the Panel
    sheetPane = new JTabbedPane(JTabbedPane.BOTTOM);

    if (allowEdits)
      sheetPane.addMouseListener(createTabListener());
    int sheetCount = wb.getNumberOfSheets();
    for (int i=0; i<sheetCount;i++) {
      String sheetName = wb.getSheetName(i);
      //Add the new sheet to the tabbed pane
      sheetPane.addTab(sheetName, makeSheetView(wb.getSheetAt(i)));
    }
    setLayout(new BorderLayout());
    add(sheetPane, BorderLayout.CENTER);
  }

  protected JComponent makeSheetView(HSSFSheet sheet) {
    sheetTable = new SVSheetTable(sheet);
    sheetTable.setFormulaDisplay(formulaDisplay);
    sheetTable.setDefaultRenderer(HSSFCell.class, cellRenderer);
    if (allowEdits)
      sheetTable.setDefaultEditor(HSSFCell.class, cellEditor);

    JTableHeader header = sheetTable.getTableHeader();
    //Don't allow column reordering
    header.setReorderingAllowed(false);
    //Only allow column resizing if editing is allowed
    header.setResizingAllowed(allowEdits);

    JScrollPane scroll = new JScrollPane(sheetTable);
    return scroll;
  }

//  public void paint(Graphics g) {
//    //JMH I am only overriding this to get a picture of the time taken to paint
//    long start = System.currentTimeMillis();
//    super.paint(g);
//    long elapsed = System.currentTimeMillis()-start;
//    System.out.println("Paint time = "+elapsed);
//  }

  protected MouseListener createTabListener() {
    return new TabListener();
  }

  public void setFormulaDisplay(JTextField formulaDisplay) {
    this.formulaDisplay = formulaDisplay;
    if (sheetTable != null)
      sheetTable.setFormulaDisplay(formulaDisplay);
  }

  /** This class defines the default MouseListener that listens to
   *  mouse events in the tabbed pane
   *
   *  The default is to popup a menu when the event occurs over a tab
   */
  private class TabListener implements MouseListener {
    public JPopupMenu popup;
    public TabListener() {
      popup = new JPopupMenu("Sheet");
      popup.add(createInsertSheetAction());
      popup.add(createDeleteSheetAction());
      popup.add(createRenameSheetAction());
    }

    protected Action createInsertSheetAction() {
      return new InsertSheetAction();
    }

    protected Action createDeleteSheetAction() {
      return new DeleteSheetAction();
    }

    protected Action createRenameSheetAction() {
      return new RenameSheetAction();
    }


    /** This method will display the popup if the mouseevent is a popup event
     *  and the event occurred over a tab
     */
    protected void checkPopup(MouseEvent e) {
      if (e.isPopupTrigger()) {
        int tab = sheetPane.getUI().tabForCoordinate(sheetPane, e.getX(), e.getY());
        if (tab != -1) {
          popup.show(sheetPane, e.getX(), e.getY());
        }
      }
    }

    public void mouseClicked(MouseEvent e) {
      checkPopup(e);
    }

    public void mousePressed(MouseEvent e) {
      checkPopup(e);
    }

    public void mouseReleased(MouseEvent e) {
      checkPopup(e);
    }

    public void mouseEntered(MouseEvent e) {}
    public void mouseExited(MouseEvent e) {}
  }

  /** This class defines the action that is performed when the sheet is renamed*/
  private class RenameSheetAction extends AbstractAction {
    public RenameSheetAction() {
      super("Rename");
    }

    public void actionPerformed(ActionEvent e) {
      int tabIndex = sheetPane.getSelectedIndex();
      if (tabIndex != -1) {
        String newSheetName = JOptionPane.showInputDialog(sheetPane, "Enter a new Sheetname", "Rename Sheet", JOptionPane.QUESTION_MESSAGE);
        if (newSheetName != null) {
          wb.setSheetName(tabIndex, newSheetName);
          sheetPane.setTitleAt(tabIndex, newSheetName);
        }
      }
    }
  }

  /** This class defines the action that is performed when a sheet is inserted*/
  private class InsertSheetAction extends AbstractAction {
    public InsertSheetAction() {
      super("Insert");
    }

    public void actionPerformed(ActionEvent e) {
      //Create a new sheet then search for the sheet and make sure that the
      //sheetPane shows it.
      HSSFSheet newSheet = wb.createSheet();
      for (int i=0; i<wb.getNumberOfSheets();i++) {
        HSSFSheet sheet = wb.getSheetAt(i);
        if (newSheet == sheet) {
          sheetPane.insertTab(wb.getSheetName(i), null, makeSheetView(sheet), null, i);
        }
      }
    }
  }

  /** This class defines the action that is performed when the sheet is deleted*/
  private class DeleteSheetAction extends AbstractAction {
    public DeleteSheetAction() {
      super("Delete");
    }

    public void actionPerformed(ActionEvent e) {
      int tabIndex = sheetPane.getSelectedIndex();
      if (tabIndex != -1) {
        if (JOptionPane.showConfirmDialog(sheetPane, "Are you sure that you want to delete the selected sheet", "Delete Sheet?", JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {
          wb.removeSheetAt(tabIndex);
          sheetPane.remove(tabIndex);
        }
      }
    }
  }

  public boolean isEditable() {
    return allowEdits;
  }

  /**Main method*/
  public static void main(String[] args) {
    if(args.length < 1) {
      throw new IllegalArgumentException("A filename to view must be supplied as the first argument, but none was given");
    }
    try {
      FileInputStream in = new FileInputStream(args[0]);
      HSSFWorkbook wb = new HSSFWorkbook(in);
      in.close();

      SViewerPanel p = new SViewerPanel(wb, true);
      JFrame frame;
      frame = new JFrame() {
        protected void processWindowEvent(WindowEvent e) {
          super.processWindowEvent(e);
          if (e.getID() == WindowEvent.WINDOW_CLOSING) {
            System.exit(0);
          }
        }
        public synchronized void setTitle(String title) {
          super.setTitle(title);
          enableEvents(AWTEvent.WINDOW_EVENT_MASK);
        }
      };
      frame.setTitle("Viewer Frame");
      frame.getContentPane().add(p, BorderLayout.CENTER);
      frame.setSize(800,640);
      Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
      frame.setLocation((d.width - frame.getSize().width) / 2, (d.height - frame.getSize().height) / 2);
      frame.setVisible(true);
    } catch (IOException ex) {
      ex.printStackTrace();
      System.exit(1);
    }
  }
}
