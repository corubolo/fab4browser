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
import java.util.HashMap;
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
import javax.media.control.TrackControl;
import javax.media.format.AudioFormat;
import javax.media.format.RGBFormat;
import javax.media.format.VideoFormat;
import javax.media.renderer.VideoRenderer;

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
public class LeafVideoJMFFormat extends Leaf implements
	TimedMedia, ControllerListener, VideoRenderer {

	private long lastfc;
	private float fps;

	
	protected Processor processor = null;
	Object waitSync = new Object();
	boolean stateTransitionOK = true;
	protected URI uri;
        protected boolean stop_;
        // private VolatileImage im2;
        protected BufferedImage im=null;
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
                //System.out.println("imComplete " + status);
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
                //System.out.println("Set Dimensions called");
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

 
	public LeafVideoJMFFormat(String name, Map<String, Object> attr, INode parent) 
			throws URISyntaxException, IOException, NoPlayerException, CannotRealizeException {
		super(name, attr, parent);
		setFullScreen(false);
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
		TrackControl audioTrack = null;
		
		for (int i = 0; i < tc.length; i++) {
			System.out.println("Format "+i+": "+ tc[i].getFormat().toString());
			if (tc[i].getFormat() instanceof VideoFormat) {
				videoTrack = tc[i];
				//break;
			}else if (tc[i].getFormat() instanceof AudioFormat){
				audioTrack = tc[i];
				//break;
			}
		}
		if (videoTrack == null) {
			System.err.println("The input media does not contain a video track.");
		}
		if (audioTrack == null) {
			System.out.println("The input media does not contain an audio track.");
		}else{
			System.out.println("An audio track exists in this file.");
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
		getBrowser().eventq(TimedMedia.MSG_PlAYTIME, duration);
		//processor.setRate((float) 2);
		System.out.println("Rate: "+ getRate());
		
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
		
		displayFormatInfo((VideoFormat)videoTrack.getFormat(), parent.getDocument());
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
					doSeek(percent);
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
		g.fillRect(14 + 26, 2,
		(int) (getPlayPositionPercent() * (w - (14 + 24 + 2))), 10);
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

	
	//public void newSeek(double aPos) {
	// doSeek(aPos);
	//}
	
	//Debug: trying to find what takes so long during paint
	//static long stage1=0,stage2=0,stage3=0,stage4=0;
	
	public boolean paintNodeContent(Context cx, int start, int end) {
	    //System.out.println("paintNodeContent");
		//long startNodeContentTime0 = System.currentTimeMillis();
		
		Graphics2D g = cx.g;
		Color or = g.getColor();
		RenderingHints rh = g.getRenderingHints();
		g.setRenderingHint(RenderingHints.KEY_RENDERING,
		RenderingHints.VALUE_RENDER_SPEED);
		
		//long finishedRenderTime1 = System.currentTimeMillis();
		
		if (fullf != null) {
		
		} else if (im != null) {
			if (resizable) {
				double d1 = ((double) bbox.width) / fWidth;
				double d2 = ((double) bbox.height) / fHeight;
				
				AffineTransform a = AffineTransform.getScaleInstance(d1, d2);
				
				//long startPaintTime = System.currentTimeMillis();
				g.drawRenderedImage(im, a);
				//long endPaintTime = System.currentTimeMillis();
			} else {
				g.drawImage(im, 0, 0, null);
			}
		}
		//long finishedDrawTime2 = System.currentTimeMillis();
		long tt = System.currentTimeMillis() - prevtt;
		if (tt >= 500) {
			fps = (float) ((lastfc - fnumber) / (tt / 500));
			// fps*=2;
			lastfc = fnumber;
			prevtt = System.currentTimeMillis();
			getBrowser().eventq(TimedMedia.MSG_PlAYTIMEPERCENT,
					getPlayPositionPercent());
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
	
	
	
	public double getDuration() {		
		//System.out.println("Duration: "+duration);		
		return processor.getDuration().getSeconds();		
	}
	
	
	public float getRate(){
		return processor.getRate();
	}
	
	public double getPosition() {
		return processor.getMediaTime().getSeconds();
	}
	
	public Graphics getGraphics() {
		return im.getGraphics();
	}
	
	public double getPlayPositionPercent() {
		double ppp = getPosition() / getDuration();
		getBrowser().eventq(TimedMedia.MSG_PlAYTIMEPERCENT,	ppp);
		return ppp; 		
	}
	
	
	
	public boolean setPosition(double dest) {
		//processor.setMediaTime(new Time(dest));
		
		switch (currentStatus) {
		case PLAY:
			processor.stop();
			processor.syncStart(new Time(dest));
		case PAUSE:
			// Do nothing: can only do if stopped or playing
		case STOP:
			processor.setMediaTime(new Time(dest));
		}
	 
		return false;
	}
	
	public void doSeek(double aPercent) {
		double newPos = getDuration()*aPercent; 
		
		getBrowser().eventq(TimedMedia.MSG_SEEK, newPos);
	}
	
	public Status setStatus(Status st) {
		Status prev = currentStatus;
		switch (st) {
		case PLAY:
			//if (processor.getState() == Controller.Started)
			if (currentStatus==Status.PAUSE){
				System.out.println("setStatus: paused-to-play");
				processor.start();
				currentStatus = Status.PLAY;
				getBrowser().eventq(TimedMedia.MSG_STATUS_CHANGED,
						currentStatus);
			}else{
				System.out.println("setStatus: stopOrPlay-to-play");
				processor.start();
				currentStatus = Status.PLAY;
				getBrowser().eventq(TimedMedia.MSG_STATUS_CHANGED,
						currentStatus);
			}
			break;
		case STOP:
			System.out.println("setStatus: stop");
			processor.stop();
			processor.setMediaTime(new Time(0));
			currentStatus = Status.STOP;
			getBrowser().eventq(TimedMedia.MSG_STATUS_CHANGED,
					currentStatus);
			break;
		case PAUSE:
			System.out.println("setStatus: pause");
			processor.stop();
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
		if (processor != null){
			setStatus(Status.STOP);
			System.out.println("video close: "+ uri);
			processor = null;
		}
		if (im != null){
			System.out.println("image close.");
			im = null;
		}
	}
	
	
	
	/*public Buffer grabFrame() { 
		Buffer buf = null;
		if(im != null) {ImageToBuffer.createBuffer(im,(float)0);
			System.out.println("grabFrame");
		}
		repaint();
		return buf; 
	} */
	
	
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
	
	public Format[] getSupportedInputFormats() {
		return new Format[] {new RGBFormat() };
	}	
	
	public Format setInputFormat(Format format) {
		//vf = (RGBFormat) format;
		return format;
	}
	
	
	//private MemoryImageSource sourceImage;
	private Buffer lastBuffer;
	
	// Debug for checking bufferToImage and repaint times
	//long t1=0,t2=0,t3=0;
	//long BtoITime=0, repaintTime=0;
	
	public int process(Buffer buffer) {
		if (lastBuffer != buffer) {
		      lastBuffer = buffer;
		      //t1 = System.currentTimeMillis();
		      im = bufferToImage(buffer);
		      //t2 = System.currentTimeMillis();
		      //System.out.println(im);
		      // repaint();
		      //System.out.println("Process - repaint");
		      repaint();
		      //t3 = System.currentTimeMillis();
		}
		
		//BtoITime += (t2-t1);
		//repaintTime += (t3-t2);
		
		//System.out.println("BufferToImage: "+ BtoITime+" repaint(): "+ repaintTime);
		
		// if (last == buffer.getSequenceNumber())
		// return BUFFER_PROCESSED_OK;
		//System.out.println("P" + buffer.getSequenceNumber());
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