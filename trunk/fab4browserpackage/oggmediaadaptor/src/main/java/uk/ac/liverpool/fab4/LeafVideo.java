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
 *  
 *******************************************************************************/
package uk.ac.liverpool.fab4;

import java.awt.AWTEvent;
import java.awt.Color;
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
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.ImageConsumer;
import java.awt.image.ImageProducer;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Hashtable;
import java.util.Map;
import java.util.Map.Entry;

import phelps.lang.Integers;

import multivalent.Context;
import multivalent.INode;
import multivalent.Leaf;
import multivalent.TreeEvent;
import uk.ac.liverpool.fab4.behaviors.TimedMedia;

import com.fluendo.jheora.BufferedImageSink;
import com.fluendo.jst.BusHandler;
import com.fluendo.jst.Clock;
import com.fluendo.jst.Element;
import com.fluendo.jst.Format;
import com.fluendo.jst.Message;
import com.fluendo.player.DurationScanner;
import com.fluendo.utils.Debug;

/**
 * Implements a video frame for the cortado applet, supporting streaming media.
 */
public class LeafVideo extends Leaf implements /* ImageObserver, */BusHandler,
ImageConsumer, TimedMedia, BufferedImageSink {

    private Fab4Pipeline pipeline;
    private URI uri;
    private boolean stop_;
    // private VolatileImage im2;
    private BufferedImage im;
    int w, h;
    boolean resizable = false;
    boolean kar = true;
    boolean showControls = false;
    ColorModel icm;
    long fnumber, cnumber;
    boolean autoplay = true;
    private long start;
    private long lastfc;
    private long prevtt;
    private float fps;
    private long originaltt;
    double duration;
    long durationBytes;
    private int durationFormat;
    boolean busy;
    int percent;
    private boolean fullScreen = false;
    private boolean embedded = false;
    private boolean paintInterface = false;
    private FrameFull fullf;
    private boolean isBuffering;
    private int desiredState;
    private boolean isError = false;
    private boolean isfile = false;

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

        /**
         * 
         */
        private static final long serialVersionUID = 1L;

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
            // System.out.println("Paint ");
            Graphics2D g = (Graphics2D) gg;
            Color or = g.getColor();
            RenderingHints rh = g.getRenderingHints();
            g.setRenderingHint(RenderingHints.KEY_RENDERING,
                    RenderingHints.VALUE_RENDER_SPEED);

            if (im != null) {

                double r2 = ((double) this.getWidth())
                / ((double) this.getHeight());
                double r1 = ((double) w) / ((double) h);
                double d1 = ((double) this.getWidth()) / w;
                double d2 = ((double) this.getHeight()) / h;
                if (r1 > r2) {
                    d2 = d1 * (r1 / r2);
                    System.out.println("a");
                } else {
                    d1 = d2 * (r1 / r2);
                }

                // double d1 = ((double) this.getWidth()) / w;
                // double d2 = ((double) this.getHeight()) / (h - 15);
                // System.out.println(d1);

                AffineTransform a = AffineTransform.getScaleInstance(d1, d2);
                // a.setToRotation(((double)rot++)/360.0, bbox.width/2,
                // bbox.height/2);
                g.drawRenderedImage(im, a);
                // System.out.print("a");

            }
            long tt = System.currentTimeMillis() - prevtt;
            if (tt >= 500) {
                fps = (float) ((lastfc - fnumber) / (tt / 500));
                // fps*=2;
                lastfc = fnumber;
                prevtt = System.currentTimeMillis();
                getBrowser().eventq(TimedMedia.MSG_PlAYTIMEPERCENT,
                        new Double(getPlayPositionPercent()));
                getBrowser().eventq(TimedMedia.MSG_PlAYTIME,
                        new Double(getPlayPosition()));
            }
            g.setColor(Color.white);
            g.drawString("PRESS ESC to Exit Fullscreen" + fps, 10, 10);
            paintInterface(g, getWidth(), getHeight());
            fnumber++;

            g.setRenderingHints(rh);
            g.setColor(or);
        }

        public void mouseClicked(MouseEvent e) {

            if (getHeight() - e.getY() > 15) {
                if (getStatus() == Status.PLAY)
                    setStatus(Status.PAUSE);
                else if (getStatus() == Status.PAUSE)
                    setStatus(Status.PLAY);
            } else if (getHeight() - e.getY() < 15) {
                Point rel = new Point(e.getX(), e.getY());
                rel.translate(-1, -(getHeight() - 15));
                if (PL.getBounds().contains(rel)) {
                    if (getStatus() == Status.PLAY)
                        setStatus(Status.PAUSE);
                    else if (getStatus() == Status.PAUSE)
                        setStatus(Status.PLAY);
                }
                if (ST.getBounds().contains(rel)) {
                    setStatus(Status.STOP);
                    stopFull();
                }
                if (FU.getBounds().contains(rel)) {
                    stopFull();
                }

                if (rel.x > 14 + 28) {
                    Rectangle area = new Rectangle(14 + 18, 0,
                            (getWidth() - (14 + 24 + 2)), 14);
                    if (area.contains(rel)) {
                        double percent = (double) (rel.x - 14 + 24 + 2)
                        / getWidth();
                        doSeek(percent);
                    }
                }
            }

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
            pipeline.resize(bbox.getSize());
            // System.out.println("z " +width+" "+height );
        }
        return super.formatNode(w1, h1, cx);
    }

    public LeafVideo(String name, Map<String, Object> attr, INode parent)
    throws URISyntaxException, MalformedURLException {
        super(name, attr, parent);
        if (attr == null)
            return;
        // System.out.println("++++++"+autoplay);
        Debug.level = Debug.ERROR;
        pipeline = new Fab4Pipeline(this);

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
        // System.out.println(uri);
        String scheme = uri.getScheme();
        pipeline.setUrl(uri.toURL().toString());
        pipeline.enableAudio(true);
        pipeline.enableVideo(true);
        if (scheme.contains("file")) {
            isfile  = true;
            pipeline.setBufferSize(-1);
            pipeline.setBufferLow(-1);
            pipeline.setBufferHigh(-1);
        } else {
            pipeline.setBufferSize(800);
            pipeline.setBufferLow(10);
            pipeline.setBufferHigh(90);
        }
        pipeline.setComponent(this);
        URL documentBase;
        try {
            documentBase = uri.resolve("./").toURL();
            Debug.log(Debug.INFO, "Document base: " + documentBase);
        } catch (Throwable t) {
            documentBase = null;
        }
        pipeline.setDocumentBase(documentBase);
        pipeline.getBus().addHandler(this);
        // bbox.setSize(640, 480);
        // System.out.println("bb" + bbox);

        Boolean s = (Boolean) attr.get("resize");
        if (s != null) {
            resizable = s;
        }
        s = (Boolean) attr.get("embedded");
        if (s != null) {
            embedded = s;
        }
        String ap = (String) attr.get("autoplay");
        if (ap != null) {
            if (ap.equalsIgnoreCase("true"))
                autoplay = true;
            else if (ap.equalsIgnoreCase("false"))
                autoplay = false;
        }
        if (autoplay)
            pipeline.setState(Element.PLAY);
        else
            pipeline.setState(Element.PAUSE);
        try {

            if (isfile) {
                durationBytes = new File(uri).length();
                duration = new FileDurationScanner().getDurationForFile(new File (uri));
            } else  duration = new DurationScanner().getDurationForURL(uri.toURL(),
                    "" , "");
            System.out.println("Duration for" + uri.toURL() +duration);
          getBrowser().eventq(TimedMedia.MSG_GOT_DURATION,
          new Double(duration));
        } catch (MalformedURLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public boolean formatNodeContent(Context cx, int start, int end) {
        int wh = Integers.parseInt(getAttr("width"), -1), hh = Integers
        .parseInt(getAttr("height"), -1);

        // if (wh != -1 && hh != -1) {
        // //w=wh;h=hh;
        // resizable = true;
        // bbox.setSize(wh,hh);
        // }
        return !valid_;
    }

    @Override
    public boolean eventBeforeAfter(AWTEvent e, Point rel) {

        if (e.getID() == MouseEvent.MOUSE_ENTERED) {
            //if (embedded) {
            paintInterface = true;
            repaint();
            //    }
        }
        if (e.getID() == MouseEvent.MOUSE_EXITED) {
            // if (embedded) {
            paintInterface = false;
            repaint();
            // }
        }
        return super.eventBeforeAfter(e, rel);
    }

    @Override
    public boolean eventNode(AWTEvent e, Point rel) {
        if (e.getID() == TreeEvent.STOP) {
            stop_ = true;
            pipeline.setState(Element.STOP);
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
        if (e.getID() == MouseEvent.MOUSE_CLICKED && h - rel.y > 15) {
            if (getStatus() == Status.PLAY)
                setStatus(Status.PAUSE);
            else if (getStatus() == Status.PAUSE || getStatus() == Status.STOP)
                setStatus(Status.PLAY);
        } else if (e.getID() == MouseEvent.MOUSE_CLICKED && h - rel.y < 15) {
            rel.translate(-1, -(h - 15));
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
                Rectangle area = new Rectangle(14 + 18, 0, (w - (14 + 24 + 2)),
                        14);
                if (area.contains(rel)) {
                    double percent = (double) (rel.x - 14 + 24 + 2) / w;
                    doSeek(percent);
                }

            }

        }

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

    public void handleMessage(Message msg) {

        //System.out.println("*** "+ msg.getType() + " " + msg.toString());
        switch (msg.getType()) {
        case Message.WARNING:
            System.out.println(msg.toString());
            break;
        case Message.ERROR:
            pipeline.setState(Element.STOP);
            currentStatus = Status.ERROR;
            getBrowser().eventq(TimedMedia.MSG_STATUS_CHANGED, currentStatus);
            System.out.println(msg.toString());
            isError = true;
            break;
        case Message.EOS:
            if (!isError) {
                pipeline.setState(Element.STOP);
                currentStatus = Status.STOP;
                getBrowser().eventq(TimedMedia.MSG_STATUS_CHANGED, currentStatus);
            }

            break;
        case Message.STREAM_STATUS:
            // System.out.println("STATTT" + msg.toString());

            break;
        case Message.RESOURCE:
            getBrowser().eventq(TimedMedia.MSG_RES, msg.parseResourceString());
            break;
        case Message.DURATION:
//            long duration;
//
//            duration = msg.parseDurationValue();
//            System.out.println( msg.parseDurationFormat());
//            getBrowser().eventq(TimedMedia.MSG_GOT_DURATION,
//                    new Double(duration));
            break;
        case Message.TAG:
            System.out.println("TAG" + msg.toString());
            break;
        case Message.BUFFERING:

            busy = msg.parseBufferingBusy();
            percent = msg.parseBufferingPercent();
            if (busy && !isfile ) {
                if (!isBuffering) {
                    Debug.log(Debug.INFO, "PAUSE: we are buffering");
                    if (currentStatus == Status.PLAY) {
                        pipeline.setState(Element.PAUSE);
                        currentStatus = Status.PAUSE;
                    }
                    isBuffering = true;

                }
            } else {
                if (isBuffering) {
                    Debug.log(Debug.INFO, "PLAY: we finished buffering");
                    if (currentStatus == Status.PAUSE) {
                        pipeline.setState(Element.PLAY);
                        currentStatus = Status.PLAY;

                    }
                    isBuffering = false;
                }
            }
            break;
        case Message.STATE_CHANGED:
            if (msg.getSrc() == pipeline) {
                int old, next;
                old = msg.parseStateChangedOld();
                next = msg.parseStateChangedNext();

                switch (next) {
                case Element.PAUSE:
                    currentStatus = Status.PAUSE;
                    break;
                case Element.PLAY:
                    currentStatus = Status.PLAY;
                    break;
                case Element.STOP:
                    currentStatus = Status.STOP;
                    stopFull();
                    break;
                case Element.FAILURE:
                    currentStatus = Status.ERROR;
                    break;
                }
                getBrowser().eventq(TimedMedia.MSG_STATUS_CHANGED,
                        currentStatus);
                // System.out.println(currentStatus);
            }
            break;
        case Message.BYTEPOSITION:
            System.out.println(msg.parseBytePosition());
            break;
        default:
            // System.out.println(msg.getType() + " " +msg.toString());
        }
    }

    public Image createImage(ImageProducer object) {

        return im;
    }

    // public Graphics getGraphics() {
    // return im.getGraphics();
    // }

    public void doSeek(double aPos) {
        boolean res;
        com.fluendo.jst.Event event;
        System.out.println("SEEE  "
                + (int) (aPos * 100.0 * Format.PERCENT_SCALE));
        /* get value, convert to PERCENT and construct seek event */
        event = com.fluendo.jst.Event.newSeek(Format.PERCENT,
                (int) (aPos * 100.0 * Format.PERCENT_SCALE));

        /* send event to pipeline */
        res = pipeline.sendEvent(event);
        if (!res) {
            Debug.log(Debug.WARNING, "seek failed");
        }
    }

    public double getPlayPosition() {
        return (double) pipeline.getPosition() / Clock.SECOND;
    }

    public double getPlayPositionPercent() {
        double now = (double) pipeline.getPosition() / Clock.SECOND;
        double percent = now / duration;
        return percent;
    }

    public void newSeek(double aPos) {
        doSeek(aPos);
    }

    public Rectangle getSize() {
        // System.out.println(bbox);
        return bbox;
    }

    int rot;

    public boolean paintNodeContent(Context cx, int start, int end) {

        Graphics2D g = cx.g;
        Color or = g.getColor();
        RenderingHints rh = g.getRenderingHints();
        g.setRenderingHint(RenderingHints.KEY_RENDERING,
                RenderingHints.VALUE_RENDER_SPEED);
        // System.out.println(pw + " "+ ph);
        // System.out.println(w + " "+ h);

        // if (im != null) {
        // if (resizable) {
        // // WritableRaster wr = Raster.
        // if (im2.validate(getBrowser().getGraphicsConfiguration()) ==
        // VolatileImage.IMAGE_INCOMPATIBLE) {
        // im2 = getBrowser().createVolatileImage(w, h);
        // }// javax.imageio.ImageIO.
        // Graphics2D g2 = im2.createGraphics();
        // g2.drawImage(im, null, 0, 0);
        // g2.dispose();
        // g.drawImage(im, 0, 0, bbox.width, bbox.height, null);
        // // System.out.print("a");
        // } else {
        // g.drawImage(im, null, 0, 0);
        // // g.drawImage(im2, 0,0,null);
        // }
        //			
        //			
        // }
        //
        if (fullf != null) {

        } else if (im != null) {
            if (resizable) {
                // // WritableRaster wr = Raster.
                // if (im2.validate(getBrowser().getGraphicsConfiguration()) ==
                // VolatileImage.IMAGE_INCOMPATIBLE) {
                // im2 = getBrowser().createVolatileImage(w, h);
                // }// javax.imageio.ImageIO.
                // Graphics2D g2 = im2.createGraphics();
                // g2.drawImage(im, null, 0, 0);
                // g2.dispose();
                double r2 = ((double) bbox.getWidth())
                / ((double) bbox.getHeight());
                double r1 = ((double) w) / ((double) h);
                double d1 = ((double) bbox.getWidth()) / w;
                double d2 = ((double) bbox.getHeight()) / h;
                if (r1 > r2) {
                    d2 = d1 * (r1 / r2);
                } else {
                    d1 = d2 * (r1 / r2);
                }
                // double r1 = ((double) bbox.width)/((double) bbox.height);
                // double r2 = ((double) w)/((double) h);
                // double d1 = ((double) bbox.width) / w;
                // double d2 = ((double) bbox.height) / h;
                // if (r1<1.0) {
                // d2 = d1 * r2;
                // } else {
                // d1 = d2 * r2;
                // }

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
        double now = (double) pipeline.getPosition() / Clock.SECOND;
        double percent = now / duration;

        long tt = System.currentTimeMillis() - prevtt;
        if (tt >= 500) {
            // System.out.println("now:"+now+" tot "+ duration +"percent"+
            // percent );
            fps = (float) ((lastfc - fnumber) / (tt / 500));
            // fps*=2;
            lastfc = fnumber;
            prevtt = System.currentTimeMillis();
            getBrowser().eventq(TimedMedia.MSG_PlAYTIMEPERCENT, percent);
            getBrowser().eventq(TimedMedia.MSG_PlAYTIME, getPlayPosition());
        }
        // g.setColor(Color.white);
        // g.drawString("" + fps, 10, 10);
        if (paintInterface)
            paintInterface(g, w, h);
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

    private void paintInterface(Graphics2D g, int w, int h) {
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
        time = (int) getPlayDuration();
        sec = time % 60;
        min = time / 60;
        hour = min / 60;
        min %= 60;
        String ss = "" + hour + ":" + (min < 10 ? "0" + min : "" + min) + ":"
        + (sec < 10 ? "0" + sec : "" + sec);
        if (isBuffering )
            ss+=" Buffering " + percent +"%";
        g.setColor(Color.black);
        if (getPlayDuration() > 0)
            g.drawString(s + "  of " + ss, 14 + 29, 12);
        g.translate(-1, -(h - 15));

    }

    public void imageComplete(int status) {
        // System.out.println("imComplete " + status);
        cnumber++;
        if (fullf != null)
            fullf.repaint();
        else
            repaint();

        // System.out.println(fnumber+" "+cnumber);

    }

    public void setColorModel(ColorModel model) {
        icm = model;
    }

    public void setDimensions(int width, int height) {
        if (im == null || w != width || h != height) {
            System.out.println("Set w" + width + " h " + height);
            w = width;
            h = height;
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

                im = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
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
                im = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
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

    @Override
    public void remove() {
        // TODO Auto-generated method stub
        super.remove();
        close();
    }

    public void close() {

        if (pipeline != null) {
            System.out.println("video close: " + uri);
            pipeline.setState(Element.STOP);
            pipeline.shutDown();
            pipeline = null;
        }
    }

    public double getPlayDuration() {
        // if (pipeline.getDuration()!=0){
        // System.out.println("PIPELINE DURATION;"+pipeline.getDuration());
        // return (double) pipeline.getDuration();
        // }
        // else
        return duration;
    }

    public DurationUnit getDurationUnit() {

        return DurationUnit.BYTES;
    }

    public double getPosition() {
        return getPlayPosition();
    }

    public Status getStatus() {
        return currentStatus;
    }

    public Status setStatus(Status st) {
        Status pr = currentStatus;
        switch (st) {
        case PLAY:
            pipeline.setState(Element.PLAY);
            break;
        case STOP:
            pipeline.setState(Element.STOP);
            break;
        case PAUSE:
            pipeline.setState(Element.PAUSE);
            break;
        case ERROR:
            pipeline.setState(Element.FAILURE);
        default:
            break;
        }

        return pr;

    }

    public double getDuration() {
        double a = getPlayDuration();
        if (a != 0)
            return a;

        else
            return duration;
    }

    public DurationUnit getPositionUnit() {
        return DurationUnit.SEC;
    }

    public boolean setPosition(double dest) {
        boolean res;
        com.fluendo.jst.Event event;
        event = com.fluendo.jst.Event.newSeek(Format.PERCENT,
                (int) (dest * 100.0 * Format.PERCENT_SCALE));
        res = pipeline.sendEvent(event);
        return res;
    }

    Status currentStatus;
    public int aspectx;
    public int aspecty;

    public BufferedImage getBufferedImage() {

        return im;
    }

    public boolean getDisplayGlobalUI() {
        return !embedded;
    }
}
