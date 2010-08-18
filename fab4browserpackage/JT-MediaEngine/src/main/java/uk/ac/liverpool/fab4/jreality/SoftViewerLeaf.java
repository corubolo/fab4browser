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
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.logging.Level;

import multivalent.Context;
import multivalent.INode;
import multivalent.Leaf;
import de.jreality.scene.SceneGraphComponent;
import de.jreality.scene.SceneGraphPath;
import de.jreality.scene.Viewer;
import de.jreality.softviewer.Renderer;
import de.jreality.util.LoggingSystem;

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

    public SoftViewerLeaf(String name, Map<String, Object> attr, INode parent)
            throws URISyntaxException {

        super(name, attr, parent);

        if (attr == null)
            return;
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
            uri = new URI((String) attr.get("src"));
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
        int w1 = width - 5;
        int h1 = height - 5;
        bbox.setSize(w1, h1);
        System.out.println("format" + bbox);
        imageValid = false;
        return true;// super.formatNode(w1, h1, cx);
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
        cameraPath = null;
        auxiliaryRoot = null;
        root = null;
        bgImage = null;
        renderer = null;
    }

  
}
