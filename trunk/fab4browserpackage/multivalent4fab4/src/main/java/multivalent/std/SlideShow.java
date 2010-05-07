package multivalent.std;

import java.awt.*;
//import java.awt.image.BufferStrategy;
import java.awt.event.MouseEvent;
import java.awt.event.KeyEvent;
import java.awt.geom.AffineTransform;
import java.util.TimerTask;

import multivalent.*;
import multivalent.std.ui.Multipage;
import multivalent.gui.VMenu;
import multivalent.node.Root;



/**
	Full screen slide show for multipage documents.
	Almost get same effect by enlarging window and zooming.

	@see SlideShowLinks

	@version $Revision: 1.6 $ $Date: 2002/06/27 05:27:40 $
*/
public class SlideShow extends Behavior /*implements EventListener*/ {
  static final boolean DEBUG = false;

  /**
	Take over full screen play slide show.
	<p><tt>"slideShow"</tt>.
  */
  public static final String MSG_START = "slideShow";

  /**
	Stop full screen slide show, returning to normal window size.
	<p><tt>"slideShow"</tt>.
  */
  public static final String MSG_STOP = "slideShowStop";

  static final int EDGE_MARGIN = 5;

  /**
	Toggle auto/manual advance.
	<p><tt>"slideshowSetAuto"</tt>.
  public static final String MSG_SETAUTO = "slideshowSetAuto";

  int interval_;
  boolean auto_ = true;   // attribute?
  private boolean skip_ = false;
  */
  GraphicsDevice gd_ = null;
  Frame full_ = null;
  Document doc_ = null;
  Rectangle bboxin_ = new Rectangle();
  Root root_ = null;
  TimerTask tt_ = null;
  //BufferStrategy savedstrat_;


  class FrameFull extends Frame {
	public FrameFull() {    // inner class doesn't like constructor
		enableEvents(~0L);
		setUndecorated(true);
	}

	protected void processEvent(AWTEvent e) {
		if (doc_!=null) doc_.getBrowser().event(e);     // doc_==null when delayed event from closing window
	}

	public void update(Graphics g) { paint(g); }    // don't clear background (no need)

	// just paint relevant Document, not GUI or anything else
	public void paint(Graphics g_old_api) {
//System.out.println("FrameFull paint ");
		Graphics2D g = (Graphics2D)g_old_api;
//System.out.println("clip to "+g.getClipBounds());

		// root before -- always do root observers, even though bypass root itself here
		java.util.List<Behavior> obs = root_.getObservers();
		Context rootcx = root_.getStyleSheet().getContext(g, null);
		int i=0, imax=0;
		if (obs!=null) for (imax=obs.size(); i<imax; i++) if (obs.get(i).paintBefore(rootcx, root_)) break;

		Context cx = doc_.getStyleSheet().getContext(g, null);
		cx.valid = false;

		AffineTransform xformin = g.getTransform();

		int w=full_.getWidth(), h=full_.getHeight();
		Rectangle docclip = new Rectangle(0,0, w,h);
		int dx=doc_.dx(), dy=doc_.dy();
		g.translate(-dx,-dy); docclip.translate(dx,dy); // conteract dx()/dy() in paintBeforeAfter -- but reformatted to (0,0) anyhow


		// format
		//assert !doc_.isValid();
		doc_.formatBeforeAfter(bboxin_.width, bboxin_.height, cx);
		Rectangle bboxfull = new Rectangle(0,0, doc_.getHsb().getMax()-doc_.getHsb().getMin(), doc_.getVsb().getMax()-doc_.getVsb().getMin());
//System.out.println("formatted? "+doc_.isValid()+", bbox full = "+bboxfull+" vs "+doc_.bbox);
		if (bboxfull.width != doc_.bbox.width || bboxfull.height != doc_.bbox.height) {
			doc_.setValid(false);    // doc_.markDirtySubtree(true); -- content OK
			doc_.bbox.setSize(bboxfull.width, bboxfull.height);     // give document all it wants, then scale to fit screen when paint
		}



		// scale
		//if (i==imax) => don'
		double sx = (w - EDGE_MARGIN)/(double)bboxfull.width, sy = (h - EDGE_MARGIN)/(double)bboxfull.height, scale = Math.min(sx, sy);     // uniformly scale up to full screen -- no integer division
		if (Math.abs(sx - sy)>0.001) { g.setColor(Color.BLACK); g.fillRect(0,0,w,h); }
//System.out.println("scale to "+w+"x"+h+": min "+sx+" "+sy+" = "+scale);
//System.out.println("translate to ("+((w - scale * bboxfull.width)/2)+","+((h - scale * bboxfull.height)/2)+")");
		g.translate((w - scale * bboxfull.width)/2, (h - scale * bboxfull.height)/2);    // center horizontally and vertically
		g.scale(scale, scale);
		doc_.paintBeforeAfter(docclip/*g.getClipBounds()*/, cx);   // format on demand
		//g.setColor(Color.RED); g.drawLine(0,0, 200,200);

		//g.dispose(); //-- causes flashing => don't create!

		g.translate(dx,dy); docclip.translate(-dx,-dy);
		g.setTransform(xformin);


		// root after
		for ( ; i>=0; i--) if (obs.get(i).paintAfter(rootcx, root_)) break;
	}
  }



  void start() {
	Browser br = getBrowser();
	//br.getOffImage().flush();   // interference with VolatileImage, but Browser can't be bypassed so just reconstruct

	//savedstrat_ = br.getBufferStrategy();
	//br.createBufferStrategy(0);

	doc_ = br.getCurDocument();
	bboxin_.setBounds(doc_.bbox);

	root_ = br.getRoot();
	root_.addObserver(this);
//System.out.println("ss on, doc_="+doc_.getFirstLeaf());

	//br.setVisible(false);   // iconify -- still get events?

	// bail out in a few seconds in case something goes wrong
	if (DEBUG) {
		tt_ = new TimerTask() {
		  public void run() { stop(); System.out.println("EXPIRE");  }
		};
		getGlobal().getTimer().schedule(tt_, 60*1000);
	}

	GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
	gd_ = ge.getDefaultScreenDevice();
	full_ = new FrameFull();
	//full_ = (Frame)br.getParent(); //full_.setUndecorated(true); -- can do to a displayed Frame

	gd_.setFullScreenWindow(full_);
//System.out.println("full screen supported? "+gd_.isFullScreenSupported());
  }


  // back to normal
  void stop() {
	if (!isActive()) return;    // already stopped
//System.out.println("off");

	if (isActive()) {
		gd_.setFullScreenWindow(null);
		gd_ = null;
		full_.dispose();
		full_ = null;
	}

	doc_.bbox.setBounds(bboxin_);
	doc_.setValid(false);
	doc_ = null;  // delayed event from closing window
	root_.deleteObserver(this);
	//root_.markDirtySubtree(true);
	root_.setValid(false);  // just in case
//System.out.println("root_: valid="+root_.isValid()+", "+root_.bbox+", lost="+((java.awt.image.VolatileImage)getBrowser().getOffImage()).contentsLost());
	root_ = null;

	if (tt_!=null) { tt_.cancel(); tt_ = null; }


	Browser br = getBrowser();
	//br.setBuffer... no such method
	//try { br.createBufferStrategy(2, null/*savedstrat_.getCapabilities()*/); } catch (AWTException ignore) { System.err.println("can't restore buffer"); }

	br.requestFocus();
	br.repaint(1000L);
	//br.getOffImage().flush();     // what's needed in practice, at least on Windows
	//br.setVisible(true);
  }

  public boolean isActive() { return gd_!=null; }


  /**
	"Slide Show" in menu.
  */
  public boolean semanticEventBefore(SemanticEvent se, String msg) {
	if (super.semanticEventBefore(se,msg)) return true;
/*	else if (SLIDESHOWNAME.equals(br.getName())) {
		if (Browser.MSG_CREATE_TOOLBAR==msg) {
			VCheckbox cb = (VCheckbox)createUI("checkbox", "Auto", "event "+MSG_SETAUTO, (INode)se.getOut(), null/*toggle* /, false);
			cb.setState(auto_);
		}

	} else if (Document.MSG_FORMATTED==msg) {
	}*/ else {
		if (VMenu.MSG_CREATE_GO==msg) {
			createUI("button", "Full Screen Slide Show", "event "+MSG_START, (INode)se.getOut(), "GoPan", false);
		}
	}
	return false;
  }

  /** Start slide show, toggle auto, ... */
  public boolean semanticEventAfter(SemanticEvent se, String msg) {
	if (MSG_START==msg) {
		start();
		return true;

	} else if (MSG_STOP==msg) {
		stop();
		return true;
	}

	return super.semanticEventAfter(se,msg);
  }


  // tree-based behaviors not called unless active
  public boolean paintBefore(Context cx, Node node) {
	//full_.repaint(50);    // repaint requests go to Browser not FrameFull -- delay to after Browser's VolatileImage
	return true;
  }
  public boolean paintAfter(Context cx, Node node) { return true; }


  /** Arrow keys, escape, home/end, mouse clicks. */
  //public void event(AWTEvent e) {
  public boolean eventBefore(AWTEvent e, Point rel, Node n) {    // not called unless active
	int id = e.getID();
//System.out.println("slide show event = "+id);

	String msg = null;
	if (KeyEvent.KEY_PRESSED==id) { // could be KEY_TYPED
		KeyEvent ke = (KeyEvent)e;
		switch (ke.getKeyCode()) {
		case KeyEvent.VK_RIGHT: case KeyEvent.VK_DOWN: case KeyEvent.VK_PAGE_DOWN: case KeyEvent.VK_ENTER: case ' ': case '+': case 'n'/*ext*/: msg = Multipage.MSG_NEXTPAGE; break;
		case KeyEvent.VK_LEFT: case KeyEvent.VK_UP: case KeyEvent.VK_PAGE_UP: case KeyEvent.VK_BACK_SPACE: case '-': case 'p'/*revious*/: msg = Multipage.MSG_PREVPAGE; break;
		case KeyEvent.VK_HOME: case '1': case ',': msg = Multipage.MSG_FIRSTPAGE; break;
		case KeyEvent.VK_END: case '.': msg = Multipage.MSG_LASTPAGE; break;
		// numbers set arg for next command? g=reset arg?

		case KeyEvent.VK_ESCAPE: case 'q': msg = MSG_STOP; break;

		default: //assert false: => just ignore
		}

	} else if (MouseEvent.MOUSE_CLICKED==id) {
		MouseEvent me = (MouseEvent)e;
		msg = (me.getClickCount()>1? MSG_STOP: me.getButton()==1? Multipage.MSG_NEXTPAGE: Multipage.MSG_PREVPAGE);
	}

//System.out.println("msg = "+msg);
	if (msg!=null) {
		Layer layers = doc_.getLayers();
		SemanticEvent se = new SemanticEvent(getBrowser(), msg, null);
		boolean shortcircuit = layers.semanticEventBefore(se, msg) | layers.semanticEventAfter(se, msg);
		//getBrowser().repaint(200L);
		if (isActive()) full_.repaint(200L);
		//if (isActive()) full_.update(full_.getGraphics());
	}

	//return msg!=null;
	return true;
  }

  public boolean eventAfter(AWTEvent e, Point rel, Node n) { return true; }
}
