package phelps.awt.image;

import java.awt.image.Raster;
import java.awt.image.DataBufferByte;
import java.awt.color.ColorSpace;
import java.util.Map;
import java.util.HashMap;

import phelps.awt.color.ColorSpaceCMYK;
import phelps.awt.color.ColorSpaceYCCK;
import phelps.awt.color.ColorSpaceLab;



/**
	Image rasters.

	@version $Revision$ $Date$
*/
public class Rasters {
  private static final boolean DEBUG = false;

  private Rasters() {}


  /**
	Converts raster data from <var>cs</var> to RGB,
	because Java2D can't apply AffineTransformOp to 4-component images
	and ImageIO can't process 4-component JPEG either.
	ColorSpace must be one of {@link ColorSpaceCMYK}, {@link ColorSpaceYCCK}, or {@link ColorSpaceLab}.
  */
  public static Raster toRGB(Raster r, ColorSpace cs) {
	//if (cs instanceof ColorSpaceRGB) return r;
	int width = r.getWidth(), height = r.getHeight();
	byte[] rgb = new byte[width*height*3];
	//boolean fAdobe = jcs<0; jcs = Math.abs(jcs);
//System.out.println("nComp = "+cs.getNumComponents());	// 1, 3, 4
//if (DEBUG) System.out.println("converting JPEG "+jcs+" / Adobe? "+fAdobe);

	// image can be big and samples at 4 bytes each across CMYK = 70M for 2100x2100 image, so convert in horizontal chunks
	// If conversion done internally by ImageIO, could access samples directly and avoid allocation and copying.
	final int HUNK = 50;
	Map<Integer,Integer> cache = new HashMap<Integer,Integer>(20000);	// 30 sec => 9 sec on DisneyMap.pdf
long start = System.currentTimeMillis();
	for (int x=0,y=0,w=width, base=0; y<height; y+=HUNK) {
		int h = Math.min(height-y, HUNK);

		if (cs instanceof ColorSpaceLab) {	// L*a*b* seen in colour.pdf, DisneyMap.pdf
			int[] A = r.getSamples(x,y,w,h, 0, (int[])null), B = r.getSamples(x,y,w,h, 1, (int[])null), C = r.getSamples(x,y,w,h, 2, (int[])null);
			float[] abc = new float[3];
			for (int i=0,imax=A.length; i<imax; i++, base+=3) {

				//if (a==A[i] && b==B[i] && c==C[i] && i>0) { rgb[base]=rgb[base-3]; rgb[base+1]=rgb[base-3+1]; rgb[base+2]=rgb[base-3+2]; }	// 40=>30 sec on DisneyMap.pdf => wash with hash of all conversions
				int a=A[i], b=B[i], c=C[i];
				Integer key = new Integer((a<<16) | (b<<8) | c), val = cache.get(key);
				if (val!=null) {
					int v = val.intValue();
					rgb[base] = (byte)(v>>16); rgb[base+1] = (byte)(v>>8); rgb[base+2] = (byte)v;
				} else {
					abc[0]=a*100f/255f; abc[1]=b-128f; abc[2]=c-128f;
					float[] def = cs.toRGB(abc);
//if (i<5) System.out.println(A[i]+" "+B[i]+" "+C[i]+" => "+def[0]+" "+def[1]+" "+def[2]);
					int di = (int)(def[0]*255f), ei = (int)(def[1]*255f), fi = (int)(def[2]*255f);
					rgb[base] = (byte)di; rgb[base+1] = (byte)ei; rgb[base+2] = (byte)fi;
					cache.put(key, new Integer((di<<16) | (ei<<8) | fi));
				}
			}
			A = B = C = null;

		} else if (cs instanceof ColorSpaceYCCK) {
//if (DEBUG && y==0) System.out.println("converting YCCK JPEG to RGB");
			float[] Y = r.getSamples(x,y,w,h, 0, (float[])null), Cb = r.getSamples(x,y,w,h, 1, (float[])null), Cr = r.getSamples(x,y,w,h, 2, (float[])null), K = r.getSamples(x,y,w,h, 3, (float[])null);
			//ColorSpace YCC = ColorSpace.getInstance(ColorSpace.CS_PYCC);
			//float[] ycc = new float[3];	// cache last conversion
			for (int i=0,imax=Y.length; i<imax; i++, base+=3) {
				// faster to track last cmyk and save computations on stretches of same color?
				float yy = Y[i] - K[i], cb=Cb[i], cr=Cr[i];
				float val = yy + 1.402f*(cr-128f); rgb[base] = val<=0f? (byte)0: val>=254.5f? (byte)0xff: (byte)(val+0.5f);
				val = yy - 0.34414f*(cb-128f) - 0.71414f*(cr-128f); rgb[base+1] = val<=0f? (byte)0: val>=254.5f? (byte)0xff: (byte)(val+0.5f);
				val = yy + 1.772f * (cb-128f); rgb[base+2] = val<=0f? (byte)0: val>=254.5f? (byte)0xff: (byte)(val+0.5f);
				/*
				ycc[0] = Y[i] - K[i]; ycc[1]=Cb[i]; ycc[2]=Cr[i];
				float[] cvt = YCC.toRGB(ycc);
				rgb[i] = (byte)cvt[0]; rgb[i+1] = (byte)cvt[1]; rgb[i+2] = (byte)cvt[2];
				*/
			}
			Y = Cb = Cr = K = null;

		} else { assert cs instanceof ColorSpaceCMYK;
//*if (DEBUG && y==0)*/ System.out.println("converting CMYK JPEG to RGB");
			int[] C = r.getSamples(x,y,w,h, 0, (int[])null), M = r.getSamples(x,y,w,h, 1, (int[])null), Y = r.getSamples(x,y,w,h, 2, (int[])null), K = r.getSamples(x,y,w,h, 3, (int[])null);
			for (int i=0,imax=C.length; i<imax; i++, base+=3) {
				// use cache?  as fast to compute values?
				int k = K[i];
				rgb[base] = (byte)(255 - Math.min(255, C[i] + k));
				rgb[base+1] = (byte)(255 - Math.min(255, M[i] + k));
				rgb[base+2] = (byte)(255 - Math.min(255, Y[i] + k));
			}
			C = M = Y = K = null;
		}
	}

long end = System.currentTimeMillis(); if (DEBUG) System.out.println("elapsed " +((end-start)/1000)+", cache size = "+cache.size());
	/*if (fAdobe) {
//System.out.println("inverting Photoshop data");
		for (int i=0,imax=rgb.length; i<imax; i++) rgb[i] = (byte)(255 - (rgb[i]&0xff));
	}*/

	// from other image types we know InterleavedRaster's can be manipulated by AffineTransformOp, so create one of those.
//if (DEBUG && rgb.length > 10*1024*1024) { System.gc(); System.out.println("free = "+Runtime.getRuntime().freeMemory()); }
	r = null; cache=null;	// gc
	return Raster.createInterleavedRaster(new DataBufferByte(rgb, rgb.length), width, height, width*3,3, new int[] {0,1,2}, null);
  }
}
