package multivalent.std.lens;

import java.awt.image.ConvolveOp;
import java.awt.image.Kernel;
import java.util.Map;

import multivalent.*;



/**
	Image sharpen lens, as in "Programmer�s Guide to the Java 2D� API".

	@see Blur
	@see EdgeDetect

	@version $Revision: 1.2 $ $Date: 2003/06/02 05:52:56 $
*/
public class Sharpen extends LensOp {
  private static final float[] ELEMENTS = {
	0f, -1f, 0f,
	-1f, 5f, -1f,
	0f, -1f, 0f
  };
  private static final ConvolveOp OP = new ConvolveOp(new Kernel(3,3, ELEMENTS), ConvolveOp.EDGE_NO_OP, null);


  public void restore(ESISNode n, Map<String,Object> attr, Layer layer) {
	super.restore(n, attr, layer);

	// take other kernels from attrs?

	op_ = OP;
  }
}
