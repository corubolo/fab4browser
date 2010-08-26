package multivalent.std.lens;

import java.awt.*;
//import java.awt.geom.AffineTransform;
import java.util.Map;

import multivalent.*;

import phelps.lang.Doubles;



/**
	<i>Doesn't work</i>  Rotate landscape into portrait, say.

	@version $Revision$ $Date$
*/
public class Rotate extends Lens {
  public static final String ATTR_THETA = "theta";

  public double theta_ = Math.PI/2.0;

  public boolean paintBefore(Context cx, Node node) {
	super.paintBefore(cx, node);
	//Rectangle r=getContentBounds();
	//g.translate(-r.x,-r.y);
	Graphics2D g = cx.g;
	g.rotate(theta_);
	return false;
  }

  public void restore(ESISNode n, Map<String,Object> attr, Layer layer) {
	super.restore(n,attr,layer);
	theta_ = Doubles.parseDouble(getAttr(ATTR_THETA), theta_);
  }
}
