package multivalent.node;

import java.awt.Rectangle;
import java.util.Map;

import multivalent.Context;
import multivalent.INode;



/**
	@version $Revision$ $Date$
*/
public class FixedLeafBlock extends LeafBlock implements Fixed {
  public Rectangle ibbox_;

  public FixedLeafBlock(String name, Map<String,Object> attr, INode parent, Rectangle bbox) {
	super(name,attr, parent, bbox.x, bbox.y);
	ibbox_ = bbox;
	assert bbox!=null;
  }

  public Rectangle getIbbox() { return ibbox_; }

  public boolean formatNodeContent(Context cx, int start, int end) {
	boolean ret = super.formatNodeContent(cx, start, end);
	if (start==0) bbox.setLocation(ibbox_.x, ibbox_.y);
	return ret;
  }

  // maybe paint so fits in ibbox?
}
