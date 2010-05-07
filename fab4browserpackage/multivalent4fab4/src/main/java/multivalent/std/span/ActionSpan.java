package multivalent.std.span;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.InputEvent;

import multivalent.*;



/**
	When click on span, execute {@link #action()}.

	@see ScriptSpan

	@version $Revision: 1.4 $ $Date: 2002/02/02 13:16:25 $
*/
public abstract class ActionSpan extends Span {
  /** String to show when hovering over link. */
  public static final String ATTR_TITLE = "title";

  // these can be static because only one active at a time
  protected static boolean inmediasres_=false;
  private static AWTEvent trigger_ = null;
  private static int x0_, y0_;


  public boolean eventAfter(AWTEvent e, Point rel, Node obsn) {
	if (super.eventAfter(e, rel, obsn)) return true;
	if (!isSet()) return false; // if span deleted interactively, can still get a MOUSE_MOVED

	// should restrict to button 1
	Browser br = getBrowser();
	int eid=e.getID();

	if (eid==MouseEvent.MOUSE_ENTERED) {
		br.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		br.eventq(Browser.MSG_STATUS, getAttr(ATTR_TITLE));

	} else if (eid==MouseEvent.MOUSE_EXITED) {
		br.setCursor(Cursor.getDefaultCursor());
		br.eventq(Browser.MSG_STATUS, "");
// replace this with MOUSE_CLICKED now?  Will BindingsDefault steal MOUSE_PRESSED?

	} else if (eid==MouseEvent.MOUSE_PRESSED) {
		MouseEvent me = (MouseEvent)e;

		if (inmediasres_) {
			inmediasres_ = false;   // ignore synthesized PRESSED

		} else if ((me.getModifiers() & MouseEvent.BUTTON1_MASK) != 0) {
			Point scrn = br.getCurScrn();     // => rel pt in pressed; scrn when have grab
			x0_=scrn.x; y0_=scrn.y;

			br.setGrab(this);
			inmediasres_ = true;
		}

	} else return false;

	return true;
  }


  public void event(AWTEvent e) {
	int eid = e.getID();
	Browser br = getBrowser();

	if (eid==MouseEvent.MOUSE_DRAGGED) {
		//inmediasres_ = false; -> always get dumb drag
//System.out.println("dragging in action, from "+x0_+","+y0_+"  to "+scrn.x+","+scrn.y);
		Point scrn = br.getCurScrn();
		if (Math.abs(scrn.x-x0_)>5 || Math.abs(scrn.y-y0_)>5) {
			//inmediasres_ = false; => leave as signal for when get click again
			br.releaseGrab(this);   // other clean up is done in MOUSE_PRESSED with state already ACTIVE

			MouseEvent me = (MouseEvent)e;
			br.event(new MouseEvent((Component)me.getSource(), MouseEvent.MOUSE_PRESSED, me.getWhen()+1, InputEvent.BUTTON1_MASK, x0_,y0_, me.getClickCount(), me.isPopupTrigger()));
		}

	} else if (eid==MouseEvent.MOUSE_RELEASED && br.getGrab()==this) {
		/*eid=MouseEvent.MOUSE_EXIT;*/
		//eventAfter(br.MOUSE_EXIT, br.BOGUS_POINT, getStart().node);
		eventAfter(new MouseEvent(br, MouseEvent.MOUSE_EXITED, System.currentTimeMillis(), 0, -1,-1, 1, false), new Point(-1,-1), getStart().leaf);

		br.releaseGrab(this);
		if (inmediasres_ /*&& still in span*/) { trigger_=e; action(); trigger_=null; }     // make trigger available so subclass can check modifier keys
		inmediasres_ = false;

	}
  }


  /**
	Subclasses can make <code>public</code> if desired.
	Maybe just send event instead.
  */
  protected abstract boolean action();
}
