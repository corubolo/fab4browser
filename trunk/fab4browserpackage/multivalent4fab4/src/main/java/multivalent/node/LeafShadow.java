package multivalent.node;

import java.awt.Rectangle;
import java.awt.Point;
import java.awt.AWTEvent;
import java.awt.Graphics2D;
import java.util.Map;

import multivalent.*;



/**
	Leaf to shadow structural node in absolute visual space.
	The document tree is primarily structural, but elements such as floats and menus cut across structure when painted.
	The node itself is a shadow, but it takes another node to shadow, so it operates as kind of an internal node too.

	@version $Revision: 1.4 $ $Date: 2002/02/02 13:41:40 $
*/
public class LeafShadow extends Leaf {
  Node shadowed;

  public LeafShadow(String name, Map<String,Object> attr, INode parent, Node shadowed) {
	super(name,attr, parent);
	this.shadowed = shadowed;  assert shadowed!=null;
  }

  public Node getShadowed() { return shadowed; }


  public boolean formatBeforeAfter(int width, int height, Context cx) {
	valid_ = false;     // reformat whenever content dirty
	return super.formatBeforeAfter(width, height, cx);
  }

  public boolean formatNode(int width, int height, Context cx) {
	boolean ret = false;
	//ret = shadowed.formatNode(width, height, cx); => already formatted in content space, with correct context

	// translate to absolute space
	Document doc = getDocument();
	//Point abs = shadowed.getRelLocation(doc);   // => goes to content
//System.out.print("shadow "+shadowed.bbox.x+","+shadowed.bbox.y+" => ");
	Point abs = (shadowed.getParentNode()!=null? shadowed.getParentNode().getRelLocation(doc): new Point(0,0));
	abs.translate(shadowed.bbox.x, shadowed.bbox.y);
	bbox.setBounds(abs.x, abs.y, shadowed.bbox.width, shadowed.bbox.height);
//System.out.println(abs.x+","+abs.y);
//System.out.println("shadow = "+bbox+" on "+shadowed.bbox);
	valid_ = true;
	return ret;
  }

  public void paintNode(Rectangle docclip, Context cx) {
//	bbox.setSize(shadowed.bbox.width, shadowed.bbox.height);    // keep updated (as for when image loads asynchronously) for event propagagtion
	cx.reset(shadowed,-1);
	//int dx=shadowed.dx(), dy=shadowed.dy(); //bbox.x, dy=shadowed.bbox.y;
	int dx=shadowed.bbox.x, dy=shadowed.bbox.y;
	//int dx=bbox.x, dy=bbox.y;
	Graphics2D g = cx.g;
	g.translate(-dx,-dy); docclip.translate(dx,dy);     // undo shadowed's translations
//System.out.println("shadow undo "+dx+","+dy);
	shadowed.paintBeforeAfter(docclip, cx);
	g.translate(dx,dy); docclip.translate(-dx,-dy);
//System.out.println("painting "+getName()+", bbox="+bbox+", shadowing "+shadow.bbox);
	cx.valid = false;
  }

  public boolean eventNode(AWTEvent e, Point rel) {
//System.out.println(rel);
	//int dx=shadowed.dx(), dy=shadowed.dy();
	int dx=shadowed.bbox.x, dy=shadowed.bbox.y;
	//int dx=bbox.x, dy=bbox.y;
	if (rel!=null) rel.translate(dx,dy);    // undo shadowed's translations
	boolean ret = shadowed.eventBeforeAfter(e, rel); // should be eventBeforeAfter, but want (0,0) relative offset within shadow
	if (rel!=null) rel.translate(-dx,-dy);
	return ret;
  }

/*  public void repaint(long ms, int x, int y, int w, int h) {
	getParentNode().repaint(ms, x, y, w, h);    // don't add own offset
  }*/
}
