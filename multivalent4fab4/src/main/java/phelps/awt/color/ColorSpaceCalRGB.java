package phelps.awt.color;

import java.awt.color.ColorSpace;



/**
	CalRGB CIE-based color space.  Used by PDF.

<!--
[/CalRGB <<
/WhitePoint [ 0.9505 1 1.089 ]
/Gamma [ 2.22221 2.22221 2.22221 ]
/Matrix [ 0.4124 0.2126 0.0193 0.3576 0.71519 0.1192 0.1805 0.0722 0.9505 ]
>>]
-->

	@author T.A. Phelps
	@version $Revision: 1.4 $ $Date: 2002/06/06 21:43:27 $
*/
public class ColorSpaceCalRGB extends ColorSpace {
  static final float[] IDENTITY_MATRIX = { 1f,0f,0f,  0f,1f,0f,  0f,0f,1f };

  static final ColorSpace CIEXYZ = getInstance(ColorSpace.CS_CIEXYZ);

/* doesn't seem to speed up
  static final int INCS = 1000;
  static float[] pow1 = new float[INCS];
  static { float val=0f, inc=1f/INCS; for (int i=0,imax=INCS; i<imax; i++, val+=inc) pow1[i]=val; }

  float[] powa=pow1, powb=pow1, powc=pow1;
*/
  /** White point component. */
  float wx_, wy_, wz_;
  /** Black pointg component. */
  float bx_=0f, by_=0f, bz_=0f;
  /** Gamma. */
  float gammar_=1f, gammag_=1f, gammab_=1f;

  float[] matrix_ = (float[])IDENTITY_MATRIX.clone();


  // images invoke toCIEXYZ many times, so important to cache
  //float[] lastcol = { -1f, -1f, -1f };   // initialize as invalid
  //float[] last2cie = new float[3];
  static final int CACHEMAX = 10;
  float[][] fromcol=new float[CACHEMAX][3], tocie=new float[CACHEMAX][3];
  int cachei = 0;


  /** White point is required. */
  public ColorSpaceCalRGB(float whitex, float whitey, float whitez) {
	super(TYPE_RGB, 3);

	assert whitex>=0f && whitez>0f && whitey==1f: whitex+" "+whitez+" "+whitey;    // spec says whitex and whitez positive, but means non-negative
	wx_=whitex; wy_=whitey; wz_=whitez;

	for (int i=0; i<CACHEMAX; i++) fromcol[i][0] = -1f;   // mark as invalid

  }

  public void setBlackPoint(float x, float y, float z) {
	assert x>=0f && y>=0f && z>=0f;
	bx_=x; by_=y; bz_=z;
  }

  public void setGamma(float r, float g, float b) {
	assert r>0f && g>0f && b>1f;
	gammar_=r; gammag_=g; gammab_=b;

/*
	double val, inc=1.0/INCS;
	if (gammar==1f) powa=pow1; else { val=0.0; for (int i=0,imax=INCS; i<imax; i++, val+=inc) powa[i] = (float)Math.pow(val, gammar); }
	if (gammag==1f) powb=pow1; else if (gammag==gammar) powb=powa; else { val=0.0; for (int i=0,imax=INCS; i<imax; i++, val+=inc) powb[i] = (float)Math.pow(val, gammag); }
	if (gammab==1f) powc=pow1; else if (gammab==gammag) powc=powb; else if (gammab==gammar) powc=powa; else { val=0.0; for (int i=0,imax=INCS; i<imax; i++, val+=inc) powc[i] = (float)Math.pow(val, gammab); }
*/
  }

  public void setMatrix(float[] matrix) {
	assert matrix!=null && matrix.length==9;
	matrix_ = matrix;
  }


  /** NOT IMPLEMENTED (not needed for PDF).  So don't call Color.getComponents() or Color.getColorComponents() on colors in this ColorSpace. */
  public float[] fromCIEXYZ(float[] colorvalue) {
	assert false;

	//float iag=1f/gammar, ibg=1f/gammag, icg=1f/gammab;

	float[] out = new float[3];
	// three equations in three unknowns -- LATER
	//float a1 = (float)Math.pow(colorvalue[0] / xw, igamma);
	//float a2 = (float)Math.pow(colorvalue[1] / yw, igamma);
	//float a3 = (float)Math.pow(colorvalue[2] / zw, igamma);
	out = null;

	return out;
  }

  /**
	X = L = X_W * A^G
  */
  public float[] toCIEXYZ(float[] colorvalue) {
	float[] out;

	float A=colorvalue[0], B=colorvalue[1], C=colorvalue[2];

	for (int i=0; i<CACHEMAX; i++) {
		float[] lastcol = fromcol[i];
		//if (Arrays.equals(colorvalue, fromcol[i])) return (float[])tocie[i].clone();  // necessary to clone? allocation cheap, and if unnecessary, little harm (I hope)
		if (A==lastcol[0] && B==lastcol[1] && C==lastcol[2]) return (float[])tocie[i].clone();
	}

	float ag = (float)Math.pow(A, gammar_), bg = (float)Math.pow(B, gammag_), cg = (float)Math.pow(C, gammab_);

	//if (matrix_ == IDENTITY_MATRIX) ... save multiplies, but don't get identity often, and gamma never 1.0
	out = new float[3];
	out[0] = matrix_[0]*ag + matrix_[3]*bg + matrix_[6]*cg;
	out[1] = matrix_[1]*ag + matrix_[4]*bg + matrix_[7]*cg;
	out[2] = matrix_[2]*ag + matrix_[5]*bg + matrix_[8]*cg;
	// what about white point?

//System.out.println("toCIE "+colorvalue[0]+" "+colorvalue[1]+" "+colorvalue[2]);
	//System.arraycopy(colorvalue,0, fromcol[cachei],0, 3);
	//System.arraycopy(out,0, tocie[cachei],0, 3);


	float[] f=fromcol[cachei]; f[0]=A; f[1]=B; f[2]=C;
	f=tocie[cachei]; f[0]=out[0]; f[1]=out[1]; f[2]=out[2];
	cachei++; if (cachei==CACHEMAX) cachei=0;

	return out;
  }


  public float[] fromRGB(float[] rgbvalue) { return fromCIEXYZ(CIEXYZ.fromRGB(rgbvalue)); }
  public float[] toRGB(float[] colorvalue) { return CIEXYZ.toRGB(toCIEXYZ(colorvalue)); }
}
