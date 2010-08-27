package multivalent.node;

import java.awt.Rectangle;
import java.awt.Point;
import java.awt.AWTEvent;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.util.Map;

import multivalent.Context;
import multivalent.INode;
import multivalent.Leaf;



/**
	Holds a {@link java.awt.Shape}, which is painted stroked or filled or both.

	@version $Revision: 1.3 $ $Date: 2002/09/27 23:37:04 $
*/
public class LeafShape extends Leaf {
  protected Shape shape_;
  protected boolean stroke_=false, fill_=false;

  public LeafShape(String name, Map<String,Object> attr, INode parent, Shape shape, boolean stroke, boolean fill) {
	super(name,attr, parent);
	shape_ = shape;  assert shape!=null;
	stroke_=stroke; fill_=fill;  assert stroke || fill: "object not stroked or filled -- therefore not shown";
  }

  /** Returns shape (not a copy). */
  public Shape getShape() { return shape_; }
  //public void setShape(Shape s) { shape_ = s; }

  public boolean isStroke() { return stroke_; }
  public void setStroke(boolean b) { stroke_=b; }

  public boolean isFill() { return fill_; }
  public void setFill(boolean b) { fill_=b; }


  public boolean formatNode(int width, int height, Context cx) {
	super.formatNode(width, height, cx);	// have to let formatting happen in case close spans in content

	Rectangle r = shape_.getBounds();   // expensive
//System.out.println("formatNode bounds = "+r);
	bbox.setSize(Math.max(r.width,1), Math.max(r.height,1));    // lines and points can have 0 width or height

	valid_=true;
	return !valid_;
  }

/*  public boolean formatNodeContent(Context cx, int start, int end) {
	super.formatNodeContent(cx, start, end);	// have to let formatting happen in case close spans in content

	if (start==0) {
		Rectangle r = shape_.getBounds();
//System.out.println("formatNode bounds = "+r);
		bbox.setSize(Math.max(r.width,1), Math.max(r.height,1));    // lines and points can have 0 width or height
	}

	valid_=true;
	return !valid_;
  }*/

  /** Fill, then stroke shape. */
  public boolean paintNodeContent(Context cx, int start, int end) {
	if (start==0) {
		Graphics2D g = cx.g;

		// graphically fill then stroke
		if (fill_) {
//System.out.println("fill "+shape_+", color="+cx.foreground+", "+Rectangles2D.pretty(bbox));
			g.setColor(cx.foreground);
			g.fill(shape_);
		} /*no else -- can be both*/

		if (stroke_) {  // after fill so on top
//System.out.println("stroke "/*+shape_*/+", color="+cx.strokeColor+", "+Rectangles2D.pretty(bbox));
			g.setStroke(cx.getStroke());
			g.setColor(cx.strokeColor);
			g.draw(shape_); // stroke
		}
	}
	return false;
  }

  /**
	Shapes can interfere with text -- consider a diagonal line across the page and its large rectangular bounding box, 
	so on the one hand we want to ignore them.
	However, PDF can have hyperlinked shapes, so we want hits.
	Compromise: ignore stroke only, hit if fill (or fill&stroke).
	(If too much interference, limit to small shapes or rectangles.)
  */
  public boolean eventNode(AWTEvent e, Point rel) {
	return isFill() && bbox.width * bbox.height < 50*50/*not background*/? super.eventNode(e, rel): false;
  }
}
