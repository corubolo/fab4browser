package multivalent.std.span;

import java.util.*;
import java.awt.event.MouseEvent;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Color;
import java.awt.AWTEvent;
import java.awt.Cursor;

import multivalent.*;
import multivalent.node.Root;
import multivalent.std.Executive;



/**
	<i>out of date</i>
	Many functions taken over by elide attribute in style sheet.
	Notemark - within otherwise collapsed outline sections, some important content is visible
	=> now done with elide property in style sheet for span names.	Doesn't do context, however.

	@see multivalent.std.ui.NotemarkUI

	@version $Revision: 1.2 $ $Date: 2002/02/02 13:16:27 $
*/
public class Notemark extends Span {
  int context_ = 5;
  boolean asnb_=true;    // functioning as a notemark?  if within otherwise visible text, you're not
  String signal_;

  private static boolean inmediasres=false;
  private static int x0=0,y0=0;
//	static IOutline outline = null;
  static Cursor framecursor = Cursor.getDefaultCursor();

  private Notemark(/*Node ln,int li, Node rn,int ri,*/ Layer layer, String signal) {
//	super(null,0,null,0, layer);
//	public Notemark(String signal) {
	signal_ = signal;
	if (layer!=null) layer.addBehavior(this);
//	  if (ln!=null && rn!=null) move(ln,li, rn,ri); -- moved externally, though should be moved here
  }


  // functions as a high-powered always-visible span
  public boolean appearance(Context cx, boolean all) {
	Root root = getBrowser().getRoot();
	asnb_ = (cx.elide && "ON".equals(root.getAttr(signal_)));
//	  if (asnb_) cx.elide = false;
//System.out.println("asnb_ = "+asnb_);

	if (asnb_) cx.elide = false;
	return false;
  }

	//NOTEMARKPRIORITY = PRIORITY_SELECTION+LITTLE	// even higher than Selection!
  public int getPriority() { return ContextListener.PRIORITY_SELECTION + ContextListener.LITTLE; }


  public boolean paintBefore(Context cx, Node node) {
	if (asnb_/*cx.elide*/) {
		Node n = getStart().leaf;
		int x = n.bbox.x;
		if (x>0) {	// don't do it if start of line
			Graphics2D g = cx.g;
			g.setColor(Color.RED); g.drawLine(x,n.baseline-5, x+5,n.baseline); g.drawLine(x,n.baseline-3, x+3,n.baseline);
		}
	}
	return false;
  }

  public boolean eventAfter(AWTEvent e, Point rel, Node obsn) {
	Browser br = getBrowser();
	Root root = br.getRoot();
	if (!asnb_ || !isSet() /*|| !root.isElide()*/) return false;

	Point scrn = getBrowser().getCurScrn();
	//CLGeneral gs = (CLGeneral)br.getStyleSheet().get("ROOT");

	int eid = e.getID();
	if (eid==MouseEvent.MOUSE_PRESSED/*&& if section closed up*/) {
		// open up section all enclosing (as opposed to enclosed) IOutline's

//		outline = null;

		inmediasres=true;
/*
		if (gs.getElide()) {
			inmediasres = true;
		} else {
			for (Node n=getStart().leaf; n!=null; n=n.getParentNode()) {
				if (n instanceof IOutline) { outline = (IOutline)n; inmediasres = true; break; }
			}
		}
*/

		if (inmediasres) { x0=scrn.x; y0=scrn.y; br.setGrab(this); }
		else return false;	// vs return true at bottom if in this if-then chain

	} else if (inmediasres) {
		// clicks toggles containing node
		if (eid==MouseEvent.MOUSE_RELEASED) {

			// hot spot
			Node node = getStart().leaf;	// ?
//UPDATE			Span hot = (Span)br.getGlobal("HOTSPOT");
//			hot.move(node,0, node,1);

/*
			if (outline==null) gs.invalidateElide();
			else {
				// toggle innermost.  all nodes in outline should have same priority, so most nested wins
				outline.toggle();
				if (outline.isOpen()) node.show();
			}
*/

			inmediasres=false;
			br.releaseGrab(this);

			br.setCursor(framecursor); // duplicated MOUSE_EXIT code because never actually get there due to asnb_
			br.eventq(Browser.MSG_STATUS, "");

			//br.getRoot().markDirtySubtree(); br.repaint(50);
//			  if (root.isElide()) {
				//root.setElide(false);   // need to communicate this back to Executive Summary behavior
				br.eventq(Executive.MSG_SUMMARY, "ON");
				root.markDirtySubtree(true);
//SHOW!				node.show();	// show start of nb
				//n.getINode().scrollTo(n);
//			  }


		// drags activate selection
		} else if (eid==MouseEvent.MOUSE_DRAGGED) {
			if (Math.abs(scrn.x-x0)>5 || Math.abs(scrn.y-y0)>5) {
				inmediasres=false;
				getBrowser().releaseGrab(this);

				//e.id=Event.MOUSE_DOWN; => can't mutate anymore !!!
				return false;
			}
		} // else ignore it

	} else if (eid==MouseEvent.MOUSE_ENTERED) {
		//framecursor = br.getCursorType();
		br.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		br.eventq(Browser.MSG_STATUS, "Notemark: Click to open and scroll to this position.");
		return false;

	} else if (eid==MouseEvent.MOUSE_EXITED) {
		br.setCursor(framecursor);
		br.eventq(Browser.MSG_STATUS, "");
		return false;

	} else return false;
	return true;
  }


  // shadow with always-visible of context_ words on either side
  public void moveq(Leaf ln,int lo, Leaf rn,int ro) {
	// Notemark with words of context
	if (ln.isLeaf()) {
		Node n;
		INode p;
//		int delta;

		// count back <context> words from ln, stopping at a structural boundary
		p = ln.getParentNode();
		Node newln = ln;
		for (int i=context_,childnum=ln.childNum(); i>0; i--) {
			n = p.childAt(i+childnum);
			if (n.isLeaf()) newln=n; else break;
		}
//		delta = ln.childNum() - context_;
//		Node newln = p.childAt(Math.max(0,delta)).lastLeaf();
/*		delta += ln.childNum();
		// if parent nonstructural, try previous too
		if (delta<0 && p.getName()==null) {
			n = newln.getPrevLeaf(); delta++;
			if (n!=null) {
				p = n.getParentNode();
				delta = p.size()+delta-/*0-based* /1;	 // delta<0
				newln = p.childAt(Math.max(0,delta));
			}
		}*/

		// count forward...
		p = rn.getParentNode();
		Node newrn = rn;
		for (int i=0,childnum=ln.childNum(); i<context_; i++) {
			n = p.childAt(i+childnum);
			if (n.isLeaf()) newrn=n; else break;
		}
//		delta = rn.childNum() + context_;
//		Node newrn = p.childAt(Math.min(p.size()-1,delta)).getFirstLeaf();
//System.out.println(""+(p.size()-1)+", delta="+delta+", child="+p.childAt(Math.min(p.size()-1,delta))+", firstLeaf="+newrn);
//System.out.println("rn="+rn.childNum()+"/"+rn.getName()+", newrn="+newrn.childNum()+"/"+newrn.getName());
/*		// if parent nonstructural, try next too
		delta -= newrn.childNum();
		if (delta>0 && p.getName()==null) {
			n = rn.getNextLeaf(); delta--;
			if (n!=null) {
				p = n.getParentNode();
				delta = Math.min(p.size()-1,delta);
				newrn = p.childAt(delta);
			}
		}*/

		if (newln!=getStart().leaf || newrn!=getEnd().leaf) super.moveq((Leaf)newln,0, (Leaf)newrn,newrn.size());	 // word boundaries
//System.out.println(""+ln.getName()+".."+rn.getName()+", Notemark "+newln.getName()+"/0.."+newrn.getName()+"/"+newrn.size());
	}
  }
}
