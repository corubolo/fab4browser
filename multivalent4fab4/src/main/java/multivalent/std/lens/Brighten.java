package multivalent.std.lens;

import java.awt.image.ConvolveOp;
import java.awt.image.Kernel;
import java.util.Map;

import multivalent.*;



/**
	Edge detection lens, as in "Programmer’s Guide to the Java 2D™ API".

	@see Sharpen
	@see Blur

	@version $Revision: 1.3 $ $Date: 2003/06/02 05:53:15 $
*/
public class Brighten extends LensOp {
  private static final ConvolveOp OP = new ConvolveOp(new Kernel(1,1, new float[] { 1f/1.2f }), ConvolveOp.EDGE_NO_OP, null);   // 1f/1.2f rather than 0.8f so Lighten + Darken = unchanged

  public void restore(ESISNode n, Map<String,Object> attr, Layer layer) {
	super.restore(n, attr, layer);

	// take other kernels from attrs?

	op_ = OP;
  }
}
