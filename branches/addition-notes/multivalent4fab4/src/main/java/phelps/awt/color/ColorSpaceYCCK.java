package phelps.awt.color;

import java.awt.color.ColorSpace;



/**
	YCCK color space.  Called DeviceYCCK by PDF.

	@author T.A. Phelps
	@version $Revision$ $Date$
*/
public class ColorSpaceYCCK extends ColorSpace {
  private static final ColorSpace RGB = getInstance(ColorSpace.CS_sRGB);
  private static final ColorSpaceYCCK INSTANCE = new ColorSpaceYCCK();

  private ColorSpaceYCCK() { super(TYPE_4CLR, 4); }
  public static ColorSpaceYCCK getInstance() { return INSTANCE; }


  public float[] fromCIEXYZ(float[] colorvalue) { return fromRGB(RGB.fromCIEXYZ(colorvalue)); }
  public float[] toCIEXYZ(float[] colorvalue) { return RGB.toCIEXYZ(toRGB(colorvalue)); }

  /** NOT IMPLEMENTED. */
  public float[] fromRGB(float[] rgbvalue) {
	return null;
  }

  public float[] toRGB(float[] colorvalue) {
	float[] out = new float[3];
	float yy = colorvalue[0] - colorvalue[3], cb=colorvalue[1], cr=colorvalue[2];
	float val = yy + 1.402f*(cr-128f); out[0] = val<=0f? (byte)0: val>=254.5f? (byte)0xff: (byte)(val+0.5f);
	val = yy - 0.34414f*(cb-128f) - 0.71414f*(cr-128f); out[1] = val<=0f? (byte)0: val>=254.5f? (byte)0xff: (byte)(val+0.5f);
	val = yy + 1.772f * (cb-128f); out[2] = val<=0f? (byte)0: val>=254.5f? (byte)0xff: (byte)(val+0.5f);
	return out;
  }
}
