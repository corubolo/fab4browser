package uk.ac.liverpool.media.jcodecma;



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
import java.awt.image.MemoryImageSource;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.Map.Entry;

import org.jcodec.samples.player.pipeline.ComponentSink;
import org.jcodec.samples.player.pipeline.JAVCSource;
import org.jcodec.samples.player.pipeline.Pipeline;
import org.jcodec.samples.player.pipeline.Sink;
import org.jcodec.samples.player.utils.FrameUtils;

import multivalent.Behavior;
import multivalent.Context;
import multivalent.Document;
import multivalent.INode;
import multivalent.Layer;
import multivalent.Leaf;
import multivalent.TreeEvent;
import uk.ac.liverpool.fab4.ImageInternalDataFrame;
import uk.ac.liverpool.fab4.behaviors.TimedMedia;


/**
 * Implements a video frame for the jcodec
 */
public class LeafVideoJCodec extends Leaf  implements TimedMedia, Sink {


    int fWidth, fHeight;
    boolean resizable = false;


    boolean autoplay = true;
    double duration;
    protected int durationFormat;

    protected boolean embedded = false;
    protected FrameFull fullf;
    protected boolean paintInterface = false;
    private long originaltt;
    private boolean isfile;
    private long durationBytes;
    private Pipeline pipeline;


    private int[] buffer;
    private Image lastFrame;





    public LeafVideoJCodec(String name, Map<String, Object> attr, INode parent) 
    throws URISyntaxException, IOException {
        super(name, attr, parent);
        setFullScreen(false);
        if (attr == null)
            return;
       
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
        String scheme = uri.getScheme();
        JAVCSource javcSource;
        if (scheme.contains("file")) {
            isfile  = true;
            File file = new File(uri);
            durationBytes = file.length();
            javcSource = new JAVCSource(file); 
        } else 
        {
            javcSource = new JAVCSource(uri.toURL()); 
        }

        if (pipeline != null)
            pipeline.stopPipeline();

        pipeline = new Pipeline(javcSource, this, 25);

        pipeline.start();
        pipeline.play();

    }





    synchronized void startFull() {
        //        if (im == null || fullf != null)
        //            return;
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

            bbox.setSize(w1, h1);
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




        public void setDimensions(int width, int height) {
            System.out.println("Set Dimensions called");
          
        }

    public void setHints(int hintflags) {
        // System.out.println("HF" + hintflags);

    }

    public void setPixels(int x, int y, int w, int h, ColorModel model,
            byte[] pixels, int off, int scansize) {
        // im.setRGB(x, y, w, h, pixels, off, scansize);
        System.out.println("This should not happen!");

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
    private URI uri;
    private long prevtt;
    private int lastfc;
    private int fnumber;
    private float fps;



    public boolean getDisplayGlobalUI() {
        return !embedded;
    }




    private void displayFormatInfo( Document doc){
        try {
            StringBuffer sb = new StringBuffer(5000);

            //			sb.append("Encoding: "+vf.getEncoding()+"\n");
            //			sb.append("FrameRate: "+vf.getFrameRate()+"\n");
            //			sb.append("MaxDataLength: "+vf.getMaxDataLength()+"\n");
            //			sb.append("Width: "+vf.getSize().width+"\n");
            //			sb.append("Height: "+vf.getSize().height+"\n");
            //			sb.append("Duration-secs: "+processor.getDuration().getSeconds()+"\n");
            //			
            displayFormatInfo(sb, doc);
        } catch (Exception e) {
            e.printStackTrace();
        } 
    }



    @Override
    public boolean eventNode(AWTEvent e, Point rel) {
        if (e.getID() == TreeEvent.STOP) {

            setStatus(Status.STOP);
        }

        if (e.getID() == MouseEvent.MOUSE_CLICKED && fHeight - rel.y > 15) {
            if (getStatus() == Status.PLAY)
                setStatus(Status.PAUSE);
            else if (getStatus() == Status.PAUSE)
                setStatus(Status.PLAY);
        } else if (e.getID() == MouseEvent.MOUSE_CLICKED && fHeight - rel.y < 15) {
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
                Rectangle area = new Rectangle(14 + 18, 0, (fWidth - (14 + 24 + 2)),
                        14);
                if (area.contains(rel)) {
                    double percent = (double) (rel.x - 14 + 24 + 2) / fWidth;
                    //					doSeek(percent);
                }

            }

        }

        return super.eventNode(e, rel);
    }


    private void paintInterface(Graphics2D g, int w, int h) {
        //System.out.println("painting interface");
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
        //g.fillRect(14 + 26, 2,
        //(int) (getPlayPositionPercent() * (w - (14 + 24 + 2))), 10);
        int time = (int) getPosition();
        int sec = time % 60;
        int min = time / 60;
        int hour = min / 60;
        min %= 60;
        String s = "" + hour + ":" + (min < 10 ? "0" + min : "" + min) + ":"
        + (sec < 10 ? "0" + sec : "" + sec);
        g.setColor(Color.black);
        g.drawString(s + " " + getDuration(), 14 + 29, 12);
        g.translate(-1, -(h - 15));
    }


    public boolean paintNodeContent(Context cx, int start, int end) {

        Graphics2D g = cx.g;
        Color or = g.getColor();
        RenderingHints rh = g.getRenderingHints();
        g.setRenderingHint(RenderingHints.KEY_RENDERING,
                RenderingHints.VALUE_RENDER_SPEED);

        if (fullf != null) {

        } else if (lastFrame != null) {
            if (resizable) {
                double d1 = ((double) bbox.width) / fWidth;
                double d2 = ((double) bbox.height) / fHeight;

                AffineTransform a = AffineTransform.getScaleInstance(d1, d2);
                g.drawImage(lastFrame, 0, 0, lastFrame.getWidth(null), lastFrame.getHeight(null), getBrowser());
                //long startPaintTime = System.currentTimeMillis();
                //g.drawRenderedImage(im, a);
                //long endPaintTime = System.currentTimeMillis();
            } else {
                g.drawImage(lastFrame, 0, 0, lastFrame.getWidth(null), lastFrame.getHeight(null), getBrowser());
            }
        }
        //long finishedDrawTime2 = System.currentTimeMillis();
        long tt = System.currentTimeMillis() - prevtt;
        if (tt >= 500) {
            fps = (float) ((lastfc - fnumber) / (tt / 500));
            // fps*=2;
            lastfc = fnumber;
            prevtt = System.currentTimeMillis();
            //			getBrowser().eventq(TimedMedia.MSG_PlAYTIMEPERCENT,
            //					getPlayPositionPercent());
            getBrowser().eventq(TimedMedia.MSG_PlAYTIME,
                    getPosition());
        }

        //long finishedEventQsTime3 = System.currentTimeMillis();

        g.setColor(Color.white);
        g.drawString("" + fps, 10, 10);
        if (paintInterface)
            paintInterface(g, fWidth, fHeight);
        fnumber++;

        g.setRenderingHints(rh);
        g.setColor(or);

        /*long finishedNodeContentTime4 = System.currentTimeMillis();

		stage1 += finishedRenderTime1-startNodeContentTime0;
		stage2 += finishedDrawTime2-finishedRenderTime1;
		stage3 += finishedEventQsTime3-finishedDrawTime2;
		stage4 += finishedNodeContentTime4-finishedEventQsTime3;
		System.out.println("PAINT: Stage1: "+stage1+" Stage2: "+stage2+" Stage3: "+stage3+
				" Stage4: "+stage4);
		//System.out.println("PlayPosition %: "+ getPlayPositionPercent());*/
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






    public Status setStatus(Status st) {
        Status prev = currentStatus;
        switch (st) {
        case PLAY:
            //if (processor.getState() == Controller.Started)
            if (currentStatus==Status.PAUSE){
                System.out.println("setStatus: paused-to-play");
                pipeline.play();
                //processor.start();
                currentStatus = Status.PLAY;
                getBrowser().eventq(TimedMedia.MSG_STATUS_CHANGED,
                        currentStatus);
            }else{
                System.out.println("setStatus: stopOrPlay-to-play");
                //				processor.start();
                currentStatus = Status.PLAY;
                getBrowser().eventq(TimedMedia.MSG_STATUS_CHANGED,
                        currentStatus);
            }
            break;
        case STOP:
            System.out.println("setStatus: stop");
            pipeline.stop();
            //			processor.setMediaTime(new Time(0));
            currentStatus = Status.STOP;
            getBrowser().eventq(TimedMedia.MSG_STATUS_CHANGED,
                    currentStatus);
            break;
        case PAUSE:
            System.out.println("setStatus: pause");
            pipeline.pause();
            //			processor.stop();
            currentStatus = Status.PAUSE;
            getBrowser().eventq(TimedMedia.MSG_STATUS_CHANGED,
                    currentStatus);
            break;
        default:
            break;
        }

        return prev;

    }


    public DurationUnit getDurationUnit() {
        // TODO Auto-generated method stub
        return DurationUnit.SEC;
    }



    @Override
    public void remove() {
        System.out.println("remove: "+ uri);
        super.remove();
        close();
    }


    public void close() {
        if (pipeline != null){
            setStatus(Status.STOP);
            System.out.println("video close: "+ uri);
            pipeline = null;
        }

    }

    public double getDuration() {
        // TODO Auto-generated method stub
        return 0;
    }

    public double getPosition() {
        // TODO Auto-generated method stub
        return 0;
    }

    public boolean setPosition(double d) {
        // TODO Auto-generated method stub
        return false;
    }





    public void display(org.jcodec.samples.player.pipeline.Frame f) {
        int bufferSize = f.getWidth() * f.getHeight();
        
        
        if (buffer == null || bufferSize != buffer.length) {
            buffer = new int[bufferSize];

            bbox.width = f.getWidth();

            bbox.height = f.getHeight();
             }

        FrameUtils.YUV2RGB(f, buffer);

        lastFrame = getBrowser().createImage(new MemoryImageSource(f.getWidth(), f
                .getHeight(), buffer, 0, f.getWidth()));
        markDirty();
        reformat(this);
        if (fullf != null)
            fullf.repaint();
        else
            repaint();



    }





    public void start() {
        // TODO Auto-generated method stub

    }





    public void stop() {
        // TODO Auto-generated method stub

    }





    public void forcePaint() {
        if (lastFrame == null)
            return;

        if (fullf != null)
            fullf.repaint();
        else
            repaint();

    }







}