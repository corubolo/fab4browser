package multivalent.devel;

import java.awt.AWTEvent;
import java.awt.Point;
import java.awt.Rectangle;

import multivalent.*;


/**
	Attach to node to report protocol activity on that node, filterable by protocol.
	Of course, this applies to tree protocols only: format, paint, low-level event, clipboard.
	LATER: animated on doc tree?

	@version $Revision: 1.3 $ $Date: 2002/02/02 12:34:04 $
*/
public class NodeActivity extends Behavior {
  public static final int BUILDBEFORE_MASK=1, BUILDAFTER_MASK=1<<1, FORMATBEFORE_MASK=1<<2, FORMATAFTER_MASK=1<<3, PAINTBEFORE_MASK=16, PAINTAFTER_MASK=1<<4,
	EVENTBEFORE_MASK=1<<5, EVENTAFTER_MASK=1<<6, SEMANTICEVENTBEFORE_MASK=1<<7, SEMANTICEVENTAFTER_MASK=1<<8,
	CLIPBOARDBEFORE_MASK=1<<9, CLIPBOARDAFTER_MASK=1<<10;

  Node node_ = null;
  int mask_ = ~0;  // everybody on by default

// not node level
//	public void buildBefore(Document doc) {}
//	public void buildAfter(Document doc) {}


  public boolean formatBefore(Node node) {
	if ((mask_&FORMATBEFORE_MASK)!=0) msgOut("formatBefore");
	return false;
  }
  public boolean formatAfter(Node node) {
	if ((mask_&FORMATAFTER_MASK)!=0) msgOut("formatAfter");
	return false;
  }

  public boolean paintBefore(Context cx, Node n) {
	if ((mask_&PAINTBEFORE_MASK)!=0) msgOut("paintBefore");
	return false;
  }
  public boolean paintAfter(Context cx, Node n) {
	if ((mask_&PAINTAFTER_MASK)!=0) msgOut("paintAfter");
	return false;
  }

  public boolean clipboardBefore(StringBuffer sb, Node node) {
	if ((mask_&CLIPBOARDBEFORE_MASK)!=0) msgOut("clipboardBefore");
	return false;
  }
  public boolean clipboardAfter(StringBuffer sb, Node node) {
	if ((mask_&CLIPBOARDAFTER_MASK)!=0) msgOut("clipboardAfter");
	return false;
  }

// not node level
//	public boolean semanticEventBefore(SemanticEvent se, String msg) { return false; }
//	public boolean semanticEventAfter(SemanticEvent se, String msg) { return false; }


  public boolean eventBefore(AWTEvent e, Point rel, Node obsn) {
	if ((mask_&EVENTBEFORE_MASK)!=0) msgOut("eventBefore");
	return false;
  }
  public boolean eventAfter(AWTEvent e, Point rel, Node obsn) {
	if ((mask_&EVENTAFTER_MASK)!=0) msgOut("eventAfter");
	return false;
  }

  /** Show protocol and limited, unusual state. */
  public void msgOut(String proto) {
	Node n = node_;  if (n==null) return;
	Rectangle r = n.bbox;

	System.out.println(
		proto+" "+n.getName()
		+", "+r.width+"x"+r.height+" @ "+r.x+","+r.y+" + "+n.baseline
		+(n.isValid()? "": ", INVALID")
//		+(n.align==NONE? "": ", align="+n.align)
//		+(n.valign==NONE? "": ", valign="+n.valign)
//		+(n.floats==NONE? "": ", floats="+n.floats)
//		+(n.observers_==null || n.observers_.size()==0? "": ", observers = "+n.observers)
//		+(n.sticky_==null || n.sticky_.size()==0? "": ", sticky_ = "+sticky_)
	);
  }


  /** OR if protocol masks from the class Behavior. */
  public void setMask(int mask) { mask_ = mask; }

  public int getMask() { return mask_; }

  public void setNode(Node node) { node_ = node; }
  public Node getNode() { return node_; }
}
