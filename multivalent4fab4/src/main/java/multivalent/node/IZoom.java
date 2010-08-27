package multivalent.node;

import java.awt.Rectangle;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.AWTEvent;
import java.util.Map;

import multivalent.*;

/**
	<i>Broken</i>
	Insert node into tree and set zoom factor to implement zooming on subtree.
	Formatting and repaint kept in non-zoomed coordinates.
	Events and painting zoomed.

	@version $Revision: 1.2 $ $Date: 2002/02/02 13:41:39 $
*/
public class IZoom extends INode {
  /** Zoom factor, in percent. */
  public int Zoom = 200;

  public IZoom(String name, Map<String,Object> attrs, INode parent) { super(name, attrs, parent); }

  /** Scale up formatted dimensions by zoom factor. */
  public boolean formatNode(int width, int height, Context cx) {
	boolean ret = super.formatNode(width, height, cx);
	bbox.width = (bbox.width * Zoom) / 100;  bbox.height = (bbox.height * Zoom) / 100;
	return ret;
  }

  /** Scale up from non-zoomed coordinates to zoomed visual. */
  public void paintNode(Rectangle docclip, Context cx) {
	// relative coordinates here, so origin in right place
	Graphics2D g = cx.g;
	//int dx=docclip.x, dy=docclip.y, dw=docclip.width, dh=docclip.height;
	//Rectangle gclip = g.getClipBounds();
	//g.setClip((gclip.x*Zoom)/100, (gclip.y*Zoom)/100, (gclip.width*Zoom)/100, (gclip.height*Zoom)/100);

	Rectangle r = g.getClipBounds();
	double f = Zoom/100.0;
	//double dx = r.x*(1-f), dy=r.y*(1-f);
	//g.translate(r.x - r.x*f, r.y - r.y*f); //-- Magnify, before scaling by f
	//double dx = r.x*(1.0-f), dy=r.y*(1.0-f);
	//g.translate(dx,dy);
	//g.scale(scalex, scaley);
	g.scale(f,f);
	g.setClip(0,0, bbox.width,bbox.height);

System.out.println("scale by "+(Zoom/100.0)+", clip "+r+" => "+g.getClipBounds());
//	docclip.x=(docclip.x*Zoom)/100; docclip.y=(docclip.y*Zoom)/100; docclip.width=(docclip.width*Zoom)/100; docclip.height=(docclip.height*Zoom)/100;

	super.paintNode(docclip, cx);

//	docclip.setBounds(dx,dy, dw,dh);
	//g.setClip(gclip);
/*
	g.scale(100.0/Zoom, 100.0/Zoom);
	g.translate(-r.x, -r.y);
*/
	f = 1.0/f;
	g.scale(f,f);
	//g.translate(-dx,-dy);
	//g.translate(r.x*f - r.x, r.y*f - r.y); //-- Magnify, before scaling by f
	//g.setColor(java.awt.Color.RED); g.draw(r);

  }

  /** Scale down from zoomed visual to non-zoomed coordinates. */
  public boolean eventNode(AWTEvent e, Point rel) {
	if (rel!=null) { rel.x=(rel.x*100)/Zoom; rel.y=(rel.y*100)/Zoom; }
	boolean ret = super.eventNode(e, rel);
	if (rel!=null) { rel.x = (rel.x*Zoom)/100; rel.y=(rel.y*Zoom)/100; }
	return ret;
  }

  //public void repaint(long ms, int x, int y, int w, int h) {}
}
