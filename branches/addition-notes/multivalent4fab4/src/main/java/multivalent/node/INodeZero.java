package multivalent.node;

import java.awt.Point;
import java.awt.AWTEvent;
import java.util.List;
import java.util.Map;

import multivalent.Context;
import multivalent.INode;



/**
	Internal node that has zero effect on layout and display.
	Use for comments and other tags (such as HTML NOFRAMES) that should be carried in tree
	in case user asks to write out tree.
	(Plain INode recurses over children and computes formatted size to enclose them.)

	@version $Revision: 1.2 $ $Date: 2002/01/27 02:53:06 $
*/
public class INodeZero extends INode {
  public INodeZero(String name, Map<String,Object> attr, INode parent) { super(name,attr, parent); }

  public boolean formatNode(int width,int height, Context cx) {
	super.formatNode(width,height, cx);	// have to let formatting happen in case close spans in content
	bbox.setSize(0,0);	// but size is 0x0 so skipping during painting, which cx.valid=false if necessary
	// NB: children will remain invalid
	valid_=true;
	return !valid_;	// was return valid_ and that was causing big drop outs as this node used in HTML TITLEs!  one bit boom!
  }

  //public void paintNode(Rectangle docclip, Context cx) {}
  //public void paintBeforeAfter(Rectangle docclip, Context cx) {} -- skipped because 0x0, but retain cx.valid=false iff stickies
  public boolean eventNode(AWTEvent e, Point rel) { return false; }
  //public boolean eventBeforeAfter(AWTEvent e, Point rel) { return false; }

  // just keep in tree, but don't affect formatting
  public boolean breakBefore() { return false; }
  public boolean breakAfter() { return false; }

  public boolean checkRep() {
	// no restrictions on children
	return true;
  }
}
