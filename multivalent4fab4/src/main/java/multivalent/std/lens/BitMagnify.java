package multivalent.std.lens;

import java.awt.*;
import java.awt.image.ImageObserver;

import multivalent.*;



/**
	Enlarge contexts <i>by doubling pixel width and height</i>
	as opposed to drawing at larger size (scaling with ATM).
	Usually use <tt>Magnify</tt> lens, but <tt>BitMagnify</tt> useful for debugging (layout and other kinds).
	(Broken if use Java 1.4's VolatileImage, which we don't anymore, because VolatileImage won't draw on top of itself.)

	@see Magnify

	@version $Revision: 1.2 $ $Date: 2002/11/18 04:48:05 $
*/
public class BitMagnify extends Lens implements ImageObserver {
  public boolean imageUpdate(Image img, int flags, int x,int y, int w,int h) {
	return (flags&ImageObserver.ALLBITS)==0;
  }

  public boolean paintAfter(Context cx, Node node) {
	Browser br = getBrowser();

	Rectangle r = getContentBounds();
	// if any damage that's not full visible area, have to draw whole lens (can't just enlarge clip region as already suffered damange to part to enlarge--can do this right (ah, differently, as bit magnify still valuable) when implement zoom with Java 2)
	// specify (x1,y1,x2,y2) whereas usually specify (x,y,w,h)
	Graphics2D g = cx.g;
	int x=r.x, y=r.y, width=r.width, height=r.height, sw=width/2, sh=height/2;
	//g.drawImage(br.getOffImage(), /*dest*/x,y,x+width,y+height, /*src*/x,y,x+sw,y+sh, this);    // => not smart about drawing on top of itself
	g.drawImage(br.getOffImage(), /*dest*/x+sw,y+sh,x+width,y+height, /*src*/x,y,x+sw,y+sh, this);  // first copy source to survive overwrite
	g.drawImage(br.getOffImage(), /*dest*/x,y, x+width,y+height, /*src*/x+sw,y+sh,x+width,y+height, this);

	return super.paintAfter(cx, node);
  }

/*
  // transform coordinates and pass through
  public boolean lensevent(AWTEvent e, Point scrn) {
//System.out.print("magnify on "+rel.x+","+rel.y);
	//Rectangle r = getContentBounds();
	//int newx = rel.x - (rel.x-r.x)/2;
	//relx = bbox_.x + (relx - bbox_.x)/2;
	//rely = bbox_.y + titleh + (rely - bbox_.y - titleh)/2;

	rel.translate(-rel.x/2, -rel.y/2);
//System.out.println("=> "+rel.x+","+rel.y);

	return false;
  }*/
  protected Point rel_ = new Point();
  protected boolean warp_;
  /** Transform mouse coordinates to match magnified. */
  public boolean eventBefore(AWTEvent e, Point rel, Node obsn) {
	boolean ret = super.eventBefore(e, rel, obsn);
	if (!ret) {
		Rectangle r = getContentBounds();
		warp_ = r.contains(rel);
		if (warp_) {
			rel_.setLocation(rel);
			rel.x = (rel.x - r.x)/2 + r.x;
			rel.y = (rel.y - r.y)/2 + r.y;
		}
	}
	return ret;
  }

  public boolean eventAfter(AWTEvent e, Point rel, Node obsn) {
	if (warp_) { rel.setLocation(rel_); warp_=false; }
	return super.eventAfter(e, rel, obsn);
  }

}
