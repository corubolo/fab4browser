package multivalent.std.ui;

import java.awt.Point;
import java.awt.AWTEvent;
import java.awt.event.KeyEvent;
import java.util.List;
import java.util.ArrayList;

import multivalent.*;
import multivalent.std.span.OutlineSpan;
import multivalent.std.Outliner;
import multivalent.std.Executive;
import multivalent.gui.VScrollbar;
//import multivalent.std.ui.DocumentPopup;

import phelps.lang.Integers;



/**
	Outline UI controls: smart scrolling, "Fully Open", excerpts on "executiveSummary".

	@see multivalent.std.adaptor.ManualPageVolume
	@see multivalent.std.adaptor.ManualPage
	@see multivalent.std.adaptor.Texinfo

	@version $Revision: 1.3 $ $Date: 2002/02/01 07:13:05 $
*/
public class OutlineUI extends Behavior {
  boolean active_ = false;
  OutlineSpan outActive_ = null;
  CLGeneral excerpt_ = null;
  List<Object> ssl_ = null;  // restore excerpts to same stylesheets!  StyleSheet-ContextListener pairs


  /** "Return" invokes smart scroll, which closes up current section, and opens next. */
  public boolean eventAfter(AWTEvent e, Point scrn, Node obsn) {
	int eid=e.getID();
//System.out.println("Outliner eventBefore "+eid+", outActive_="+outActive_);
	if (outActive_!=null && outActive_.isSet() && outActive_.isOpen()/*always true?*//* && outSects_.indexOf(outActive_)==-1*/
		&& /*active_ &&*/ eid==KeyEvent.KEY_PRESSED && ((KeyEvent)e).getKeyChar()==' '//).getKeyCode()==KeyEvent.VK_ENTER
	) {
		Browser br = getBrowser();

		// find next
		OutlineSpan next = nextSection(outActive_.getStart().leaf);
		Node outn = outActive_.getStart().leaf;
		IScrollPane isp = outn.getIScrollPane();
		VScrollbar vsb = isp.getVsb();
		int y = vsb.getValue(), h=isp.bbox.height;

		if (next!=null) {
			Leaf l = (Leaf)next.getStart().leaf;
			Point rel = l.getRelLocation(isp);

			// if next on screen open next, closing up previous of <= level
//System.out.println(y+" .. "+l.getNextLeaf()+"/"+rel.y+" .. "+(y+h));
			if (y < rel.y && rel.y < y+h) {
				// close up current and previous while <= level

				int level = Integers.parseInt(next.getAttr(OutlineSpan.ATTR_LEVEL), 1);

				for (OutlineSpan prev=outActive_; prev!=null; prev = prevSection(prev.getStart().leaf)) {
					br.eventq(OutlineSpan.MSG_CLOSE, prev);
					if (Integers.parseInt(prev.getAttr(OutlineSpan.ATTR_LEVEL), Integer.MIN_VALUE) <= level) break;    // stop after first at same level as next/new current
				}


				outActive_ = next;
				br.eventq(OutlineSpan.MSG_OPEN, outActive_);   // includes l.scrollTo(0,0, true);
//System.out.println("open "+outActive_);
			} // else fall through to scroll
			return true;

		} else if (y+h >= vsb.getMax()) {   // at end, just close up
//System.out.println("couldn't find next OutlineSpan"); -- at end
			br.eventq(OutlineSpan.MSG_CLOSE, outActive_);
			outActive_ = null;
		}

		// close current and open next
		//br.setGrab(this);	// get key release
		//return true;
	}

	return false;
  }


  OutlineSpan nextSection(Node after) {
	for (Leaf l=after.getNextLeaf(); l!=null; l=l.getNextLeaf()) {
		for (int i=0,imax=l.sizeSticky(); i<imax; i++) {
			Mark m = l.getSticky(i);
			Object owner = m.getOwner();
			if (owner instanceof OutlineSpan && ((OutlineSpan)owner).getStart()==m) {
				return (OutlineSpan)owner;
//System.out.println("found next active = "+l.getNextLeaf()+", owner="+owner);
			}
		}
	}
	return null;
  }

  OutlineSpan prevSection(Node before) {
	for (Leaf l=before.getPrevLeaf(); l!=null; l=l.getPrevLeaf()) {
		for (int i=l.sizeSticky()-1; i>=0; i--) {   // doesn't matte
			Mark m = l.getSticky(i);
			Object owner = m.getOwner();
			if (owner instanceof OutlineSpan && ((OutlineSpan)owner).getStart()==m) {
				return (OutlineSpan)owner;
//System.out.println("found next active = "+l.getNextLeaf()+", owner="+owner);
			}
		}
	}
	return null;
  }


/*
  public void event(AWTEvent e) {
	if (e.getID()==KeyEvent.KEY_RELEASED) getBrowser().releaseGrab(this);
  }*/



  public boolean semanticEventAfter(SemanticEvent se, String msg) {
//System.out.println("outlineOpen on active_="+active_+", msg="+msg);
	if (Outliner.MSG_MADE==msg) {
		Object o = se.getArg();
		if (o instanceof Node) {
			active_=true;
			((Node)o).addObserver(this);
				Browser br = getBrowser();
				br.eventq(new SemanticEvent(br, Executive.MSG_SUMMARY, "ON"));
		}

	} else if (!active_) {
		// nothing -- page fully on screen

	} else if (DocumentPopup.MSG_CREATE_DOCPOPUP==msg && se.getIn()!=getBrowser().getSelectionSpan()) {	 //null)) { -- just plain-doc popup
//System.out.println("**** f/b createMenu "+super.toString()+", in="+se.getIn());
		INode menu = (INode)se.getOut();
		createUI("button", "Fully Collapse Outline", "event "+OutlineSpan.MSG_CLOSE_ALL, menu, "SPECIFIC", false);
		createUI("button", "Fully Open (or Space to smart scroll)", "event "+Executive.MSG_SUMMARY+" OFF; event outlineOpenAll", menu, "SPECIFIC", false);

/*	} else if (msg.startsWith("outline")) {
		//if (first_/* && false* /) {
			CLGeneral gs = (CLGeneral)getDocument().getStyleSheet().get("excerpt");
			if (gs.getSize()!=CLGeneral.INVALID) {
				gs.setElide(CLGeneral.BOOLINVALID); gs.setSize(CLGeneral.INVALID);
				for (Iterator<> i=excerptSpans_.iterator(); i.hasNext(); ) ((Span)i.next()).markDirty();  // Span class only marks dirty if set
			}
		//}
*/
	} else if (OutlineSpan.MSG_OPEN==msg) {
		Object o=se.getArg();
		if (o instanceof OutlineSpan) outActive_ = (OutlineSpan)o;
//System.out.println("outlineOpen on "+o+" / "+outActive_.getStart().leaf.getNextLeaf());

	} else if (Executive.MSG_SUMMARY==msg) {
		Object o=se.getArg();
		if (o instanceof String) {
//			CLGeneral gs = (CLGeneral)getDocument().getStyleSheet().get("excerpt");
/*			if ("ON".equalsIgnoreCase((String)o)) { gs.setElide(CLGeneral.FALSE); gs.setSize(10); outActive_=null;
			} else { gs.setElide(CLGeneral.BOOLINVALID); gs.setSize(CLGeneral.INVALID); }*/
			StyleSheet ss = getDocument().getStyleSheet();
			if (ssl_==null) {
				ssl_ = new ArrayList<Object>(6);
				for (StyleSheet ssn=ss; ssn!=null; ssn=ssn.getCascade()) {
					Object sse = ssn.get("excerpt");
					if (sse!=null) { ssl_.add(ssn); ssl_.add(sse); }
				}
				//if (excerpt_==null) excerpt_ = (CLGeneral)ss.get("excerpt");
			}
//System.out.println(ss.getName()+" => excerpt "+excerpt_);
			if ("ON".equalsIgnoreCase((String)o)) {
				for (int i=0,imax=ssl_.size(); i<imax; i+=2) ((StyleSheet)ssl_.get(i)).put("excerpt", (ContextListener)ssl_.get(i+1));
				//ss.put("excerpt", excerpt_);
				outActive_=null;
//System.out.println("putting excerpt in "+getDocument().getFirstLeaf()+" "+excerpt_.getSize()+" / "+excerpt_.getElide());
			} else { //ss.remove("excerpt");
				for (int i=0,imax=ssl_.size(); i<imax; i+=2) ((StyleSheet)ssl_.get(i)).remove("excerpt");
			}
			//for (Iterator<> i=excerptSpans_.iterator(); i.hasNext(); ) ((Span)i.next()).markDirty(); -- execsum already reformats whole tree
		}
	} else if (Document.MSG_CLOSE==msg) {
		for (int i=0,imax=ssl_.size(); i<imax; i+=2) ((StyleSheet)ssl_.get(i)).put("excerpt", (ContextListener)ssl_.get(i+1));
//		getDocument().getStyleSheet().put("excerpt", excerpt_);    // stylesheet shared, so restore!
	}
	return super.semanticEventAfter(se,msg);
  }
}
