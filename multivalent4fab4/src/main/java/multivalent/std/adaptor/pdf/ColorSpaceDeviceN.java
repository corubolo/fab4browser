package multivalent.std.adaptor.pdf;

import java.awt.Color;
import java.awt.color.ColorSpace;



/**
	DeviceN color space.

	@version $Revision: 1.1 $ $Date: 2004/04/27 15:25:09 $
*/
public class ColorSpaceDeviceN extends ColorSpace {
  private static final ColorSpace CS_GRAY = ColorSpace.getInstance(ColorSpace.CS_GRAY);

  private Object[] name_;     // Alt, None, PANTONE..., ("All" not allowed)
  private ColorSpace base_;
  private Function tint_;

  private ColorSpaceDeviceN(Object[] name, ColorSpace alt, Function tint) {
	super((name.length==1? TYPE_GRAY: TYPE_2CLR-2 + name.length), name.length);
	name_ = name;
	base_ = alt;
	tint_ = tint;
  }

  public static ColorSpaceDeviceN getInstance(Object[] name, ColorSpace alt, Function fun) {
	return new ColorSpaceDeviceN(name, alt, fun);
  }

  float[] invert(float[] vals) {
	int len = vals.length;
	float[] invert = new float[vals.length];    // make changes in copy
	for (int i=0; i<len; i++) invert[i] = 1f - vals[i];
	return invert;
  }

  public float[] fromCIEXYZ(float[] colorvalue) {
	return CS_GRAY.fromCIEXYZ(invert(colorvalue));
  }
  public float[] toCIEXYZ(float[] colorvalue) {
	return CS_GRAY.toCIEXYZ(invert(colorvalue));
  }

  public float[] fromRGB(float[] rgbvalue) {
	return CS_GRAY.fromRGB(invert(rgbvalue));
  }

  public float[] toRGB(float[] colorvalue) {
	// for display, never have colorants, so map to alternative
	float[] rgb;
	if (name_==null || name_.length==0) {
	} if ("None".equals(name_[0])) {
		rgb = null;	// FIX: transparent
	} else { assert !"All".equals(name_[0]);
		float[] tint = new float[base_.getNumComponents()];
		tint_.compute(colorvalue, tint);
		rgb = base_.toRGB(tint);
//System.out.println(colorvalue[0]+" on "+base_+" => "+rgb[0]+"/"+rgb[1]+"/"+rgb[2]+" vs gray "+CS_GRAY.toRGB(tint)[0]);
	}
	return rgb;
  }
}
