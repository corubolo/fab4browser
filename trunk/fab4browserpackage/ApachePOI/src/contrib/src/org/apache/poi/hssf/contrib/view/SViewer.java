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

import org.apache.poi.hssf.usermodel.HSSFWorkbook;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowEvent;
import java.io.*;
import java.net.URL;
import java.net.URLConnection;

/**
 * Sheet Viewer - Views XLS files via HSSF.  Can be used as an applet with
 * filename="" or as a applications (pass the filename as the first parameter).
 * Or you can pass it a URL in a "url" parameter when run as an applet or just
 * that first parameter must start with http:// and it will guess its a url. I
 * only tested it as an applet though, so it probably won't work...you fix it.
 *
 * @author Andrew C. Oliver
 * @author Jason Height
 */
public class SViewer extends JApplet {
  private SViewerPanel panel;
  boolean isStandalone = false;
  String filename = null;

  /** Get a parameter value */
  public String getParameter(String key, String def) {
    return isStandalone ? System.getProperty(key, def) : (getParameter(key) !=
        null ? getParameter(key) : def);
  }

  /** Construct the applet */
  public SViewer() {
  }

  public SViewer(String filename) {
    this.filename = filename;
  }

  /** Initialize the applet */
  public void init() {
    try {
      jbInit();
    } catch (Exception e) {
      e.printStackTrace();
      System.exit(1);
    }
  }

  /** Component initialization */
  private void jbInit() throws Exception {
    if (filename == null)
      filename = getParameter("filename");
    if (filename == null)
      filename = getParameter("url");

    if (filename != null && filename.substring(0,7).equals("http://")) {
        panel = new SViewerPanel(new URL(filename), false);
    } else {
        panel = new SViewerPanel(filename, false);
    }

    getContentPane().setLayout(new BorderLayout());
    getContentPane().add(panel, BorderLayout.CENTER);
  }

  /** Start the applet */
  public void start() {
  }

  /** Stop the applet */
  public void stop() {
  }

  /** Destroy the applet */
  public void destroy() {
  }

  /** Get Applet information */
  public String getAppletInfo() {
    return "Applet Information";
  }

  /** Get parameter info */
  public String[][] getParameterInfo() {
    return null;
  }

  private static int openFiles = 0;

  /** Main method */
  public static void main(String[] args) {
    if (args.length < 1) {
      throw new IllegalArgumentException(
          "At least one filename to view must be supplied");
    }

    for (String filename : args) {
      openFile(filename, null);
    }
  }

  private static void openFile(String filename, JFrame openedFrom) {
    SViewer applet = new SViewer();
    applet.isStandalone = true;
    applet.filename = filename;
    JFrame frame;
    frame = new JFrame() {
      protected void processWindowEvent(WindowEvent e) {
        super.processWindowEvent(e);
        if (e.getID() == WindowEvent.WINDOW_CLOSING && --openFiles <= 0) {
          System.exit(0);
        }
      }

      public synchronized void setTitle(String title) {
        super.setTitle(title);
        enableEvents(AWTEvent.WINDOW_EVENT_MASK);
      }
    };
    frame.setTitle(applet.filename);
    frame.add(applet, BorderLayout.CENTER);
    applet.init();
    applet.createMenuBar(frame);
    applet.start();
    frame.pack();
    Dimension size = frame.getPreferredSize();
    frame.setSize(size.width, size.height);
    frame.setLocationRelativeTo(openedFrom);
    frame.setVisible(true);
    openFiles++;
  }

  private JMenuBar createMenuBar(JFrame frame) {
    JMenuBar menuBar = new JMenuBar();
    frame.setJMenuBar(menuBar);

    createFileMenu(frame, menuBar);
    createViewMenu(frame, menuBar);

    return menuBar;
  }

  private void createFileMenu(final JFrame frame, JMenuBar menuBar) {
    JMenu fileMenu = new JMenu("File");
    menuBar.add(fileMenu);

    final JFileChooser fc = new JFileChooser(System.getProperty("user.home"));
    addMenuItem(fileMenu, "Open", KeyEvent.VK_O, 'O', ActionEvent.ALT_MASK,
        new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            int returnVal = fc.showOpenDialog(frame);
            if (returnVal == JFileChooser.APPROVE_OPTION) {
              File file = fc.getSelectedFile();
              openFile(file.toString(), frame);
            }
          }
        });

    addMenuItem(fileMenu, "Close", KeyEvent.VK_C, 'C', ActionEvent.ALT_MASK,
        new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            if (--openFiles <= 0)
              System.exit(0);
            frame.setVisible(false);
            frame.dispose();
          }
        });

    fileMenu.addSeparator();
    addMenuItem(fileMenu, "Quit", KeyEvent.VK_Q, 'Q', ActionEvent.ALT_MASK,
        new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            System.exit(0);
          }
        });
  }

  private void createViewMenu(JFrame frame, JMenuBar menuBar) {
    JMenu viewMenu = new JMenu("View");
    menuBar.add(viewMenu);

    JTextField formulaText = new JTextField();
    JLabel formulaLbel = new JLabel("Formula: ");
    final JPanel formulaBar = new JPanel();
    formulaBar.setLayout(new BorderLayout());
    formulaBar.add(formulaLbel, BorderLayout.WEST);
    formulaBar.add(formulaText, BorderLayout.CENTER);
    frame.add(formulaBar, BorderLayout.NORTH);
    formulaBar.setVisible(false);

    formulaText.setBorder(BorderFactory.createLoweredBevelBorder());
    formulaText.setEditable(false);
    panel.setFormulaDisplay(formulaText);

    final JCheckBoxMenuItem formula = new JCheckBoxMenuItem("Formula Bar");
    addMenuItem(viewMenu, formula, 0, 'F', 0, new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        formulaBar.setVisible(formula.isSelected());
      }
    });
  }

  private static JMenuItem addMenuItem(JMenu menu, String text, int mnemonic,
      char accelerator, int modifiers, ActionListener actionListener) {

    JMenuItem item = new JMenuItem(text);
    addMenuItem(menu, item, mnemonic, accelerator, modifiers, actionListener);
    return item;
  }

  private static JMenuItem addMenuItem(JMenu menu, JMenuItem item, int mnemonic,
      char accelerator, int modifiers, ActionListener actionListener) {

    item.setMnemonic(mnemonic);
    if (Character.isUpperCase(accelerator))
      modifiers |= ActionEvent.SHIFT_MASK;
    item.setAccelerator(KeyStroke.getKeyStroke(accelerator, modifiers));
    item.addActionListener(actionListener);
    menu.add(item);
    return item;
  }
}
