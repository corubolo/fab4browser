package multivalent.std.lens;

import java.awt.*;
import java.awt.image.RescaleOp;
import java.awt.image.BufferedImage;

import multivalent.*;



/**
	Rescale lens.
	As described in the "Programmer’s Guide to the Java 2D™ API":
	"Rescaling can increase or decrease the intensity of
	all points. Rescaling can be used to increase the dynamic range of an otherwise
	neutral image, bringing out detail in a region that appears neutral or flat."

	@see Sharpen
	@see EdgeDetect

	@version $Revision: 1.1 $ $Date: 2002/11/18 04:49:54 $
*/
public class Rescale extends Lens {
  private static RescaleOp op_ = new RescaleOp(1.5f, 1.0f, null);


  public boolean paintAfter(Context cx, Node node) {
	//if (g==null) return true;

	Graphics2D g = cx.g;
	Rectangle r = getContentBounds();

	if (r.width>0 && r.height>0) {
		Browser br = getBrowser();
		BufferedImage bi = (BufferedImage)br.getOffImage();
		BufferedImage sub = bi.getSubimage(r.x,r.y, r.width,r.height);
		g.drawImage(sub, op_, r.x,r.y);
	}

	return super.paintAfter(cx, node);
  }
}
