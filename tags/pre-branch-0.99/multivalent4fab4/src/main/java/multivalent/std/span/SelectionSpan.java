package multivalent.std.span;

import java.awt.Color;

import multivalent.Node;
import multivalent.Leaf;
import multivalent.Span;
import multivalent.SemanticEvent;
import multivalent.ContextListener;
import multivalent.Context;
import multivalent.Document;



/**
	Default selection behavior.
	Lightens/darkens background of selected text.
	Reports change in position by sending MSG_SET semantic event.

	<p>The change in appearance is restricted in a couple ways.
	It does not change font or spacing or other attributes that would require formatting,
	as this would make it hard to drag out a selection.
	It does not reverse colors, XOR, or use fixed foreground and background colors (e.g., white on blue),
	since there is no single foreground color on grayscale scanned paper.

	@version $Revision: 1.3 $ $Date: 2002/02/02 13:16:27 $
*/
public class SelectionSpan extends Span {
  /**
	Announces that selection has been moved to a new location.
	<p><tt>"setSelection"</tt>: <tt>arg=</tt> <var>this</var>
  */
  public static final String MSG_SET = "setSelection";


  /** Repaint delay, which is short because want fast updates when dragging out selection. */
  static final int MS=10;

  //private static double FACTOR=0.7;
  private static int BUMP=64;
  private Color lastin_=null, lastout_=null;


  // can be faked out on mid-range colors
  public boolean appearance(Context cx, boolean all) {
	Color bg = cx.background;
	if (bg!=lastin_) {
		lastin_ = bg;
		if (bg==null) lastout_=Color.LIGHT_GRAY;
		else {
			// use slightly contrasting background color
			int r=bg.getRed(), g=bg.getGreen(), b=bg.getBlue();
			//lastout_ = (r+g+b < 256*3/2? bg.brighter(): bg.darker());	// brighter/darker on r+g+b

/* brighter/darker individual r,g,b
			if (r<128) r+=(1.0-FACTOR)*(256-r); else r*=FACTOR;
			if (g<128) g+=(1.0-FACTOR)*(256-g); else g*=FACTOR;
			if (b<128) b+=(1.0-FACTOR)*(256-b); else b*=FACTOR;
*/
/* alternatively, selection always same recognizable color => doesn't work well on scanned, where can't control foreground color completely
			cx.background = Color.BLUE;
			cx.foreground = Color.WHITE;	// set text color too, or may end up with something unreadable

			cx.background = Color.ORANGE;
			cx.foreground = Color.BLACK;	// set text color too, or may end up with something unreadable
*/

/* reverse colors
			Color tmp = cx.background;
			cx.background = cx.foreground;
			cx.foreground = tmp;
*/
/* XOR?
			cx.xor = ?
*/
			r += (r<128? BUMP: -BUMP); g += (g<128? BUMP: -BUMP); b += (b<128? BUMP: -BUMP);

			lastout_ = new Color(r, g, b);
		}
	}
	cx.background = lastout_;

	cx.elide = false;	// always should see selection(?)
	return false;
  }

  /** Paints on top of everything else. */
  public int getPriority() { return ContextListener.PRIORITY_SELECTION; }


  /** No formatting, so can be more efficient, as for dragging out selection. */
  public void move(Leaf ln,int lo, Leaf rn,int ro) {
	Node oldstart=getStart().leaf, oldend=getEnd().leaf;
	int oldstartoff=-1, oldendoff=-1;  if (oldstart!=null && oldend!=null) { oldstartoff=getStart().offset; oldendoff=getStart().offset; }
	//repaint(MS);	// unshow old

	moveqSwap(ln,lo, rn,ro);

	//repaint(MS);	// show new

	// assumption: span doesn't necessitate reformatting, just repainting
	if (oldstart==null) {
		ln.commonAncestor(rn).repaint(MS);
	} else if (ln==null) {
		oldstart.commonAncestor(oldend).repaint(MS);
	} else if (ln==oldstart && lo==oldstartoff) {	// start same, draw difference in endpoints
		oldend.commonAncestor(rn).repaint(MS);
	} else if (rn==oldend && ro==oldendoff) {	// end same...
		ln.commonAncestor(oldstart).repaint(MS);
	} else {
//System.out.println("unshow "+Node.commonAncestor(oldstart,oldend));
		oldstart.commonAncestor(oldend).repaint(MS);	// unshow old
//System.out.println("show "+Node.commonAncestor(ln,rn));
		ln.commonAncestor(rn).repaint(MS);	// show new
	}
  }

  /** Report movement with MSG_SET semantic event, with <var>arg</var> = this. */
  public void moveq(Leaf ln,int lo, Leaf rn,int ro) {	// you guarantee ln<rn || (ln==rn && li<ri)
	super.moveq(ln,lo, rn,ro);
	getBrowser().eventq(MSG_SET, this);
  }

/* destroy destroys but clients should almost always use move(null) instead
  public void destroy() {
//new Exception().printStackTrace();
	moveq(null);	// don't remove from layer
	//super.destroy();
  }*/

  /** Doesn't have Morph/Delete/.... */
  public boolean semanticEventBefore(SemanticEvent se, String msg) { return false; }

  /** Remove self when referenced document is closed. */
  public boolean semanticEventAfter(SemanticEvent se, String msg) {
	if (Document.MSG_CLOSE==msg && isSet() && getStart().leaf.getDocument()==se.getArg()) moveq(null);//destroy();
/*	else if (DocumentPopup.MSG_CREATE_DOCPOPUP==msg && se.getIn()==this) {
		Browser br = getBrowser();
		br.eventq(new SemanticEvent(br, "createWidget/SELECTIONPOPUP", se.getArg(), this/*se.getIn()* /, se.getOut()));
	}*/
	return false;
  }
}
