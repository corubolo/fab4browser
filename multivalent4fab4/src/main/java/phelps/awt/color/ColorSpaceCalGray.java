package phelps.awt.color;

import java.awt.color.ColorSpace;



/**
	CalGray CIE-based color space.

	@see multivalent.std.adaptor.pdf.PDF

	@author T.A. Phelps
	@version $Revision: 1.3 $ $Date: 2002/06/06 21:46:00 $
*/
public class ColorSpaceCalGray extends ColorSpace {
  static final ColorSpace CIEXYZ = getInstance(ColorSpace.CS_CIEXYZ);


  /** White point component. */
  float wx_, wy_, wz_;
  /** Black point component. */
  float bx_=0f, by_=0f, bz_=0f;
  /** Gamma correction. */
  float gamma_ = 1f;

  /** White point is required. */
  public ColorSpaceCalGray(float whitex, float whitey, float whitez) {
	super(TYPE_GRAY, 1);
	assert whitex>0f && whitez>0f && whitey==1f;
	wx_=whitex; wy_=whitey; wz_=whitez;
  }

  public void setBlackPoint(float x, float y, float z) {
	assert x>=0f && y>=0f && z>=0f;
	bx_=x; by_=y; bz_=z;
  }

  public void setGamma(float gamma) {
	assert gamma>0f;
	gamma_ = gamma;
  }


  public float[] fromCIEXYZ(float[] colorvalue) {
	assert colorvalue!=null && colorvalue.length>=3;

	float igamma = 1f / gamma_;
	float a1 = (float)Math.pow(colorvalue[0] / wx_, igamma);
	float a2 = (float)Math.pow(colorvalue[1] / wy_, igamma);
	float a3 = (float)Math.pow(colorvalue[2] / wz_, igamma);

	float[] out = new float[1];
	out[0] = (a1 + a2 + a3) / 3f;
	return out;
  }

  public float[] toCIEXYZ(float[] colorvalue) {
	assert colorvalue!=null && colorvalue.length>=1;

	// X = L = X_W * A^G
	float[] out = new float[3];
	float a = colorvalue[0];
	float agamma = (float)Math.pow(a, gamma_);
	out[0] = wx_ * agamma;
	out[1] = wy_ * agamma;
	out[2] = wz_ * agamma;
	return out;
	//return new float[] { xw*agamma, yw*agamma, zw*agamma };
  }


  public float[] fromRGB(float[] rgbvalue) { return fromCIEXYZ(CIEXYZ.fromRGB(rgbvalue)); }
  public float[] toRGB(float[] colorvalue) { return CIEXYZ.toRGB(toCIEXYZ(colorvalue)); }
}
