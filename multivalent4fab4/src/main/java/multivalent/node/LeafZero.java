package multivalent.node;

import java.awt.Point;
import java.awt.AWTEvent;
import java.util.Map;

import multivalent.Context;
import multivalent.INode;
import multivalent.Leaf;



/**
	Leaf node that has zero effect on layout and display.
	Use for comments and other tags (such as HTML NOFRAMES) that should be carried in tree
	in case user asks to write out tree.

	@version $Revision: 1.3 $ $Date: 2002/02/02 13:41:40 $
*/
public class LeafZero extends Leaf {
  public LeafZero(String name, Map<String,Object> attr, INode parent) { super(name,attr, parent); }

  public boolean formatNode(int width,int height, Context cx) {
	super.formatNode(width,height, cx);	// have to let formatting happen in case close spans in content
	bbox.setSize(0,0);
	valid_=true;
	return !valid_;
  }

  //public void paintNodeContent(Rectangle docclip, Context cx) {}
  //public void paintBeforeAfter(Rectangle docclip, Context cx) { /*cx.valid=false;?*/ }
  public boolean eventNode(AWTEvent e, Point rel) { return false; }
}
