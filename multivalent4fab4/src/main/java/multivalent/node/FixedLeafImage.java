package multivalent.node;

import java.awt.Rectangle;
import java.awt.Image;
import java.util.Map;

import multivalent.Context;
import multivalent.INode;



/**
	Holds an {@link java.awt.Image}.

	@version $Revision: 1.1 $ $Date: 2002/01/27 02:55:49 $
*/
public class FixedLeafImage extends LeafImage implements Fixed {
  public Rectangle ibbox_ = new Rectangle(0,0,0,0);

  public FixedLeafImage(String name, Map<String,Object> attr, INode parent, Image img) {
	super(name,attr, parent, img);
	assert img!=null;
  }

  public Rectangle getIbbox() { return ibbox_; }

  public boolean formatNodeContent(Context cx, int start, int end) {
	boolean ret = super.formatNodeContent(cx, start, end);
	if (start==0) bbox.setLocation(ibbox_.x, ibbox_.y);
	return ret;
  }

  // maybe paint so fits in ibbox?
}
