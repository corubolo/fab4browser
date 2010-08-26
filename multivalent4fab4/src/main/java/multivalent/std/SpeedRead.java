package multivalent.std;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.KeyEvent;
import java.util.Map;
import java.util.TimerTask;

import phelps.lang.Integers;

import com.pt.awt.NFont;

import multivalent.*;
import multivalent.gui.VFrame;
import multivalent.gui.VCheckbox;
import multivalent.node.LeafZero;
import multivalent.std.ui.DocumentPopup;



/**
	Starting at cursor/selection, flash words up at some rate,
	after and superior to <a href='http://www.halcyon.com/chigh/vortex.html'>Vortex</a>.
	Not sure if this is a good way to do it, but it only took two hours to implement,
	and makes a great example of Multivalent's advantages over that applet:
		don't need three copies of text, 
		can start at any point in document, 
		works on any page vs ones prepared with applet.

	@version $Revision: 1.4 $ $Date: 2002/02/01 03:01:42 $
*/
public class SpeedRead extends Behavior {
  /**
	Another semantic command, which should be given more descriptive name.
	<p><tt>"togglePause"</tt>.
  */
  public static final String MSG_PAUSE = "togglePause";

  /**
	Sets the delay between words, in milliseconds.
	<p><tt>"speedreadDelay"</tt>: <tt>arg=</tt> {@link java.lang.Number} <var>delay</var>.
  */
  public static final String MSG_DELAY = "speedreadDelay";

  /**
	Show words faster by decreasing the delay.
	<p><tt>"speedreadFaster"</tt>.
  */
  public static final String MSG_FASTER = "speedreadFaster";

  /**
	Another semantic command, which should be given more descriptive name.
	<p><tt>"speedreadSlower"</tt>.
  */
  public static final String MSG_SLOWER = "speedreadSlower";

  public static final String ATTR_DELAY = "delay";



  static final int DELAYINC=10, IDELAY=500;     // 10ms

  static Color BACKGROUND = new Color(0xe0,0xe0,0xe0);
  static int ix_=100, iy_=100, iwidth_=300, iheight_=100;
  static NFont bigwordFont = NFont.getInstance("Times", NFont.WEIGHT_NORMAL, NFont.FLAG_SERIF, 24f);

  Node point_ = null;
  boolean stop_ = false;
  int delay_;		// delay between words
  VFrame win_=null;

  TimerTask tt = null;


  /** Entries in frame popup: pause, faster, slower, .... */
  public boolean semanticEventBefore(SemanticEvent se, String msg) {
	if (super.semanticEventBefore(se,msg)) return true;
	else if (DocumentPopup.MSG_CREATE_DOCPOPUP==msg && se.getIn()==win_) {
		Browser br = getBrowser();
		INode menu = (INode)se.getOut();
		VCheckbox cb = (VCheckbox)createUI("checkbox", "Pause (also click in content)", new SemanticEvent(br, MSG_PAUSE, win_, this, null), menu, "SPECIFIC", false);
		cb.setState(stop_);
		//createUI("button", "Delay "+delay_+"ms", null, menu, "EDIT", true); => can't read number when disabled
		createUI("button", "Faster than "+delay_+"ms  (also '->')", new SemanticEvent(br, MSG_FASTER, win_, this, null), menu, "EDIT", delay_ <= DELAYINC);
		createUI("button", "Slower (also '<-')", new SemanticEvent(br, MSG_SLOWER, win_, this, null), menu, "EDIT", false);
		return true;
	}
	return false;
  }

  /** Take action: windowClosed, pause, faster, .... */
  public boolean semanticEventAfter(SemanticEvent se, String msg) {
	if (win_!=null && win_==se.getArg()) {
		if (VFrame.MSG_CLOSED==msg) destroy();

		else if (VFrame.MSG_MOVED==msg || VFrame.MSG_RESIZED==msg) {
			Rectangle r = win_.bbox;
			ix_=r.x; iy_=r.y; iwidth_=r.width; iheight_=r.height;

		} else if (MSG_PAUSE==msg) {
			stop_ = !stop_;
			// on unpause, reset delay
			//if (!stop_) lastPing_ = System.currentTimeMillis() + IDELAY;

		} else if (MSG_SLOWER==msg) delay_ += DELAYINC;

		else if (MSG_FASTER==msg) {
			if (delay_ - DELAYINC > 0) delay_ -= DELAYINC;

		} else if (MSG_DELAY==msg) {
		}
//System.out.println("delay ="+delay_);
		//else if speed "EDIT"
	} else if (CursorMark.MSG_SET==msg) {
		setStart();
		//lastPing_ += IDELAY;
	}

	return super.semanticEventAfter(se, msg);
  }


  public void destroy() {
	stop_ = true;
	tt.cancel();
	win_.deleteObserver(this);
	win_.remove();
	//getGlobal().getTimer().deleteObserver(this);
	getBrowser().repaint(100);	// or leave for next repaint

	super.destroy();
  }

  public boolean paintBefore(Context cx, Node node) {
	if (super.paintBefore(cx, node)) return true;
	else {
		Graphics2D g = cx.g;
		Rectangle r = win_.bbox;
		g.setColor(BACKGROUND/*Color.WHITE*/); //g.fill(r);		//g.fillRect(0,0, bbox_.width,bbox_.height);
		g.fillRect(0,0, r.width,r.height);
	}
	return false;
  }

  /** Draw in frame, rather than change content node, format, paint cycle -- probably wrong choice as doesn't compose. */
  public boolean paintAfter(Context cx, Node node) {
	// draw word, centered
	String txt = point_.getName();
//System.out.println("grab = "+getBrowser().getGrab());
	if (txt!=null && txt.length()>0) {
		Graphics2D g = cx.g;
		Shape clipin = g.getClip();

		Rectangle r = win_.getContentBounds();
		r.translate(-win_.bbox.x, -win_.bbox.y);
		g.clip(r);

		//g.setColor(BACKGROUND/*Color.WHITE*/); g.fill(r);		//g.fillRect(0,0, bbox_.width,bbox_.height);

		NFont f = bigwordFont;
		g.setColor(Color.BLACK);
		f.drawString(g, txt, (float)((r.width-f.stringAdvance(txt).getX())/2), (float)((r.height-f.getHeight())/2+f.getHeight()));

		g.setClip(clipin);
	}

	return super.paintAfter(cx, node);
  }

  /** Click in content area to pause. */
  public boolean eventBefore(AWTEvent e, Point rel, Node n) {
	if (e.getID() == MouseEvent.MOUSE_PRESSED) {
		Rectangle r = win_.getContentBounds();
		if (rel!=null && rel.y > r.y-win_.bbox.y) getBrowser().eventq(MSG_PAUSE, win_);
		//return true; -- resize
	}
	return false;
  }

  /** Arrow keys adjust speed (actually delay). */
  public boolean eventAfter(AWTEvent e, Point rel, Node n) {
	if (e.getID()==KeyEvent.KEY_PRESSED) {
		int key = ((KeyEvent)e).getKeyCode();
		Browser br = getBrowser();
		if (key==KeyEvent.VK_RIGHT) br.eventq(MSG_FASTER, win_);
		else if (key==KeyEvent.VK_LEFT) br.eventq(MSG_SLOWER, win_);
	}
	return false;
  }

  /** Show next word at next Timer heartbeat.
  public void event(AWTEvent e) {
	if (e.getID()==TreeEvent.HEARTBEAT) {
		long ms = System.currentTimeMillis();
		if (point_!=null && ms-lastPing_ > delay_ && !stop_) {
			lastPing_=ms;
		}
	}
  }*/

  void next() {
	if (point_!=null && !stop_) {
		point_ = point_.getNextLeaf();
		if (point_==null) destroy()/*getBrowser().eventq(VFrame.MSG_CLOSE, this)*/;
		else win_.getDocument()/*--compose with magnify, and rely on faster machines for faster readers*/.repaint(Math.min(delay_,100));
	}
  }


  /** Set current point from cursor/selection/first word in document. */
  void setStart() {
	Browser br = getBrowser();
	CursorMark curs = br.getCursorMark();
	Span span = br.getSelectionSpan();

	if (curs.isSet()) {
		point_ = curs.getMark().leaf;
		curs.move(null,-1);
	} else if (br.getSelectionSpan().isSet()) {
		point_ = span.getStart().leaf;
		span.moveq(null);	//remove();
	} else {
		point_ = br.getCurDocument().getFirstLeaf();
	}
  }


  /** Create VFrame. */
  public void restore(ESISNode n, Map<String,Object> attr, Layer layer) {
	super.restore(n,attr,layer);

	delay_ = Integers.parseInt(getAttr(ATTR_DELAY), 50);
	//lastPing_ = System.currentTimeMillis() + IDELAY;	 // initial pause

	Browser br = getBrowser();
	Document doc = br.getCurDocument();
	//getGlobal().getTimer().addObserver(this);

	win_ = new VFrame("SpeedRead",null, doc);
	win_.setPinned(false);
	win_.setTitle("Speed Read");
	new LeafZero("TRANSPARENT",null, win_);
	win_.setBounds(Integers.parseInt(getAttr("x"),ix_),Integers.parseInt(getAttr("y"),iy_), Integers.parseInt(getAttr("width"),iwidth_),Integers.parseInt(getAttr("height"),iheight_));
	win_.addObserver(this);

	setStart();

	tt = new TimerTask() { public void run() { next(); } };
	getGlobal().getTimer().schedule(tt, IDELAY, delay_);
  }
}
