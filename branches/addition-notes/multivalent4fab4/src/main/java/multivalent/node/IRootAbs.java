package multivalent.node;

import java.awt.Point;
import java.awt.AWTEvent;
import java.util.Map;

import multivalent.INode;
import multivalent.Context;



/**
	Visual layer containing elements absolutely positioned <i>within current document</i>--
	that is, relative to the current document's virtual canvas including scrolling,
	but independent of content.

	@see multivalent.node.IRootScreen

	@version $Revision: 1.3 $ $Date: 2002/03/06 02:14:04 $
*/
public class IRootAbs extends INode {
  // if get lots of children, maybe later keep in sorted order by y, but then child has to do markDirty() so know when to reformat
  //List<> list = new LinkedList<>();	// just keep sorted by y, as x well limited by page width
  //int maxy = 0;

  // I hate these no-arg constructors required by newInstance()
  public IRootAbs() { this(null,null,null); }
  public IRootAbs(String name, Map<String,Object> attr, INode parent) {
	super(name,attr, parent);
  }

  public boolean formatBeforeAfter(int width, int height, Context cx) {
	valid_ = false;     // reformat whenever content dirty as some children may be anchored to content -- usually all children valid so no work
	return super.formatBeforeAfter(width, height, cx);
  }

  public boolean formatNode(int width,int height, Context cx) {
	boolean ret = super.formatNode(width,height, cx);
	bbox.add(0,0);	// children always relative to (0,0) so no coordinate translation
	return ret;
  }

  /** Never claims {@link multivalent.Browser#setCurNode(Node, int)} itself. */
  public boolean eventNode(AWTEvent e, Point rel) {
	boolean shortcircuit=false;
	for (int i=size()-1; i>=0 && !shortcircuit; i--) shortcircuit = childAt(i).eventBeforeAfter(e,rel);
	return shortcircuit;
  }
}
