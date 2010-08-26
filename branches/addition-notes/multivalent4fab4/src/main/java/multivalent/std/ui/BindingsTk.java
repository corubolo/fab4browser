package multivalent.std.ui;

import java.awt.*;
import java.awt.event.*;

import multivalent.*;


/**
	Bindings after Tk.
	<ul>
	<li>text widget: press and drag with middle button scroll.  (Note that scroll wheel works now.)
	</ul>

	Pluggable events via hub document: pan-document type mouse and keyboard events.
	Other modules can provide Emacs, vi, Macintosh, Windows, ...

	@see multivalent.std.ui.BindingsDefault
	@see multivalent.std.ui.BindingsEmacs

	@version $Revision: 1.3 $ $Date: 2002/02/01 04:51:54 $
*/
public class BindingsTk extends Behavior implements EventListener {
  static Cursor handcur_ = Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR);   //null;
  static Cursor curcur_=null;

  // saved state while dragging -- static because only interactive with user so only one active at a time
  private static int dragx_=0, dragy_=0, hpos_=0, vpos_=0;
  private static IScrollPane sp_ = null;



  public BindingsTk() { // rare case of a behavior having a constructor
	if (handcur_==null) {   // weird scaling
		Toolkit tk = Toolkit.getDefaultToolkit();
		Image img = tk.getImage(getClass().getResource("/sys/images/hand.xbm"));    // => ImageIO
		handcur_ = tk.createCustomCursor(img, new Point(3,2), "hand");
	}
  }


  /** Set self as observer on docroot to catch unclaimed MOUSE_PRESSED event. */
  public void buildAfter(Document doc) {
	doc.getRoot().addObserver(this);
  }


  /** On button 2 down, take grab; else ignore. */
  public boolean eventAfter(AWTEvent e, Point rel, Node obsn) {
	int eid=e.getID();

	if (eid==MouseEvent.MOUSE_PRESSED && (((MouseEvent)e).getModifiers()&MouseEvent.BUTTON2_MASK)!=0) {
		Browser br = getBrowser();
//System.out.println("curnode="+br.getCurMark()+", curdoc = "+br.getCurDocument());
		//sp_ = br.getCurMark().node.getIScrollPane();
		Node curn = br.getCurNode();
		sp_ = (curn!=null? curn.getIScrollPane(): br.getCurDocument());
		hpos_=sp_.getHsb().getValue(); vpos_=sp_.getVsb().getValue();

		MouseEvent me = (MouseEvent)e;
		dragx_ = me.getX(); dragy_ = me.getY();

		br.setGrab(this);
		curcur_ = br.getCursor(); br.setCursor(handcur_);

		return true;
	}
	return false;
  }


  /** On MOUSE_DRAGGED, scroll current Document. */
  public void event(AWTEvent e) {
	int eid = e.getID();
	Browser br = getBrowser();

	if (eid==MouseEvent.MOUSE_DRAGGED) {
		MouseEvent me = (MouseEvent)e;
		int x=me.getX(), y=me.getY();

		// scroll
		sp_.scrollTo(hpos_-10*(x-dragx_), vpos_-10*(y-dragy_));

	} else if (eid==MouseEvent.MOUSE_RELEASED) {
		br.setCursor(curcur_);
		br.releaseGrab(this);
	}
  }
}
