package phelps.awt.color;

import java.awt.color.ColorSpace;



/**
	CMYK color space.  Called DeviceCMYK by PDF.

	@author T.A. Phelps
	@version $Revision: 1.3 $ $Date: 2002/06/06 21:47:18 $
*/
public class ColorSpaceCMYK extends ColorSpace {
  private static final ColorSpace RGB = getInstance(ColorSpace.CS_sRGB);
  private static final ColorSpaceCMYK INSTANCE = new ColorSpaceCMYK();

  private ColorSpaceCMYK() { super(TYPE_CMYK, 4); }
  public static ColorSpaceCMYK getInstance() { return INSTANCE; }


  public float[] fromCIEXYZ(float[] colorvalue) { return fromRGB(RGB.fromCIEXYZ(colorvalue)); }
  public float[] toCIEXYZ(float[] colorvalue) { return RGB.toCIEXYZ(toRGB(colorvalue)); }

  /** Black generation and undercolor removal hardcoded to maximum.  See PDF Ref 1.4, page 377. */
  public float[] fromRGB(float[] rgbvalue) {
	float[] out = new float[4];
	float c = 1f - rgbvalue[0];   // c = 1-r
	float m = 1f - rgbvalue[1];   // m = 1-g
	float y = 1f - rgbvalue[2];   // y = 1-b
	float k = Math.min(Math.min(c,m), y);

	out[0]=c-k; out[1]=m-k; out[2]=y-k; out[3]=k;

	return out;
  }

  public float[] toRGB(float[] colorvalue) {
	// following PDF Ref 1.4, page 380.  Mathematically correct, but too bright.
	float[] out = new float[3];
	float k = colorvalue[3];
	assert colorvalue[0]>=0f && colorvalue[1]>=0f && colorvalue[2]>=0f && k>=0f;
	out[0] = 1f - Math.min(1f, colorvalue[0] + k);
	out[1] = 1f - Math.min(1f, colorvalue[1] + k);
	out[2] = 1f - Math.min(1f, colorvalue[2] + k);
	return out;
  }
}
