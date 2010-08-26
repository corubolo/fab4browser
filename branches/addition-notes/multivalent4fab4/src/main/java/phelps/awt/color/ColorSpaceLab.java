package phelps.awt.color;

import java.awt.color.ColorSpace;

import phelps.lang.Maths;



/**
	L*a*b* CIE-based color space.

<!--
[ /Lab
<< /WhitePoint [0.9505 1.0000 1.0890]
/Range [-128 127 -128 127]
>>
]
-->

	@author T.A. Phelps
	@version $Revision: 1.4 $ $Date: 2002/06/06 21:45:18 $
*/
public class ColorSpaceLab extends ColorSpace {
  static final ColorSpace CIEXYZ = getInstance(ColorSpace.CS_CIEXYZ);


  /** White point component. */
  float wx_, wy_, wz_;
  /** Black point component. */
  float bx_=0f, by_=0f, bz_=0f;
  /** Range (L always 0..100). */
  float amin_=-100f, amax_=100f, bmin_=-100f, bmax_=100f;

  /** Pass white point (x,y,z). */
  public ColorSpaceLab(float whitex, float whitey, float whitez) {
	super(TYPE_Lab, 3);
	assert whitex>0f && whitez>0f && whitey==1f;
	wx_=whitex; wy_=whitey; wz_=whitez;
  }

  public void setBlackPoint(float x, float y, float z) {
	assert x>=0f && y>=0f && z>=0f;
	bx_=x; by_=y; bz_=z;
  }

  public void setRange(float amin, float amax, float bmin, float bmax) {
	assert amin<=amax && bmin<=bmax;
	amin_=amin; amax_=amax; bmin_=bmin; bmax_=bmax;
//System.out.println("L*a*b* white "+wx_+" "+wy_+" "+wz_+", range "+amin_+".."+amax_+"  "+bmin_+".."+bmax_);
  }

  public float getMinValue(int component) { return (component==0? 0: component==1? amin_: bmin_); }
  public float getMaxValue(int component) { return (component==0? 100: component==1? amax_: bmax_); }


  /**
	NOT IMPLEMENTED (not needed for PDF).
	Do not invoke Color.getComponents() or Color.getColorComponents() on colors in this ColorSpace.
  */
  public float[] fromCIEXYZ(float[] colorvalue) {
	assert false;
	return null;
  }

  /** Goofy Lab-specific function. */
  private final float g(float x) {
	return x >= 6f/29f? x*x*x: (108f/841f) * (x - 4f/29f);
  }

  public float[] toCIEXYZ(float[] colorvalue) {
//System.out.print("Lab to XYZ "+colorvalue[0]+" "+colorvalue[1]+" "+colorvalue[2]);
	float Lstar=colorvalue[0], astar=Maths.minmax(amin_, colorvalue[1], amax_), bstar=Maths.minmax(bmin_, colorvalue[2], bmax_);
//System.out.print(", astar="+astar+", bstar="+bstar);
	float Lstar16 = (Lstar+16f)/116f;
	float L = Lstar16 + astar/500f, M = Lstar16, N = Lstar16 - (bstar / 200f);

	float[] out = new float[3];
	out[0] = wx_ * g(L);
	out[1] = wy_ * g(M);
	out[2] = wz_ * g(N);
//System.out.println(", => "+out[0]+" "+out[1]+" "+out[2]);
//float[] rgb = CIEXYZ.toRGB(out); System.out.println("\trgb "+rgb[0]+" "+rgb[1]+" "+rgb[2]);
//out[0]=1f-out[0]; out[1]=1f-out[1]; out[2]=1f-out[2];
	return out;
  }

  public float[] fromRGB(float[] rgbvalue) { return fromCIEXYZ(CIEXYZ.fromRGB(rgbvalue)); }
  public float[] toRGB(float[] colorvalue) { return CIEXYZ.toRGB(toCIEXYZ(colorvalue)); }
}
