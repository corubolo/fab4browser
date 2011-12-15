package uk.ac.liverpool.fab4;


import java.awt.AWTEvent;
import java.awt.Color;
import java.awt.Component;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferInt;
import java.awt.image.DirectColorModel;
import java.awt.image.Raster;
import java.awt.image.SampleModel;
import java.awt.image.SinglePixelPackedSampleModel;
import java.awt.image.WritableRaster;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Hashtable;
import java.util.Map;
import java.util.Map.Entry;

import javax.media.Buffer;
import javax.media.CannotRealizeException;
import javax.media.ConfigureCompleteEvent;
import javax.media.ControllerEvent;
import javax.media.ControllerListener;
import javax.media.EndOfMediaEvent;
import javax.media.Format;
import javax.media.Manager;
import javax.media.NoPlayerException;
import javax.media.NotConfiguredError;
import javax.media.PrefetchCompleteEvent;
import javax.media.Processor;
import javax.media.RealizeCompleteEvent;
import javax.media.ResourceUnavailableEvent;
import javax.media.ResourceUnavailableException;
import javax.media.Time;
import javax.media.UnsupportedPlugInException;
import javax.media.control.FrameGrabbingControl;
import javax.media.control.TrackControl;
import javax.media.format.RGBFormat;
import javax.media.format.VideoFormat;
import javax.media.renderer.VideoRenderer;
import javax.media.util.ImageToBuffer;

import multivalent.Context;
import multivalent.INode;
import multivalent.Leaf;
import uk.ac.liverpool.fab4.behaviors.TimedMedia;
import uk.ac.liverpool.fab4.behaviors.TimedMedia.DurationUnit;
import uk.ac.liverpool.fab4.behaviors.TimedMedia.Status;


/**
 * Implements a video frame for the cortado applet, supporting streaming media.
 */
public class LeafVideoJMFFormatFabio extends Leaf implements
TimedMedia, ControllerListener, VideoRenderer, FrameGrabbingControl {

    protected URI uri;
    protected boolean stop_;
    // private VolatileImage im2;
    protected BufferedImage im = null;
    int fWidth, fHeight;

    ColorModel icm;

    protected int durationFormat;

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



    public boolean formatNodeContent(Context cx, int start, int end) {

        return !valid_;
    }

    @Override
    public boolean eventBeforeAfter(AWTEvent e, Point rel) {

        if (e.getID() == MouseEvent.MOUSE_ENTERED) {
            if (embedded) {
                paintInterface = true;
                repaint();
            }
        }
        if (e.getID() == MouseEvent.MOUSE_EXITED) {
            if (embedded) {
                paintInterface = false;
                repaint();
            }
        }
        return super.eventBeforeAfter(e, rel);
    }

    @Override
    public boolean eventNode(AWTEvent e, Point rel) {
        return super.eventNode(e, rel);
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
        System.out.println("imComplete " + status);
        cnumber++;
        if (fullf != null)
            fullf.repaint();
        else
            repaint();

        System.out.println(fnumber+" "+cnumber);

    }

    public void setColorModel(ColorModel model) {
        icm = model;
    }

    public void setDimensions(int width, int height) {
        System.out.println("Set Dimensions called");
        if (fWidth != width || fHeight != height) {
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

                im = new BufferedImage(fWidth, fHeight, BufferedImage.TYPE_INT_RGB);
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
                im = new BufferedImage(fWidth, fHeight, BufferedImage.TYPE_INT_ARGB);
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
            System.out.println("NEW IMAGE!!!");
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
    boolean resizable = false;
    boolean kar = true;
    boolean showControls = false;
    long fnumber, cnumber;
    boolean autoplay = true;
    private long lastfc;
    private float fps;
    double duration;
    boolean busy;
    int percent;
    boolean paused=false;
    Time pauseTime;

    protected Processor processor = null;
    Object waitSync = new Object();
    boolean stateTransitionOK = true;


    public LeafVideoJMFFormatFabio(String name, Map<String, Object> attr, INode parent) 
    throws URISyntaxException, IOException, NoPlayerException, CannotRealizeException {
        super(name, attr, parent);
        if (attr == null)
            return;
        Manager.setHint(Manager.LIGHTWEIGHT_RENDERER, new Boolean(true));
        for (Entry a: attr.entrySet()){
            System.out.println(a.getKey()+"  =  "+ a.getValue());
        }
        if (attr.get("uri") instanceof String)
            uri = new URI ((String) attr.get("uri"));
        else if (attr.get("uri") instanceof URI)
            uri = ((URI) attr.get("uri"));
        else if (attr.get("uri") == null) {
            System.out.println("NULL URI???");
            uri = new URI ((String) attr.get("src"));
        }
        try{
            processor = Manager.createProcessor(uri.toURL());
        }catch (Exception e){
            System.err.println("Failed to create a processor from the given url: "+uri.toURL());
        }
        processor.addControllerListener(this);

        // Put the Processor into configured state.
        processor.configure();    
        if(!waitForState(processor.Configured)){
            System.err.println("Failed to configure the processor.");
        }
        // So I can use it as a player
        processor.setContentDescriptor(null);
        TrackControl tc[] = processor.getTrackControls();

        if (tc == null){
            System.err.println("Failed to obtain track controls from the processor");
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
            System.err.println("The input media does not contain a video track.");
        }


        // ***** FABIO here I set the renderer to be this object; this should be the fastest method
        try {
            videoTrack.setRenderer(this);
        } catch (UnsupportedPlugInException e1) {


            e1.printStackTrace();
        } catch (NotConfiguredError e1) {
            e1.printStackTrace();
        }


        System.err.println("Video format: " + videoTrack.getFormat());
        fWidth = ((VideoFormat)videoTrack.getFormat()).getSize().width;
        fHeight = ((VideoFormat)videoTrack.getFormat()).getSize().height;


        // **** FABIO: Need to set the size of the video object here otherwise nothing gets drawn to the screen


        bbox.setSize(fWidth, fHeight);
        System.out.println("Width: "+fWidth + ", Height:"+ fHeight);


        processor.prefetch();
        if (!waitForState(processor.Prefetched)) {
            System.err.println("Failed to realize the processor.");
        }

        duration = getDuration();
        System.out.println("Duration: "+duration);

        if (autoplay){
            System.out.println("autoplay==true: about to set status to Play");
            setStatus(Status.PLAY);
        }
        else{
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
    }

    private boolean waitForState(int state) {
        synchronized (waitSync){
            try{
                while (processor.getState() != state && stateTransitionOK)
                    waitSync.wait();
            }catch (Exception e){}
        }
        return stateTransitionOK;
    }

    public void controllerUpdate(ControllerEvent evt) {
        System.out.println("ControllerEvent called");
        if (evt instanceof ConfigureCompleteEvent ||
                evt instanceof RealizeCompleteEvent ||
                evt instanceof PrefetchCompleteEvent) {
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

    //


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



    public Graphics getGraphics() {
        return im.getGraphics();
    }


    public double getPlayPosition() {
        return processor.getMediaTime().getSeconds();
        //return -1;//(double) pipeline.getPosition() / Clock.SECOND;
    }

    public double getPlayPositionPercent() {
        return getPlayPosition() / getDuration() ;
    }

    //public void newSeek(double aPos) {
    // doSeek(aPos);
    //}

    int rot;

    public boolean paintNodeContent(Context cx, int start, int end) {
        System.out.println("paint");
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
                System.out.println(d1);

                AffineTransform a = AffineTransform.getScaleInstance(d1, d2);


                g.drawRenderedImage(im, a);
                System.out.print("a");
            } else {


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
            getBrowser().eventq(TimedMedia.MSG_PlAYTIME,
                    getPlayPosition());
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


    public double getPlayDuration() {
        return processor.getDuration().getSeconds();
    }

    public double getPosition() {
        return getPlayPosition();
    }

    public Status setStatus(Status st) {
        Status prev = currentStatus;
        switch (st) {
        case PLAY:
            if (paused){
                processor.syncStart(pauseTime);
            }else{
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
        return processor.getDuration().getSeconds();
        //return duration;
    }

    public DurationUnit getDurationUnit() {
        // TODO Auto-generated method stub
        return DurationUnit.SEC;
    }
    public boolean setPosition(double dest) {
        /*boolean res;
com.fluendo.jst.Event event;
event = com.fluendo.jst.Event.newSeek(Format.PERCENT,
(int) (dest * 100.0 * Format.PERCENT_SCALE));
res = pipeline.sendEvent(event);
return res;*/
        return false;
    }




    public Buffer grabFrame() { 
        Buffer buf = null;
        if(im != null) {ImageToBuffer.createBuffer(im,(float)0);
        System.out.println("grabFrame");
        }
        repaint();
        return buf; 
    } 


    public Rectangle getBounds() {
        return this.getBbox();
    }

    public Component getComponent() {


        return getBrowser();
    }

    public void setBounds(Rectangle arg0) {
        // TODO Auto-generated method stub

    }

    public boolean setComponent(Component arg0) {
        // TODO Auto-generated method stub
        return false;
    }

    public Format[] getSupportedInputFormats() {return new Format[] {new RGBFormat() };}





    public Format setInputFormat(Format format) {
        System.out.println("Fobs Java2DRenderer: setInputFormat");
        //vf = (RGBFormat) format;
        return format;
    }


    //private MemoryImageSource sourceImage;
    private Buffer lastBuffer;

    public int process(Buffer buffer) {
        if (lastBuffer != buffer) {
            lastBuffer = buffer;
            im = bufferToImage(buffer);
            //System.out.println(im);
            // repaint();
            repaint();
        }

        // if (last == buffer.getSequenceNumber())
        // return BUFFER_PROCESSED_OK;
        System.out.println("P" + buffer.getSequenceNumber());
        // last = buffer.getSequenceNumber();
        // im = bufferToImage(buffer);
        // repaint();
        return BUFFER_PROCESSED_OK;
    }

    public void start() {
        System.out.println("start");

    }

    public void stop() {
        System.out.println("stop");

    }

    public void open() throws ResourceUnavailableException {
        System.out.println("open");

    }

    public Object getControl(String arg) { 
        Object rc = null; 
        if(arg.equals("javax.media.control.FrameGrabbingControl")) rc = this; 
        return rc; 
    }
    public Object[] getControls() { 
        Object[] obj = { this }; 
        return obj; 
    } 


    public Component getControlComponent() {
        // TODO Auto-generated method stub
        return null;
    }

    public void close() {

        if (processor != null) {
            System.out.println("video close: " + uri);
            processor.stop();
            //processor.close();
           processor = null;
            // processor.deallocate();

        }
    }

    public void reset() {
        im = null;
    }
    private BufferedImage bufferToImage(Buffer buffer) {


        RGBFormat format = (RGBFormat) buffer.getFormat();
        int rMask, gMask, bMask;
        Object data = buffer.getData();
        DirectColorModel dcm;

        rMask = format.getRedMask();
        gMask = format.getGreenMask();
        bMask = format.getBlueMask();
        int [] masks = new int[3];
        masks[0] = rMask;
        masks[1] = gMask;
        masks[2] = bMask;

        DataBuffer db = new DataBufferInt((int[])data,
                format.getLineStride() *
                format.getSize().height);

        SampleModel sm = new SinglePixelPackedSampleModel(DataBuffer.TYPE_INT,
                format.getLineStride(),
                format.getSize().height,
                masks);
        WritableRaster wr = Raster.createWritableRaster(sm, db, new Point(0, 0));

        dcm = new DirectColorModel(24, rMask, gMask, bMask);
        return new BufferedImage(dcm, wr, true, null);
    }



}

