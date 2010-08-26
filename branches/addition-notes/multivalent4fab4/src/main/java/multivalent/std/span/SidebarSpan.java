package multivalent.std.span;

import java.awt.Color;
import java.awt.Graphics2D;
import java.util.Map;

import multivalent.Span;
import multivalent.Context;
import multivalent.Mark;
import multivalent.Node;
import multivalent.Leaf;
import multivalent.Behavior;
import multivalent.Layer;
import multivalent.ESISNode;


/**
	UNDER DEVELOPMENT.
	Sidebar - draw side bar as defined by span.  not perfect during repaints

	@version $Revision: 1.2 $ $Date: 2002/02/02 13:16:27 $
*/
public class SidebarSpan extends Span {
  Node observing=null;
  int x,y1,y2;

  // for now this can get clipped out by clipping rectangle
  public boolean paintAfter(Context cx, Node n) {
	Graphics2D g = cx.g;
	g.setColor(getLayer().getAnnoColor());
	g.drawLine(x,y1, x,y2);
	return false;
  }

  public boolean appearance(Context cx, boolean all) { return false; }	// gotta define appearance


  void attach() {
	// find lowest common ancestor of endpoints, register interest on that node so paint whenever it is
	Node startn = getStart().leaf, endn=getEnd().leaf;
	observing = startn.commonAncestor(endn);
	if (observing==startn) observing=observing.getParentNode();
	observing.addObserver(this);

	// children given relative coordinates and draw in parent's coordinate space...
	x = observing.bbox.x - 10;
	y1=0; y2=endn.bbox.height;
	for (Node n=startn; n!=observing; n=n.getParentNode()) y1 += n.bbox.y;
	for (Node n=endn; n!=observing; n=n.getParentNode()) y2 += n.bbox.y;
  }

  public void move(Leaf ln,int lo, Leaf rn,int ro) { if (observing!=null) observing.deleteObserver(this); super.move(ln,lo, rn,ro); attach(); }

  public void restore(ESISNode n, Map<String,Object> attr, Layer layer) {
	super.restore(n,attr,layer);
	attach();
  }


  public void destroy() {
	if (observing!=null) observing.deleteObserver(this);
	super.destroy();
  }
}
