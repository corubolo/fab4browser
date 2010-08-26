package phelps.awt.color;

import java.awt.color.ColorSpace;
import java.awt.Color;



/**
	Hue - Saturation - Value/Brightness (HSV/HSB) color space.

	@author T.A. Phelps
	@version $Revision: 1.1 $ $Date: 2002/06/06 19:07:33 $
*/
public class ColorSpaceHSV extends ColorSpace {
  static final ColorSpace RGB = getInstance(ColorSpace.CS_sRGB);
  static ColorSpaceHSV instance_ = null;

  private ColorSpaceHSV() { super(ColorSpace.TYPE_HSV, 3); }
  public static ColorSpaceHSV getInstance() {
	if (instance_==null) instance_ = new ColorSpaceHSV();
	return instance_;
  }


  public float[] fromCIEXYZ(float[] colorvalue) { return fromRGB(RGB.fromCIEXYZ(colorvalue)); }
  public float[] toCIEXYZ(float[] colorvalue) { return RGB.toCIEXYZ(toRGB(colorvalue)); }


  /** Black generation and undercolor removal hardcoded to maximum.  See PDF Ref 1.4, page 377. */
  public float[] fromRGB(float[] rgbvalue) {
	return Color.RGBtoHSB((int)(rgbvalue[0]*255f), (int)(rgbvalue[1]*255f), (int)(rgbvalue[2]*255f), null);
  }

  public float[] toRGB(float[] colorvalue) {
	int rgb = Color.HSBtoRGB(colorvalue[0], colorvalue[1], colorvalue[2]);

	float[] out = new float[3];
	out[0] = ((rgb>>16)&0xff) / 255f;
	out[1] = ((rgb>>8)&0xff) / 255f;
	out[2] = (rgb&0xff) / 255f;

	return out;
  }
}
