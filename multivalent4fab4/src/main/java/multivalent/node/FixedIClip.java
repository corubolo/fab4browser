package multivalent.node;

import java.awt.*;
import java.util.Map;

import multivalent.INode;
import multivalent.Context;



/**
	Clips children to be within its bbox, WxH of which are given in constructor and are independent of size and position of children.

	@version $Revision: 1.2 $ $Date: 2002/06/01 16:02:10 $
*/
public class FixedIClip extends IClip implements Fixed {
  Rectangle ibbox_ = new Rectangle();

  public FixedIClip(String name, Map<String,Object> attr, INode parent, Shape clip, Rectangle bounds) {
	super(name,attr, parent, clip, bounds);
//System.out.println(name+" "+clip+" "+phelps.text.Rectangles2D.pretty(bounds));
	getIbbox().setBounds(bounds);
  }

  public FixedIClip(String name, Map<String,Object> attr, INode parent, Shape clip) {
	this(name, attr, parent, clip, clip.getBounds());
  }

  public Rectangle getIbbox() { return ibbox_; }

  /** Make children relative according to initial bboxes. */
  public boolean formatNode(int width,int height, Context cx) {
//Rectangle r1 = new Rectangle(getIbbox());
	FixedI.formatNode(this, false, width,height, cx);

// PDF sometimes sets clip much bigger than children, sometimes "resetting" to full page

	// grow clip(!).  if clip is irregular, leave along (would be hard anyway)
//System.out.print("FixedIClip "+ibbox_);
//Rectangle r2 = new Rectangle(getIbbox());
	if (clip_ instanceof Rectangle) ((Rectangle)clip_).setSize(Math.max(ibbox_.width, bbox.width), Math.max(ibbox_.height, bbox.height));
//System.out.println(getName()+" "+phelps.text.Rectangles2D.pretty(r1)+" => "+phelps.text.Rectangles2D.pretty(r2)+" => "+phelps.text.Rectangles2D.pretty(getIbbox()));
//System.out.println(" => "+bbox+" / "+clip_);

	valid_ = true;
	return false;
  }
}
