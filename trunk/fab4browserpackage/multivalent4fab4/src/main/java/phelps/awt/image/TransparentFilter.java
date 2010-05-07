package phelps.awt.image;

import java.awt.image.ColorModel;
import java.awt.Color;
import java.awt.image.IndexColorModel;
import java.awt.image.ImageFilter;



/**
	Make pixel with value closest to instantiation parameter transparent.
	Used to make background of scanned images transparent so can show selection, highlights.

	@see berkeley.adaptor.Xdoc

	@version $Revision: 1.3 $ $Date: 2002/02/02 13:01:27 $
*/
public class TransparentFilter extends ImageFilter {
  int poofr_, poofg_, poofb_;
  ColorModel newcm_;  // field not local var for setPixels below

  // instantiate with color to make transparent; <i>nearest</i> color in image will be made so
  public TransparentFilter(Color c) { this(c.getRed(), c.getGreen(), c.getBlue()); }
  public TransparentFilter(int r, int g, int b) {
	poofr_=r; poofg_=g; poofb_=b;
  }

  public void setColorModel(ColorModel model) {
	if (model instanceof IndexColorModel && ((IndexColorModel)model).getTransparentPixel()==-1) {
		IndexColorModel icm = (IndexColorModel)model;
		//int oldtrans = icm.getTransparentPixel();
		//assert oldtrans == -1: oldtrans; => may already have a transparent pixel, but may not be right one

		// can't modify model's transparent, but can replace it

		// find closest color in color map
		int size = icm.getMapSize();
		byte r[]=new byte[size], g[]=new byte[size], b[]=new byte[size];
		icm.getReds(r); icm.getGreens(g); icm.getBlues(b);
		int trans=0, transdist=Integer.MAX_VALUE;

//System.out.println("closest to "+poofr_+", "+poofg_+", "+poofb_);
		for (int i=0; i<size; i++) {
			int dist = 0;
			int tmp = Math.abs(poofr_-(r[i]&0xff)); dist += tmp*tmp;
			tmp = Math.abs(poofg_-(g[i]&0xff)); dist += tmp*tmp;
			tmp = Math.abs(poofb_-(b[i]&0xff)); dist += tmp*tmp;
//System.out.println("#"+i+" ["+r[i]+","+g[i]+","+b[i]+"] => "+Integer.toHexString(dist));
			if (dist < transdist) { trans=i; transdist=dist; }
		}
//System.out.println("transparent pixel is #"+trans+" of "+size+": r="+(r[trans]&0xff)+", g="+(g[trans]&0xff)+", b="+(b[trans]&0xff)+" vs "+oldtrans);

		//newcm_ = trans != oldtrans? new IndexColorModel(icm.getPixelSize(), size, r,g,b, trans): model;
		newcm_ = new IndexColorModel(icm.getPixelSize(), size, r,g,b, trans);
//System.out.println("transparency => "+((IndexColorModel)newcm_).getTransparentPixel()+" vs computed "+trans);

	} else newcm_ = model;

	super.setColorModel(newcm_);
  }

  // can't just replace color model -- have to setPixels too
  public void setPixels(int x, int y, int w, int h, ColorModel m, byte pixels[], int off, int scansize) {
//System.out.print(" "+((IndexColorModel)newcm_).getTransparentPixel());
	consumer.setPixels(x, y, w, h, newcm_, pixels, off, scansize);
  }
  public void setPixels(int x, int y, int w, int h, ColorModel m, int pixels[], int off, int scansize) {
//System.out.print(" "+((IndexColorModel)newcm_).getTransparentPixel());
	consumer.setPixels(x, y, w, h, newcm_, pixels, off, scansize);
  }

/*
  public int filterRGB(int x, int y, int rgb) {
System.out.println("never called");
	return rgb;
  }*/
}
