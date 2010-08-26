package multivalent.std.adaptor.pdf;

import java.awt.Color;
import java.awt.color.ColorSpace;



/**
	Separation color space.

	@version $Revision: 1.2 $ $Date: 2002/09/12 06:55:22 $
*/
public class ColorSpaceSeparation extends ColorSpace {
  private static final ColorSpace CS_GRAY = ColorSpace.getInstance(ColorSpace.CS_GRAY);

  private String name_;     // Alt, None, PANTONE..., whatever
  private ColorSpace base_;
  private Function tint_;

  private ColorSpaceSeparation(String name, ColorSpace alt, Function tint) {
	super(TYPE_GRAY, 1);    // always one component in
	name_ = name;
	base_ = alt;
	tint_ = tint;
  }

  public static ColorSpaceSeparation getInstance(String name, ColorSpace alt, Function fun) {
	return new ColorSpaceSeparation(name, alt, fun);
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
	// "For an additive device such as a computer display, a Separation color space never applies a process colorant directly; it always reverts to the alternate color space."
	float[] rgb;
//System.out.print("L*a*b* "+name_+" toRGB "+colorvalue[0]+"/"+colorvalue.length+" in "+base_+" x "+tint_+" = ");
	if ("All".equals(name_)) {	// "...ignore the alternateSpace and tintTransform parameters"
		// fill tint with 1.0s?
		float v = 1f - colorvalue[0];
		rgb = new float[] { v,v,v };

	} else if ("None".equals(name_)) {
		rgb = null;	// FIX: transparent

	} else {
		float[] tint = new float[base_.getNumComponents()];
		//for (int i=0,imax=tint.length; i<imax; i++) System.out.print("  "+tint[i]);
		tint_.compute(colorvalue, tint);	// "...transform a tint value into color component values in the alternate color space."
		//float[] out = base_.toRGB(tint);
		//System.out.print(", tinted to RGB "); for (int i=0,imax=out.length; i<imax; i++) System.out.print("  "+out[i]);  System.out.println();
		//System.out.println();
		rgb = base_.toRGB(tint);
	}

	return rgb;
  }
}
