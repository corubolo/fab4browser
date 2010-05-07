package edu.berkeley.lens;

import java.awt.Graphics2D;
import java.awt.Graphics;
import java.awt.Font;
import java.awt.Color;
import java.awt.Rectangle;

import multivalent.*;
import multivalent.std.lens.Lens;
import multivalent.std.GraphicsWrap;


/**
	Shadow hack

	@version $Revision$ $Date$
*/
public class Shadow extends Lens {
  static class GraphicsMonochrome extends GraphicsWrap {
	GraphicsMonochrome(Graphics2D g) { super(g); }
	public Graphics create() { return new GraphicsMonochrome((Graphics2D)wrapped_.create()); }
	public void setColor(Color c) {}
  }


  private Color shadowColor_ = new Color(100,100,100);


  public boolean paintAfter(Context cx, Node node) {
	Browser br = getBrowser();

	// draw twice
	Rectangle clip = getContentBounds(); //clip.translate(-br.xoff,-br.yoff);
	Graphics2D g = cx.g;
	Graphics2D gcopy = (Graphics2D)g.create();
	Graphics2D g2 = new GraphicsMonochrome(gcopy); g2.clipRect(clip.x,clip.y, clip.width,clip.height); clip = g2.getClipBounds();
//	CLGeneral gcx = (CLGeneral)br.getStyleSheet().get("ROOT");
//	Color bkgnd = gcx.getBackground();
//	g.setColor(bkgnd); g.fillRect(clip.x,clip.y, clip.width,clip.height);
//	gcx.setBackground(null);
	gcopy.setColor(shadowColor_); gcopy.translate(10,10); br.getRoot().paintBeforeAfter(g2.getClipBounds(), cx);
	br.getRoot().paintBeforeAfter(g.getClipBounds(), cx);  // draws background again, alas
//	gcx.setBackground(bkgnd);
	gcopy.dispose();

	return super.paintAfter(cx, node);
  }
}
