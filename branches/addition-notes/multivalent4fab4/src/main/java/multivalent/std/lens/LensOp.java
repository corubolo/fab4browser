package multivalent.std.lens;

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;

import multivalent.*;



/**
	Superclass for lenses that set operation for {@link java.awt.Graphics2D#drawImage(BufferedImage, BufferedImageOp, int, int)}.

	@version $Revision: 1.1 $ $Date: 2003/01/11 05:57:22 $
*/
public abstract class LensOp extends Lens {
  protected BufferedImageOp op_ = null;

  public boolean paintAfter(Context cx, Node node) {
	//if (g==null) return true;

	Rectangle r = getContentBounds();
	BufferedImage bi = (BufferedImage)getBrowser().getOffImage();
	int x=r.x, y=r.y, w=Math.min(r.width, bi.getWidth()-r.x), h=Math.min(r.height, bi.getHeight()-r.y);

	if (w>0 && h>0) {
		BufferedImage sub = bi.getSubimage(x,y, w,h);
		cx.g.drawImage(sub, op_, x,y);
	}

	return super.paintAfter(cx, node);
  }
}
