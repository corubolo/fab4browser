package multivalent.std.span;

import java.util.*;
import java.awt.Component;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.geom.QuadCurve2D;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Point;
import java.awt.AWTEvent;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.awt.event.KeyEvent;

import phelps.doc.RobustLocation;

import multivalent.*;
import multivalent.std.ui.DocumentPopup;



/**
	Executable copy editor markup that will move marked text to another point in the document.
	Set source text span by making selection and invoking menu CopyEd / Move Text.
	Set destination point by pressing mouse button down in source text, dragging to end point, and releasing
	(keyboard scrolling is still active in case the end point is not on the same screen).
	Source range can be moved in same way as other span annotations, that is, by selecting the new
	destination span and selecting <tt>Morph</tt> from the span-specific popup menu.
	Destination point can be moved by <!--dragging as before, and action turns from execute
	to move destination point after dragging movement has passed a threshold;
	alternatively, first remove-->removing the current destination by invoking
	<tt>Re-set move to point</tt> from that same menu and setting the new one as before.
	Annotation can subsequently be executed by clicking in the source text.

	<p>The illegal operations of trying to place destination point in source text
	or subsequently moving source text on top of destination
	are checked and result in the removal of the destination point.
	If the destination point is one the same line as the start of the source text,
	the arrow arches up over the intervening text rather that plowing through the middle.


	<p>Implementation steps:
	<ol start=0>
	<li>Set location of class.  (Put in main set of classes but could have put anywhere as long as findable by Java in CLASSPATH.)
	<li>UI for setting source text.  (Add to Anno menu by adding line to hub via SpanUI in Anno.hub patterned after ReplaceWithSpan.)
	<li>Set display for source text, depending on whether not destination point is set.  (Model after {@link HyperlinkSpan#appearance(Context, boolean)}.)
	<li>UI in span-specific popup menu.  (Add "Re-set move to point" by copying semanticEvent methods from HyperlinkSpan and editing.)
	<li>UI for setting destination point.  (Model after {@link HyperlinkSpan#eventAfter(AWTEvent, Point, Node)}.)
	<li>Link arrow display to update when an endpoint point changes.  (Override {@link Span#moveq(Leaf,int, Leaf,int)} and {@link Span#formatAfter(Node)} and catch {@link Document#MSG_FORMATTED} semantic event to recompute display, and {@link #destroy()} to destroy destination point as well.)
	<li>Display of arrow from source to destination.  (Override {@link Behavior#paintAfter(Context, Node)}.)
	<li>Implement move text action.  (Model after {@link ActionSpan#action()}.)
	<li>Save and restore annotation to disk.  (Extend {@link Span#save()} and {@link Span#restore(ESISNode, Map, Layer)} to include destination point -- superclass already saves range of source text.)
	<li>(Tweaking to use java.awt.geom.QuadCurve instead of straight lines and for dragging within active span to revert to setting destination point.)
	</ol>

	@see multivalent.std.span.HyperlinkSpan for another elaborately commented example {@link Behavior}.

	@version $Revision: 1.9 $ $Date: 2003/04/28 03:56:27 $
*/
public class MoveTextSpan extends Span {
  /**
	Semantic event that removes the destination point so that it may be placed elsewhere.
	<p><tt>"resetMoveTo"</tt>: <tt>arg=</tt> {@link MoveTextSpan} <var>instance</var>, <tt>in=</tt> {@link MoveTextSpan} <var>instance</var>.
  */
  public static final String MSG_RESET_DEST = "resetMoveTo";


  private static final int DRAG_THRESHOLD = 25;

  private static boolean active_ = false;
  private static boolean skip_=false;
  /** Record cursor location at start of drag in setting move destination. */
  private static int x0_, y0_;


  /** Destination point. */
  private Mark moveTo_ = new Mark(null,-1, null);

  /** Lowest node covering both source and destination.  Root of tree would also work, but would be less efficient and would not scale. */
  private Node obs_ = null;

  /** Can draw straight line, but Java2D has handy class. */
  private QuadCurve2D spline_ = new QuadCurve2D.Double();

  private Map<String,Object> pdest_=null;


  /** Add "Re-Set move to point" to span-specific popup menu. */
  public boolean semanticEventBefore(SemanticEvent se, String msg) {
	if (super.semanticEventBefore(se,msg)) return true;
	else if (Document.MSG_FORMATTED==msg /*&& this==se.getArg()*/) setDisplay();
//System.out.println("formattedDoc in MoveText, set="+isSet()+", dest set="+moveTo_.isSet()+", dest="+moveTo_);

	else if (this!=se.getIn()) return false;
	else if (DocumentPopup.MSG_CREATE_DOCPOPUP==msg) {
		INode menu = (INode)se.getOut();
		Browser br = getBrowser();
		if (isEditable()) {
			if (moveTo_.isSet()) createUI("button", "Re-set move-to point", new SemanticEvent(br, MSG_RESET_DEST, this, this, null), menu, "EDIT", false);
		}
	}
	return false;
  }

  /** Handle {@link #MSG_RESET_DEST} event.... */
  public boolean semanticEventAfter(SemanticEvent se, String msg) {
	if (this!=se.getIn()) {}
	else if (MSG_RESET_DEST==msg) {
		moveTo_.remove();
		setDisplay();
	}
	return super.semanticEventAfter(se,msg);
  }


  /** Draw source text differently depending on whether executable or waiting for destination point. */
  public boolean appearance(Context cx, boolean all) {
	if (moveTo_.isSet()) {
		cx.underline = Color.RED;
		// paint arrow in paintAfter
	} else {
		cx.foreground=Color.WHITE; cx.background=Color.LIGHT_GRAY;
	}
	return false;
  }


  /**
	If no destination point, click-drag to set (re-fire keyboard events out so can still scroll).
	If destination set, execute.
  */
  public boolean eventAfter(AWTEvent e, Point rel, Node obsn) {
	//if (super.eventAfter(e/*,rel*/) || skip_) return true;
	if (skip_) return false;

	boolean destset = moveTo_.isSet();
	// collect up values needed in several places below
	// even though we're not sure these values will be used, it is not expensive in performance to collect them
	Browser br = getBrowser();
	int eid=e.getID();

	if (eid==MouseEvent.MOUSE_ENTERED) {
		br.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		br.eventq(Browser.MSG_STATUS, destset? "Click to move text": "Choose move to point by pressing down now and dragging to destination.");
		repaint();	// maybe changed in response to style sheet

	} else if (eid==MouseEvent.MOUSE_EXITED) {
		spanExit();

	} else if (eid==MouseEvent.MOUSE_PRESSED && contains(br.getCurNode(), br.getCurOffset()/*br.getCurMark()*/)) {
		if ((((MouseEvent)e).getModifiers()&InputEvent.BUTTON1_MASK)!=0) {
			if (destset) {
				active_ = true;	// mark execution in progress
				Point scrn = br.getCurScrn();	// => take from event
				x0_=scrn.x; y0_=scrn.y;	// record cursor location at start of activation
			}
			br.setGrab(this);	// have to set grab during mouse down in order to see mouse up
		}
	}
	return super.eventAfter(e, rel, obsn);
  }


  public void event(AWTEvent e) {
	int eid = e.getID();
	Browser br = getBrowser();

	// permit keyboard events to scroll document in case destination not on same screen
	if (KeyEvent.KEY_FIRST<=eid && eid<=KeyEvent.KEY_LAST && br.getGrab()==this) {
		br.releaseGrab(this); skip_=true;
		br.event(e);
		br.setGrab(this); skip_=false;
		// don't need skip_ => send directly to root, but still bypass eventAfter()

	} else if (eid==MouseEvent.MOUSE_DRAGGED) {
		//if (br.getGrab()==this) ...	// needed?

		// find point corresponding to current cursor location
		// (have throw event here to update since grab sends all events directly here)
		if (active_) {
			//spanExit(); -- old way
			// If drag far enough, assume want to move endpoint instead.
			// Mouse drag is too sensitive, so compute our own threshold
			Point scrn = br.getCurScrn();
			if (isEditable() && (Math.abs(scrn.x-x0_)>DRAG_THRESHOLD || Math.abs(scrn.y-y0_)>DRAG_THRESHOLD)) {
				//active_ = false;	// during grab, passed relative Point is absolute => interferes with subsequent selection
				spanExit();
				skip_=true;
				MouseEvent me = (MouseEvent)e;
				br.event(new MouseEvent((Component)e.getSource(), MouseEvent.MOUSE_PRESSED, me.getWhen()+1, InputEvent.BUTTON1_MASK, x0_,y0_, me.getClickCount(), me.isPopupTrigger()));
				skip_=false;
			}
		} else {
			br.setCurNode(null);
			getRoot().eventBeforeAfter(e, br.getCurScrn());
			//Mark m = br.getCurMark();
			//Leaf destn=m.leaf; int desti=m.offset;
			Node destn=br.getCurNode(); int desti=br.getCurOffset();
			if (destn!=null && destn.isLeaf()) {
				if (contains(destn,desti)) moveTo_.remove();	// if ask to move text to somewhere internal to span, disable
				else moveTo_.move((Leaf)destn, desti/*m*/);
				setDisplay();

				// compute x-coordinate within destination
				// Subsequently made obsolete by new method Leaf.offset2rel(), used in setDisplay
				//Point rel = destn.getAbsLocation();
				//destpt.translate(scrn.x - rel.x, 0);
			}
		}

	} else if (eid==MouseEvent.MOUSE_RELEASED) {
		boolean active = active_;
		spanExit();
		if (active) execute();
	}
  }


  /** Several exit points in event(), so collect together. */
  protected void spanExit() {
	Browser br = getBrowser();
	if (br!=null) {
		br.releaseGrab(this);
		br.setCursor(Cursor.getDefaultCursor());
		br.eventq(Browser.MSG_STATUS, "");
		repaint();
	}
	active_ = false;	// reset on re-entry
  }


  /** Set observer, compute coordinates. */
  protected void setDisplay() {
	// clean up old settings
	if (obs_!=null) obs_.deleteObserver(this);

	if (isSet() && moveTo_.isSet()) {
		Node sn=getStart().leaf, dn=moveTo_.leaf;
		// observe on lowest common ancestor
		obs_ = (Node)sn.commonAncestor(dn);	// works but not as efficient: obs_ = getDocument();
		obs_.addObserver(this);

		// compute coordinates (relative to lca since painted in that coordinate space)
		Point srcpt = sn.getRelLocation(obs_), destpt = dn.getRelLocation(obs_);
		srcpt.translate(0, sn.bbox.height/2); destpt.translate(0, dn.bbox.height/2);	// center vertically

		// map logical offset into horizontal position within leaf
		Point internal = ((Leaf)sn).offset2rel(getStart().offset); srcpt.translate(internal.x, internal.y);
		internal = ((Leaf)dn).offset2rel(moveTo_.offset); destpt.translate(internal.x, internal.y);

		int x0=srcpt.x,y0=srcpt.y, x1=destpt.x,y1=destpt.y, dx=x1-x0, dy=y1-y0;
		int xctrl = x0+dx/2, yctrl = y0 + dy/2 + (dy<=-10? 20: -20);
		spline_.setCurve(x0,y0, xctrl,yctrl, x1,y1);

		// if (change), switch observer, reset spline, repaint()

	} else {
		// maybe overwrite source text with message to choose destination point
		obs_=null;
	}

	getBrowser().repaint();
  }


  /** Draw line between source and destination. */
  public boolean paintAfter(Context cx, Node node) {
	if (node==obs_) {
		Graphics2D g = cx.g;
		g.setColor(Color.RED);

		g.draw(spline_);	// straight line works, but a little bit of curviness is so much more civilized

		// let Java2D handle trigonometry on arrow heads
		AffineTransform af = g.getTransform();

		double x0=spline_.getX1(),y0=spline_.getY1(), x1=spline_.getX2(), y1=spline_.getY2(), dx=x1-x0, dy=y1-y0;
		double theta = Math.atan(dy/(double)dx), little=Math.PI/12.0;

		// direction of arrow + reverse direction of arrow - little + adjustment because arctan doesn't know which quadrant it's in
		g.translate(x1,y1);
		g.rotate(theta + Math.PI - little + (dx>0.0? 0.0: Math.PI));
		g.drawLine(0,0, 10,0);
		g.rotate(2.0 * little);	// back to reverse + bit
		g.drawLine(0,0, 10,0);

		g.setTransform(af);
	}
	return false;
  }

  /** After formatted affected area, recompute coordinates of circle and arrow. */
  public boolean formatAfter(Node node) { setDisplay(); /*System.out.println("MoveText formatAfter");*/ return false; }


  /** Also remove destination point and recompute display (to remove circle and arrow). */
  public void destroy() {
	moveTo_.remove();
	if (obs_!=null) obs_.deleteObserver(this);
	super.destroy();
  }

  /** Also recompute display. */
  public void moveq(Leaf ln,int lo, Leaf rn,int ro) {
	super.moveq(ln,lo, rn,ro);
	if (moveTo_.isSet() && isSet() && contains(moveTo_)) moveTo_.remove();
	//if () setDisplay();
  }

  /** Remove source text, insert at destination point. */
  public void execute() {
	Leaf sn=getStart().leaf, en=getEnd().leaf, dn=moveTo_.leaf;
	int si=getStart().offset, ei=getEnd().offset, di=moveTo_.offset;

	destroy();
	// cut+insert loses spans on moved text

	// could use a cut() that preserves extracted tree nodes and an insert that takes them
	String txt = sn.cut(si, en,ei);
	dn.insert(di, txt, null);
  }

  /** Source text location saved by superclass, so just need to save endpoint (using a Robust Location, of course). */
  public ESISNode save() {
	ESISNode e = super.save();
	if (e!=null && moveTo_.isSet()) {
		if (moveTo_.isSet()) {
			if (pdest_==null) pdest_=new CHashMap<Object>(5); else pdest_.clear();
			RobustLocation.descriptorFor(moveTo_.leaf,moveTo_.offset, getDocument(), pdest_);
			e.appendChild(new ESISNode("moveto", pdest_));
		}
	}
	return e;
  }

  public void restore(ESISNode n, Map<String,Object> attr, Layer layer) {
	super.restore(n,attr,layer);

	if (n!=null) for (int i=0,imax=n.size(); i<imax; i++) {
		Object o = n.childAt(i);
		if (o instanceof ESISNode) {
			ESISNode child = (ESISNode)o;
			if ("moveto".equals(child.getGI())) { pdest_=child.attrs; break; }
		}
	}

	// ok if no destination point
  }

  /** Attach destination point as well. */
  public void buildAfter(Document doc) {
	if (pdest_!=null) moveTo_.move(RobustLocation.attach(pdest_, doc));
	super.buildAfter(doc);
  }

}
