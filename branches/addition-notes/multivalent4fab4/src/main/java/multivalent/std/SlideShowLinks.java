package multivalent.std;

import java.awt.AWTEvent;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TimerTask;

import multivalent.*;
import multivalent.std.span.HyperlinkSpan;
import multivalent.std.ui.ForwardBack;
import multivalent.gui.VMenu;
import multivalent.gui.VCheckbox;
import multivalent.node.LeafImage;

import phelps.lang.Integers;



/**
	Collect links in current document, march through in other window.
	Use the standard page forward/back to manually step through slide show.
	Press down on forward and back buttons to choose page farther forward or back.
	Auto button.

<!--
	<p>TO DO:
	type-in box for interval
	Keep slide show in current frame?

	optionally dive into tree to given depth
	rename behavior to Tour?
	generate site map from links

	turn on full-screen mode?
-->

	@see SlideShow

	@version $Revision: 1.5 $ $Date: 2003/06/02 05:59:41 $
*/
public class SlideShowLinks extends Behavior {
  /**
	Open browser with name {@link #SLIDESHOWNAME} and play slide show.
	<p><tt>"slideShowLinks"</tt>.
  */
  public static final String MSG_START = "slideShowLinks";

  /**
	Toggle auto/manual advance.
	<p><tt>"slideshowSetAuto"</tt>.
  */
  public static final String MSG_SETAUTO = "slideshowSetAuto";

  /** Name of browser running slideshow. */
  public static final String SLIDESHOWNAME = "SLIDESHOW";

  int interval_;
  boolean auto_ = true;   // attribute?
  private boolean skip_ = false;


  /**
	If not slideshow browser, menu option to start.
	If slideshow, auto checkbox.
  */
  public boolean semanticEventBefore(SemanticEvent se, String msg) {
	Browser br = getBrowser();

	if (super.semanticEventBefore(se,msg)) return true;
	else if (SLIDESHOWNAME.equals(br.getName())) {
		if (Browser.MSG_CREATE_TOOLBAR==msg) {
			VCheckbox cb = (VCheckbox)createUI("checkbox", "Auto", "event "+MSG_SETAUTO, (INode)se.getOut(), null/*toggle*/, false);
			cb.setState(auto_);
		}

	} else {
		if (VMenu.MSG_CREATE_GO==msg) {
			createUI("button", "Slide Show of Links", "event "+MSG_START, (INode)se.getOut(), "GoPan", false);
		}
	}
	return false;
  }


  /** Start slide show, toggle auto, ... */
  public boolean semanticEventAfter(SemanticEvent se, String msg) {
	Browser br = getBrowser();
	if (!SLIDESHOWNAME.equals(br.getName())) { // shouldn't get here otherwise, but double check
		if (MSG_START==msg) {
			Object arg = se.getArg();
			List<Object> links = arg instanceof List? (List<Object>)arg: collectLinks();
			if (links.size()>0) {
				Browser showbr = getGlobal().getBrowser(SLIDESHOWNAME);
				showbr.eventq(ForwardBack.MSG_OPEN_DOCUMENTS, links);
				showbr.eventq(ForwardBack.MSG_FORWARD, null);
				//return true;
			}
		}

	} else {
		if (MSG_SETAUTO==msg) {
			Object arg = se.getArg();
			if (arg==null) auto_ = !auto_;
			//else interpret true/false

		} else if (Document.MSG_FORMATTED==msg) {
			// RESTORE THIS
			//lastPing_ = System.currentTimeMillis();     // start the clock after see page, so big pages and f/b get full time
		}
	}

	return super.semanticEventAfter(se,msg);
  }



  /**
	Collect hyperlinks from source Document.
	Range is everything in selection, everything from cursor to end, or whole document.
  */
  List<Object> collectLinks() {
	Browser br = getBrowser();

	List<Object> links=new ArrayList<Object>(10);

	// determine range
	Node s=null,e=null;
	Span sel = br.getSelectionSpan(); CursorMark curs = br.getCursorMark();
	if (sel.isSet()) {  // within selection
		s=sel.getStart().leaf; e=sel.getEnd().leaf;

	} else if (curs.isSet()) {  // cursor on
		s=curs.getMark().leaf;
		Document doc = s.getDocument();
		e = (doc!=null? doc.getLastLeaf().getNextLeaf(): null);

	} else {    // all inks
		Document doc = br.getCurDocument();
		if (doc!=null) { s=doc.getFirstLeaf(); e=doc.getLastLeaf().getNextLeaf(); }
		else { s=br.getRoot().getFirstLeaf(); e=null; }
	}

	// collect links, no duplicates
	for (Node l=s; l!=null && l!=e; l=l.getNextLeaf()) {
//ystem.out.println("checking "+l+", marks="+marks);
		for (int i=0,imax=l.sizeSticky(); i<imax; i++) {
			Mark m = l.getSticky(i);
			Object o = m.getOwner();
			if (o instanceof HyperlinkSpan) {
				HyperlinkSpan hs = (HyperlinkSpan)o;
				Object target = hs.getTarget();
				//if (m==hs.getStart()) links.add(m); -- duplication can come from other links too
				if (links.indexOf(target)==-1) links.add(target); 	// a Set would throw out duplicates but wouldn't preserve order
//if (o!=null) System.out.println("link = "+((HyperlinkSpan)o).getURI());
			}
		}
	}
	return links;
  }


  /**
	On heartbeat, if in slideshow browser, show next document.
  */
  public boolean eventBefore(AWTEvent e, Point rel, Node n) {
	int eid=e.getID();
	if (super.eventBefore(e, rel, n)) return true;    // respect superclass short-circuit
	else if (eid==MouseEvent.MOUSE_CLICKED) {
		Browser br = getBrowser();
		if (br.getCurNode() instanceof LeafImage) {
			MouseEvent me = (MouseEvent)e;
			getBrowser().eventq((me.getButton()==1? ForwardBack.MSG_FORWARD: ForwardBack.MSG_BACKWARD), null);
			return true;
		}

	} else if (eid==KeyEvent.KEY_PRESSED) {
		KeyEvent ke = (KeyEvent)e;
		int code = ke.getKeyCode();
		Browser br = getBrowser();
		// <-=prev, space=pause, ESC=exit, any other=next slide

		if (code == KeyEvent.VK_LEFT || code=='p' || code=='b') {
			br.eventq(ForwardBack.MSG_BACKWARD, null);
			skip_ = true;
			return true;

		} else if (code == KeyEvent.VK_RIGHT /*|| code==' ' -- scroll down*/ || code=='n' || code=='f') {
			br.eventq(ForwardBack.MSG_FORWARD, null);
			skip_ = true;
			return true;

		} else if (code == KeyEvent.VK_UP || code=='a') {
			br.eventq(MSG_SETAUTO, null);
			return true;
		}
	}

	return false;
  }



  /**
	Take interval from "intervalms" attribute.
  */
  public void restore(ESISNode n, Map<String,Object> attr, Layer layer) {
	super.restore(n,attr, layer);
	interval_ = Integers.parseInt(getAttr("intervalms"), 3 * 1000);

	Browser br = getBrowser();
	if (SLIDESHOWNAME.equals(br.getName())) {
		getRoot().addObserver(this);

		TimerTask tt = new TimerTask() {
			public void run() {
				if (skip_) skip_=false;
				else if (auto_) getBrowser().eventq(ForwardBack.MSG_FORWARD, null);
			}
		};
		getGlobal().getTimer().schedule(tt, 2000L, interval_);
	}
  }
}
