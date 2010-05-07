package multivalent.std.lens;

import java.awt.image.ConvolveOp;
import java.awt.image.Kernel;
import java.util.Map;

import multivalent.*;



/**
	Image blur lens, as in "Programmer’s Guide to the Java 2D™ API".

	@see Sharpen
	@see EdgeDetect

	@version $Revision: 1.2 $ $Date: 2003/01/11 05:58:35 $
*/
public class Blur extends LensOp {
/* Gaussian slow and blurs too much.  for more blue, overlap another blur
  // Gaussian blur
  private static final int[] elements0_ = {
	1, 4, 8, 10, 8, 4, 1,
	4, 12, 25, 29, 25, 12, 4,
	8, 25, 49, 58, 49, 25, 8,
	10, 29, 58, 67, 58, 29, 10,
	8, 25, 49, 58, 49, 25, 8,
	4, 12, 25, 29, 25, 12, 4,
	1, 4, 8, 10, 8, 4, 1
  };
  private static final float[] elements_ = new float[elements0_.length];
  static {
	assert elements0_.length == 7*7;
	int sum = 0; for (int i=0,imax=elements0_.length; i<imax; i++) sum += elements0_[i];
	float fsum = (float)sum; for (int i=0,imax=elements0_.length; i<imax; i++) { elements_[i] = elements0_[i] / fsum; }
  }
  private static Kernel kernel_ = new Kernel(7,7, elements_);
*/
  private static final float[] ELEMENTS = {
	 1F/9f, 1f/9f, 1f/9f,
	 1f/9f, 1f/9f, 1f/9f,
	 1f/9f, 1f/9f, 1f/9f
  };
  private static final ConvolveOp OP = new ConvolveOp(new Kernel(3,3, ELEMENTS), ConvolveOp.EDGE_NO_OP, null);


  public void restore(ESISNode n, Map<String,Object> attr, Layer layer) {
	super.restore(n, attr, layer);

	// take other kernels from attrs?

	op_ = OP;
  }
}
