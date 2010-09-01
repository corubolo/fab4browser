package uk.ac.liverpool.fab4.jreality;

import java.awt.AWTEvent;
import java.awt.Canvas;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.logging.Level;

import javax.swing.JPanel;

import phelps.lang.Integers;
import uk.ac.liv.jt.debug.DebugJTReader;
import uk.ac.liv.jt.segments.LSGSegment;
import uk.ac.liv.jt.viewer.JTReader;
import uk.ac.liverpool.fab4.Fab4;

import multivalent.Behavior;
import multivalent.Context;
import multivalent.INode;
import multivalent.Leaf;
import multivalent.SemanticEvent;
import multivalent.std.adaptor.HTML;
import de.jreality.math.MatrixBuilder;
import de.jreality.reader.Readers;
import de.jreality.scene.Appearance;
import de.jreality.scene.Camera;
import de.jreality.scene.DirectionalLight;
import de.jreality.scene.Light;
import de.jreality.scene.SceneGraphComponent;
import de.jreality.scene.SceneGraphPath;
import de.jreality.scene.Viewer;
import de.jreality.shader.CommonAttributes;
import de.jreality.softviewer.Renderer;
import de.jreality.tools.ClickWheelCameraZoomTool;
import de.jreality.tools.RotateTool;
import de.jreality.toolsystem.ToolSystem;
import de.jreality.ui.viewerapp.SelectionManager;
import de.jreality.ui.viewerapp.SelectionManagerImpl;
import de.jreality.ui.viewerapp.SelectionRenderer;
import de.jreality.util.CameraUtility;
import de.jreality.util.Input;
import de.jreality.util.LoggingSystem;
import de.jreality.util.RenderTrigger;

/**
 * The software renderer component.
 * 
 * @version 1.0
 * @author <a href="mailto:hoffmann@math.tu-berlin.de">Tim Hoffmann</a>
 */
public class SoftViewerLeaf extends Leaf implements Runnable, Viewer {

    private static final long serialVersionUID = 1L;

    // TODO: remove ENFORCE_... ?

    private static final boolean ENFORCE_PAINT_ON_MOUSEEVENTS = false;

    // synchronizes the render thread
    private final Object renderAsyncLock = new Object();

    // synchronizes the rendering
    private final Object renderImplLock = new Object();

    private SceneGraphPath cameraPath;

    private SceneGraphComponent root;

    private SceneGraphComponent auxiliaryRoot;

    private transient BufferedImage offscreen;

    private Renderer renderer;

    private boolean upToDate = false;

    private boolean backgroundExplicitlySet;

    private boolean imageValid;

    private Image bgImage;

    // be lazy in synchronous render: if there is a request but renderImpl is
    // still busy just return...
    private boolean lazy = false;

    private boolean embedded = false;

    private URI uri;

    private Component dummy;

    private SceneGraphComponent sgc;

    private SceneGraphComponent geometryNode;

    private int fixedw =0;

    private int fixedh = 0;
    private boolean reg = false;

    private RenderTrigger rt;

    public Appearance lastapp;
    public SceneGraphComponent lastcomp;

    public SceneGraphComponent rootNode;
    
    public SceneGraphPath sgp;

    private SelectionRenderer selectionRenderer;

    public SelectionManager selectionManager;
    
    
    public SoftViewerLeaf(String name, Map<String, Object> attr, INode parent)
            throws URISyntaxException {

        super(name, attr, parent);
        if (reg == false) {
            Readers.registerReader("JT", JTReader.class);
            // register the file ending .jt for files containing JT-format data
            Readers.registerFileEndings("JT", "jt");
            DebugJTReader.debugCodec = false;
            DebugJTReader.debugMode = false;
            LSGSegment.doRender = false;
            reg = true;

        }
        if (attr == null)
            return;
        fixedw = Integers.parseInt((String)attr.get("width"), 0);
        fixedh = Integers.parseInt((String)attr.get("height"), 0);
        bbox.setSize(fixedw, fixedh);
        dummy = new Canvas() {
            public int getWidth() {return bbox.width;};
            public int getHeight() {return bbox.height;};
        };
//        for (Entry a : attr.entrySet()) {
//            System.out.println(a.getKey() + "  =  " + a.getValue());
//        }
        if (attr.get("uri") instanceof String)
            uri = new URI((String) attr.get("uri"));
        else if (attr.get("uri") instanceof URI)
            uri = ((URI) attr.get("uri"));
        else if (attr.get("uri") == null) {
            System.out.println("NULL URI???");
           // uri = new URI((String) attr.get("src"));
        }
        if (uri!=null ) {
            try {
                sgc = Readers.read(new Input(uri.toURL()));

            } catch (MalformedURLException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

        }
        Boolean s = (Boolean) attr.get("embedded");
        if (s != null) {
            embedded = s;
        }
        String ap = (String) attr.get("lazy");
        if (ap != null) {
            if (ap.equalsIgnoreCase("true"))
                lazy = true;
            else if (ap.equalsIgnoreCase("false"))
                lazy = false;
        }

        setBackground(Color.white);
        rootNode = new SceneGraphComponent("root");
        SceneGraphComponent cameraNode = new SceneGraphComponent("camera");
        geometryNode = new SceneGraphComponent("geometry");
        SceneGraphComponent lightNode = new SceneGraphComponent("light");
        rootNode.addChild(geometryNode);
        rootNode.addChild(cameraNode);
        cameraNode.addChild(lightNode);

        Light dl = new DirectionalLight();
        lightNode.setLight(dl);
        if (sgc!=null)
            geometryNode.addChild(sgc);
        RotateTool rotateTool = new RotateTool();
        geometryNode.addTool(rotateTool);
        geometryNode.addTool(new ClickWheelCameraZoomTool());
        // geometryNode.addTool(new PickShowTool() {});

        MatrixBuilder.euclidean().translate(0, 0, 3).assignTo(cameraNode);

        Appearance rootApp = new Appearance();
        rootApp.setAttribute(CommonAttributes.BACKGROUND_COLOR, new Color(.8f,
                .8f, .8f));
        rootApp.setAttribute(CommonAttributes.DIFFUSE_COLOR, new Color(1f, 0f,
                0f));
        rootApp.setAttribute(CommonAttributes.SMOOTH_SHADING, true);
        rootNode.setAppearance(rootApp);

        Camera camera = new Camera();
        cameraNode.setCamera(camera);
        SceneGraphPath camPath = new SceneGraphPath(rootNode, cameraNode);
        camPath.push(camera);
        setSceneRoot(rootNode);
        setCameraPath(camPath);

        ToolSystem toolSystem = ToolSystem.toolSystemForViewer(this);
        toolSystem.initializeSceneTools();
        //

        rt = new RenderTrigger();
        rt.addSceneGraphComponent(rootNode);
        rt.addViewer(this);
        // rt.forceRender();
        // render();
        CameraUtility.encompass(this);
         Fab4.getMVFrame(getBrowser()).annoPanels.get(getBrowser()).threednote.setVisible(true);
         selectionManager = SelectionManagerImpl.selectionManagerForViewer(this);
         // a utility class which handles highlighting the selected component
             selectionRenderer = new SelectionRenderer(selectionManager, this);
             selectionRenderer.setVisible(true);
    }

    

    /*
     * (non-Javadoc)
     * 
     * @see de.jreality.soft.Viewer#getViewingComponent()
     */
    public Object getViewingComponent() {
        return dummy;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * de.jreality.soft.Viewer#setSceneRoot(de.jreality.scene.SceneGraphComponent
     * )
     */
    public void setSceneRoot(SceneGraphComponent c) {
        root = c;
        if (renderer != null)
            renderer.setSceneRoot(c);
    }

    /*
     * (non-Javadoc)
     * 
     * @see de.jreality.soft.Viewer#getSceneRoot()
     */
    public SceneGraphComponent getSceneRoot() {
        return root;
    }

    
    
    public void render() {
       // System.out.println("render sync");

         if ((lazy && isRendering)) {
         //avoid deadlock
             System.out.println("LL");
         return;
         }
        renderImpl(getSize(), false);
        paintImmediately();
    }

    private Dimension getSize() {
        Dimension d = this.bbox.getSize();
        return d;
    }

    public void invalidate() {

        imageValid = false;
        upToDate = false;
    }

    public boolean formatNode(int width, int height, Context cx) {
        if (fixedw == 0 && fixedh==0){
        int w1 = width - 5;
        int h1 = height - 5;
        bbox.setSize(w1, h1);
        System.out.println("format" + bbox);
        imageValid = false;
        return true;// super.formatNode(w1, h1, cx);
        } else 
            return false;
    }

    public boolean paintNodeContent(Context cx, int start, int end) {

        Graphics2D g = cx.g;

        paint(g);
        return super.paintNodeContent(cx, start, end);
    }

    public void paint(Graphics g) {
        Rectangle clip = g.getClipBounds();
        if (clip != null && clip.isEmpty())
            return;
        synchronized (this) {
            if (imageValid) {
                if (offscreen != null) {
                    if (bgImage != null)
                        g.drawImage(bgImage, 0, 0, Color.GREEN, null);
                    g.drawImage(offscreen, 0, 0, null);
                    return;
                } else
                    System.err.println("paint: no offscreen in paint");
            } else if (!upToDate)
                synchronized (renderAsyncLock) {
                    renderAsyncLock.notify();
                   render();
                }
        }

    }

    public void clipboardNode(StringBuffer sb) {
        sb.append("[JReality 3D]");

    }

    public void update(Graphics g) {
        paint(g);
    }

    private boolean disposed;

    private int metric;

    public void run() {
//       
    }
    
    
    private void loadNew() {
        if (uri!=null ) {
            try {
                this.setSgc(Readers.read(new Input(uri.toURL())));
                
                CameraUtility.encompass(this);
            } catch (MalformedURLException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

        }    }
    private boolean isRendering = false;

    private Color background = Color.white;

    private void renderImpl(Dimension d, boolean quality) {

        synchronized (renderImplLock) {
            isRendering = true;
            if (d.width > 0 && d.height > 0) {
                if (offscreen == null || offscreen.getWidth() != d.width
                        || offscreen.getHeight() != d.height) {
                    imageValid = false;
                    offscreen = new BufferedImage(d.width, d.height,
                            BufferedImage.TYPE_INT_ARGB);
                    offscreen.setAccelerationPriority(1.f);
                    renderer = new Renderer(offscreen);
                    renderer.setBestQuality(quality);

                    Color c = getBackground();
                    renderer.setBackgroundColor(c != null ? c.getRGB() : 0);
                    renderer.setCameraPath(cameraPath);
                    renderer.setSceneRoot(root);
                    renderer.setAuxiliaryRoot(auxiliaryRoot);
                } else if (!backgroundExplicitlySet) {
                    Color c = getBackground();// inherited from parent
                    renderer.setBackgroundColor(c != null ? c.getRGB() : 0);
                }

                try {
                    renderer.render();
                } catch (Exception e) {
                    LoggingSystem.getLogger(this).log(Level.SEVERE,
                            "renderer.render() failed! ", e);

                }
                synchronized (this) {
                    renderer.update();
                    imageValid = true;
                }
            }
            isRendering = false;
        }
    }

    private Color getBackground() {

        return background;
    }

    
    public boolean eventBeforeAfter(AWTEvent e, Point rel) {
        if (e instanceof SemanticEvent) {
            SemanticEvent se = (SemanticEvent) e;
            if (se.getMessage() == HTML.SET_URL) {
                uri = ((URI)se.getArg());
                loadNew();
                return true;
            }
        }
        e.setSource(dummy);
        dummy.dispatchEvent(e);
        return super.eventBeforeAfter(e, rel);
    }

   

    public void setBackground(Color c) {
        backgroundExplicitlySet = c != null;
        if (backgroundExplicitlySet && renderer != null)
            renderer.setBackgroundColor(c.getRGB());
    }

    private void paintImmediately() {
        this.repaint();
    }

    /*
     * (non-Javadoc)
     * 
     * @see de.jreality.scene.Viewer#getCameraPath()
     */
    public SceneGraphPath getCameraPath() {
        return cameraPath;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * de.jreality.scene.Viewer#setCameraPath(de.jreality.util.SceneGraphPath)
     */
    public void setCameraPath(SceneGraphPath p) {
        cameraPath = p;
        if (renderer != null)
            renderer.setCameraPath(p);
    }

    /*
     * (non-Javadoc)
     * 
     * @see de.jreality.scene.Viewer#hasViewingComponent()
     */
    public boolean hasViewingComponent() {
        return true;
    }

    /*
     * (non-Javadoc)
     * 
     * @see de.jreality.scene.Viewer#initializeFrom(de.jreality.scene.Viewer)
     */
    public void initializeFrom(Viewer v) {
        setSceneRoot(v.getSceneRoot());
        setCameraPath(v.getCameraPath());
        setAuxiliaryRoot(v.getAuxiliaryRoot());
        // setMetric(v.getMetric());
    }

    /*
     * (non-Javadoc)
     * 
     * @see de.jreality.scene.Viewer#getMetric()
     */
    public int getMetric() {
        return metric;
    }

    /*
     * (non-Javadoc)
     * 
     * @see de.jreality.scene.Viewer#setMetric(int)
     */
    public void setMetric(int sig) {
        metric = sig;
    }

    /*
     * (non-Javadoc)
     * 
     * @see de.jreality.scene.Viewer#setAuxiliaryRoot(de.jreality.scene.
     * SceneGraphComponent)
     */
    public void setAuxiliaryRoot(SceneGraphComponent ar) {
        auxiliaryRoot = ar;
        if (renderer != null)
            renderer.setAuxiliaryRoot(ar);
    }

    /*
     * (non-Javadoc)
     * 
     * @see de.jreality.scene.Viewer#getAuxiliaryRoot()
     */
    public SceneGraphComponent getAuxiliaryRoot() {
        return auxiliaryRoot;
    }

    public Image getBackgroundImage() {
        return bgImage;
    }

    public void setBackgroundImage(Image bgImage) {
        this.bgImage = bgImage;
    }

    public Dimension getViewingComponentSize() {
        return getSize();
    }

    public boolean canRenderAsync() {
        return true;
    }

    public void renderAsync() {
        render();

    }

    public void dispose() {
        synchronized (renderAsyncLock) {
            disposed = true;
            renderAsyncLock.notify();
        }
    }

    public BufferedImage renderOffscreen(int width, int height) {

        renderImpl(new Dimension(width, height), true);
        BufferedImage bi = offscreen;
        return bi;
    }

    public void close() {
        
        Fab4.getMVFrame(getBrowser()).annoPanels.get(getBrowser()).threednote.setVisible(false);
        rt.removeSceneGraphComponent(root);
        rt.removeViewer(this);
        ToolSystem toolSystem = ToolSystem.toolSystemForViewer(this);
        toolSystem.dispose();
        cameraPath = null;
        auxiliaryRoot = null;
        root = null;
        bgImage = null;
        renderer = null;
    }


public void setSgc(SceneGraphComponent sgc) {
    if (this.sgc!=null)
        geometryNode.removeChild(this.sgc);
    geometryNode.addChild(sgc);
    this.sgc = sgc;
}
    public SceneGraphComponent getSGC() {
        // TODO Auto-generated method stub
        return sgc;
    }

  
}
