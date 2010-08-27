package uk.ac.liverpool.fab4.jreality;

import static de.jreality.shader.CommonAttributes.POLYGON_SHADER;
import static de.jreality.shader.CommonAttributes.TRANSPARENCY;
import static de.jreality.shader.CommonAttributes.TRANSPARENCY_ENABLED;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import multivalent.CLGeneral;
import multivalent.Document;
import multivalent.INode;
import multivalent.Layer;
import multivalent.MediaAdaptor;
import multivalent.StyleSheet;
import phelps.awt.Colors;
import uk.ac.liv.jt.debug.DebugJTReader;
import uk.ac.liv.jt.segments.JTSceneGraphComponent;
import uk.ac.liv.jt.segments.LSGSegment;
import uk.ac.liv.jt.viewer.JTReader;
import uk.ac.liverpool.fab4.Fab4;
import de.jreality.reader.Readers;
import de.jreality.scene.Appearance;
import de.jreality.scene.SceneGraphComponent;
import de.jreality.scene.SceneGraphPath;
import de.jreality.scene.Viewer;
import de.jreality.scene.tool.InputSlot;
import de.jreality.scene.tool.ToolContext;
import de.jreality.tools.ActionTool;
import de.jreality.ui.treeview.SceneTreeModel;
import de.jreality.ui.viewerapp.Navigator;
import de.jreality.ui.viewerapp.Navigator.SelectionEvent;
import de.jreality.ui.viewerapp.Navigator.SelectionListener;
import de.jreality.ui.viewerapp.Selection;

public class JRealityMA extends MediaAdaptor {

    SoftViewerLeaf l;
    Document doc;
    JPanel f;
    //private SelectionManager selectionManager;
    protected Appearance lastapp;
    protected SceneGraphComponent lastcomp;

    @Override
    public void close() throws IOException {

        if (l != null) {
            l.close();
            JPanel p = (JPanel)getBrowser().getClientProperty(Fab4.PANEL);
            p.remove(f);
        }
        l = null;
        // doc.removeAttr(TimedMedia.TIMEDMEDIA);
        super.close();
    }

    @Override
    public Object parse(INode parent) throws Exception {

        doc = parent.getDocument();
        if (doc.getFirstChild() != null) {
            doc.clear();
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
        
        //selectionManager = SelectionManagerImpl.selectionManagerForViewer(l);
        //new SelectionRenderer(selectionManager, l).setVisible(true);
        f = new JPanel();

        final Navigator n = new Navigator(l);
        Component c = n.getComponent();
        f.setLayout(new BorderLayout(5, 5));
        f.add(c, BorderLayout.CENTER);
        addGeometryActions(sgc, n, l);
        if (sgc instanceof JTSceneGraphComponent) {

            TreeSelectionModel tsm = n.getSceneTree().getSelectionModel();
            // tsm.setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
            //
            // tsm.addTreeSelectionListener(new SelectionListener(){
            //
            // public void selectionChanged(SelectionEvent e) {
            //
            // Selection currentSelection = e.getSelection();
            // //currentSelection.getLastElement());
            // }
            // });
            final JTable list = new JTable();
            tsm.addTreeSelectionListener(new SelectionListener() {

                @Override
                public void selectionChanged(SelectionEvent e) {

                    final Selection currentSelection = e.getSelection();
                    if (currentSelection != null) {
                        list.setModel(new AbstractTableModel() {

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
                                        // JTSceneGraphComponent s =
                                        // (JTSceneGraphComponent)
                                        // n.getSelection().asComponent();
                                        return 2;
                                    }
                                return 0;
                            }
                        });
                       // selectionManager.setSelection(currentSelection);
                    } else
                        list.setModel(null);
                }
            });
            list.setAutoResizeMode(list.AUTO_RESIZE_ALL_COLUMNS);
            f.add(list, BorderLayout.SOUTH);
        }
        JPanel p = (JPanel)getBrowser().getClientProperty(Fab4.PANEL);
        p.add(f, BorderLayout.WEST);
        // JTree sceneTree = new JTree();
        // SceneTreeModel treeModel = new SceneTreeModel(rootNode);
        //
        // sceneTree.setModel(treeModel);
        //
        // // sceneTree.setAnchorSelectionPath(new
        // TreePath(treeModel.convertSelection(null)));
        // sceneTree.setCellRenderer(new JTreeRenderer());
        // sceneTree.getInputMap().put(KeyStroke.getKeyStroke("ENTER"),
        // "toggle"); //collaps/expand nodes with ENTER
        //
        // TreeSelectionModel tsm = sceneTree.getSelectionModel();
        // tsm.setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        //
        // tsm.addTreeSelectionListener(new SelectionListener(){
        //
        // public void selectionChanged(SelectionEvent e) {
        //
        // Selection currentSelection = e.getSelection();
        // //currentSelection.getLastElement());
        // }
        // });
        // f.getContentPane().add(sceneTree);

        f.validate();
        f.doLayout();

        Layer ll = doc.getLayer(Layer.PERSONAL);
        if (ll != null) {
            ll.destroy();
            // doc.putAttr(TimedMedia.TIMEDMEDIA, l);
        }

        return parent;
    }

    private void addGeometryActions(SceneGraphComponent geometryNode,
            final Navigator n, final Viewer v) {

        List<SceneGraphComponent> l = geometryNode.getChildComponents();
        for (SceneGraphComponent s : l) {
            if (s.getName().startsWith("PartNodeElement")) {
                ActionTool at = new ActionTool(InputSlot.MIDDLE_BUTTON);
                at.addActionListener(new ActionListener() {


                    public void actionPerformed(ActionEvent e) {
                        if (e.getSource() instanceof ToolContext) {
                            ToolContext tc = (ToolContext) e.getSource();
                            final SceneGraphPath sgp = tc
                                    .getRootToToolComponent();
                            // System.out.println(sgp);
                            TreePath p = new TreePath(((SceneTreeModel) n
                                    .getSceneTree().getModel())
                                    .convertSceneGraphPath(sgp));
                            n.getTreeSelectionModel().setSelectionPath(p);
                            n.getSceneTree().scrollPathToVisible(p);
                            Appearance app = sgp.getLastComponent().getAppearance();
                            if (lastcomp!=null ){
                                lastcomp.setAppearance(lastapp);
                                if (lastapp!=null)
                                    lastapp.setAttribute(TRANSPARENCY_ENABLED, false);
                            }
                            lastcomp = sgp.getLastComponent();
                            lastapp =  sgp.getLastComponent().getAppearance();
                            if (app == null){
                                app = new Appearance("Select");
                                sgp.getLastComponent().setAppearance(app);
                            }
                            app.setAttribute(TRANSPARENCY_ENABLED, true);
                            app.setAttribute(POLYGON_SHADER+"."+TRANSPARENCY, .4);
                          //  selectionManager.setSelection(new Selection(sgp));

                        }
                    }
                });
                s.addTool(at);

            }
            addGeometryActions(s, n, v);
        }
    }

}
