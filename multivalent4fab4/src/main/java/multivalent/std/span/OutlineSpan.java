package multivalent.std.span;

import java.awt.Point;
import java.awt.AWTEvent;
import java.awt.Image;
import java.awt.event.MouseEvent;
import java.util.Map;
import javax.imageio.ImageIO;

import multivalent.*;
import multivalent.node.LeafImage;

import phelps.lang.Integers;



/**
	Assumes LeafImage as first node, which shows open or closed arrow.
	Set elide=false in style sheet for headers.
	Fault in by observing outlineOpen event on that span (getArg()==this span), building up content, moving span's end to end of content.
	Nested by setting level:  1=topmost, 2=nested, 3=doubly nested, ...
	Searching needs to fault in all.

	@see multivalent.std.Outliner
	@see multivalent.std.ui.OutlineUI
	@see multivalent.std.adaptor.ManualPageVolume
	@see multivalent.std.adaptor.ManualPage
	@see multivalent.std.ui.History

	@version $Revision: 1.3 $ $Date: 2002/02/02 13:16:27 $
*/
public class OutlineSpan extends Span {
  /**
	Open outline span.
	<p><tt>"outlineOpen"</tt>: <tt>arg=</tt> {@link Span} <var>span-to-open</var>.
  */
  public static final String MSG_OPEN = "outlineOpen";

  /**
	Open all outline span.
	<p><tt>"outlineOpenAll"</tt>.
  */
  public static final String MSG_OPEN_ALL = "outlineOpenAll";

  /**
	Close outline span.
	<p><tt>"outlineClose"</tt>: <tt>arg=</tt> {@link Span} <var>span-to-close</var>.
  */
  public static final String MSG_CLOSE = "outlineClose";

  /**
	Close all outline spans.
	<p><tt>"outlineCloseAll"</tt>.
  */
  public static final String MSG_CLOSE_ALL = "outlineCloseAll";

  /**
	Toggle outline span.
	<p><tt>"outlineToggle"</tt>: <tt>arg=</tt> {@link Span} <var>span-to-close</var>.
  */
  public static final String MSG_TOGGLE = "outlineToggle";

  public static final String ATTR_LEVEL = "level";
  public static final String ATTR_OPEN = "open";



  static Image OPENED, CLOSED;


  boolean closed_=true, trigger_=false;
  public int level=0;


  public boolean isOpen() { return !closed_; }

  public boolean appearance(Context cx, boolean all) {
	//if (closed_) cx.elide=true;
	//if (closed_) cx.size=10f;	// make excerpts small
	if (!cx.elide && closed_ /*|| level>0*/) cx.elide=true;
	return false;	// if closed, elide; if open, anti-elide against exec sum
  }

  /**
	Lower priority than style sheet-based settings.
	More deeply nested has higher priority so a nested closed overrides a prevailing open.
	This forces section opening on mouse click to eventAfter so more deepy nested open and scroll last.
  */
  public int getPriority() { return /*ContextListener.PRIORITY_STRUCT*/multivalent.std.adaptor.CSS.PRIORITY_BLOCK - ContextListener.LOT + level; }


  /** When click, resolve click to leaf, undo elision, scroll to leaf. */
  //public boolean eventBefore(AWTEvent e, Point scrn, Node obsn) { => let enclosing span open up and scroll first
  public boolean eventAfter(AWTEvent e, Point rel, Node obsn) {
//System.out.println(e.getID()+" @ "+getBrowser().getCurMark()+", closed_="+closed_);
	// elaborate/filter
	if (e.getID()==MouseEvent.MOUSE_CLICKED && ((((MouseEvent)e).getModifiers())&MouseEvent.BUTTON1_MASK)!=0) {
		Browser br = getBrowser();
		//Node cur = br.getCurNode();
		// span covers title and body, but only toggle if on title

		if (closed_) {	// closed=> open
			//setClosed(false);
			br.eventq(MSG_OPEN, this);
			//br.callSemanticEvent(MSG_OPEN, this);
System.out.println("click on level "+level);
			br.eventq(IScrollPane.MSG_SCROLL_TO, br.getCurNode());    // Notemark

			//return true;	  // don't propagate to parents in nested?
		} else {
			Node curn = br.getCurNode();
			// span covers title and body, but only toggle if on title
			if (curn!=null && curn.getParentNode()==getStart().leaf.getParentNode()) br.eventq(MSG_CLOSE, this);
			/*for (Node n = br.getCurNode().getParentNode(), doc=getDocument(); n!=doc; n=n.getParentNode()) {
				if ("secthead".equals(n.getName())) br.eventq(MSG_CLOSE, this);	//br.callSemanticEvent(MSG_CLOSE, this);	 //setClosed(true);
			}*/
			//br.event(new SemanticEvent(br, Executive.MSG_SUMMARY, "OFF")); -- exec picks this up on its own
		}
	}
	return false;
  }

  public boolean semanticEventAfter(SemanticEvent se, String msg) {
//if (this==se.getArg()) System.out.println("outlinespan "+this+", closed_=|"+closed_+"|, ="+msg);
//System.out.println("outlinespan "+this+", closed_="+closed_+", this? "+(se.getArg()==this)+", msg="+msg);
	Browser br = getBrowser();
/*	if (Executive.MSG_SUMMARY==msg) {
		Object o = se.getArg();
		if (o==null || !(o instanceof String)) {
			// skip it
		} else if ("ON".equalsIgnoreCase((String)o)) {
			// popup menu turns all on
			// click on notemark turns all/one off -- all for now
			setClosed(true);
		//} else if ("OFF".equalsIgnoreCase((String)o)) {
		//	  setClosed(false);
		}

	} else*/ if (MSG_OPEN_ALL==msg) {
		setClosed(false);

	} else if (MSG_OPEN==msg && this==se.getArg() /*|| null or special that refers to all? -- then zap outlineOpenAll*/) {
//System.out.println("OPEN "+", closed_="+closed_);
		if (closed_) {
//System.out.println("BEFORE");
//for (Node n=getStart().leaf,stopn=n.getIScrollPane().getParentNode(); n!=stopn; n=n.getParentNode()) System.out.println("  "+n.getName()+", valid="+n.isValid());
			setClosed(false);
//System.out.println("AFTER");
//for (Node n=getStart().leaf,stopn=n.getIScrollPane().getParentNode(); n!=stopn; n=n.getParentNode()) System.out.println("  "+n.getName()+", valid="+n.isValid());

			//br.eventq(new SemanticEvent(br, IScrollPane.MSG_SCROLL_TO, getStart().leaf));	// on queue first, possibly to be overridden by Notemark
			br.event/*no q*/(new SemanticEvent(br, IScrollPane.MSG_SCROLL_TO, getStart().leaf));	// on queue first, possibly to be overridden by Notemark
			//br.getCursorMark().moveq(null); // not .remove() -- removes a behavior in the midst of
			br.getCursorMark().move(null,-1); // not .remove() -- removes a behavior in the midst of
			//br.getSelectionSpan().moveq(this);
		}
		//outclosed_ = this;	// regardless, set current

	} else if (MSG_TOGGLE==msg && this==se.getArg()) {
		//br.eventq(closed_?"outlineOpen":"outlineClose", this);
		br.eventq(new SemanticEvent(br, closed_? MSG_OPEN: MSG_CLOSE, this));

	} else if (MSG_CLOSE_ALL==msg) {
		setClosed(true);

	} else if (MSG_CLOSE==msg && this==se.getArg()) {
		setClosed(true);
//System.out.println("CLOSE");
		//outclosed_ = this;

	} else if (trigger_) {
		br.eventq(MSG_OPEN, this);
		trigger_=false;
	}

	return false;
  }


  // should make protected so forced to send event, but Outliner still calls directly
  public void setClosed(boolean active) {
	if (active==closed_) return;	// avoid unnecessary reformatting
	Browser br = getBrowser();
	closed_=active;
//System.out.println("setActive "+active+" for "+getStart().leaf.getName()+" .. "+getEnd().leaf.getName());
	LeafImage n = (LeafImage)getStart().leaf;
	n.setImage(closed_? CLOSED: OPENED);
	//n.getParentNode().setChildAt((closed_?closed:opened), 0); => doesn't preserve spans
	markDirty();
	br.repaint(50);	// 50ms so get time to complete whatever else before paint thread formats everybody.  ... event 0 still goes on queue
  }


  public void moveq(Leaf ln,int lo, Leaf rn,int ro) {
	if (ln!=null && !(ln instanceof LeafImage)) {
		LeafImage n = new LeafImage(null,null, null, closed_? CLOSED: OPENED);//(Image)null);
//System.out.println("inserting LeafImage");
		ln.getParentNode().insertChildAt(n, 0);
		ln = n; lo=0;	  // extend span to include arrow
	}
//System.out.println("span "+ln+" .. "+rn);
	super.moveq(ln,lo, rn,ro);
//System.out.println("modifying LeafImage "+ln.getClass().getName());
	if (ln!=null) ((LeafImage)ln).setImage(closed_? CLOSED: OPENED);
  }


  public void restore(ESISNode n, Map<String,Object> attr, Layer layer) {
	super.restore(n,attr,layer);
	level = Integers.parseInt(getAttr(ATTR_LEVEL), 0);
	trigger_ = (getAttr(ATTR_OPEN)!=null);	// not just closed_=false... as other behaviors have own ideas?

	if (OPENED==null) try {
		ImageIO.setUseCache(false);
		OPENED = ImageIO.read(getClass().getResourceAsStream("/sys/images/opened.xbm"));
		CLOSED = ImageIO.read(getClass().getResourceAsStream("/sys/images/closed.xbm"));
	} catch (java.io.IOException shouldnthappen) { System.err.println(shouldnthappen); }
  }
}
