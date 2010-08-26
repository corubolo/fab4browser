package multivalent.node;

import java.awt.Rectangle;
import java.awt.Point;
import java.awt.AWTEvent;
import java.util.Map;

import multivalent.INode;
import multivalent.Context;
import multivalent.IScrollPane;


/**
	Visual layer containing elements absolutely positioned in document <i>window</i>.

	@see multivalent.node.IRootAbs

	@version $Revision: 1.3 $ $Date: 2002/03/06 02:14:31 $
*/
public class IRootScreen extends INode {
  //List<> list = new LinkedList<>();
  //int maxy = 0;

  // I hate these no-arg constructors required by newInstance()
  public IRootScreen() { this(null,null,null); }
  public IRootScreen(String name, Map<String,Object> attr, INode parent) {
	super(name,attr, parent);
  }

  /** Counteract containing IScrollPane's x-scroll position. */
  public int dx() {
	IScrollPane isp = getIScrollPane();
	return super.dx() + (isp!=null? isp.getHsb().getValue(): 0);
  }

  /** Counteract containing IScrollPane's y-scroll position. */
  public int dy() {
	IScrollPane isp = getIScrollPane();
	return super.dy() + (isp!=null? isp.getVsb().getValue(): 0);
  }

  public boolean intersects(Rectangle r) { return true; }
  public boolean contains(Point p) { return true; }


  // set to exactly size of other content, so always paint but never influence size
  public boolean formatNode(int width,int height, Context cx) {
	boolean ret = super.formatNode(width,height, cx);
	bbox.add(0,0);	// origin at (0,0) so don't need coordinate translation relative to random point
	return ret;
  }

  /** Never claims {@link multivalent.Browser#setCurNode(Node, int)} itself. */
  public boolean eventNode(AWTEvent e, Point rel) {
	boolean shortcircuit=false;
	for (int i=size()-1; i>=0 && !shortcircuit; i--) shortcircuit = childAt(i).eventBeforeAfter(e,rel);
	return shortcircuit;
  }
}
