package uk.ac.liverpool.fab4.jreality;

import static de.jreality.shader.CommonAttributes.DIFFUSE_COLOR;
import static de.jreality.shader.CommonAttributes.POLYGON_SHADER;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTree;
import javax.swing.SwingConstants;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import multivalent.CLGeneral;
import multivalent.Document;
import multivalent.INode;
import multivalent.Layer;
import multivalent.MediaAdaptor;
import multivalent.StyleSheet;
import phelps.awt.Colors;
import uk.ac.liv.jt.segments.JTSceneGraphComponent;
import uk.ac.liverpool.fab4.Fab4;
import uk.ac.liverpool.fab4.jreality.Navigator.SelectionEvent;
import uk.ac.liverpool.fab4.jreality.Navigator.SelectionListener;
import de.jreality.scene.Appearance;
import de.jreality.scene.SceneGraphComponent;
import de.jreality.scene.tool.InputSlot;
import de.jreality.scene.tool.ToolContext;
import de.jreality.tools.ActionTool;
import de.jreality.ui.treeview.SceneTreeModel;
import de.jreality.ui.viewerapp.Selection;
import de.jreality.ui.viewerapp.SelectionManagerImpl;

public class JRealityMA extends MediaAdaptor {

    SoftViewerLeaf l;
    Document doc;
    JPanel f;
    private Navigator n;
    protected TableCellRenderer cellrender = new CustomTableCellRenderer();

    @Override
    public void close() throws IOException {
        super.close();
        if (l != null) {

            n.destroy();

            doc.removeAllChildren();
            l.dispose();

            l.close();

            JSplitPane p = (JSplitPane) getBrowser().getClientProperty(
                    Fab4.PANEL);
            p.remove(f);

        }
        l = null;
        f = null;
        n = null;

        System.gc();
    }

    @Override
    public Object parse(INode parent) throws Exception {

        doc = parent.getDocument();
        if (doc.getFirstChild() != null) {
            doc.clear();
        }
        Layer ll = doc.getLayer(Layer.PERSONAL);
        if (ll != null) {
            ll.destroy();
            // doc.putAttr(TimedMedia.TIMEDMEDIA, l);
        }
        final StyleSheet ss = doc.getStyleSheet();
        CLGeneral gs = new CLGeneral();
        gs.setForeground(Colors.getColor(getAttr("foreground"), Color.WHITE));
        gs.setBackground(Colors.getColor(getAttr("background"), Color.gray));
        gs.setPadding(8);
        ss.put(doc.getName(), gs);
        Map<String, Object> attr = new HashMap<String, Object>(1);
        attr.put("resize", true);
        attr.put("embedded", false);
        attr.put("uri", getURI().toString());
        doc.uri = getURI();
        if (getURI() == null) {
            // new LeafUnicode("File not found",attr,parent);
            throw new IOException("File not found");
        }
        attr.put("uri", getURI());
        l = new SoftViewerLeaf("JReality3D", attr, parent);

        SceneGraphComponent sgc = null;
        sgc = l.getSGC();

        f = new JPanel();

        n = new Navigator(l);

        addGeometryActions(sgc, n, l);

        Component c = n.getComponent();
        f.setLayout(new BorderLayout(5, 5));
        f.add(c, BorderLayout.CENTER);
        JTree tree = n.getSceneTree();
        if (sgc instanceof JTSceneGraphComponent) {

            TreeSelectionModel tsm = tree.getSelectionModel();

            final JTable list = new JTable();

            JScrollPane scroller = new JScrollPane(list);
            tsm.addTreeSelectionListener(new SelectionListener() {

                @Override
                public void selectionChanged(SelectionEvent e) {

                    final Selection currentSelection = e.getSelection();
                    if (currentSelection != null) {
                        list.setModel(new AbstractTableModel() {
                            public boolean isCellEditable(int rowIndex,
                                    int columnIndex) {
                                return true;
                            }

                            public Object getValueAt(int rowIndex,
                                    int columnIndex) {
                                if (currentSelection != null)
                                    if (currentSelection.getLastComponent() instanceof JTSceneGraphComponent) {
                                        JTSceneGraphComponent s = (JTSceneGraphComponent) currentSelection
                                                .getLastComponent();
                                        if (s != null)
                                            if (s.properties != null)
                                                if (columnIndex < 2
                                                        && rowIndex < s.properties.length
                                                        && s.properties[rowIndex][columnIndex] != null)
                                                    return s.properties[rowIndex][columnIndex].ovalue;
                                    }
                                return null;
                            }

                            public String getColumnName(int column) {
                                if (currentSelection != null)
                                    if (currentSelection.getLastComponent() instanceof JTSceneGraphComponent) {
                                        if (column == 0)
                                            return "Property";
                                        else
                                            return "Value";
                                    }
                                return null;
                            };

                            public int getRowCount() {
                                if (currentSelection != null)
                                    if (currentSelection.getLastComponent() instanceof JTSceneGraphComponent) {
                                        JTSceneGraphComponent s = (JTSceneGraphComponent) currentSelection
                                                .getLastComponent();
                                        if (s != null)
                                            if (s.properties != null)
                                                return s.properties.length;
                                    }
                                return 0;

                            }

                            public int getColumnCount() {
                                if (currentSelection != null)
                                    if (currentSelection.getLastComponent() instanceof JTSceneGraphComponent) {
                                        return 2;
                                    }
                                return 0;
                            }
                        });
                        list.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

                        for (int i = 0; i < list.getColumnCount(); i++) {
                            int vColIndex = i;
                            DefaultTableColumnModel colModel = (DefaultTableColumnModel) list
                                    .getColumnModel();
                            TableColumn col = colModel.getColumn(vColIndex);
                            int width = 0;

                            // Get width of column header
                            TableCellRenderer renderer = col
                                    .getHeaderRenderer();

                            if (renderer == null) {
                                renderer = list.getTableHeader()
                                        .getDefaultRenderer();
                            }

                            Component comp = renderer
                                    .getTableCellRendererComponent(list,
                                            col.getHeaderValue(), false, false,
                                            0, 0);

                            width = comp.getPreferredSize().width;

                            // Get maximum width of column data
                            for (int r = 0; r < list.getRowCount(); r++) {
                                renderer = list.getCellRenderer(r, vColIndex);
                                comp = renderer.getTableCellRendererComponent(
                                        list, list.getValueAt(r, vColIndex),
                                        false, false, r, vColIndex);
                                width = Math.max(width,
                                        comp.getPreferredSize().width);
                            }

                            // Add margin
                            width += 2 * 5;

                            // Set the width
                            col.setPreferredWidth(width);
                        }
                        ((DefaultTableCellRenderer) list.getTableHeader()
                                .getDefaultRenderer())
                                .setHorizontalAlignment(SwingConstants.LEFT);
                        list.getTableHeader().setReorderingAllowed(false);
                       // SelectionManagerImpl.selectionManagerForViewer(l).setSelection(currentSelection);

                    } else {
                        list.setModel(null);
                        list.setFillsViewportHeight(false);
                    }
                }
            });
            list.setCellSelectionEnabled(true);
            f.add(scroller, BorderLayout.SOUTH);

        }
        f.validate();
        f.doLayout();

        JSplitPane p = (JSplitPane) getBrowser().getClientProperty(Fab4.PANEL);
        p.setLeftComponent(f);
        tree.setToggleClickCount(1);
        System.gc();
        return parent;
    }

    private void addGeometryActions(SceneGraphComponent geometryNode,
            final Navigator n, final SoftViewerLeaf v) {
        if (geometryNode== null)
            return;
        List<SceneGraphComponent> l = geometryNode.getChildComponents();
        for (SceneGraphComponent s : l) {
            if (s.getName().contains("PNE")) {
                ActionTool at = new ActionTool(InputSlot.MIDDLE_BUTTON);
                at.addActionListener(new ActionListener() {

                    public void actionPerformed(ActionEvent e) {
                        if (e.getSource() instanceof ToolContext) {
                            ToolContext tc = (ToolContext) e.getSource();
                            v.sgp = tc.getRootToToolComponent();
                            // System.out.println(sgp);
                            if (n != null) {
                                TreePath p = new TreePath(((SceneTreeModel) n
                                        .getSceneTree().getModel())
                                        .convertSceneGraphPath(v.sgp));
                                n.getTreeSelectionModel().setSelectionPath(p);
                                n.getSceneTree().scrollPathToVisible(p);
                            }
                            // v.selectionManager.setSelection(new
                            // Selection(v.sgp));

                            Appearance app = v.sgp.getLastComponent()
                                    .getAppearance();
                            Color selectionColor = Color.white;
                            if (v.lastcomp != null) {
                                v.lastcomp.setAppearance(v.lastapp);
                                // if (v.lastapp!=null)
                                // v.lastapp.setAttribute(TRANSPARENCY_ENABLED,
                                // false);
                            }
                            v.lastcomp = v.sgp.getLastComponent();
                            v.lastapp = v.sgp.getLastComponent()
                                    .getAppearance();
                            if (app == null) {
                                app = new Appearance("Select");
                            }
                            app.setAttribute(POLYGON_SHADER + "."
                                    + DIFFUSE_COLOR, selectionColor);
                            v.lastcomp.setAppearance(app);

                            // app.setAttribute(TRANSPARENCY_ENABLED, true);
                            // app.setAttribute(POLYGON_SHADER+"."+TRANSPARENCY,
                            // .4);
                            // selectionManager.setSelection(new
                            // Selection(sgp));

                        }
                    }
                });
                s.addTool(at);

            }
            addGeometryActions(s, n, v);
        }
    }

    public class CustomTableCellRenderer extends DefaultTableCellRenderer {
        public Component getTableCellRendererComponent(JTable table,
                Object obj, boolean isSelected, boolean hasFocus, int row,
                int column) {
            Component cell = super.getTableCellRendererComponent(table, obj,
                    isSelected, hasFocus, row, column);
            if (obj instanceof String) {
                String s = (String) obj;
                cell = new JLabel(s);
            }
            if (isSelected) {

            } else {
                if (row % 2 == 0) {
                    cell.setBackground(Color.white);
                } else {
                    cell.setBackground(Color.lightGray);
                }

            }
            return cell;
        }
    }
}
