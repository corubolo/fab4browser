package multivalent.node;

import java.awt.Rectangle;
import java.awt.Shape;
import java.util.Map;

import multivalent.Context;
import multivalent.INode;



/**
	Holds a {@link java.awt.Shape}.

	@version $Revision: 1.2 $ $Date: 2002/02/02 13:41:39 $
*/
public class FixedLeafShape extends LeafShape implements Fixed {
  public Rectangle ibbox_ = new Rectangle(0,0,0,0);  //-1,-1);	 // actually, (x,y) computed from current children

  public FixedLeafShape(String name, Map<String,Object> attr, INode parent, Shape shape, boolean stroke, boolean fill) { super(name,attr, parent, shape, stroke, fill); }

  public Rectangle getIbbox() { return ibbox_/*.clone()*/; }

  public boolean formatNodeContent(Context cx, int start, int end) {
	boolean ret = super.formatNodeContent(cx, start, end);  // => does full shape_.getBounds
	if (start==0) bbox.setLocation(ibbox_.x, ibbox_.y);
	return ret;
  }
}
