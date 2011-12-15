package uk.ac.liverpool.fab4;

import java.awt.AWTEvent;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.ImageConsumer;
import java.awt.image.ImageProducer;
import java.awt.image.MemoryImageSource;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.Map.Entry;

import javax.media.Buffer;
import javax.media.CannotRealizeException;
import javax.media.Codec;
import javax.media.ConfigureCompleteEvent;
import javax.media.ControllerEvent;
import javax.media.ControllerListener;
import javax.media.EndOfMediaEvent;
import javax.media.Format;
import javax.media.Manager;
import javax.media.NoPlayerException;
import javax.media.PrefetchCompleteEvent;
import javax.media.Processor;
import javax.media.RealizeCompleteEvent;
import javax.media.ResourceUnavailableEvent;
import javax.media.Time;
import javax.media.UnsupportedPlugInException;
import javax.media.control.TrackControl;
import javax.media.format.RGBFormat;
import javax.media.format.VideoFormat;

import multivalent.Behavior;
import multivalent.Context;
import multivalent.Document;
import multivalent.INode;
import multivalent.Layer;
import multivalent.Leaf;
import multivalent.TreeEvent;
import uk.ac.liverpool.fab4.behaviors.TimedMedia;

/**
 * Implements a video frame for the cortado applet, supporting streaming media.
 */
public class LeafVideoJMFFormatGrab extends Leaf implements ImageConsumer,
        TimedMedia, ControllerListener {

    protected URI uri;
    protected boolean stop_;
    // private VolatileImage im2;
    protected BufferedImage im = null;
    int fWidth, fHeight;
    boolean resizable = false;
    boolean kar = true;
    boolean showControls = false;
    ColorModel icm;
    long fnumber, cnumber;
    boolean autoplay = true;
    double duration;
    protected int durationFormat;
    boolean busy;
    int percent;
    protected boolean embedded = false;
    protected FrameFull fullf;
    protected boolean paintInterface = false;
    protected long prevtt;
    private long originaltt;

    synchronized void startFull() {
        if (im == null || fullf != null)
            return;
        fullf = new FrameFull();

        GraphicsEnvironment ge = GraphicsEnvironment
                .getLocalGraphicsEnvironment();
        GraphicsDevice gd_ = ge.getDefaultScreenDevice();
        gd_.setFullScreenWindow(fullf);

    }

    // back to normal
    synchronized void stopFull() {
        if (fullf != null) {
            GraphicsEnvironment ge = GraphicsEnvironment
                    .getLocalGraphicsEnvironment();
            GraphicsDevice gd_ = ge.getDefaultScreenDevice();
            gd_.setFullScreenWindow(null);
            gd_ = null;
            fullf.dispose();
            fullf = null;
        }

    }

    class FrameFull extends Frame implements MouseListener, KeyListener {

        public FrameFull() {
            super();
            setUndecorated(true);
            addKeyListener(this);
            addMouseListener(this);
        }

        public void update(Graphics g) {
            paint(g);

        }

        @Override
        public void paint(Graphics gg) {

        }

        public void mouseClicked(MouseEvent e) {

        }

        public void mouseEntered(MouseEvent e) {
            // TODO Auto-generated method stub

        }

        public void mouseExited(MouseEvent e) {
            // TODO Auto-generated method stub

        }

        public void mousePressed(MouseEvent e) {
            // TODO Auto-generated method stub

        }

        public void mouseReleased(MouseEvent e) {
            // TODO Auto-generated method stub

        }

        public void keyPressed(KeyEvent e) {
            // TODO Auto-generated method stub

        }

        public void keyReleased(KeyEvent e) {
            switch (e.getKeyCode()) {
            case KeyEvent.VK_ESCAPE:
            case 'q':
                stopFull();
                break;
            default:

            }
        }

        public void keyTyped(KeyEvent e) {
            // TODO Auto-generated method stub

        }
    }

    @Override
    public boolean formatNode(int width, int height, Context cx) {
        int w1 = width - 5;
        int h1 = height - 5;
        if (resizable) {

            // if (kar){
            // double rw = w / w1;
            // double rh = h / h1;
            // double ro = w / h;
            // if (rw > rh ){
            // w1 = width;
            // h1 =(int)( width * ro);
            //
            // } else if (rw < rh){
            // h1 = height;
            // w1 =(int)( height / ro);
            // }
            // }
            // System.out.println("a "+w1+" "+h1 );
            bbox.setSize(w1, h1);
            // System.out.println("z " +width+" "+height );
        }
        return super.formatNode(w1, h1, cx);
    }

    protected void displayFormatInfo(StringBuffer sb, Document doc){        
        Layer sc = doc.getLayer(Layer.SCRATCH);
        Map<String, Object> m = new HashMap<String, Object>();
        m.put("text", sb.toString());
        ImageInternalDataFrame ls = (ImageInternalDataFrame) Behavior
                        .getInstance("ImageInternalDataFrame",
                                        "uk.ac.liverpool.fab4.ImageInternalDataFrame",
                                        null, m, sc);
        ls.setTitle("Video Metadata");
        ls.setTransparent(false);
        ls.setBounds(0, 0, 120, 160);
        ls.setTransparent(true);
}

    public boolean formatNodeContent(Context cx, int start, int end) {

        return !valid_;
    }

    @Override
    public boolean eventBeforeAfter(AWTEvent e, Point rel) {

        if (e.getID() == MouseEvent.MOUSE_ENTERED) {
            if (embedded) {
                paintInterface = true;
                System.out.println("eventBeforeAfter-repaint()");
                repaint();
            }
        }
        if (e.getID() == MouseEvent.MOUSE_EXITED) {
            if (embedded) {
                paintInterface = false;
                System.out.println("eventBeforeAfter-repaint()");
                repaint();
            }
        }
        return super.eventBeforeAfter(e, rel);
    }

    public void clipboardNode(StringBuffer sb) {
        sb.append("[Video]");

    }

    public void setFullScreen(boolean fullScreen) {
        if (fullScreen)
            startFull();
        else
            stopFull();
    }

    public boolean isFullScreen() {
        return fullf != null;
    };

    public Rectangle getSize() {
        System.out.println(bbox);
        return bbox;
    }

    public void imageComplete(int status) {
        // System.out.println("imComplete " + status);
        cnumber++;
        if (fullf != null)
            fullf.repaint();
        else
            repaint();

        System.out.println(fnumber + " " + cnumber);

    }

    public void setColorModel(ColorModel model) {
        icm = model;
    }

    public void setDimensions(int width, int height) {
        // System.out.println("Set Dimensions called");
        if (im == null || fWidth != width || fHeight != height) {
            System.out.println("Set w" + width + " h " + height);
            fWidth = width;
            fHeight = height;
            if (bbox.width <= 0) {
                bbox.width = width;
                markDirty();
                reformat(this);
            }
            if (bbox.height <= 0) {
                bbox.height = height;
                markDirty();
                reformat(this);
            }

            if (resizable) {

                im = new BufferedImage(fWidth, fHeight,
                        BufferedImage.TYPE_INT_RGB);
                // im =
                // getBrowser().getGraphicsConfiguration().createCompatibleImage
                // (w, h, Transparency.OPAQUE);

                //
                im.setAccelerationPriority(1);
                // try {
                // im2 = getBrowser().getGraphicsConfiguration()
                // .createCompatibleVolatileImage(w, h,
                // new ImageCapabilities(true));
                // } catch (AWTException e) {
                // im2 = getBrowser().getGraphicsConfiguration()
                // .createCompatibleVolatileImage(w, h);
                // e.printStackTrace();
                // }
                // im2.setAccelerationPriority(1);
            } else {
                im = new BufferedImage(fWidth, fHeight,
                        BufferedImage.TYPE_INT_ARGB);
                // im.setAccelerationPriority(1);
                // System.out.println(im);
                // System.out.println(im.getCapabilities(getBrowser().
                // getGraphicsConfiguration()).isAccelerated());
                // im =
                // getBrowser().getGraphicsConfiguration().createCompatibleImage
                // (w, h);
                // im.setAccelerationPriority(1);
                // System.out.println(im);
                // System.out.println(im.getCapabilities(getBrowser().
                // getGraphicsConfiguration()).isAccelerated());
                im.setAccelerationPriority(1);
            }
            originaltt = prevtt = System.currentTimeMillis();
            // System.out.println("NEW IMAGE!!!");
        }
    }

    public void setHints(int hintflags) {
        // System.out.println("HF" + hintflags);

    }

    public void setPixels(int x, int y, int w, int h, ColorModel model,
            byte[] pixels, int off, int scansize) {
        // im.setRGB(x, y, w, h, pixels, off, scansize);
        System.out.println("This should not happen!");

    }

    public void setPixels(int x, int y, int w, int h, ColorModel model,
            int[] pixels, int off, int scansize) {
        im.setRGB(x, y, w, h, pixels, off, scansize);

    }

    public void setProperties(Hashtable<?, ?> props) {
        System.out.println("set props");

    }

    public Status getStatus() {
        return currentStatus;
    }

    public DurationUnit getDurationUnit() {

        return DurationUnit.BYTES;
    }

    public DurationUnit getPositionUnit() {
        return DurationUnit.SEC;
    }

    Status currentStatus;

    public BufferedImage getBufferedImage() {

        return im;
    }

    public boolean getDisplayGlobalUI() {
        return !embedded;
    }

    private long lastfc;
    private float fps;

    boolean paused = false;
    Time pauseTime;

    protected Component component = null;
    protected Frame fr = new Frame();
    // protected Player player = null;

    protected Processor processor = null;
    Object waitSync = new Object();
    boolean stateTransitionOK = true;

    // private FrameGrabbingControl frameGrabber = null;

    public LeafVideoJMFFormatGrab(String name, Map<String, Object> attr,
            INode parent) throws URISyntaxException, IOException,
            NoPlayerException, CannotRealizeException {
        super(name, attr, parent);
        if (attr == null)
            return;

        for (Entry a : attr.entrySet()) {
            System.out.println(a.getKey() + "  =  " + a.getValue());
        }
        if (attr.get("uri") instanceof String)
            uri = new URI((String) attr.get("uri"));
        else if (attr.get("uri") instanceof URI)
            uri = ((URI) attr.get("uri"));
        else if (attr.get("uri") == null) {
            System.out.println("NULL URI???");
            uri = new URI((String) attr.get("src"));
        }

        try {
            processor = Manager.createProcessor(uri.toURL());
        } catch (Exception e) {
            System.err
                    .println("Failed to create a processor from the given url: "
                            + uri.toURL());
        }

        Manager.setHint(Manager.LIGHTWEIGHT_RENDERER, new Boolean(true));

        processor.addControllerListener(this);

        // Put the Processor into configured state.
        processor.configure();
        if (!waitForState(processor.Configured)) {
            System.err.println("Failed to configure the processor.");
        }

        // So I can use it as a player
        processor.setContentDescriptor(null);

        TrackControl tc[] = processor.getTrackControls();

        if (tc == null) {
            System.err
                    .println("Failed to obtain track controls from the processor");
        }

        // Search for the track control for the video track.
        TrackControl videoTrack = null;

        for (int i = 0; i < tc.length; i++) {
            if (tc[i].getFormat() instanceof VideoFormat) {
                videoTrack = tc[i];
                break;
            }
        }

        if (videoTrack == null) {
            System.err
                    .println("The input media does not contain a video track.");
        }

        System.err.println("Video format: " + videoTrack.getFormat());
        fWidth = ((VideoFormat) videoTrack.getFormat()).getSize().width;
        fHeight = ((VideoFormat) videoTrack.getFormat()).getSize().height;
        System.out.println("Width: " + fWidth + ", Height:" + fHeight);

        // Instantiate and set the frame access codec to the data flow path.
        try {
            Codec codec[] = { new PreAccessCodec(), new PostAccessCodec() };
            videoTrack.setCodecChain(codec);
        } catch (UnsupportedPlugInException e) {
            System.err.println("The process does not support effects.");
        }

        // Realize the processor.
        processor.prefetch();
        if (!waitForState(processor.Prefetched)) {
            System.err.println("Failed to realize the processor.");
        }

        // Display the visual & control component if there's one.

        fr.setLayout(new BorderLayout());
        fr.addNotify();

        Component cc;

        Component vc;
        if ((vc = processor.getVisualComponent()) != null) {
            fr.add("Center", vc);
        }

        if ((cc = processor.getControlPanelComponent()) != null) {
            fr.add("South", cc);
        }

        duration = getDuration();
        System.out.println("Duration: " + duration);
        getBrowser().eventq(TimedMedia.MSG_GOT_DURATION, duration);
        if (autoplay) {
            System.out.println("autoplay==true: about to set status to Play");
            setStatus(Status.PLAY);
        } else {
            setStatus(Status.PAUSE);
        }
        Boolean s = (Boolean) attr.get("resize");
        if (s != null) {
            resizable = s;
        }
        s = (Boolean) attr.get("embedded");
        if (s != null) {
            embedded = s;
        }
        displayFormatInfo((VideoFormat) videoTrack.getFormat(),
                parent.getDocument());
    }
    
    private void displayFormatInfo(VideoFormat vf, Document doc){
        try {
                StringBuffer sb = new StringBuffer(5000);
                
                sb.append("Encoding: "+vf.getEncoding()+"\n");
                sb.append("FrameRate: "+vf.getFrameRate()+"\n");
                sb.append("MaxDataLength: "+vf.getMaxDataLength()+"\n");
                sb.append("Width: "+vf.getSize().width+"\n");
                sb.append("Height: "+vf.getSize().height+"\n");
                sb.append("Duration-secs: "+processor.getDuration().getSeconds()+"\n");
                
                displayFormatInfo(sb, doc);
        } catch (Exception e) {
                e.printStackTrace();
        } 
}


    private boolean waitForState(int state) {
        synchronized (waitSync) {
            try {
                while (processor.getState() != state && stateTransitionOK)
                    waitSync.wait();
            } catch (Exception e) {
            }
        }
        return stateTransitionOK;
    }

    public boolean imageUpdate(Image img, int infoflags, int x, int y,
            int width, int height) {
        return true;
    }

    public void controllerUpdate(ControllerEvent evt) {
        System.out.println("ControllerEvent called");
        if (evt instanceof ConfigureCompleteEvent
                || evt instanceof RealizeCompleteEvent
                || evt instanceof PrefetchCompleteEvent) {
            synchronized (waitSync) {
                stateTransitionOK = true;
                waitSync.notifyAll();
            }
        } else if (evt instanceof ResourceUnavailableEvent) {
            synchronized (waitSync) {
                stateTransitionOK = false;
                waitSync.notifyAll();
            }
        } else if (evt instanceof EndOfMediaEvent) {
            setStatus(Status.STOP);
        }
    }

    public void ProcessComponentEvent(ComponentEvent e) {
        component.getGraphics();
    }

    private void paintInterface(Graphics2D g, int w, int h) {
        System.out.println("drawing image");
        g.setColor(Color.gray);
        g.translate(1, h - 15);

        g.fillRect(0, 0, 14 + 26, 14);
        g.setColor(Color.white);
        if (getStatus() != Status.PLAY) {
            g.fill(PL);
        } else {
            g.fill(PA1);
            g.fill(PA2);
        }

        if (getStatus() == Status.STOP) {
            g.setColor(Color.lightGray);
        }
        g.fill(ST);

        g.draw(FU);
        g.draw(FU2);
        g.setColor(Color.yellow);

        g.fillRect(14 + 26, 0, (w - (14 + 24 + 2)), 14);
        g.setColor(Color.orange);
        g.fillRect(14 + 26, 2,
                (int) (getPlayPositionPercent() * (w - (14 + 24 + 2))), 10);
        int time = (int) getPlayPosition();
        int sec = time % 60;
        int min = time / 60;
        int hour = min / 60;
        min %= 60;
        String s = "" + hour + ":" + (min < 10 ? "0" + min : "" + min) + ":"
                + (sec < 10 ? "0" + sec : "" + sec);
        g.setColor(Color.black);
        g.drawString(s + " " + getPlayDuration(), 14 + 29, 12);
        g.translate(-1, -(h - 15));
    }

    @Override
    public boolean eventNode(AWTEvent e, Point rel) {
        if (e.getID() == TreeEvent.STOP) {
            stop_ = true;
            setStatus(Status.STOP);
        }
        // if (e.getID() == MouseEvent.MOUSE_CLICKED) {
        // if (e instanceof MouseEvent) {
        // MouseEvent me = (MouseEvent) e;
        // if (me.getClickCount()==2){
        // startFull();
        // return true;
        // }
        // }
        // }
        if (e.getID() == MouseEvent.MOUSE_CLICKED && fHeight - rel.y > 15) {
            if (getStatus() == Status.PLAY)
                setStatus(Status.PAUSE);
            else if (getStatus() == Status.PAUSE)
                setStatus(Status.PLAY);
        } else if (e.getID() == MouseEvent.MOUSE_CLICKED
                && fHeight - rel.y < 15) {
            rel.translate(-1, -(fHeight - 15));
            if (PL.getBounds().contains(rel)) {
                if (getStatus() == Status.PLAY)
                    setStatus(Status.PAUSE);
                else if (getStatus() == Status.PAUSE)
                    setStatus(Status.PLAY);
            }
            if (ST.getBounds().contains(rel)) {
                setStatus(Status.STOP);
            }
            if (FU.getBounds().contains(rel)) {
                startFull();
            }

            if (rel.x > 14 + 28) {
                Rectangle area = new Rectangle(14 + 18, 0,
                        (fWidth - (14 + 24 + 2)), 14);
                if (area.contains(rel)) {
                    double percent = (double) (rel.x - 14 + 24 + 2) / fWidth;
                    doSeek(percent);
                }

            }

        }

        return super.eventNode(e, rel);
    }

    public Graphics getGraphics() {
        return im.getGraphics();
    }

    public void doSeek(double aPercent) {
        double newPos = getDuration() * aPercent;

        getBrowser().eventq(TimedMedia.MSG_SEEK, newPos);
    }

    public double getPlayPosition() {
        return processor.getMediaTime().getSeconds();
        // return -1;//(double) pipeline.getPosition() / Clock.SECOND;
    }

    public double getPlayPositionPercent() {
        return getPlayPosition() / getDuration();
    }

    // public void newSeek(double aPos) {
    // doSeek(aPos);
    // }

    int rot;

    public boolean paintNodeContent(Context cx, int start, int end) {
        // System.out.println("paint");
        Graphics2D g = cx.g;
        Color or = g.getColor();
        RenderingHints rh = g.getRenderingHints();
        g.setRenderingHint(RenderingHints.KEY_RENDERING,
                RenderingHints.VALUE_RENDER_SPEED);

        if (fullf != null) {

        } else if (im != null) {
            if (resizable) {

                double d1 = ((double) bbox.width) / fWidth;
                double d2 = ((double) bbox.height) / fHeight;
                // System.out.println(d1);

                AffineTransform a = AffineTransform.getScaleInstance(d1, d2);
                // a.setToRotation(((double)rot++)/360.0, bbox.width/2,
                // bbox.height/2);
                g.drawRenderedImage(im, a);
                // System.out.print("a");
            } else {
                // g.drawRenderedImage(im, new AffineTransform());
                // g.drawRenderedImage(im, new AffineTransform());
                g.drawImage(im, 0, 0, null);
            }

        }
        long tt = System.currentTimeMillis() - prevtt;
        if (tt >= 500) {
            fps = (float) ((lastfc - fnumber) / (tt / 500));
            // fps*=2;
            lastfc = fnumber;
            prevtt = System.currentTimeMillis();
            getBrowser().eventq(TimedMedia.MSG_PlAYTIMEPERCENT,
                    getPlayPositionPercent());
            getBrowser().eventq(TimedMedia.MSG_PlAYTIME, getPlayPosition());
        }
        g.setColor(Color.white);
        g.drawString("" + fps, 10, 10);
        if (paintInterface)
            paintInterface(g, fWidth, fHeight);
        fnumber++;

        g.setRenderingHints(rh);
        g.setColor(or);
        return true;
    }

    Shape PL = new Polygon(new int[] { 1, 11, 1 }, new int[] { 1, 7, 13 }, 3);
    Shape ST = new Polygon(new int[] { 0 + 14, 0 + 14, 12 + 14, 12 + 14 },
            new int[] { 1, 13, 13, 1 }, 4);
    Shape PA1 = new Polygon(new int[] { 2, 2, 5, 5 },
            new int[] { 1, 13, 13, 1 }, 4);
    Shape PA2 = new Polygon(new int[] { 7, 7, 11, 11 }, new int[] { 1, 13, 13,
            1 }, 4);
    Shape FU = new Polygon(new int[] { 12 + 17, 12 + 17, 23 + 14, 23 + 14 },
            new int[] { 3, 11, 11, 3 }, 4);
    Shape FU2 = new Polygon(new int[] { 14 + 17, 14 + 17, 23 + 12, 23 + 12 },
            new int[] { 5, 9, 9, 5 }, 4);

    @Override
    public void remove() {
        super.remove();
        close();
    }

    public void close() {

        if (processor != null) {
            System.out.println("video close: " + uri);
            processor.stop();
            processor.close();
            processor.deallocate();

        }
    }

    public Time getPlayDuration() {
        return processor.getDuration();
    }

    public double getPosition() {
        return getPlayPosition();
    }

    public Status setStatus(Status st) {
        Status prev = currentStatus;
        switch (st) {
        case PLAY:
            if (paused) {
                processor.syncStart(pauseTime);
            } else {
                processor.start();
            }
            break;
        case STOP:
            processor.stop();
            break;
        case PAUSE:
            processor.stop();
            paused = true;
            pauseTime = processor.getStopTime();
            break;
        default:
            break;
        }

        return prev;

    }

    public double getDuration() {
        Double a = getPlayDuration().getSeconds();
        if (a != 0)
            return a;

        else
            return duration;
    }

    public boolean setPosition(double dest) {
        /*
         * boolean res; com.fluendo.jst.Event event; event =
         * com.fluendo.jst.Event.newSeek(Format.PERCENT, (int) (dest * 100.0 *
         * Format.PERCENT_SCALE)); res = pipeline.sendEvent(event); return res;
         */
        return false;
    }

    public Image createImage(ImageProducer ip, VideoFormat vf) {
        im = new BufferedImage(vf.getSize().width, vf.getSize().height,
                BufferedImage.TYPE_INT_RGB);
        ip.startProduction(this);
        return im;
    }

    /*********************************************************
     * Inner class.
     * 
     * A pass-through codec to access to individual frames.
     *********************************************************/

    public class PreAccessCodec implements Codec {

        /**
         * Callback to access individual video frames.
         */
        void accessFrame(Buffer buf) {

            // For demo, we'll just print out the frame #, time &
            // data length.

            long t = (long) (buf.getTimeStamp() / 10000000f);

            System.err.println("Pre: frame #: " + buf.getSequenceNumber()
                    + ", time: " + ((float) t) / 100f + ", len: "
                    + buf.getLength());

            /*
             * // Convert frame to a buffered image so it can be processed and
             * saved FormatControl fC = (FormatControl)processor.getControl(
             * "javax.media.control.FormatControl"); VideoFormat vf =
             * (VideoFormat)fC.getFormat();
             * 
             * System.out.println("buf format encoding: "+ vf.getEncoding()); //
             * **encoding equals mpeg
             * 
             * 
             * 
             * 
             * BufferToImage bufToImg = new BufferToImage(vf);
             * System.out.println("created bufferToImage: "+ bufToImg); //
             * **bufToImg NOT null Image img = bufToImg.createImage(buf);
             * System.out.println("grabbed image, image: "+ img); // **img
             * equals null !!
             * 
             * /* Incomplete .... w = img.getWidth(null);
             * System.out.println("done width: " + w); h = img.getHeight(null);
             * System.out.println("done height: " + h); setDimensions(w, h);
             * //im = new BufferedImage(img.getWidth(this), img.getHeight(this),
             * BufferedImage.TYPE_INT_ARGB); Graphics2D g = im.createGraphics();
             * paintInterface(g, w, h);
             */
        }

        /**
         * The code for a pass through codec.
         */

        // We'll advertize as supporting all video formats.
        protected Format supportedIns[] = new Format[] { new VideoFormat(null) };

        // We'll advertize as supporting all video formats.
        protected Format supportedOuts[] = new Format[] { new VideoFormat(null) };

        Format input = null, output = null;

        public String getName() {
            return "Pre-Access Codec";
        }

        // No op.
        public void open() {
        }

        // No op.
        public void close() {
        }

        // No op.
        public void reset() {
        }

        public Format[] getSupportedInputFormats() {
            return supportedIns;
        }

        public Format[] getSupportedOutputFormats(Format in) {
            if (in == null)
                return supportedOuts;
            else {
                // If an input format is given, we use that input format
                // as the output since we are not modifying the bit stream
                // at all.
                Format outs[] = new Format[1];
                outs[0] = in;
                return outs;
            }
        }

        public Format setInputFormat(Format format) {
            input = format;
            return input;
        }

        public Format setOutputFormat(Format format) {
            output = format;
            return output;
        }

        public int process(Buffer in, Buffer out) {

            // This is the "Callback" to access individual frames.
            accessFrame(in);

            // Swap the data between the input & output.
            Object data = in.getData();
            in.setData(out.getData());
            out.setData(data);

            // Copy the input attributes to the output
            out.setFormat(in.getFormat());
            out.setLength(in.getLength());
            out.setOffset(in.getOffset());

            return BUFFER_PROCESSED_OK;
        }

        public Object[] getControls() {
            return new Object[0];
        }

        public Object getControl(String type) {
            return null;
        }
    }

    public class PostAccessCodec extends PreAccessCodec {
        // We'll advertize as supporting all video formats.
        public PostAccessCodec() {
            supportedIns = new Format[] { new RGBFormat() };
        }

        /**
         * Callback to access individual video frames.
         */
        void accessFrame(Buffer buf) {

            // For demo, we'll just print out the frame #, time &
            // data length.

            long t = (long) (buf.getTimeStamp() / 10000000f);

            System.err.println("Post: frame #: " + buf.getSequenceNumber()
                    + ", time: " + ((float) t) / 100f + ", len: "
                    + buf.getLength());

            // begin AG's code:
            VideoFormat vf = (VideoFormat) buf.getFormat();

            bbox.setSize(vf.getSize().width, vf.getSize().height);
            // System.out.println("in.getFormat(): "+ vf);
            // ==
            // "RGB, 720x480, FrameRate=0.0, 32-bit, Masks=16711680:65280:255, LineStride=720, class [I"

            Object obj = buf.getData();

            // System.out.println("buf.GetData() Object type: "+
            // obj.getClass().getName());
            // System.out.println( "buf.GetData() Data Length: "+
            // ((int[])obj).length );

            // OLD: im = getImage((int[]) obj, fWidth, fHeight); //But getImage
            // is for arrays of Ints
            // NEW:
            int pix[] = (int[]) obj;
            // System.out.println( "pix.length: "+ pix.length);
            // MemoryImageSource mis = new MemoryImageSource(fWidth, fHeight,
            // pix, 0, fWidth);
            createImage(new MemoryImageSource(vf.getSize().width,
                    vf.getSize().height, pix, 0, vf.getSize().width), vf);
            // System.out.println( "im: "+ im.toString());

            // end AG's code
        }

        public String getName() {
            return "Post-Access Codec";
        }
    }

    /*
     * private BufferedImage getImage(byte array[], int width, int height) {
     * ColorSpace cs = ColorSpace.getInstance(ColorSpace.CS_GRAY); int[] nBits =
     * {8}; ColorModel cm = new ComponentColorModel(cs, nBits, false, true,
     * Transparency.OPAQUE, DataBuffer.TYPE_BYTE); SampleModel sm =
     * cm.createCompatibleSampleModel(width, height); DataBufferByte db = new
     * DataBufferByte(array, width*height); WritableRaster raster =
     * Raster.createWritableRaster(sm, db, null); BufferedImage bm = new
     * BufferedImage(cm, raster, false, null); return bm; }
     */

}
