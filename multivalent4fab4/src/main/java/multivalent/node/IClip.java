package multivalent.node;

import java.awt.*;
import java.util.Map;

import multivalent.INode;
import multivalent.Context;



/**
	Clips children to be within its bbox, WxH of which are given in constructor and are independent of size and position of children.

	@version $Revision: 1.2 $ $Date: 2002/03/20 04:19:28 $
*/
public class IClip extends INode {
  Shape clip_;	// same as bbox?
  Rectangle cropbox_;	// could be expensive to compute

  public IClip(String name, Map<String,Object> attr, INode parent, Shape clip) { this(name,attr, parent, clip, clip.getBounds()); }

  /**
	@param clip   Generally clip has origin at (0,0) (not copied)
	@param bounds (not copied)
  */
  public IClip(String name, Map<String,Object> attr, INode parent, Shape clip, Rectangle bounds) {
	super(name,attr, parent);
	clip_ = clip;
	cropbox_ = bounds;
  }


  /** Dimensions of clipping region. */
  public Rectangle getCrop() { return cropbox_; }

  public Shape getClip() { return clip_; }



  /** Dimensions set to WxH of clip. */
  public boolean formatNode(int width,int height, Context cx) {
	boolean ret = super.formatNode(width, height, cx);
	//if (start==0) bbox.setSize(bounds.width, bounds.height);
	bbox.setSize(cropbox_.width, cropbox_.height);
	//bbox.setBounds(cropbox_);	// harmless if parent positions, better if parent INode or one that doesn't position
	return ret;
  }


  /** Set clip, draw content, remove clip. */
  public void paintNode(Rectangle docclip, Context cx) {
	Graphics2D g = cx.g;
	//Rectangle r = g.getClipBounds();
	Shape clip = g.getClip();

//System.out.println("clipping to "+clip_+" "+clip_.getBounds());
	g.clip(clip_);	// intersect
	//g.clipRect(0,0, bbox.width+1,bbox.height+1);	// intersect => wrong, rectangle not shape.  keep pixels on border.
	super.paintNode(docclip/*?*/, cx);
//g.setColor(Color.RED); g.drawRect(0,0,bbox.width,bbox.height);	// debug by showing clip rect
//g.setColor(Color.BLUE); g.draw(clip_);
	g.setClip(clip);
  }
}
