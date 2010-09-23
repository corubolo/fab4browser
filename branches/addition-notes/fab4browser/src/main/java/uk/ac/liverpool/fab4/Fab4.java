/*******************************************************************************
 *
 *  * Copyright (C) 2007, 2010 - The University of Liverpool
 *  * This program is free software; you can redistribute it and/or modify it under the terms
 *  * of the GNU General Public License as published by the Free Software Foundation; either version 3 of the License,
 *  * or (at your option) any later version.
 *  * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 *  * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *  * You should have received a copy of the GNU General Public License along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *  *
 *  * Author: Fabio Corubolo
 *  * Email: corubolo@gmail.com
 *  test
 *******************************************************************************/

package uk.ac.liverpool.fab4;

import it.sauronsoftware.junique.AlreadyLockedException;
import it.sauronsoftware.junique.JUnique;
import it.sauronsoftware.junique.MessageHandler;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.KeyEventDispatcher;
import java.awt.KeyboardFocusManager;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.Map.Entry;
import java.util.logging.Level;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.spi.IIORegistry;
import javax.imageio.spi.ImageReaderSpi;
import javax.swing.AbstractButton;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.WindowConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;
import javax.swing.filechooser.FileView;
import javax.swing.plaf.basic.BasicButtonUI;
import javax.swing.text.JTextComponent;

import jbig2dec.LibJPEGNestedReaderSPI;

import multivalent.Behavior;
import multivalent.Browser;
import multivalent.DocInfo;
import multivalent.Document;
import multivalent.Layer;
import multivalent.Multivalent;
import multivalent.Node;
import multivalent.SemanticEvent;
import multivalent.Span;
import multivalent.devel.ShowDocTree;
import multivalent.gui.VScrollbar;
import multivalent.std.Note;
import multivalent.std.OcrView;
import multivalent.std.Search;
import multivalent.std.span.BackgroundSpan;
import multivalent.std.ui.BindingsDefault;
import multivalent.std.ui.ForwardBack;
import multivalent.std.ui.Multipage;
import multivalent.std.ui.SaveAs;
import multivalent.std.ui.StandardEdit;
import multivalent.std.ui.Zoom;
import phelps.awt.Colors;
import uk.ac.liverpool.fab4.behaviors.Bookmark;
import uk.ac.liverpool.fab4.behaviors.FullScreen;
import uk.ac.liverpool.fab4.behaviors.Print;
import uk.ac.liverpool.fab4.behaviors.TimedMedia;
import uk.ac.liverpool.fab4.ui.CloseEvent;
import uk.ac.liverpool.fab4.ui.JTabbedPaneClosing;
import uk.ac.liverpool.fab4.ui.LookAndFeelPanel;
import uk.ac.liverpool.fab4.ui.TabCloseListener;
import uk.ac.liverpool.fab4.ui.UISwitchListener;

/**
 * Main Class, a JFrame with all the UI and different resources.
 * 
 * This class contains many of the User Inferface extension to the Multivalent
 * System. It creates he main JFrame that hold all the contents for the Fab4
 * browser.
 * 
 */
public class Fab4 extends JFrame implements TabCloseListener, ActionListener,MessageHandler, WindowListener {
    public static FabPreferences prefs;

    public static Multivalent mv;

    public static final String title = "Fab4";

    public final static String dest = PersonalAnnos.dest;

    public static final String MSG_ASK_PASSWORD = "msg_ask_pass";

    public static final String beta = "This is a beta version of Fab4\n"
        + "intended for internal use.\n"
        + "Many aspects of the present beta are going\n"
        + "to change. Please report your impressions to:\n"
        + "corubolo@gmail.com\n" + "Thanks\n" + "Fabio Corubolo";

    public static final String FABANNO = "fabanno";

    private static final long serialVersionUID = 1L;

    public static final String PANEL = "innerPanel";

    private static int contin = 0;

    private static boolean singleInstance = false;

    Object singleInstanceListenter = null;

    private static BasicButtonUI basicButtonUI = null;

    /** the Vector of frames of Fab4 */
    static Vector<Fab4> fr;

    private final String[] zooms = new String[] { "15%", "25%", "50%", "75%",
            "90%", "100%", "110%", "125%", "150%", "175%", "200%", "400%" };

    /** the bookmarks */
    static Bookmark bm;

    boolean annotationExtension = hasDistrAnno();

    /**
     * Given a Browser, finds which frame holds it.
     * 
     * @param b
     *            the Browser
     * @return tha MVFrame that holds it
     */
    public static Fab4 getMVFrame(Browser b) {
        Fab4 ret = null;
        for (Fab4 f : Fab4.fr)
            for (Browser bb : f.br)
                if (b.equals(bb)) {
                    ret = f;
                    break;
                }
        return ret;
    }

    public String handle(String message) {
        System.out.println("Open new tab to: " + message);
        openNewTab(message);
        updateDocOpened(getCurBr());
        updateFormatted(getCurBr());
        getMVFrame(getCurBr()).toFront();
        return null;
    }


    /**
     * The main method! Creates a Fab4 frame, sets the look and feel, and looks
     * for parameters
     * 
     * @param args
     */
    public static void main(String[] args) {


        String appId = "uk.ac.uliv.Fab4Browser";


        for (String a : args)
            if (a.compareTo("-single")==0)
                Fab4.singleInstance = true;
        // single instance (with or without webstart)
        if (Fab4.singleInstance){
            try {
                JUnique.acquireLock(appId);
            } catch (AlreadyLockedException e) {
                for (String a : args)
                    if (a.startsWith("-")) {
                    } else {
                        String target = completeAddress(a);
                        JUnique.sendMessage(appId, target);
                    }
                System.out.println("sent message; done");
                return;
            }
            JUnique.releaseLock(appId);

        }

        Fab4.prefs = new FabPreferences();
        String target = null;
        for (String a : args)
            if (a.startsWith("-aserver=")) {
                a = a.replaceAll("-aserver=", "").trim();
                Fab4.prefs.wsserver = a;
                System.out.println("Override sword server: "+ a);

            } else if (a.startsWith("-aserveruser=")) {
                a = a.replaceAll("-aserveruser=", "").trim();
                System.out.println("Override sword user: "+ a);
                Fab4.prefs.wsuser = a;
            } else if (a.startsWith("-aserverpass=")) {
                a = a.replaceAll("-aserverpass=", "").trim();
                System.out.println("Override sword password: "+ a);
                Fab4.prefs.wspass = a;

            } else if (a.startsWith("-asearchserver=")) {
                a = a.replaceAll("-asearchserver=", "").trim();
                System.out.println("Override SRU server: "+ a);
                Fab4.prefs.searchaddress = a;

            } else if (!a.startsWith("-"))
                target = a;

        String lookAndFeel = "com.jgoodies.looks.plastic.PlasticXPLookAndFeel";
        if (System.getProperty("os.name").indexOf("Mac") >= 0) {
            lookAndFeel = "apple.laf.aqualaf";
            UIManager.put("OptionPane.border",
                    new javax.swing.plaf.BorderUIResource.EmptyBorderUIResource(
                            15 - 3, 24 - 3, 20 - 3, 24 - 3));
            UIManager.put("OptionPane.messageAreaBorder",
                    new javax.swing.plaf.BorderUIResource.EmptyBorderUIResource(0,
                            0, 0, 0));
            UIManager.put("OptionPane.buttonAreaBorder",
                    new javax.swing.plaf.BorderUIResource.EmptyBorderUIResource(
                            16 - 3, 0, 0, 0));
            // UIManager.put("TabbedPane.useSmallLayout", Boolean.TRUE);
            Color MAC_OS_SELECTED_ROW_COLOR = new Color(0.24f, 0.50f, 0.87f);
            UIManager.put("List.selectionBackground", MAC_OS_SELECTED_ROW_COLOR);
            UIManager.put("List.selectionForeground", Color.WHITE);
            UIManager.put("Table.selectionBackground", MAC_OS_SELECTED_ROW_COLOR);
            UIManager.put("Table.selectionForeground", Color.WHITE);
            System.setProperty("apple.laf.useScreenMenuBar", "true");
            System.setProperty("apple.awt.fileDialogForDirectories", "true");
        } else
            try {
                UIManager.setLookAndFeel(lookAndFeel);
            } catch (Exception e) {
            }
            Fab4.fr = new Vector<Fab4>(1);
            final Fab4 f = new Fab4();

            if (Fab4.singleInstance)
                try {
                    JUnique.acquireLock(appId, f);
                } catch (AlreadyLockedException e1) {
                    for (String a : args)
                        if (a.startsWith("-")) {
                        } else {
                            target = completeAddress(a);
                            JUnique.sendMessage(appId, target);
                        }
                }
                Multivalent.getLogger().setLevel(Level.CONFIG);
                System.out
                .println("Distributed annotation extension:" + hasDistrAnno());

                // !!!!!!!!!!!!!!!!!!!!!
                Multivalent.getInstance().getGenreMap().put("mp3", "MP3AudioMA");


                target = completeAddress(target);

                try {
                    f.populate(target);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (InvocationTargetException e) {
                    e.printStackTrace();
                }
                //		Iterator<String> i = Multivalent.getInstance().prefKeyIterator();
                //		Set<Entry<String, String>> s = Multivalent.getInstance().getGenreMap().entrySet();
                //		while (i.hasNext())
                //			System.out.println(i.next());
                //		for (Entry<String, String>e : s)
                //			System.out.println(e.getKey() + " "+ e.getValue());

    }

    private static String completeAddress(String target) {
        if (target != null)
            if (!target.startsWith("http") && !target.startsWith("ftp")&& !target.startsWith("file")
                    && !(target.indexOf("://") != -1) && !(target.indexOf(":/") != -1))
                if (target.startsWith("www") || target.endsWith(".com")|| target.endsWith(".org")|| target.endsWith(".net"))
                    target = "http://" + target;
                else
                    try {
                        File fil = new File(target);
                        target = fil.toURI().toString();
                    } catch (Exception e) {
                        target = "http://" + target;
                    }
                    return target;
    }

    static boolean hasDistrAnno() {
        try {
            Class.forName("uk.ac.liv.c3connector.FabNote");
            return true;
        } catch (Exception edd) {
            return false;
        }
    }

    /** the number of the currently selected browser */
    public int currentBrowser = 0;

    /** the total number of browsers (in the frame) */
    public int totalFrameBrowsers = -1;

    private javax.swing.JPanel jContentPane = null;

    /** the main emnu bar */
    private JMenuBar jJMenuBar = null;

    /** the status line at the bottom of the main frame */
    private JLabel statusLine;

    private JPopupMenu tabpopup;

    public JMenu mfile = null;

    public JMenu mview = null;

    public JMenu mlens = null;

    public JMenu mhelp = null;

    TopButtobBar topButtonBar = null;

    private JToolBar bottomButtonBar = null;

    private JPanel pstatus = null;

    private JMenuItem newtab = null, mclose = null, mexit = null,
    mprint = null;

    private JMenuItem msave = null, neww = null, open = null;

    private JMenuItem mcut = null, mcopy = null, mclear = null;

    private JButton np, pp, fp, lp;

    private JLabel paget;

    private static File currFile = null;

    private JToolBar pright;

    private JToggleButton ocrb;

    JButton banno;

    private JMenuItem mprintp;

    private JPanel prefPane;

    public Map<Browser, AnnotationSidePanel> annoPanels = new HashMap<Browser, AnnotationSidePanel>(
            10);

    private JFileChooser fileChoser;

    public JMenu medit = null;

    public JMenu manno = null;
    
    ///SAM
	public JMenu mtag = null;
	///

    public JMenu mcopyed = null;

    public JMenu mtools = null;

    public JMenu mstyle = null;

    public JMenu mbookmarks = null;

    JTabbedPane ptabs = null;

    /** The Vector of browsers of every frame */

    Vector<Browser> br;

    JMenuItem mpaste = null;

    JMenuItem mselall = null;

    JDialog searchFrame;

    JTextField pagec;

    JMenuItem itspan;

    JMenuItem mannos[] = new JMenuItem[3];

    JMenuItem mnewnote;
    
	///SAM
	JMenuItem mtagThis;
	JMenuItem mshowTags;
	///

    JMenuItem msearch;

    JMenuItem mpublish;

    JDialog publishFrame;

    //JCheckBoxMenuItem loadRemote;

    JMenuItem mpref;

    JMenuItem mnewnote2;

    private boolean verticalAnnoPanel = false;

    private JComboBox jcb;

    private JPanel topPanel;

    public MediaUI pmedia;

    public static boolean JAVA_WS = false;



    /**
     * builds and display a new Frame of Fab4.
     */
    public Fab4() {

        super();


    }
    public Fab4(boolean f) {

    }

    /**
     * HERE following are all the actions. These result as never used locally
     * because they are called by the ActionListener using the actionCommad as
     * the method name.
     */
    public void actionPerformed(ActionEvent e) {

        String command = e.getActionCommand();
        // 1st semantic events:
        if (command.startsWith("#")) {
            getCurBr().eventq(command.substring(1), null);
            return;
        }
        // 2nd LENSES : with the class name
        if (command.startsWith("@")) {
            String name = command.substring(command.lastIndexOf('.') + 1,
                    command.length());
            name = name.replaceAll(" ", "");
            Behavior b = Behavior.getInstance(name, command.substring(1), null,
                    new HashMap<String, Object>(1), getCurBr().getRoot()
                    .getLayer(Layer.SYSTEM));
            if (b == null)
                System.out.println("Lens not found!");
            return;
        }
        // Otherwise it's a call to a local (Fab4) method
        Method call = null;
        try {
            call = Fab4.class.getDeclaredMethod(command, (Class[]) null);/* declared */
        } catch (SecurityException e1) {
            e1.printStackTrace();
            return;
        } catch (NoSuchMethodException e1) {
            e1.printStackTrace();
            return;
        }
        try {
            call.invoke(this, (Object[]) null);
        } catch (IllegalArgumentException e1) {
            e1.printStackTrace();
        } catch (IllegalAccessException e1) {
            e1.printStackTrace();
        } catch (InvocationTargetException e1) {
            e1.printStackTrace();
        }

    }

    void doneLoading(Browser br2) {
        topButtonBar.stopButton.setEnabled(false);
    }

    void startedLoading(Browser br2) {
        topButtonBar.stopButton.setEnabled(true);
    }

    void closeAllTabs() {
        while (totalFrameBrowsers > 0)
            closeTab();
        closeTab();
    }

    /** closes the Tab at index index */
    public void closeTab(int index) {
        if (totalFrameBrowsers < 1) {
            open(Fab4.mv.getPreference("homepage", null));
            return;
        }
        Browser bro = br.get(index);
        annoPanels.remove(bro);
        bro.event(new SemanticEvent(bro, Browser.MSG_CLOSE, bro
                .getCurDocument(), bro.getCurDocument(), null));
        bro.eventq(Browser.MSG_CLOSE, bro);
        br.remove(index);
        br = null;
        totalFrameBrowsers--;
        ptabs.remove(index);
    }

    /** Closes the active Fab4 *window* */
    public void closeWindow() {
        if (totalFrameBrowsers > 0) {
            Object[] options = { "Cancel", "Close Window", };
            int n = JOptionPane.showOptionDialog(this, "You have "
                    + (totalFrameBrowsers + 1) + " tabs open, close anyway?",
                    "Attention", JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE, null, options, options[0]);
            if (n == 0)
                return;
        }
        Fab4.fr.remove(this);
        if (Fab4.fr.size() == 0) {
            Fab4utils.writeIcoCache();
            try {
                for (Browser bro : br) {
                    bro.event(new SemanticEvent(bro, Browser.MSG_CLOSE, bro
                            .getCurDocument(), bro.getCurDocument(), null));
                    //bro.eventq(Browser.MSG_CLOSE, bro);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            Rectangle pos = getBounds();
            Fab4.prefs.getProps().setProperty("wwidth", ""+pos.width);
            Fab4.prefs.getProps().setProperty("wheight", ""+pos.height);
            Fab4.prefs.getProps().setProperty("wx", ""+pos.x);
            Fab4.prefs.getProps().setProperty("wy", ""+pos.y);
            Fab4.prefs.savePrefs();
            dispose();
            System.exit(0);
            return;
        }

        for (Browser bro : br) {
            bro.event(new SemanticEvent(bro, Browser.MSG_CLOSE, bro
                    .getCurDocument(), bro.getCurDocument(), null));
            bro.eventq(Browser.MSG_CLOSE, bro);
        }
        dispose();

    }

    /** does a short dump of the active layers */
    public void dumpLayers() {
        Layer l = getCurDoc().getLayers();
        Fab4utils.listBe(l);
        l.dump();
    }

    /**
     * Finds the Behaviour with this name registered in the CURRENT _browser_
     * 
     * @param name
     *            the firendly name of the behaviour
     * @return The behaviour if found, null otherwise.
     */
    public Behavior getBe(String name) {
        Layer l = getCurBr().getRoot().getLayers();
        return Fab4utils.getBe(name, l);
    }

    /**
     * Finds the Behaviour with this name registered in the CURRENT _DOCUMENT_
     * of the currentbrowser
     * 
     * @param name
     *            the firendly name of the behaviour
     * @return The behaviour if found, null otherwise.
     */
    public Behavior getBeDoc(String name) {
        Layer l = getCurDoc().getLayers();
        return Fab4utils.getBe(name, l);
    }

    /**
     * This method initializes 1 icon buttons for the tool bar with rised effect
     */
    public JButton getButtonBorder(String name) {
        JButton tm = getButton(name);
        tm.setBorderPainted(true);
        return tm;
    }

    /** Returns the current browser (in the tab) */
    public Browser getCurBr() {
        return br.get(currentBrowser);
    }

    /** Returns the document int the current browser (in the tab) */
    public Document getCurDoc() {
        return (Document) br.get(currentBrowser).getRoot().findBFS("content");
        // return getCurBr().getCurDocument();
    }

    public void goHome() {
        open(Fab4.mv.getPreference("homepage", null), currentBrowser);
    }

    /** Shows (opens) all the notes */
    public void showNotes() {
        Document mvDocument = (Document) getCurBr().getRoot()
        .findBFS("content");
        Layer personal = mvDocument.getLayer(Layer.PERSONAL);
        if (personal != null) {
            for (int i = 0; i < personal.size(); i++)
                if (personal.getBehavior(i) instanceof multivalent.std.Note)
                    personal.getBehavior(i).putAttr("closed", "false");
            getCurBr().eventq(Note.MSG_SHOW, "");
            getCurBr().repaint();
        }
    }

    /** hides the annotations */
    public void hideAnnos() {
        getCurBr().eventq(PersonalAnnos.MSG_HIDE, null);
    }

    /** iconifies the open notes */
    public void iconifyNotes() {
        Document mvDocument = (Document) getCurBr().getRoot()
        .findBFS("content");
        Layer personal = mvDocument.getLayer(Layer.PERSONAL);
        if (personal != null) {
            getCurBr().eventq(PersonalAnnos.MSG_ICON, "");
            getCurBr().repaint();
        }
    }

    /** creates a new Fab4 window */
    public void newWindow() {
        Fab4 f = new Fab4();
        f.toFront();
        try {
            f.populate(null);
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }

    }

    /** Shows the file dialog */
    public void openFileDialog() {
        getCurrentDir();
        if (fileChoser == null) {
            fileChoser = new JFileChooser(Fab4.currFile);
            FileView fv = new FileView() {
                public Icon getIcon(File f) {
                    Icon icon = Fab4utils.getIconForExt(f, f.getName());
                    return icon;
                }
            };
            fileChoser.setFileView(fv);
        }
        int returnVal = fileChoser.showOpenDialog(this);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            Fab4.currFile = fileChoser.getSelectedFile();
            getCurBr().eventq(Document.MSG_OPEN, Fab4.currFile.toURI());
            
			///SAM            
			try {
				Class disAnnos = Class.forName("uk.ac.liv.c3connector.DistributedPersonalAnnos");
				
				String curServer = (String) disAnnos.getDeclaredMethod("getCurrentRemoteServer").invoke(null);
				
				if(curServer.equals("REST")){
					Class parameterTypes = Class.forName("java.lang.String");
					disAnnos.getDeclaredMethod("askForDocumentInfo", parameterTypes ).invoke(null, Fab4.currFile.toURI().toString());
				}
				
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			///
            
            Fab4.currFile = Fab4.currFile.getAbsoluteFile();
            Fab4.prefs.getProps()
            .setProperty("lastSelectedDir", Fab4.currFile.toString());
        }
    }

    public static File getCurrentDir() {

        if (Fab4.currFile == null) {
            String s = Fab4.prefs.getProps().getProperty("lastSelectedDir");
            if (s != null)
                Fab4.currFile = new File(s);
            else
                Fab4.currFile = new File(new File(Fab4utils.USER_HOME_DIR),
                "Desktop");
        }
        return Fab4.currFile;
    }

    /** Opens a new tab to the homepage */
    public void openNewTab() {
        ptabs.addTab("---", null, createBrowser(), null);
        open(Fab4.mv.getPreference("homepage", null), totalFrameBrowsers);
    }

    /** Opens a new tab to specified url */
    public void openNewTab(String url) {
        ptabs.addTab("---", null, createBrowser(), null);
        open(url, totalFrameBrowsers);
        ptabs.setSelectedIndex(totalFrameBrowsers);
    }

    @SuppressWarnings("boxing")
    /* Shows the edit preferences dialog */
    public void preferences() {
        /* if we have an annotation extension, we let it create the UI */
        if (annotationExtension) {
            getCurBr().eventq(PersonalAnnos.MSG_DIST_ANNO_PREF_CREATE,
                    getCurBr());
            return;
        }
        /* otherwise we use the standard (OLD ) Fab4 */
        JDialog dia = new JDialog(this, true);
        if (prefPane == null) {
            prefPane = new JPanel(new BorderLayout());
            JTabbedPane conts = new JTabbedPane();
            conts.add("Look And Feel", new LookAndFeelPanel());
            prefPane.add(conts, BorderLayout.CENTER);

        }
        dia.getContentPane().add(prefPane);
        dia.pack();
        dia.setVisible(true);

    }

    /** prints the current document */
    public void print() {
        getCurBr().eventq(Print.MSG_PRINT, getCurBr());
    }

    /** print-previews the current document */
    public void printPreview() {
        getCurBr().eventq(Print.MSG_PRINT_PREVIEW, getCurBr());
    }

    /** Sets the text in the status line */
    public void setStatus(String s) {
        if (s == null)
            statusLine.setText("");
        else if (!statusLine.getText().equals(s))
            statusLine.setText(s);
    }

    public void showDocTree() {
        Behavior.getInstance("ShowDocTree", "multivalent.devel.ShowDocTree",
                null, null, getCurBr().getRoot().getLayer(Layer.SYSTEM));
        getCurBr().eventq(ShowDocTree.MSG_SHOW, getCurBr().getRoot());
    }

    /** Shows the OCR layer, if present (currently for specific PDF documents */

    public void showOcr() {
        if (ocrb.isSelected())
            getCurBr().eventq(OcrView.MSG_OCR, null);
        else
            getCurBr().eventq(OcrView.MSG_IMAGE_OCR, null);
    }

    /** shows the source of the current document */
    public void showSource() {
        DocInfo di = new DocInfo(getCurDoc().getURI());
        di.genre = "ASCII"; // until cache is a behavior
        getCurBr().eventq(Document.MSG_OPEN, di);
    }

    /** closes a specific tab, in response to a tab closing event */
    public void tabClosing(CloseEvent e) {
        if (totalFrameBrowsers < 1) {
            open(Fab4.mv.getPreference("homepage", null));
            e.unClose();
            return;
        }
        Browser bro = br.get(e.getTabIndex());
        bro.event(new SemanticEvent(bro, Document.MSG_CLOSE, bro
                .getCurDocument(), bro.getCurDocument(), null));
        bro.eventq(Browser.MSG_CLOSE, bro);
        br.remove(e.getTabIndex());
        totalFrameBrowsers--;
    }

    /**
     * updates the annotation icons to enabled / disabled, according to the
     * current status
     */
    public void updateAnnoIcon() {
        AnnotationSidePanel ap = annoPanels.get(getCurBr());
        if (ap == null)
            return;
        Document mvDocument = (Document) getCurBr().getRoot()
        .findBFS("content");
        Layer personal = mvDocument.getLayer(Layer.PERSONAL);
        boolean toPublish = false;
        if (personal != null)
            if (personal.size() > 0)
                for (int i = 0; i < personal.size(); i++) {
                    Behavior bb = personal.getBehavior(i);
                    if (bb.getValue(Fab4.FABANNO) == null)
                        toPublish = true;
                }
        ap.bPubAnno.setEnabled(toPublish);
        ap.bSaveAnno.setEnabled(toPublish);
        mpublish.setEnabled(toPublish);

        // if (toPublish){
        // ap.annoUserList.clearSelection();
        // }

        final PersonalAnnos bu = (PersonalAnnos) Fab4utils.getBe("Personal",
                getCurBr().getRoot().getLayers());
        if (bu != null && bu.user.getSize() > 0) {
            ap.bShowAnno.setEnabled(true);
            ap.bHideAnno.setEnabled(true);
            ap.bIconAnno.setEnabled(true);
        } else {
            ap.bShowAnno.setEnabled(false);
            ap.bHideAnno.setEnabled(false);
            ap.bIconAnno.setEnabled(false);

        }
        JPanel annoPanel = annoPanels.get(getCurBr());
        if (((JSplitPane) annoPanel.getParent()).getDividerLocation() <= 1)
            banno.setIcon(FabIcons.getIcons().BICO2);
        else
            banno.setIcon(FabIcons.getIcons().BICO1);

    }

    /**
     * Updates the UI after a document has been completely loaded.
     * 
     */
    public void updateDocOpened(Browser bb) {
        Document doc = (Document) bb.getRoot().findBFS("content");
        String testo;
        String a;
        // Title in the tab
        testo = doc.getAttr(Document.ATTR_TITLE);
        if (testo.length() > 18)
            a = testo.substring(0, 18) + "...";
        else
            a = testo;
        ptabs.setTitleAt(ptabs.indexOfComponent(bb.getParent().getParent()), a);
        Icon ii = null;
        if (doc.uri != null)
            if (doc.getURI().getScheme() != null)
                if (doc.getURI().getScheme().equals("http")) {
                    String inHtml = null;
                    Node n;
                    n = doc.findBFS("link", "rel", "icon", 10);
                    if (n != null)
                        inHtml = n.getAttr("href");
                    else {
                        n = doc.findBFS("link", "rel", "shortcut icon", 10);
                        if (n != null)
                            inHtml = n.getAttr("href");
                    }
                    if (inHtml != null)
                        try {
                            URI dest1 = doc.getURI().resolve(inHtml);
                            ii = Fab4utils.getIcon(dest1.toURL());
                        } catch (MalformedURLException e) {
                        }
                        if (ii == null)
                            ii = Fab4utils.getFavIcon(doc.getURI());

                }
        if (doc.getAttr(Document.ATTR_GENRE).equals("AdobePDF"))
            ii = FabIcons.getIcons().ICOPDF;
        if (doc.getAttr(Document.ATTR_GENRE).equals("ASCII"))
            ii = FabIcons.getIcons().ICOTXT;
        if (doc.getAttr(Document.ATTR_GENRE).equals("ODT"))
            ii = FabIcons.getIcons().ICOODT;
        if (doc.getAttr(Document.ATTR_GENRE).equals("RawImage"))
            ii = FabIcons.getIcons().ICOIMA;
        if (doc.getAttr(Document.ATTR_GENRE).equals("SVGImage"))
            ii = FabIcons.getIcons().ICOIMA;
        else if (ii == null)
            ii = FabIcons.getIcons().ICOURL;
        ptabs.setIconAt(ptabs.indexOfComponent(bb.getParent().getParent()), ii);
        // Address bar
        if (bb == getCurBr()) {
            topButtonBar.address.setText(doc.getURI().toString());
            setTitle("  " + testo + " - " + Fab4.title);
            topButtonBar.urlbutton.setIcon(ii);
        }
        // Page count
        int pages;
        if (bb == getCurBr()) {
            if (doc.getAttr(Document.ATTR_PAGECOUNT) == null)
                pages = 0;
            else
                pages = Integer.parseInt(doc.getAttr(Document.ATTR_PAGECOUNT));
            pagec.setColumns(Integer.toString(pages).length());
            updatePagec();
        }

    }

    /** Updates the UI after formatting is completed */
    public void updateFormatted(Browser br2) {
        Document doc = (Document) br2.getRoot().findBFS("content");
        if (doc.childAt(0) != null && doc.childAt(0).bbox != null) {

            ((FabScrollbars) doc.getHsb()).setVisible(doc.bbox.width);
            ((FabScrollbars) doc.getVsb()).setVisible(doc.bbox.height);
            ((FabScrollbars) doc.getHsb()).setMinMax(0,
                    doc.childAt(0).bbox.width);
            ((FabScrollbars) doc.getVsb()).setMinMax(0,
                    doc.childAt(0).bbox.height);
            if (getBeDoc("ocrview") != null)
                ocrb.setVisible(true);
            else
                ocrb.setVisible(false);
            ocrb.setSelected(false);
            updateAnnoIcon();
            updatePagec();
            if (doc.getMediaAdaptor() != null) {
                float f = doc.getMediaAdaptor().getZoom();
                int c = (int) (f * 100);
                String p = c + "%";
                boolean found = false;
                for (String s : zooms)
                    if (s.equals(p)) {
                        jcb.setSelectedItem(s);
                        found = true;
                    }
                if (!found)
                    jcb.setSelectedItem(p);
                if (doc.getFirstChild() instanceof TimedMedia) {
                    TimedMedia tm = (TimedMedia) doc.getFirstChild();
                    pmedia.setVisible(tm.getDisplayGlobalUI());
                } else
                    pmedia.setVisible(false);

                pstatus.doLayout();
            }
        }

    }

    /** updates the page count */
    public void updatePagec() {
        Document doc = getCurDoc();
        int pc = doc.getAttr(Document.ATTR_PAGE) != null ? Integer.parseInt(doc
                .getAttr(Document.ATTR_PAGE)) : 1;
        int pt = doc.getAttr(Document.ATTR_PAGECOUNT) != null ? Integer
                .parseInt(doc.getAttr(Document.ATTR_PAGECOUNT)) : 1;
                if (pt < 2) {
                    pright.setVisible(false);
                    return;
                }
                pright.setVisible(true);
                if (pt > 1) {
                    if (!pagec.getText().equals("" + pc))
                        pagec.setText("" + pc);
                    if (!paget.getText().equals("of " + pt))
                        paget.setText("of " + pt);
                    pagec.setEnabled(true);
                } else {
                    if (!pagec.getText().equals(""))
                        pagec.setText("");
                    if (!paget.getText().equals(""))
                        paget.setText("");
                    pagec.setEnabled(false);
                }
                if (pc == 1) {
                    fp.setEnabled(false);
                    pp.setEnabled(false);
                } else {
                    fp.setEnabled(true);
                    pp.setEnabled(true);
                }
                if (pc == pt) {
                    np.setEnabled(false);
                    lp.setEnabled(false);
                } else {
                    np.setEnabled(true);
                    lp.setEnabled(true);
                }
    }

    public void windowActivated(WindowEvent e) {
    }

    public void windowClosed(WindowEvent e) {
    }

    public void windowClosing(WindowEvent e) {
        closeWindow();
    }

    public void windowDeactivated(WindowEvent e) {
    }

    public void windowDeiconified(WindowEvent e) {
    }

    public void windowIconified(WindowEvent e) {
    }

    public void windowOpened(WindowEvent e) {
    }

    public void zoomFitPage() {
        getCurBr().eventq(Zoom.MSG_SET, Zoom.ARG_FIT_PAGE);
    }

    public void zoomFitWidth() {
        getCurBr().eventq(Zoom.MSG_SET, Zoom.ARG_FIT_WIDTH);
    }

    public void zoomIn() {
        getCurBr().eventq(Zoom.MSG_SET, Zoom.ARG_BIGGER);
    }

    public void zoomOut() {
        getCurBr().eventq(Zoom.MSG_SET, Zoom.ARG_SMALLER);
    }

    public void closeTab() {
        closeTab(currentBrowser);
    }

    void closeTab(Browser bro) {
        int index = br.indexOf(bro);
        closeTab(index);

    }

    /**
     * This method initializes lower buttonbar (excluding the page control
     * buttons)
     */
    private JToolBar getBottomLeftToolbar() {
        if (bottomButtonBar == null) {
            bottomButtonBar = new JToolBar();
            bottomButtonBar.setBorder(new EmptyBorder(0, 4, 2, 0));
            JButton tm = getButton("zoom_in", "zoom in");
            setAction(tm, "zoomIn");
            bottomButtonBar.add(tm);
            tm = getButton("zoom_out", "zoom out");
            setAction(tm, "zoomOut");
            bottomButtonBar.add(tm);
            tm = getButton("zoom_fit", "zoom fit page");
            setAction(tm, "zoomFitPage");
            bottomButtonBar.add(tm);
            tm = getButton("zoomwidth", "zoom fit width");
            setAction(tm, "zoomFitWidth");
            bottomButtonBar.add(tm);
            jcb = new JComboBox(zooms);
            jcb.setSelectedItem(zooms[5]);
            jcb.setEditable(true);
            jcb.addItemListener(new ItemListener() {
                public void itemStateChanged(ItemEvent e) {
                    if (e.getStateChange() == ItemEvent.SELECTED)
                        getCurBr().eventq(Zoom.MSG_SET, e.getItem());
                }
            });
            jcb.setFont(jcb.getFont().deriveFont(10.0f));
            bottomButtonBar.add(jcb);
            ocrb = new JToggleButton();
            ocrb.setBackground(topButtonBar.getBackground());
            try {
                ocrb.setIcon(new ImageIcon(getClass().getResource(
                "/res/ocr1.png")));
                ocrb.setSelectedIcon(new ImageIcon(getClass().getResource(
                "/res/ocr2.png")));
            } catch (NullPointerException e) {
                ocrb.setText("ocr");
            }
            if (Fab4.basicButtonUI == null)
                Fab4.basicButtonUI = new BasicButtonUI();
            ocrb.setUI(Fab4.basicButtonUI);
            ocrb.setMargin(new java.awt.Insets(0, 0, 0, 0));
            setAction(ocrb, "showOcr");
            bottomButtonBar.add(ocrb);
            banno = new JButton("");
            banno.setUI(Fab4.basicButtonUI);
            banno.setMargin(new java.awt.Insets(0, 0, 0, 0));
            setAction(banno, "toggleAnnoPanel");
            banno.setIcon(FabIcons.getIcons().BICO1);
            banno.setToolTipText("Show/hide the annotation panel");
            bottomButtonBar.add(banno);
            bottomButtonBar.setFloatable(false);
            bottomButtonBar.setRollover(true);
            bottomButtonBar.doLayout();

        }
        return bottomButtonBar;
    }

    /**
     * This method initializes 1 icon buttons for the tool bar with rised effect
     * plus tooltip
     */
    private JButton getButtonBorder(String name, String tooltip) {
        JButton tm = getButton(name);
        tm.setBorderPainted(true);
        tm.setToolTipText(tooltip);
        return tm;
    }

    /**
     * This method initializes main content Pane. Uses a Border Layout with
     * North, Center and South occupied. East and West are available for
     * additional components
     * 
     * @return javax.swing.JPanel
     */
    private javax.swing.JPanel getJContentPane() {
        if (jContentPane == null) {
            jContentPane = new javax.swing.JPanel();
            jContentPane.setLayout(new java.awt.BorderLayout());
            topPanel = getPTop();
            jContentPane.add(topPanel, java.awt.BorderLayout.NORTH);
            jContentPane.add(getStatusPanel(), java.awt.BorderLayout.SOUTH);
            jContentPane.add(getPtabs(), java.awt.BorderLayout.CENTER);
        }
        return jContentPane;
    }

    /** This method initializes menu bar. */
    private JMenuBar getJJMenuBar() {
        if (jJMenuBar == null) {
            jJMenuBar = new JMenuBar();
            jJMenuBar.add(getMfile());
            jJMenuBar.add(getMedit());
            jJMenuBar.add(getMbookmarks());
            jJMenuBar.add(getManno());
            ///SAM
			jJMenuBar.add(getTagmenu());
			///
            jJMenuBar.add(getMcopyed());
            jJMenuBar.add(getMstyle());
            jJMenuBar.add(getMlens());
            jJMenuBar.add(getMview());
            jJMenuBar.add(getMTools());

            jJMenuBar.add(getMhelp());
        }
        return jJMenuBar;
    }

    /** This method initializes Annotation menu */
    private JMenu getManno() {
        manno = new JMenu("Notes");
        manno.setMnemonic(java.awt.event.KeyEvent.VK_A);
        JMenuItem tm;
        mnewnote = new JMenuItem("New note");
        mnewnote.setIcon(FabIcons.getIcons().NOTE_ICO);
        mnewnote.setMnemonic(java.awt.event.KeyEvent.VK_M);
        mnewnote.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (annotationExtension)
                    Behavior.getInstance("Note",
                            "uk.ac.liv.c3connector.FabNote", null,
                            new HashMap<String, Object>(1), getCurDoc()
                            .getLayer(Layer.PERSONAL));
                else
                    Behavior.getInstance("Note", "multivalent.std.Note", null,
                            new HashMap<String, Object>(1), getCurDoc()
                            .getLayer(Layer.PERSONAL));

            }
        });
        manno.add(mnewnote);

        try {
            Class.forName("uk.ac.liv.c3connector.DistributedPersonalAnnos");
            mnewnote2 = new JMenuItem("New anchored note");
            mnewnote2.setIcon(FabIcons.getIcons().NOTE_ICO_CALL);
            mnewnote2.setMnemonic(java.awt.event.KeyEvent.VK_C);
            mnewnote2.addActionListener(new ActionListener() {
                @SuppressWarnings("unchecked")
                public void actionPerformed(ActionEvent e) {
                    HashMap hh = new HashMap<String, Object>(1);
                    hh.put("callout", "");
                    Behavior.getInstance("Note",
                            "uk.ac.liv.c3connector.FabNote", null, hh,
                            getCurDoc().getLayer(Layer.PERSONAL));
                }
            });
            manno.add(mnewnote2);

        } catch (Exception edd) {
            System.out.println(edd);

        }

        manno.addSeparator();

        tm = getSpanMI("Comment", "TextSpanNote",
                "uk.ac.liverpool.fab4.behaviors.TextSpanNote", true, null,
                FabIcons.getIcons().ICOCOM);
        manno.add(tm);
        tm = getSpanMI("Citation", "CitesSpanNote",
                "uk.ac.liverpool.fab4.behaviors.CitesSpanNote", true, null,
                FabIcons.getIcons().ICOCOM);
        manno.add(tm);

        tm = getSpanMI("Highlight", "Highlight",
                "multivalent.std.span.BackgroundSpan", false, null, FabIcons
                .getIcons().ICOHIGHLIGHT);
        tm.setBackground(Color.YELLOW);
        manno.add(tm);
        tm = getSpanMI("Hyperlink", "hyperlink",
                "multivalent.std.span.HyperlinkSpan", true, null, FabIcons
                .getIcons().ICOLINK);
        manno.add(tm);

        //		 tm = getSpanMI("Anchor", "anchor", "multivalent.std.span.AnchorSpan",
        //		 true, null,null);
        //		 manno.add(tm);
        //		tm = getSpanMI("FBI Redaction", "redact",
        //				"edu.berkeley.span.FBISpan", false, null, null);
        //		manno.add(tm);

        manno.addSeparator();

        JMenuItem c = new JMenuItem("Show notes");
        c.setIcon(FabIcons.getIcons().ICOSHO);
        c.setToolTipText("Show all the Notes");
        setAction(c, "showNotes");
        manno.add(c);
        mannos[0] = c;
        //		c = new JMenuItem("Iconify notes");
        //		c.setIcon(FabIcons.getIcons().ICOICO);
        //		c.setToolTipText("Reduces to icon all the notes");
        //		setAction(c, "iconNotes");
        //		manno.add(c);
        //		mannos[1] = c;

        c = new JMenuItem("Hide notes");
        c.setIcon(FabIcons.getIcons().ICOHID);
        c.setMnemonic(java.awt.event.KeyEvent.VK_C);
        setAction(c, "hideAnnos");
        mannos[2] = c;

        manno.add(c);
        manno.addSeparator();
        mpublish = new JMenuItem("Publish notes", FabIcons.getIcons().ICOPUB);
        setAction(mpublish, "publishAnnos");
        manno.add(mpublish);
        return manno;
    }

  ///SAM
	/** This method initializes Tag menu */
	private JMenu getTagmenu() {
		mtag = new JMenu("Tags");
		mtag.setMnemonic(java.awt.event.KeyEvent.VK_A);
//		JMenuItem tm;
		
		mtagThis = new JMenuItem("Tag this page");
		
		//mtagThis.setIcon(FabIcons.getIcons().NOTE_ICO);
		mtagThis.setMnemonic(java.awt.event.KeyEvent.VK_M);
		mtagThis.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				//if (annotationExtension)
					Behavior.getInstance("tag",
							"uk.ac.liv.c3connector.TagResource", null,
							new HashMap<String, Object>(1), getCurDoc()
							.getLayer(Layer.PERSONAL));
				/*else
					Behavior.getInstance("Note", "multivalent.std.Note", null,
							new HashMap<String, Object>(1), getCurDoc()
							.getLayer(Layer.PERSONAL));*/

			}
		});
		
		mtag.add(mtagThis);
		
		mshowTags = new JMenuItem("Show all tags");
		
		//mtagThis.setIcon(FabIcons.getIcons().NOTE_ICO);
		
		setAction(mshowTags, "showAllTags");
		mshowTags.setEnabled(true);
		mtag.add(mshowTags);
		return mtag;
	}
///
    
    void publishAnnos() {
        getCurBr().eventq(PersonalAnnos.MSG_PUBLISH_ANNOS, "");

    }

    ///SAM
	void showAllTags(){
		getCurBr().eventq("showAllTags", "");	
	}
	///
	
    /** This method initializes bookmarks */
    private JMenu getMbookmarks() {
        if (mbookmarks == null) {
            mbookmarks = new JMenu("Bookmarks");
            mbookmarks.setMnemonic(java.awt.event.KeyEvent.VK_B);
            mbookmarks.setName("menubookmarks");
        }
        return mbookmarks;
    }

    /** This method initializes Copyed menu */
    private JMenu getMcopyed() {
        if (mcopyed == null) {
            mcopyed = new JMenu("CopyEd");
            mcopyed.setMnemonic(java.awt.event.KeyEvent.VK_C);
            JMenuItem tm;
            Map<String, Object> att;
            att = new HashMap<String, Object>(1);
            tm = getSpanMI("Move text", "Movetextspan",
                    "multivalent.std.span.MoveTextSpan", false, null, null);
            mcopyed.add(tm);
            tm = getSpanMI("Replace With", "replacetext",
                    "multivalent.std.span.ReplaceWithSpan", true, null, null);
            mcopyed.add(tm);
            att = new HashMap<String, Object>(1);
            att.put("point", " ");
            itspan = getSpanMI("Insert text", "inserttextspan",
                    "multivalent.std.span.InsertSpan", true, att, null);
            mcopyed.add(itspan);
            tm = getSpanMI("Delete", "deletetextspan",
                    "multivalent.std.span.DeleteSpan", true, null, null);
            mcopyed.add(tm);
            tm = getSpanMI("Short comment", "awkSpan",
                    "multivalent.std.span.AwkSpan", true, null, null);
            mcopyed.add(tm);

            mcopyed.addSeparator();

            att = new HashMap<String, Object>(1);
            att.put("captype", "ICAP");
            tm = getSpanMI("Initial Cap", "CapSpan",
                    "multivalent.std.span.CapSpan", false, att, null);
            mcopyed.add(tm);
            att = new HashMap<String, Object>(1);
            att.put("captype", "CAP");
            tm = getSpanMI("ALL CAPS", "CapSpan",
                    "multivalent.std.span.CapSpan", false, att, null);
            mcopyed.add(tm);
            att = new HashMap<String, Object>(1);
            att.put("captype", "LC");
            tm = getSpanMI("lowercase", "CapSpan",
                    "multivalent.std.span.CapSpan", false, att, null);
            mcopyed.add(tm);

        }
        return mcopyed;
    }

    /**
     * This method initializes edit menu
     */
    private JMenu getMedit() {
        medit = new JMenu("Edit");
        medit.setMnemonic(java.awt.event.KeyEvent.VK_E);
        medit.setName("menuedit");
        medit.addMenuListener(new MenuListener() {
            public void menuCanceled(MenuEvent e) {
            }

            public void menuDeselected(MenuEvent e) {
            }

            public void menuSelected(MenuEvent e) {
                Span sel = getCurBr().getSelectionSpan();
                if (sel.isSet())
                    for (Component cmp : medit.getMenuComponents())
                        cmp.setEnabled(true);
                else
                    for (Component cmp : medit.getMenuComponents())
                        cmp.setEnabled(false);
                mselall.setEnabled(true);
                msearch.setEnabled(true);
                mpref.setEnabled(true);
                Transferable xfer = Toolkit.getDefaultToolkit()
                .getSystemClipboard().getContents(this);
                if (xfer != null
                        && xfer.isDataFlavorSupported(DataFlavor.stringFlavor))
                    mpaste.setEnabled(true);
                else
                    mpaste.setEnabled(false);
            }
        });

        mcut = new JMenuItem("Cut", FabIcons.getIcons().ICOCUT);// ,new
        mcut.setMnemonic(java.awt.event.KeyEvent.VK_X);
        setActionS(mcut, StandardEdit.MSG_CUT);
        medit.add(mcut);
        mcopy = new JMenuItem("Copy");// ,new
        mcopy.setMnemonic(java.awt.event.KeyEvent.VK_C);
        setActionS(mcopy, StandardEdit.MSG_COPY);
        medit.add(mcopy);
        mpaste = new JMenuItem("Paste", FabIcons.getIcons().ICOPASTE);// ,new
        mpaste.setMnemonic(java.awt.event.KeyEvent.VK_P);
        setActionS(mpaste, StandardEdit.MSG_PASTE);
        medit.add(mpaste);
        mselall = new JMenuItem("Select all");// ,new
        // ImageIcon(getClass().getResource("/res/exit.png")));
        mselall.setMnemonic(java.awt.event.KeyEvent.VK_A);
        setActionS(mselall, StandardEdit.MSG_SELECT_ALL);
        medit.add(mselall);

        medit.addSeparator();

        msearch = new JMenuItem("Search", new ImageIcon(getClass().getResource(
        "/res/search.png")));
        msearch.setMnemonic(java.awt.event.KeyEvent.VK_D);
        setAction(msearch, "search");
        medit.add(msearch);

        medit.addSeparator();

        mclear = new JMenuItem("Clear");// ,new
        mclear.setMnemonic(java.awt.event.KeyEvent.VK_D);
        setActionS(mclear, StandardEdit.MSG_CLEAR);
        medit.add(mclear);

        medit.addSeparator();

        mpref = new JMenuItem("Preferences", new ImageIcon(getClass()
                .getResource("/res/pref.png")));
        mpref.setMnemonic(java.awt.event.KeyEvent.VK_P);
        setAction(mpref, "preferences");
        medit.add(mpref);

        return medit;
    }

    /**
     * This method initializes edit menu
     */
    private JMenu getMTools() {
        mtools = new JMenu("Tools");
        mtools.setMnemonic(java.awt.event.KeyEvent.VK_T);
        mtools.setName("menutools");

        JCheckBoxMenuItem spi = new JCheckBoxMenuItem("Use emulated jpeg library");
        spi.setSelected(false); 
        spi.setMnemonic(java.awt.event.KeyEvent.VK_J);
        setAction(spi, "jpegSwitch");

        mtools.add(spi);
        return mtools;
    }


    List<ImageReaderSpi> imageSpiJpg = new LinkedList<ImageReaderSpi>();
    List<ImageReaderSpi> imageSpiJpgnested = new LinkedList<ImageReaderSpi>();
    public void jpegSwitch(){

        IIORegistry iio = IIORegistry.getDefaultInstance();
        Iterator<ImageReader> i = ImageIO.getImageReadersBySuffix("jpg");
        if (!useNestedProvider){
            useNestedProvider = true;
            while (i.hasNext()){
                ImageReader ir = i.next();
                if (!(ir.getOriginatingProvider() instanceof LibJPEGNestedReaderSPI)){
                    System.out.println("Unregister " + ir);
                    iio.deregisterServiceProvider(ir.getOriginatingProvider());
                    imageSpiJpg.add(ir.getOriginatingProvider());
                }
            }
            iio.registerServiceProviders(imageSpiJpgnested.iterator());
            imageSpiJpgnested.clear();
        } else {
            useNestedProvider= false;
            while (i.hasNext()){
                ImageReader ir = i.next();
                if ((ir.getOriginatingProvider() instanceof LibJPEGNestedReaderSPI)){
                    System.out.println("Unregister " + ir);
                    iio.deregisterServiceProvider(ir.getOriginatingProvider());
                    imageSpiJpgnested.add(ir.getOriginatingProvider());
                }
            }
            iio.registerServiceProviders(imageSpiJpg.iterator());
            imageSpiJpg.clear();
        }
    }
    /** created a dump of the current information, for debug */
    public void sysinfo() {
        Properties p = System.getProperties();
        File out;
        try {
            out = File.createTempFile("debug", ".txt");
            PrintStream ps = new PrintStream(out);
            Document doc = (Document) getCurBr().getRoot().findBFS("content");
            ps.println("URI = " + doc.getURI().toString());
            for (Entry<Object, Object> e : p.entrySet())
                ps.println(e.getKey() + " = " + e.getValue());
            Map<String, String> m = System.getenv();
            for (Entry<String, String> e : m.entrySet())
                ps.println(e.getKey() + " = " + e.getValue());
            openNewTab(out.toURI().toString());

        } catch (IOException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }

    }

    /** This method initializes File menu */
    private JMenu getMfile() {
        mfile = new JMenu("File");
        mfile.setMnemonic(java.awt.event.KeyEvent.VK_F);
        mfile.setName("menufile");
        //		neww = new JMenuItem();
        //		neww.setText("New window");
        //		neww.setMnemonic(java.awt.event.KeyEvent.VK_N);
        //		setAction(neww, "newWindow");
        //		mfile.add(neww);
        newtab = new JMenuItem("New tab", new ImageIcon(getClass().getResource(
        "/res/ntico.png")));
        newtab.setMnemonic(java.awt.event.KeyEvent.VK_T);
        setAction(newtab, "openNewTab");
        mfile.add(newtab);
        mfile.addSeparator();
        open = new JMenuItem("Open file", new ImageIcon(getClass().getResource(
        "/res/open.png")));
        open.setMnemonic(java.awt.event.KeyEvent.VK_O);
        setAction(open, "openFileDialog");
        mfile.add(open);
        msave = new JMenuItem("Save as", FabIcons.getIcons().ICOSAVE);
        msave.setMnemonic(java.awt.event.KeyEvent.VK_S);
        setActionS(msave, SaveAs.MSG_SAVE_AS);
        mfile.add(msave);
        mfile.addSeparator();
        mprintp = new JMenuItem("Print preview", new ImageIcon(getClass()
                .getResource("/res/print_preview.png")));
        mprintp.setMnemonic(java.awt.event.KeyEvent.VK_P);
        setAction(mprintp, "printPreview");
        mfile.add(mprintp);
        mprint = new JMenuItem("Print", new ImageIcon(getClass().getResource(
        "/res/printer.png")));
        mprint.setMnemonic(java.awt.event.KeyEvent.VK_P);
        setAction(mprint, "print");
        mfile.add(mprint);
        mfile.addSeparator();
        mclose = new JMenuItem("Close Tab", new ImageIcon(getClass()
                .getResource("/res/close.png")));
        mclose.setMnemonic(java.awt.event.KeyEvent.VK_C);
        setAction(mclose, "closeTab");
        mfile.add(mclose);
        mexit = new JMenuItem("Quit", new ImageIcon(getClass().getResource(
        "/res/exit.png")));
        mexit.setMnemonic(java.awt.event.KeyEvent.VK_X);
        setAction(mexit, "closeWindow");

        mfile.add(mexit);

        return mfile;
    }

    /** This method initializes Help menu */
    private JMenu getMhelp() {
        if (mhelp == null) {
            mhelp = new JMenu();
            mhelp.setText("Help");
            mhelp.setMnemonic(java.awt.event.KeyEvent.VK_H);
            mhelp.setName("menuhelp");
            JMenuItem tm;
            tm = new JMenuItem("About Fab4", new ImageIcon(getClass()
                    .getResource("/res/about.png")));
            tm.setMnemonic(java.awt.event.KeyEvent.VK_F);
            tm.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    openNewTab();
                    ptabs.setSelectedIndex(totalFrameBrowsers);
                }
            });
            mhelp.add(tm);
            tm = new JMenuItem("Annotations user manual", new ImageIcon(
                    getClass().getResource("/res/help.png")));
            tm.setMnemonic(java.awt.event.KeyEvent.VK_H);
            tm.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    openNewTab();
                    ptabs.setSelectedIndex(totalFrameBrowsers);
                    open("systemresource:/sys/Manual.html");
                }
            });
            mhelp.add(tm);

            tm = new JMenuItem("Development home page");
            tm.setMnemonic(java.awt.event.KeyEvent.VK_D);
            tm.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    open("http://code.google.com/p/fab4browser/");
                }
            });
            mhelp.add(tm);
            tm = new JMenuItem("Multivalent home page");
            tm.setMnemonic(java.awt.event.KeyEvent.VK_M);
            tm.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    open("http://multivalent.sourceforge.net/");
                }
            });
            mhelp.add(tm);

        }
        return mhelp;
    }

    /** This method initializes Lens menu */
    private JMenu getMlens() {

        if (mlens == null) {
            mlens = new JMenu("Lens");
            mlens.setMnemonic(java.awt.event.KeyEvent.VK_L);
            JMenuItem tm;
            tm = new JMenuItem("Speed Read");
            tm.setMnemonic(java.awt.event.KeyEvent.VK_M);
            setActionB(tm, "uk.ac.liverpool.fab4.lens.SpeedRead");
            mlens.add(tm);
            tm = new JMenuItem("Voice Read");
            tm.setMnemonic(java.awt.event.KeyEvent.VK_M);
            setActionB(tm, "uk.ac.liv.freettsMA.FreeTTSBehaviour");
            mlens.add(tm);

            tm = new JMenuItem("Magnify");
            tm.setMnemonic(java.awt.event.KeyEvent.VK_M);
            setActionB(tm, "multivalent.std.lens.Magnify");
            mlens.add(tm);
            tm = new JMenuItem("Mirror");
            setActionB(tm, "multivalent.std.lens.Mirror");
            tm.setMnemonic(java.awt.event.KeyEvent.VK_I);
            mlens.add(tm);
            tm = new JMenuItem("Plain view");
            tm.setMnemonic(java.awt.event.KeyEvent.VK_I);
            setActionB(tm, "multivalent.std.lens.PlainView");
            mlens.add(tm);
            tm = new JMenuItem("Blur");
            tm.setMnemonic(java.awt.event.KeyEvent.VK_I);
            setActionB(tm, "multivalent.std.lens.Blur");
            mlens.add(tm);
            tm = new JMenuItem("Sharpen");
            tm.setMnemonic(java.awt.event.KeyEvent.VK_I);
            setActionB(tm, "multivalent.std.lens.Sharpen");
            mlens.add(tm);
            tm = new JMenuItem("Bounds");
            setActionB(tm, "multivalent.devel.lens.Bounds");
            tm.setMnemonic(java.awt.event.KeyEvent.VK_I);
            mlens.add(tm);
            tm = new JMenuItem("Brighten");
            tm.setMnemonic(java.awt.event.KeyEvent.VK_I);
            setActionB(tm, "uk.ac.liverpool.fab4.lens.Brighten");
            mlens.add(tm);
            tm = new JMenuItem("Darken");
            tm.setMnemonic(java.awt.event.KeyEvent.VK_I);
            setActionB(tm, "multivalent.std.lens.Darken");
            mlens.add(tm);
            tm = new JMenuItem("Ruler");
            tm.setMnemonic(java.awt.event.KeyEvent.VK_I);
            setActionB(tm, "multivalent.devel.lens.Ruler");
            mlens.add(tm);
            tm = new JMenuItem("Key hole");
            tm.setMnemonic(java.awt.event.KeyEvent.VK_I);
            setActionB(tm, "uk.ac.liverpool.fab4.lens.KeyHole");
            mlens.add(tm);
            tm = new JMenuItem("Show OCR");
            tm.setMnemonic(java.awt.event.KeyEvent.VK_I);
            tm.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    Map<String, Object> hm = new HashMap<String, Object>(3);
                    hm.put("signal", "viewOcrAs");
                    hm.put("value", "ocr");
                    hm.put("title", "Show OCR");
                    Behavior.getInstance("ShowOcr",
                            "multivalent.std.lens.SignalLens", null, hm,
                            getCurBr().getRoot().getLayer(Layer.SYSTEM));
                }
            });
            mlens.add(tm);
        }
        return mlens;
    }

    /** This method initializes Style menu */
    private JMenu getMstyle() {
        if (mstyle == null) {
            mstyle = new JMenu("Style");
            mstyle.setMnemonic(java.awt.event.KeyEvent.VK_S);
            JMenuItem tm;

            tm = getSpanMI("Blink", "blinkSpan",
                    "multivalent.std.span.BlinkSpan", false, null, null);
            mstyle.add(tm);
            tm = getSpanMI("Underline", "underlineSpan",
                    "multivalent.std.span.UnderlineSpan", false, null, FabIcons
                    .getIcons().ICOUND);
            mstyle.add(tm);
            tm = getSpanMI("Overstrike", "OverstrikeSpan",
                    "multivalent.std.span.OverstrikeSpan", false, null,
                    FabIcons.getIcons().ICOSTR);
            mstyle.add(tm);
            tm = getSpanMI("Invisible", "InvisibleSpan",
                    "multivalent.std.span.InvisibleSpan", false, null, null);
            mstyle.add(tm);
            tm = getSpanMI("Elide", "ElideSpan",
                    "multivalent.std.span.ElideSpan", false, null, null);
            mstyle.add(tm);

            mstyle.addSeparator();

            tm = getSpanMI("Plain", "PlainSpan",
                    "multivalent.std.span.PlainSpan", false, null, null);
            mstyle.add(tm);
            tm = getSpanMI("Italic", "ItalicSpan",
                    "multivalent.std.span.ItalicSpan", false, null, FabIcons
                    .getIcons().ICOITA);
            mstyle.add(tm);
            tm = getSpanMI("Bold", "BoldSpan", "multivalent.std.span.BoldSpan",
                    false, null, FabIcons.getIcons().ICOBOLD);
            mstyle.add(tm);

        }
        return mstyle;
    }

    /** This method initializes View menu */
    private JMenu getMview() {
        if (mview == null) {
            mview = new JMenu("View");
            mview.setMnemonic(java.awt.event.KeyEvent.VK_V);
            mview.setName("menuview");
            JMenuItem tm;
            tm = new JMenuItem("Full screen", new ImageIcon(getClass()
                    .getResource("/res/maximize.png")));
            tm.setMnemonic(java.awt.event.KeyEvent.VK_S);
            setActionS(tm, FullScreen.MSG_START);
            mview.add(tm);
            tm = new JMenuItem("Take a screenshot");
            tm.setMnemonic(java.awt.event.KeyEvent.VK_M);
            setAction(tm, "sshot");
            mview.add(tm);
            JMenuItem sysinfo = new JMenuItem();
            sysinfo.setText("Show debug information");
            sysinfo.setMnemonic(java.awt.event.KeyEvent.VK_S);
            setAction(sysinfo, "sysinfo");

            mview.add(sysinfo);

        }
        return mview;
    }

    /** This method initializes the status (lower) panel */
    private JPanel getStatusPanel() {
        if (pstatus == null) {
            pstatus = new JPanel(new BorderLayout(2, 2));
            pstatus.setName("southpanel");
            pright = getPageButtons();
            pstatus.add(pright, BorderLayout.EAST);

            pstatus.add(getBottomLeftToolbar(), BorderLayout.WEST);
            statusLine = new JLabel(" ", SwingConstants.LEFT);
            statusLine.setFont(statusLine.getFont().deriveFont(12.0f));
            //statusLine.setBorder(new EmptyBorder(0,0,0,0));
            JPanel p2 = new JPanel (new BorderLayout(0, 0));
            p2.setBorder(new EmptyBorder(0,0,0,0));
            p2.add(statusLine, BorderLayout.CENTER);
            p2.add(getMediaControl(), BorderLayout.EAST);
            //getMediaControl();
            pstatus.add(p2, BorderLayout.CENTER);
            pstatus.validate();

        }
        return pstatus;
    }

    private JToolBar getPageButtons() {
        pright = new JToolBar();
        fp = getButton("first");
        setActionS(fp, Multipage.MSG_FIRSTPAGE);
        fp.setEnabled(false);
        pright.add(fp);
        pp = getButton("pp");
        setActionS(pp, Multipage.MSG_PREVPAGE);
        pp.setEnabled(false);
        pright.add(pp);
        pagec = new JTextField("", 3);
        pagec.setFont(pagec.getFont().deriveFont(10.0f));
        pagec.addMouseListener(new MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getButton() == java.awt.event.MouseEvent.BUTTON1)
                    pagec.selectAll();
            }
        });
        pagec.addKeyListener(new KeyAdapter() {
            public void keyReleased(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER)
                    try {
                        Integer c = new Integer(pagec.getText());
                        getCurBr().eventq(Multipage.MSG_GOPAGE, c);
                    } catch (NumberFormatException ef) {
                        updatePagec();
                    }
            }
        });
        pagec.setMargin(new java.awt.Insets(0, 0, 0, 0));
        pright.add(pagec);
        paget = new JLabel("/");
        paget.setEnabled(false);
        paget.setFont(pagec.getFont().deriveFont(10.0f));
        pright.add(paget);
        np = getButton("np");
        setActionS(np, Multipage.MSG_NEXTPAGE);
        np.setEnabled(false);
        pright.add(np);
        lp = getButton("last");
        setActionS(lp, Multipage.MSG_LASTPAGE);
        lp.setEnabled(false);
        pright.add(lp);
        pright.setFloatable(false);
        pright.setRollover(true);
        pright.doLayout();
        return pright;
    }

    private JToolBar getMediaControl() {
        if (pmedia == null)
            pmedia = new MediaUI(this);
        return pmedia;
    }

    /** This method initializes jTabbedPane, the main content area. */
    private JTabbedPane getPtabs() {
        if (ptabs == null) {
            ptabs = new JTabbedPaneClosing();
            ptabs.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0,
                    0, 0));

            ((JTabbedPaneClosing) ptabs).addCloseListener(this);
            tabpopup = new JPopupMenu();
            JMenuItem mclosetab = new JMenuItem("Close this tab",
                    new ImageIcon(getClass().getResource("/res/close.png")));
            setAction(mclosetab, "closeTab");
            tabpopup.add(mclosetab);

            JMenuItem mcloseatab = new JMenuItem("Close all tabs",
                    new ImageIcon(getClass().getResource("/res/close.png")));
            setAction(mcloseatab, "closeAllTab");
            tabpopup.add(mcloseatab);

            tabpopup.pack();
            ptabs.addMouseListener(new PopupListener(tabpopup));
            ptabs.addMouseListener(new MouseAdapter() {

                public void mouseClicked(java.awt.event.MouseEvent e) {
                    if (e.getButton() == java.awt.event.MouseEvent.BUTTON1
                            && e.getClickCount() == 2) {
                        openNewTab();
                        ptabs.setSelectedIndex(totalFrameBrowsers);
                        e.consume();
                    }
                }
            });
            ptabs.addChangeListener(new ChangeListener() {
                public void stateChanged(ChangeEvent e) {
                    // System.out.println(e);
                    currentBrowser = ptabs.getSelectedIndex();
                    // System.out.println(currentBrowser);
                    String testo;
                    Document doc = getCurDoc();
                    testo = doc.getAttr(Document.ATTR_URI);
                    topButtonBar.address.setText(testo);
                    testo = doc.getAttr(Document.ATTR_TITLE);
                    setTitle("  " + testo + " - " + Fab4.title);
                    topButtonBar.urlbutton.setIcon(ptabs
                            .getIconAt(currentBrowser));
                    getCurBr().eventq(UiBehavior.MSG_TAB_CHANGED, null);
                }
            });

        }

        return ptabs;
    }

    /** inizialises the top panel */

    private JPanel getPTop() {
        if (topButtonBar == null)
            topButtonBar = new TopButtobBar(this);
        return topButtonBar;
    }

    /**
     * @param name
     * @param cln
     */
    JMenuItem getSpanMI(String mtitle, final String name, final String cln,
            final boolean edit, final Map<String, Object> att, Icon ico) {
        JMenuItem tm;
        tm = new JMenuItem(mtitle, ico);
        tm.addActionListener(new SpanActionListener(att, name, cln, edit, this,
                tm));
        return tm;
    }

    /**
     * @param name
     * @param cln
     */
    JButton getSpanButton(String mtitle, final String name, final String cln,
            final boolean edit, final Map<String, Object> att, Icon ico) {
        JButton tm;
        tm = new JButton(mtitle, ico);
        tm.addActionListener(new SpanActionListener(att, name, cln, edit, this,
                tm));
        return tm;
    }


    boolean useNestedProvider;
    /**
     * 
     * Populates the main fame of Fab4
     * 
     * @throws InvocationTargetException
     * @throws InterruptedException
     * 
     */

    private void populate(String target) throws InterruptedException,
    InvocationTargetException {
        IIORegistry iio = IIORegistry.getDefaultInstance();
        System.out.println("ImageIO fix");
        ImageIO.scanForPlugins();
        boolean found = false;
        Iterator<ImageReader> i = ImageIO.getImageReadersBySuffix("jpg");
        useNestedProvider = false;
        while (i.hasNext()){
            ImageReader ir = i.next();
            if ((ir.getOriginatingProvider().getVendorName().contains("fab4"))){
                System.out.println("Unregister " + ir);
                found = true;
                iio.deregisterServiceProvider(ir.getOriginatingProvider());
                imageSpiJpgnested.add(ir.getOriginatingProvider());
            }
        }
        if (!found){
            imageSpiJpgnested.add(new LibJPEGNestedReaderSPI());
        }
//        i = ImageIO.getImageReadersBySuffix("jp2");
//        while (i.hasNext()){
//            ImageReader ir = i.next();
//            if ((!ir.getOriginatingProvider().getVendorName().contains("fab4"))){System.out.println("Unregister " + ir);
//            found = true;
//            iio.deregisterServiceProvider(ir.getOriginatingProvider());
//            imageSpiJpgnested.add(ir.getOriginatingProvider());
//            }
//        }

        br = new Vector<Browser>(10);
        Fab4.fr.add(this);
        final Fab4 t = this;

        // System.out.println("Target: "+ target2);
        final String t2 = target;
        if (!SwingUtilities.isEventDispatchThread())
            SwingUtilities.invokeAndWait(new Runnable() {
                public void run() {
                    innerPopulate(t2);
                }
            });
        else
            innerPopulate(target);

        final KeyboardFocusManager km = KeyboardFocusManager
        .getCurrentKeyboardFocusManager();
        km.addKeyEventDispatcher(new KeyEventDispatcher() {

            public boolean dispatchKeyEvent(KeyEvent e) {
                Component component2 = e.getComponent();
                if (!(component2 instanceof JTextComponent)
                        && getContentPane().isAncestorOf(component2)
                        // e.getID()==KeyEvent.KEY_PRESSED
                ) {// && (e.getModifiers()==0 || e.isShiftDown()) ) {
                    Object o = e.getSource();

                    e.setSource(t.getContentPane());
                    km.redispatchEvent(t.getContentPane(), e);
                    e.setSource(getCurBr());

                    km.redispatchEvent(getCurBr(), e);
                    final BindingsDefault bu = (BindingsDefault) Fab4utils
                    .getBe("BindingsDefault", getCurBr().getRoot()
                            .getLayers());
                    bu.eventAfter(e, BindingsDefault._DUMMY, null);
                    e.setSource(o);
                    return false;
                }
                return false;
            }
        });
    }

    /** Sets the action command and the action listener */
    void setAction(AbstractButton c, String s) {
        c.setActionCommand(s);
        c.addActionListener(this);
    }

    /**
     * Sets the action command and the action litener to a semantic event with
     * the message specified
     */
    private void setActionB(AbstractButton c, String message) {
        c.setActionCommand("@" + message);
        c.addActionListener(this);
    }

    /**
     * Sets the action command and the action litener to a semantic event with
     * the message specified
     */
    void setActionS(AbstractButton c, String message) {
        c.setActionCommand("#" + message);
        c.addActionListener(this);
    }

    /** inizialises the search */
    protected void search() {
        /* we create the search dialog only once... */
        if (searchFrame == null) {
            searchFrame = new JDialog(this, "Search");
            JPanel p2 = new JPanel(new FlowLayout(FlowLayout.CENTER, 2, 2));
            JLabel info = new JLabel("Search for: ");
            p2.add(info);
            final JTextField f = new JTextField(14);
            f.addKeyListener(new KeyAdapter() {
                public void keyReleased(KeyEvent e) {
                    if (e.getKeyCode() == KeyEvent.VK_ENTER)
                        getCurBr().eventq(Search.MSG_SEARCHFOR, f.getText());
                    super.keyReleased(e);
                }
            });
            p2.add(f);
            JButton up = getButtonBorder("up");
            setActionS(up, Search.MSG_PREV);
            p2.add(up);
            JButton dw = getButtonBorder("down");
            setActionS(dw, Search.MSG_NEXT);
            p2.add(dw);
            searchFrame.add(p2, BorderLayout.CENTER);
            searchFrame.pack();
            searchFrame.setResizable(false);
            searchFrame.setVisible(true);
            searchFrame.addWindowListener(new WindowAdapter() {
                public void windowClosing(WindowEvent e) {
                    getCurBr().eventq(Search.MSG_SEARCHFOR, null);
                    searchFrame.setVisible(false);

                }
            });
        } else {
            searchFrame.setVisible(true);
            searchFrame.toFront();
        }
    }

    /** move backward in history */
    void back() {
        Document doc = (Document) getCurBr().getRoot().findBFS("content");
        getCurBr().setCurDocument(doc);
        getCurBr()
        .event(new SemanticEvent(this, ForwardBack.MSG_BACKWARD, doc));
    }

    /** move forward in history */
    void forward() {
        Document doc = (Document) getCurBr().getRoot().findBFS("content");
        getCurBr().setCurDocument(doc);
        getCurBr().event(new SemanticEvent(this, ForwardBack.MSG_FORWARD, doc));

    }

    /** This method creates a new browser */
    synchronized JSplitPane createBrowser() {
        JPanel innerPanel = new JPanel(new BorderLayout(0, 0));

        totalFrameBrowsers++;

        Browser theBrowser = Fab4.mv.getBrowser("name" + Fab4.contin++, "Fab4", false);
        theBrowser.putClientProperty(PANEL, innerPanel);
        br.add(theBrowser);
        theBrowser.setOpaque(true);
        innerPanel.add(theBrowser, BorderLayout.CENTER);
        FabScrollbars h = new FabScrollbars(VScrollbar.HORIZONTAL);
        FabScrollbars v = new FabScrollbars(VScrollbar.VERTICAL);
        ((Document) theBrowser.getRoot().findBFS("content")).setHsb(h);
        ((Document) theBrowser.getRoot().findBFS("content")).setVsb(v);
        innerPanel.add(h.s, BorderLayout.SOUTH);
        innerPanel.add(v.s, BorderLayout.EAST);
        AnnotationSidePanel annoPanel;
        JSplitPane splitp;
        try {
            Class.forName("uk.ac.liv.c3connector.DistributedPersonalAnnos");
            annoPanel = new AnnotationSidePanel(theBrowser, this);
            AnnoToolBar atb = new AnnoToolBar(theBrowser, this, annoPanel);
            atb.setFloatable(true);
            innerPanel.add(atb, BorderLayout.NORTH);
            annoPanels.put(theBrowser, annoPanel);
        } catch (Exception e){
            annoPanel = null;
        }
        splitp = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, annoPanel,
                innerPanel);
        splitp.setContinuousLayout(true);
        splitp.setOneTouchExpandable(false);
        splitp.setOpaque(true);
        getCurDoc().setScrollbarShowPolicy(VScrollbar.SHOW_NEVER);
        updateFormatted(theBrowser);
        return splitp;
    }

    /** This method initializes generic 3 status buttons for the tool bar */
    JButton getButton(String name) {
        JButton tm = new JButton();
        try {
            URL u = getClass().getResource("/res/" + name + "1.png");
            if (u == null)
                u = getClass().getResource("/res/" + name + "1.gif");
            if (u == null)
                u = getClass().getResource("/res/" + name);
            tm.setIcon(new ImageIcon(u));
        } catch (NullPointerException e) {
            tm.setText(name);
        }
        try {
            tm.setRolloverIcon(new ImageIcon(getClass().getResource(
                    "/res/" + name + "2.png")));
        } catch (NullPointerException e) {
        }
        try {
            tm.setDisabledIcon(new ImageIcon(getClass().getResource(
                    "/res/" + name + "3.png")));
        } catch (NullPointerException e) {
        }
        if (Fab4.basicButtonUI == null)
            Fab4.basicButtonUI = new BasicButtonUI();
        tm.setUI(Fab4.basicButtonUI);
        tm.setMargin(new java.awt.Insets(0, 0, 0, 0));
        tm.setBorderPainted(false);
        return tm;
    }

    JButton getButton(String name, String tooltip) {
        JButton tm = getButton(name);
        tm.setToolTipText(tooltip);
        return tm;
    }

    void go() {
        String testo = topButtonBar.address.getText();
        if (!testo.startsWith("http") && !testo.startsWith("ftp")
                && !(testo.indexOf("://") != -1)
                && !(testo.indexOf(":/") != -1)) {
            testo = "http://" + testo;
            topButtonBar.address.setText(testo);
        }
        open(testo, currentBrowser);
		///SAM
		try {
			Class disAnnos = Class.forName("uk.ac.liv.c3connector.DistributedPersonalAnnos");
			String curServer = (String) disAnnos.getDeclaredMethod("getCurrentRemoteServer").invoke(null);			
			if(curServer.equals("REST")){
				Class parameterTypes = Class.forName("java.lang.String");			
				disAnnos.getDeclaredMethod("askForDocumentInfo", parameterTypes ).invoke(null, testo);
			}
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		///
    }

    /**
     * Initializes the bookmarks menu.
     */
    void initBookmarks() {
        if (Fab4.bm == null)
            Fab4.bm = (Bookmark) getBe("Bookmarks");
        JMenuItem tm;
        tm = new JMenuItem("Add Bookmark", FabIcons.getIcons().ICOBMK);
        tm.setMnemonic(java.awt.event.KeyEvent.VK_B);
        tm.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                getCurBr().eventq(Bookmark.MSG_ADD, null);
                JMenuItem tm2;
                tm2 = new JMenuItem(getCurDoc().getAttr(Document.ATTR_TITLE));
                tm2.setActionCommand(getCurDoc().getAttr(Document.ATTR_URI));
                Icon i = Fab4utils.getFavIcon(getCurDoc().getURI());
                if (i == null)
                    i = FabIcons.getIcons().ICOURL;
                tm2.setIcon(i);
                tm2.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e2) {
                        open(e2.getActionCommand());
                    }
                });
                mbookmarks.add(tm2);
                try {
                    Fab4.bm.write();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }

            }
        });
        mbookmarks.add(tm);
        tm = new JMenuItem("Manage Bookmarks", new ImageIcon(getClass()
                .getResource("/res/book_edit.png")));
        tm.setMnemonic(java.awt.event.KeyEvent.VK_M);
        tm.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                new EditBookmarks(Fab4.bm).setVisible(true);
            }
        });
        mbookmarks.add(tm);
        mbookmarks.addSeparator();
        mbookmarks.addSeparator();

        Thread bookms = new Thread(new Runnable() {
            public void run() {
                try {
                    JMenuItem tm2;

                    for (Bookmark.Entry e : Bookmark.bookmarks_) {
                        tm2 = new JMenuItem(e.title);
                        tm2.setActionCommand(e.uri);
                        Icon ii = null;
                        try {
                            ii = Fab4utils.getFavIcon(new URI(e.uri));
                        } catch (URISyntaxException e1) {
                        }
                        if (ii == null)
                            ii = Fab4utils.getIconForExt(null, e.uri);
                        if (ii == null)
                            ii = FabIcons.getIcons().ICOURL;
                        tm2.setIcon(ii);
                        tm2.addActionListener(new ActionListener() {
                            public void actionPerformed(ActionEvent e2) {
                                open(e2.getActionCommand());
                            }
                        });
                        mbookmarks.add(tm2);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        bookms.start();
    }

    /** initialises the Fab4 frame */
    void initialize() {
        setContentPane(getJContentPane());
        setJMenuBar(getJJMenuBar());
        setTitle(Fab4.title);
        pack();

        Dimension s = Toolkit.getDefaultToolkit().getScreenSize();
        s.width-=100;
        s.height-=80;

        Point p = null;
        String w = Fab4.prefs.getProps().getProperty("wwidth");
        if (w!=null)
            try {
                s.width = Integer.parseInt(w);
                String h = Fab4.prefs.getProps().getProperty("wheight");
                String x = Fab4.prefs.getProps().getProperty("wx");
                String y = Fab4.prefs.getProps().getProperty("wy");
                s.height = Integer.parseInt(h);
                p= new Point (Integer.parseInt(x),Integer.parseInt(y));
            }catch (Exception e){}
            setSize(s.width, s.height);
            if (p!=null)
                setBounds(p.x, p.y, s.width, s.height);
            else
                setLocationRelativeTo(null);
            validate();
            setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
            addWindowListener(this);
            setVisible(true);
    }

    void memgc() {
        System.gc();
        System.out.println((Runtime.getRuntime().totalMemory() - Runtime
                .getRuntime().freeMemory()) / 1000
                + "|" + Runtime.getRuntime().maxMemory() / 1000);

    }

    /** opens a specific URL */
    void open(String url) {
        getCurBr().setCurDocument(getCurDoc());
        getCurBr().event(new SemanticEvent(getCurBr(), Document.MSG_OPEN, url));
    }

    void open(String url, int cu) {
        Browser toOpen = br.get(cu);
        Document doc = (Document) toOpen.getRoot().findBFS("content");
        try {
            doc.uri = new URI("system://toload");
        } catch (URISyntaxException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        toOpen.setCurDocument(doc);
        toOpen.eventq(new SemanticEvent(toOpen, Document.MSG_OPEN, url));
    }

    void reload() {
        Document doc = (Document) getCurBr().getRoot().findBFS("content");
        getCurBr().setCurDocument(doc);
        getCurBr().event(new SemanticEvent(this, Document.MSG_RELOAD, doc));

    }

    /** takes a screenshot of the current desktop */
    void sshot() {
        byte[] image = Fab4utils.screenShot("jpg", Fab4.getMVFrame(getCurBr()));
        if (image != null) {
            JFileChooser jc = new JFileChooser();
            jc.setDialogType(JFileChooser.SAVE_DIALOG);
            jc.setDialogTitle("Save screenshot");

            File dir = jc.getCurrentDirectory();

            String file = "fab4_Scrshot.jpg";

            File newpath = new File(dir, file);
            // System.out.println("select "+newpath+", file="+uri.getFile()+" =>
            // "+file);
            jc.setSelectedFile(newpath);

            if (jc.showSaveDialog(Fab4.getMVFrame(getCurBr())) == JFileChooser.APPROVE_OPTION) {
                File out = jc.getSelectedFile();
                FileOutputStream os = null;
                try {
                    os = new FileOutputStream(out);
                    os.write(image);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    try {
                        os.close();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

            }
        }

    }

    /** toggles the display of the annotation list side panel */
    void toggleAnnoPanel() {
        JPanel annoPanel = annoPanels.get(getCurBr());
        if (annoPanel == null)
            return;
        JSplitPane sp = (JSplitPane) annoPanel.getParent();
        if (verticalAnnoPanel) {
            System.out.println(sp.getDividerLocation());
            if (sp.getDividerLocation() > sp.getMaximumDividerLocation()) {
                sp.setDividerLocation(sp.getMaximumDividerLocation());
                // System.out.println("1");
                // sp.setDividerLocation(-1);
                banno.setIcon(FabIcons.getIcons().BICO1);
            } else {
                sp.setDividerLocation(999999999);
                banno.setIcon(FabIcons.getIcons().BICO2);
                // System.out.println("" +
                // "2");

            }

        } else if (sp.getDividerLocation() <= 1) {
            sp.setDividerLocation(-1);
            banno.setIcon(FabIcons.getIcons().BICO1);
        } else {
            sp.setDividerLocation(0);
            banno.setIcon(FabIcons.getIcons().BICO2);
        }
    }

    /** part of Populate, for a frame */
    private void innerPopulate(String target) {
        if (Fab4.mv == null) {
            Fab4.mv = Multivalent.getInstance();
            Fab4.mv.getGenreMap().remove("");
            String hc = Fab4.prefs
            .getProps()
            .getProperty("HighlightColors",
            "Yellow Orange Green Blue Red Black White Gray Purple Pink");
            String newCol = Fab4.prefs
            .getProps()
            .getProperty(
                    "DefineColors",
            "Aquamarine #70DB93 Copper #B87333 Orchid #DB70DB Plum #EAADEA Violet #4F2F4F Green_Yellow #93DB70 OldGold #CFB53B");
            addColors(newCol);

            Fab4.mv.putPreference(BackgroundSpan.ATTR_COLORS, hc);

            // Handle webstart breaking the system: URLs
            try {
                Class.forName("javax.jnlp.BasicService");
                Fab4.mv.putPreference("homepage",
                "http://bodoni.lib.liv.ac.uk/fab4/About.html");
                Fab4.JAVA_WS = true;
            } catch (ClassNotFoundException e) {
                Fab4.JAVA_WS = false;
            }
            Fab4utils.readIcoCache();
            Toolkit.getDefaultToolkit().setDynamicLayout(true);
            UIManager.addPropertyChangeListener(new UISwitchListener(
                    getRootPane()));
        }
        initialize();
        setIconImage(FabIcons.getIcons().FAB4ICO);
        ptabs.addTab("---", null, createBrowser(), null);

        String ta = target == null ? Fab4.mv.getPreference("homepage", null)
                : target;

        open(ta, totalFrameBrowsers);
        Browser toOpen = br.get(totalFrameBrowsers);
        updateDocOpened(toOpen);
        updateFormatted(toOpen);
        initBookmarks();
    }

    /**
     * adds color definition to the Colors utility class of Multivalent, to
     * enable user selected colors for annotations (still lacking UI for
     * definition)
     */
    private void addColors(String s) {
        StringTokenizer st = new StringTokenizer(s, " ");
        while (st.hasMoreTokens()) {
            String name = st.nextToken();
            if (!st.hasMoreTokens())
                break;
            String col = st.nextToken();
            Colors.addColor(name, col);
            // System.out.println(name+" "+col);
        }
    }

}