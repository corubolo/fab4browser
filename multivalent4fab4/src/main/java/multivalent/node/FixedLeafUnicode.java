package multivalent.node;

import java.awt.Rectangle;
import java.util.Map;

import multivalent.INode;
import multivalent.Context;

import phelps.awt.geom.Rectangles2D;



/**
	Leaf subclass for fixed-formatted ASCII.

	@version $Revision: 1.3 $ $Date: 2002/02/02 13:41:39 $
*/
public class FixedLeafUnicode extends LeafUnicode implements Fixed {
  Rectangle ibbox_ = new Rectangle(0,0,0,0);  // actually, (x,y) computed from current children

  public FixedLeafUnicode(String name, Map<String,Object> attr, INode parent) { this(name,null, attr, parent);	}
  public FixedLeafUnicode(String name, String glyphs,  Map<String,Object> attr, INode parent) { super(name,glyphs, attr, parent);	}


  public Rectangle getIbbox() { return ibbox_/*.clone()*/; }


  public boolean formatNodeContent(Context cx, int start, int end) {
	boolean ret = super.formatNodeContent(cx, start, end);
	if (start==0) bbox.setLocation(ibbox_.x, ibbox_.y);
//if ("people".equals(getName()) || "retrieved".equals(getName())) System.out.println(getName()+", points="+cx.getFont().getSize()+", w="+bbox.width+" vs "+cx.getFont().getStringBounds(getName()).getWidth());
	return ret;
  }


  public void dump(int level, boolean recurse) {
	System.out.println(getName()+", ibbox="+Rectangles2D.pretty(ibbox_)+", bbox="+Rectangles2D.pretty(bbox));
  }
}
