package multivalent.std.adaptor.pdf;

import static multivalent.std.adaptor.pdf.COS.CLASS_ARRAY;
import static multivalent.std.adaptor.pdf.COS.CLASS_DICTIONARY;
import static multivalent.std.adaptor.pdf.COS.CLASS_NAME;
import static multivalent.std.adaptor.pdf.COS.OBJECT_NULL;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Transparency;
import java.awt.color.ColorSpace;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.ComponentColorModel;
import java.awt.image.ComponentSampleModel;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.IndexColorModel;
import java.awt.image.Raster;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;

import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

import jbig2dec.Jbig2dec;
import phelps.awt.color.ColorSpaceCMYK;
import phelps.awt.color.ColorSpaceLab;
import phelps.awt.color.ColorSpaceYCCK;
import phelps.awt.image.Rasters;
import phelps.io.InputStreams;
import uk.ac.liverpool.fab4.Fab4utils;
import uk.co.mmscomputing.imageio.ppm.PPMImageReader;

import com.pt.imageio.plugins.Fax;
import com.pt.imageio.plugins.JPEG;

/**
 * Interpret PDF image types, from objects or inline (content stream): DCT
 * (JPEG), JPX (JPEG2000), CCITT FAX (Group 3, Group 3 2D, Group 4), raw samples
 * (bit depth 1,2,4,8), JBIG2. Does no cacheing; always creates new image.
 * 
 * @see javax.imageio
 * @see com.pt.imageio.plugins.Fax
 * @version $Revision: 1.89 $ $Date: 2005/07/26 20:25:46 $
 */
public class Images {
	private static final boolean DEBUG = false && multivalent.Meta.DEVEL;

	private static BufferedImage jpeg2000 = null, jbig2 = null;

	private Images() {
	}

	/**
	 * Constructs new BufferedImage from dictionary attributes and data in
	 * stream. Applies <var>ctm</var>, adjusting image origin from PDF
	 * lower-left to Java upper-left.
	 * 
	 * @param imgdict
	 *            image XObject, or Dict with {@link #STREAM_DATA} key set for
	 *            inline images
	 */
	public static BufferedImage createImage(Dict imgdict, InputStream is,
			AffineTransform ctm, Color fillcolor, PDFReader pdfr)
			throws IOException {
		assert imgdict != null
				&& is != null
				&& ctm != null
				&& ("Image".equals(imgdict.get("Subtype")) || imgdict
						.get("Subtype") == null/* inline */);
		// if (ctm==null) ctm=new AffineTransform();

		// 1. decimate data as read? Not pretty but necessary, if separate image
		// creation and scaling.
		int w = pdfr.getObjInt(imgdict.get("Width")), h = pdfr
				.getObjInt(imgdict.get("Height"));
		int big = 1;
		double sx = Math.abs(ctm.getScaleX()) / w, sy = Math.abs(ctm
				.getScaleY())
				/ h;
		String filter = getFilter(imgdict, pdfr);
		if (!ctm.isIdentity() // no savings
				&& sx != 0.0 && sy != 0.0 // rotation not supported yet
				&& w * h/* (w*h*bpc*spd)/8 */> 16 * 1024 * 1024 // need it =>
				// Runtime.getRuntime().totalMemory()
				// / maxMemory()
				&& Math.abs(sx - sy) <= 0.01) { // maintaining aspect ratio
			for (int i = 32; i >= 2; i = i / 2) {
				// (1f / sx) /2 to nearest lower power of 2
				float f = 1f / (i * 2);
				if (sx <= f && sy <= f) {
					big = i;
					break;
				} // additional 2 for higher quality pixel averaging for scaling
			}
			// if (DEBUG && big > 1)
			// System.out.println("big over "+sx+"/"+sy+" = "+big+" ("+(big*big)+"X)");
		}

		// 2. create image
		BufferedImage img = null; // AffineImageOp requires BufferedImage, not
		// just java.awt.Image
		try {
			// System.out.println(imgdict.get("Name")+", filter="+filter+" "+w+"x"+h);
			// long start = System.currentTimeMillis();
			if ("DCTDecode".equals(filter))
				img = createJPEG(imgdict, is, pdfr);
			else if ("JPXDecode".equals(filter))
				img = createJPEG2000(imgdict, is);
			else if ("CCITTFaxDecode".equals(filter))
				img = createFAX(imgdict, is, big, fillcolor, pdfr);
			else if ("JBIG2Decode".equals(filter) /*
												 * ||
												 * "no abbreviation".equals(filter
												 * )
												 */)
				img = createJBIG2(imgdict, is, fillcolor, pdfr);
			else { /* assert filter==null: filter;--still FlateDecode */
				img = createRaw(imgdict, w, h, is, big, fillcolor, pdfr);
			} // raw samples, including most inline images
			// Object cs = pdfr.getObject(imgdict.get("ColorSpace"));
			// System.out.println(filter+","+imgdict.get("BitsPerComponent")+", "+(CLASS_ARRAY==cs.getClass()?
			// " / "+((Object[])cs)[0]: cs));
		} catch (Exception e) { // unchecked exceptions from MediaLib
			e.printStackTrace();
		}
		if (img == null || img == jpeg2000 || img == jbig2)
			return img; // unsupported (JBIG2, perhaps JPEG2000), IOException
		// long end = System.currentTimeMillis();
		// System.out.println("img = "+imgdict.get("Name")+", scale = "+ctm);
		// System.out.println("time = "+(end-start));
		// assert w==img.getWidth(): "width="+img.getWidth()+" vs param "+w; //
		// possible that parameters are wrong
		// X assert h==img.getHeight():
		// "height="+img.getHeight()+" vs param "+h; => if short of data, shrink
		// height
		if (ctm.isIdentity())
			return img;
		/* if (big>1) { */w = img.getWidth();
		h = img.getHeight(); // maybe decimated; trust actual image over PDF
		// metadata

		// 3. apply affine transform
		// "transformation from image space to user space could be described by the matrix [1/w 0 0 -1/h 0 1]."
		AffineTransform iat;
		if (ctm.getScaleX() != 0.0 && false && ctm.getScaleY() != 0.0) {
			Point2D srcpt = new Point2D.Double(w, h), transpt = new Point2D.Double();
			srcpt.setLocation(1.0, 1.0);
			ctm.deltaTransform(srcpt, transpt);
			System.out.println(srcpt + " => " + transpt + " in " + w + "X" + h
					+ " " + ctm);

			double xscale = ctm.getScaleX(), yscale = ctm.getScaleY();
			iat = new AffineTransform(xscale / w, ctm.getShearY(), ctm
					.getShearX(),
					-/* invert */(yscale / h + (yscale < 0.0 ? -1.0 : 1.0) / h), // "+1.0/h"
					// is
					// fuzz
					// so
					// don't
					// get
					// choppy
					// image
					// from
					// bad
					// PDF
					// distillation
					transpt.getX() >= 0.0 ? 0.0 : -transpt.getX(), transpt
							.getY() >= 0.0 ? 0.0 : -transpt.getY());

		} else if (ctm.getScaleX() != 0.0) { // Math.abs(ctm.getScaleX()) >
			// Math.abs(ctm.getShearX())
			double xscale = ctm.getScaleX(), yscale = ctm.getScaleY();
			// if (Math.abs(xscale*w)<1.0) xscale=(xscale<0.0? -1.0: 1.0); if
			// (Math.abs(yscale*h)<1.0) yscale=(yscale<0.0? -1.0: 1.0); // tiny
			// images at least 1x1 => always add one pixel, so 0+1=1 min
			// System.out.println("scaled "+(xscale*w)+"x"+(yscale*h));
			// double xscale=ctm.getScaleX()/w, yscale =
			// ctm.getScaleY()/h;//(h>2? ctm.getScaleY()/h:
			// 1.0);//ctm.getScaleY()<0? -1.0: 1.0); -- what, flip 1 pixel?
			// if (ctm.getShearX()!=0.0)
			// System.out.println("scale + shear "+ctm);
			iat = new AffineTransform(xscale / w, ctm.getShearY(), ctm
					.getShearX(),
					-/* invert */(yscale / h + (yscale < 0.0 ? -1.0 : 1.0) / h),
					0.0, 0.0); // "+1.0/h" is fuzz so don't get choppy image
			// from bad PDF distillation
			iat = new AffineTransform(xscale / w, 0.0, 0.0,
					-/* invert */(yscale + (yscale < 0.0 ? -1.0 : 1.0)) / h,
					0.0, 0.0); // Java bug with scale+shear on images of more
			// than small size, so ignore shear
			// double xround = 1.0/w /* 0.5*/, yround = -1.0/h /* 0.5*/; // copy
			// images
			// iat = new AffineTransform(xscale/w + xround, ctm.getShearX(),
			// ctm.getShearY(), /*-invert*/yscale/h + yround, 0.0, 0.0);
			if (iat.getScaleX() < 0.0)
				iat.translate(-w, 0); // it happens
			if (iat.getScaleY() < 0.0)
				iat.translate(0, -h); // in transformed space
			// System.out.println(ctm+": yscale="+yscale+" / height="+h+" => "+iat.getScaleY());

		} else { // 90 or -90 degree rotation
			// System.out.println(ctm);
			double xshear = ctm.getShearX(), yshear = ctm.getShearY();
			iat = new AffineTransform(0.0, yshear / w, -xshear / h, 0.0, 0.0,
					0.0);
			if (iat.getShearX() < 0.0)
				iat.translate(0, -h);
			if (iat.getShearY() < 0.0)
				iat.translate(-w, 0);
			// System.out.println("rotated image: "+w+"x"+h+"  "+ctm+" => "+iat);
			// //+", pt="+pt);
		}
		// if (DEBUG) System.out.println(w+"x"+h+", "+ctm+" => "+iat);

		// final image
		// Image imgout = img;
		// System.out.println(iat);
		// String filter = getFilter(imgdict, pdfr);
		// almost always have to transform, if only to invert
		// boolean isFAX = "CCITTFaxDecode".equals(getFilter(imgdict, pdfr));
		/*
		 * if (isFAX && iat.getScaleY()<0.0) iat = new
		 * AffineTransform(iat.getScaleX(), iat.getShearY(), iat.getShearX(),
		 * -iat.getScaleY(), 0.0, 0.0);
		 * 
		 * int aftype = iat.getType(); if (AffineTransform.TYPE_IDENTITY==aftype
		 * || AffineTransform.TYPE_TRANSLATION==aftype) { // -- never happens //
		 * nothing
		 * 
		 * } else if (iat.getScaleY()>0.0 &&
		 * (AffineTransform.TYPE_GENERAL_SCALE==aftype ||
		 * AffineTransform.TYPE_UNIFORM_SCALE==aftype)) { //
		 * Image.getScaledInstance not faster with SMOOTH, FAST stinks, AA slow
		 * and bad, DEFAULT fast but bad. And doesn't handle PDF's upside down
		 * images (can flip FAX). // faster to handle as drawImage()?
		 * (hardware?) => magnify lens not that much faster int
		 * neww=(int)Math.round(w * iat.getScaleX()), newh=(int)Math.round(h *
		 * iat.getScaleY()); imgout = img.getScaledInstance(neww, newh,
		 * Image.SCALE_DEFAULT);
		 * System.out.println("scale only: "+iat.getScaleX(
		 * )+" x "+iat.getScaleY()+", img="+img);
		 * 
		 * } else
		 */

		try {
			boolean fshrink = false && Math.abs(iat.getScaleX()) < 0.5
					&& Math.abs(iat.getScaleY()) < 0.5;
			// if (fshrink) System.out.println(getFilter(imgdict,
			// pdfr)+", "+iat+", small? "+fshrink);
			if (fshrink) {
				AffineTransformOp aop = new AffineTransformOp(iat,
						AffineTransformOp.TYPE_NEAREST_NEIGHBOR); // faster but
				// drops
				// text
				// within
				// image on
				// scale up
				// -- buggy:
				// Tekton
				// tomato
				// goes
				// black
				img = aop.filter(img, null);

			} else if ("CCITTFaxDecode".equals(getFilter(imgdict, pdfr))) { // custom
				// scaling
				// for
				// FAX
				img = Fax.scale(img, iat);
				// System.out.println("CCITT through Fax.Scale "+w+"x"+h+" * "+iat+" => "+img);

			} else if (Boolean.TRUE == pdfr.getObject(imgdict.get("ImageMask"))
					&& Color.BLACK.equals(fillcolor) && w * h < 5 * 1024) { // same
				// test
				// as
				// in
				// createRaw()
				img = Fax.scale(img, iat);

			} else {
				AffineTransformOp aop = new AffineTransformOp(iat,
						AffineTransformOp.TYPE_BILINEAR); // TYPE_NEAREST_NEIGHBOR
				// faster but drops
				// text within image
				// on scale up
				img = aop.filter(img, null);
			}

		} catch (/* java.awt.image.{RasterFormat,ImagingOp} */Exception ioe) {
			ioe.printStackTrace();
			// System.out.println(img); System.out.println(ctm+" => "+iat);
			// System.out.println(img.getColorModel());
			System.out.println(ioe);
			System.err.println(imgdict.get("Name") + " "
					+ getFilter(imgdict, pdfr) + " " + w + "X" + h + ", w/"
					+ ctm + " => " + iat);// +" => "+pt);
			if (PDF.DEBUG)
				System.exit(1);
			// img=null; -- return untransformed?
		}

		// assert img!=null: imgdict; // could be too small to show
		return img;
	}

	/** Process inline image into Node. */
	public static BufferedImage createInline(
	/* imgdict created here, */InputStreamComposite is, Dict csres,
			AffineTransform ctm, Color fillcolor, PDFReader pdfr)
			throws IOException {
		Dict iidict = pdfr.readInlineImage(is);

		InputStream iis = pdfr.getInputStream(iidict);
		Object csobj = iidict.get("ColorSpace");
		if (csres != null && csres.get(csobj) != null)
			iidict.put("ColorSpace", csres.get(csobj)); // key not literal
		BufferedImage img = createImage(iidict, iis, ctm, fillcolor, pdfr);
		iis.close();

		assert img != null : "bad INLINE IMG " + iidict; // +", len="+bout.size();
		// // no JBIG2 in
		// inline
		return img;
	}

	/**
	 * Return image part of filter, which may be in a cascade, or
	 * <code>null</code> if none. Expands abbreviations ("DCT" => "DCTDecode",
	 * "CCF" => "CCITTFaxDecode"). For example, from
	 * <code>[ASCII85Decode CCF]</code>, returns <code>CCITTFaxDecode</code>.
	 */
	public static String getFilter(Dict imgdict, COSSource coss)
			throws IOException {
		Object attr = imgdict.get("Filter");
		if (coss != null)/* inline */
			attr = coss.getObject(attr);
		String f;
		if (attr == null || OBJECT_NULL == attr)
			f = null; // raw samples, uncompressed -- found in inline images
		else if (CLASS_NAME == attr.getClass())
			f = (String) attr;
		else {
			assert CLASS_ARRAY == attr.getClass(); // usually image filter
			// wrapped in ASCII. no JPG
			// wrapping FAX I hope!
			Object[] oa = (Object[]) attr;
			Object o = oa.length > 0 ? oa[oa.length - 1] : null; // image filter
			// must be
			// last
			f = (String) (coss != null ? coss.getObject(o) : o);
		}

		if ("DCT".equals(f))
			f = "DCTDecode";
		else if ("CCF".equals(f))
			f = "CCITTFaxDecode"; // canonicalize
		if (!"DCTDecode".equals(f) && !"CCITTFaxDecode".equals(f)
				&& !"JBIG2Decode".equals(f) && !"JPXDecode".equals(f))
			f = null; // make sure it's image

		return f;
	}

	public static double[] getDecode(Dict imgdict, COSSource coss)
			throws IOException {
		double[] da = null;

		Object o = coss.getObject(imgdict.get("Decode"));
		if (o != null && CLASS_ARRAY == o.getClass()) {
			Object[] oa = (Object[]) o;
			da = new double[oa.length];
			for (int i = 0, imax = oa.length; i < imax; i++)
				da[i] = ((Number) oa[i]).doubleValue();
		}
		return da;
	}

	/**
	 * Returns image's /DecodeParms, or <code>null</code> if none (or
	 * {@link COS#OBJECT_NULL}). If /DecodeParms is an array, the one
	 * corresponding to the image is always the last array element.
	 */
	public static Dict getDecodeParms(Dict imgdict, PDFReader pdfr)
			throws IOException {
		Object o = pdfr.getObject(imgdict.get("DecodeParms"));
		Object dp = o == null || OBJECT_NULL == o ? OBJECT_NULL
				: CLASS_DICTIONARY == o.getClass() ? o
						:
						/* assert CLASS_ARRAY==o.getClass()? */pdfr
								.getObject(((Object[]) o)[((Object[]) o).length - 1/*
																					 * better
																					 * be
																					 * last
																					 */]); // 07001
		// has
		// /DecodeParms
		// [null
		// null]

		return dp != OBJECT_NULL ? (Dict) dp : null;
	}

	/**
	 * Returns file type suffix corresponding to PDF <var>filter</var>, e.g.,
	 * PDF <code>DCTDecode</code> returns <code>jpg</code>.
	 */
	public static String getSuffix(String filter) {
		return "DCTDecode".equals(filter) ? "jpg"
				: "JPXDecode".equals(filter) ? "jp2" : "CCITTFaxDecode"
						.equals(filter) ? "fax"
						: "JBIG2Decode".equals(filter) ? "jbig2" : "raw";
	}

	/** Hand off to ImageIO. */
	static BufferedImage createJPEG(Dict imgdict, InputStream is, PDFReader pdfr)
			throws IOException {
		assert imgdict != null && is != null;

		// If 4-color, have to distinguish between YCCK and CMYK.
		// Don't want to rely on sun.* class, so parse JPEG, which means we need
		// two copies of data: one for IIO to consume and another for Adobe
		// APP14.
		ColorSpace cs = pdfr.getColorSpace(imgdict.get("ColorSpace"), null,
				null);
		int jcs = JPEG.CS_YCbCr3;
		int nComp = cs.getNumComponents();
		if (nComp == 4 /* || true */) {
			Object o = imgdict.get("Length");
			byte[] data = InputStreams.toByteArray(is, o != null ? pdfr
					.getObjInt(o) : 10 * 1024);
			jcs = JPEG.getTransform(data);
			is = new ByteArrayInputStream(data);
		}

		BufferedImage img;
		// img = ImageIO.read(is); -- easiest, but already know it's JPEG
		ImageReader iir = (ImageReader) ImageIO.getImageReadersByFormatName(
				"JPEG").next();
		ImageIO.setUseCache(false);
		ImageInputStream iis = ImageIO.createImageInputStream(is);
		iir.setInput(iis, true); // new MemoryCacheImageInputStream(is));
		try {
			// Object o = pdfr.getObject(imgdict.get("ColorSpace")), o2 =
			// CLASS_ARRAY==o.getClass()? ((Object[])o)[0]: o;
			// System.out.println("colorspace: "+o+"/"+o2+" => "+cs);
			// if (DEBUG)
			// System.out.println("JPEG, colorspace: "+cs+" vs RGB "+ColorSpace.getInstance(ColorSpace.CS_sRGB)+", jcs="+jcs+", nComp="+nComp);
			if (nComp == 4 || cs instanceof ColorSpaceLab/*
														 * jcs==JPEG.CS_CMYK/*cs
														 * != RGB /*||true/*&&
														 * jcs!=JPEG.CS_YCbCr3
														 */) {
				ImageReadParam irp = iir.getDefaultReadParam();
				// irp.setSourceRenderSize(new Dimension(1000,1000)); => can't
				// for JPEG
				// irp.setSourceSubsampling(2,2, 0,0); => not what we want
				cs = JPEG.CS_CMYK == jcs ? ColorSpaceCMYK.getInstance()
						: JPEG.CS_YCCK == jcs ? ColorSpaceYCCK.getInstance()
								: cs;
				Raster r = iir.readRaster(0, irp/* null */); // Fact that
				// ImageIO can
				// return raster
				// only pointed
				// out by MArk
				// Stephens.
				r = Rasters.toRGB(r, cs); // don't store ref so can gc?
				ColorModel cm = new ComponentColorModel(ColorSpace
						.getInstance(ColorSpace.CS_sRGB), false, true,
						Transparency.OPAQUE, DataBuffer.TYPE_BYTE);
				img = new BufferedImage(cm, (WritableRaster) r, true, null);
				// ColorConvertOp cco = new
				// ColorConvertOp(ColorSpace.getInstance(ColorSpace.CS_LINEAR_RGB),
				// ColorSpace.getInstance(ColorSpace.CS_LINEAR_RGB), null);
				// img = cco.filter(img, null); => dog slow!

			} else { // YCbCr, grayscale, RGB
				img = iir.read(0, null);
			}

		} catch (IOException e) {
			img = null;
			System.err.println("Couldn't read JPEG: " + e);
		}

		iir.dispose();
		iis.close();

		return img;
	}

	/** Hand off to ImageIO. */
	static BufferedImage createJPEG2000(Dict imgdict, InputStream is)
			throws IOException {
		assert /* imgdict!=null && */is != null;

		// img = ImageIO.read(is); -- easiest, but already know it's JPEG
		if (jpeg2000 != null)
			return jpeg2000;

		Iterator<ImageReader> i = ImageIO
				.getImageReadersByFormatName("JPEG2000");
		BufferedImage img;
		if (i.hasNext()) {
			ImageReader iir = i.next();
			ImageIO.setUseCache(false);
			ImageInputStream iis = ImageIO.createImageInputStream(is);
			iir.setInput(iis, true); // new MemoryCacheImageInputStream(is));
			try {
				img = iir.read(0);
			} catch (IOException e) {
				img = null;
			}
			iir.dispose();
			iis.close();
		} else {
			System.err
					.println("No decoder for JPEG2000 -- install ImageIO from Sun or Apple");
			int w = 20, h = 20;
			img = jpeg2000 = new BufferedImage(w, h,
					BufferedImage.TYPE_INT_ARGB);
			// color -- like blue sky and green grass
			Graphics g = jpeg2000.getGraphics();
			g.setColor(Color.BLUE);
			g.fillRect(0, 0, w, h / 2);
			g.setColor(Color.GREEN);
			g.fillRect(0, h / 2, w, h / 2);
			g.dispose();
		}

		return img;
	}

	/**
	 * Decode parameters from PDF dictionary and pass on to
	 * {@link com.pt.imageio.plugins.Fax}.
	 */
	static BufferedImage createFAX(Dict imgdict, InputStream is, int big,
			Color fillcolor, PDFReader pdfr) throws IOException {
		assert imgdict != null && is != null/* && pdfr!=null -- testing */;

		// System.out.println(imgdict.get("Name")+", K="+K+", cols="+cols+", rows="+rows+", swap="+swapbw);
		Dict dp = getDecodeParms(imgdict, pdfr);
		if (dp == null)
			dp = new Dict(3); // no /DP very rare, at least have /K -1

		Object o;
		int K = (o = dp.get("K")) != null ? pdfr.getObjInt(o) : 0;
		int cols = (o = dp.get("Columns")) != null ? pdfr.getObjInt(o) : 1728;
		assert cols >= 1 /* && cols%8==0 -- not root_cz.pdf by Ghostscript 7.05 */: cols; // rounds
		// up
		// to
		// nearest
		// byte
		// boundary
		int width = pdfr.getObjInt(imgdict.get("Width")); // can be less than
		// /Columns
		if (cols != width) {
			int mod = width % 8;
			if (mod != 0)
				width += (8 - mod);
		} // round up to multiple of 8, as needed by pdfdb/000208.pdf
		int height = pdfr.getObjInt(imgdict.get("Height")); // Estimate rows by
		// height, not
		// cols*1.5 as can
		// get "FAX strips"
		// of 2500 width but
		// only 107 height,
		// so hugely
		// overestimate and
		// provoke flood of
		// garbage
		// collections!
		int rows = (o = dp.get("Rows")) != null ? pdfr.getObjInt(o) : -height/*
																			 * just
																			 * go
																			 * until
																			 * hit
																			 * EOF
																			 * --
																			 * estimate
																			 * for
																			 * performance
																			 */; // assert
		// rows>=1;
		// //if
		// (rows<=0)
		// rows=Integer.MAX_VALUE;

		boolean EndOfLine = Boolean.TRUE == pdfr.getObject(dp.get("EndOfLine")); // defaults
		// to
		// false
		// and
		// only
		// Boolean.TRUE
		// makes
		// it
		// true
		boolean EndOfBlock = Boolean.FALSE != pdfr.getObject(dp
				.get("EndOfBlock")); // defaults to true and only Boolean.FALSE
		// makes it false
		boolean EncodedByteAlign = Boolean.TRUE == pdfr.getObject(dp
				.get("EncodedByteAlign"));
		// if (EncodedByteAlign)
		// multivalent.Meta.sampledata("FAX /EncodedByteAlign");
		boolean BlackIs1 = Boolean.TRUE == pdfr.getObject(dp.get("BlackIs1"));
		int DamagedRowsBeforeError = (o = dp.get("DamagedRowsBeforeError")) != null ? pdfr
				.getObjInt(o)
				: 0;

		// o=pdfr.getObject(imgdict.get("Decode")); => handled in scale()'s
		// color map
		double[] da = getDecode(imgdict, pdfr);
		boolean swapbw = da != null && da[0] == 1.0 && da[1] == 0.0;
		Object csobj = pdfr.getObject(imgdict.get("ColorSpace")); // usually
		// DeviceGray
		if (csobj != null
				&& CLASS_ARRAY == csobj.getClass()
				&& ("Indexed".equals(((Object[]) csobj)[0]) || "I"
						.equals(((Object[]) csobj)[0]))) {
			IndexColorModel icm = ColorSpaces.createIndexColorModel(csobj, 1,
					pdfr);
			if (icm.getRGB(0) != Color.BLACK.getRGB()
					&& icm.getRGB(1) == Color.BLACK.getRGB())
				swapbw = true; // = !swapbw?
			// System.out.println(imgdict+" => "+Integer.toHexString(icm.getRGB(0))+" "+Integer.toHexString(icm.getRGB(1))+" => "+swapbw);
		}

		final byte white = (byte) (BlackIs1 ^ swapbw ? 1 : 0); // /Decode [1 0]
		// different
		// than
		// BlackIs1.
		// INVERTED for
		// performance:
		// set to 0 as
		// Java clears
		// page to 0 and
		// most of page
		// is white, so
		// save
		// rewriting;
		// then swap in
		// indexed color
		// map.

		// System.out.println("FAX  "+imgdict);
		BufferedImage img = Fax
				.decode(K, width, cols, rows, big, EndOfLine, EndOfBlock,
						EncodedByteAlign, white, DamagedRowsBeforeError, is);

		// if image mask, replace color map
		if (Boolean.TRUE == pdfr.getObject(imgdict.get("ImageMask"))
				&& fillcolor.getRed() > 8 && fillcolor.getBlue() > 8
				&& fillcolor.getBlue() > 8 // seen r=0,g=0,b=1. already black
		// and white
		) {
			// System.out.println("image mask "+fillcolor);// imgdict);
			ColorModel cm = new IndexColorModel(8, 2, new int[] { 0,
					fillcolor.getRGB() }, 0, true, 0/* trans */,
					DataBuffer.TYPE_BYTE);
			img = new BufferedImage(cm, img.getRaster(), false,
					new java.util.Hashtable());
		}

		return img;
	}

	/**
	 * IMPELENTED as nestedvm process.
	 * 
	 * @param pdfr
	 * @param fillcolor
	 */

	static BufferedImage createJBIG2(Dict imgdict, InputStream is,
			Color fillcolor, PDFReader pdfr) throws IOException {

		byte[] header = new byte[] { (byte) 0x97, 0x4a, 0x42, 0x32, 0x0d, 0x0a,
				0x1a, 0x0a };// , 0x01, 0x00, 0x00, 0x00, 0x01 };
		byte[] single = new byte[] { 0x01, 0x00, 0x00, 0x00, 0x01 };
		byte[] eop = new byte[] { 0x00, 0x00, 0x00, 0x03, 0x31, 0x00, 0x01,
				0x00, 0x00, 0x00, 0x00 };
		byte[] eof = new byte[] { 0x00, 0x00, 0x00, 0x14, 0x33, 0x00, 0x00,
				0x00, 0x00, 0x00 };
		BufferedImage img;
		if (jbig2 != null)
			return jbig2;
		Dict dp = getDecodeParms(imgdict, pdfr);
		ImageIO.setUseCache(false);
//		boolean useNative = false;
		if (dp == null) {

			try {
//				if (useNative) {
//					System.out.println("native imageio");
//					File in = File.createTempFile("multv", ".jbig2");
//					in.deleteOnExit();
//					FileOutputStream os = new FileOutputStream(in);
//					os.write(header);
//					os.write(single);
//					Fab4utils.copyInputStream(is, os);
//					os.write(eop);
//					os.write(eof);
//					is.close();
//					os.close();
//
//					JBIG2Decoder dec = new JBIG2Decoder();
//					dec.decodeJBIG2(in);
//					img = dec.getPageAsBufferedImage(0);
//					System.out.println(img);
//					return img;
//				} else {
					File out;
					System.out.println("decoding single page JBIG2");
					out = File.createTempFile("multv", ".pbm");
					out.deleteOnExit();
					File in = File.createTempFile("multv", ".jbig2");
					in.deleteOnExit();
					FileOutputStream os = new FileOutputStream(in);
					os.write(header);
					os.write(single);
					Fab4utils.copyInputStream(is, os);
					os.write(eop);
					os.write(eof);
					is.close();
					os.close();

					Jbig2dec me = new Jbig2dec();
					String[] args = new String[3];
					args[0] = "-o";
					args[1] = out.getAbsolutePath();
					args[2] = in.getAbsolutePath();
					System.out.println(args[1]);
					int status = me.run("Jbig2dec", args);
					System.out.println(status);
					// ImageReader iir = i.next();
					PPMImageReader r = new PPMImageReader();
					img = r.read(ImageIO.createImageInputStream(new File(
							args[1])));
					System.out.println("aaa " + img);
					return img;
//				}
			} catch (Exception e) {
				System.out.println(e);
			}
		} else {
			Object o = dp.get("JBIG2Globals");
			InputStream isg = pdfr.getInputStream(o);
			try {
//				if (useNative) {
//					System.out.println("native imageio");
//					File in = File.createTempFile("multv", ".jbig2");
//					in.deleteOnExit();
//					FileOutputStream os = new FileOutputStream(in);
//					//os.write(header);
//					//os.write(single);
//					Fab4utils.copyInputStream(is, os);
//					//os.write(eop);
//					//os.write(eof);
//					is.close();
//					os.close();
//
//					JBIG2Decoder dec = new JBIG2Decoder();
//					dec.setGlobalData(InputStreams.toByteArray(isg));
//					dec.decodeJBIG2(in);
//					img = dec.getPageAsBufferedImage(0);
//					System.out.println(img);
//					return img;
//				} else {
					File out;
					System.out.println("decoding Multi-page JBIG2");
					out = File.createTempFile("multv", ".pbm");
					out.deleteOnExit();
					File in = File.createTempFile("multv", ".jpage");
					in.deleteOnExit();

					File in2 = Fab4utils.copyToTemp(isg, "multv", ".jglob");
					in2.deleteOnExit();
					FileOutputStream os = new FileOutputStream(in);

					Fab4utils.copyInputStream(is, os);

					is.close();
					os.close();

					String[] args = new String[4];
					args[0] = "-o";
					args[1] = out.getAbsolutePath();
					args[2] = in2.getAbsolutePath();
					args[3] = in.getAbsolutePath();

					System.out.println(args[1]);
					// Interpreter inter = new Interpreter(new
					// FileInputStream("/Users/fabio/Desktop/jbig2dec"));
					// int status=inter.run("Jbig2dec", args);
					Jbig2dec me = new Jbig2dec();
					int status = me.run("Jbig2dec", args);
					System.out.println(status);
					// ImageReader iir = i.next();

					;
					PPMImageReader r = new PPMImageReader();
					img = r.read(ImageIO.createImageInputStream(new File(
							args[1])));
					System.out.println("aaa " + img);
					return img;
//				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return null;

	}

	/**
	 * Create image from raw samples, in various bit depths (8, 4, 2, 1), in a
	 * variety of color spaces, with various numbers of samples per component
	 * (4,3,1).
	 */
	static BufferedImage createRaw(Dict imgdict, int w, int h, InputStream is,
			int big, Color fillcolor, PDFReader pdfr) throws IOException {
		assert imgdict != null && w > 0 && h > 0 && is != null
				&& fillcolor != null && pdfr != null;

		// 1. colorspace
		Boolean fmask = (Boolean) pdfr.getObject(pdfr.getObject(imgdict
				.get("ImageMask")));
		int bpc = fmask == Boolean.TRUE ? 1 : pdfr.getObjInt(imgdict
				.get("BitsPerComponent"));
		ColorModel cm = createRawColorModel(pdfr.getObject(imgdict
				.get("ColorSpace")), fmask, bpc, fillcolor, pdfr);
		// System.out.println("colorspace = "+imgdict.get("ColorSpace")+" => "+cm+" "+w+"x"+h);
		int spd = cm instanceof IndexColorModel ? 1 : cm.getNumComponents(); // 1
		// for
		// gray
		// or
		// indexed
		// (regardless
		// of
		// cm.getNumComponents()),
		// 3
		// for
		// rgb,
		// 4
		// for
		// CMYK

		// 2. data, tweaked for /Decode and CMYK=>RGB
		// check that have enough data
		// if don't, reduce height. Ghostscript 5.50 reports one line too high,
		// as on netzwerke.pdf
		int scanline = (w * spd * bpc + 7) / 8;

		// System.out.println("RAW: "+w+"x"+h+" @ "+bpc+" = "+(w*h*spd)+", spd="+spd+", "+cm+", "+cm.getNumComponents());
		// int est = (w*h * spd*bpc + (bpc-1))/8;
		byte[] rawdata;
		if (big > 1 && bpc == 8) {
			int bigwidth = w / big, bigheight = h / big;
			rawdata = new byte[bigwidth * bigheight * spd];
			// System.out.println("decimate "+(w*h*spd)+" => "+rawdata.length+", "+w+"=>"+bigwidth+" X "+h+"=>"+bigheight+", spd="+spd);
			byte[] line = new byte[scanline];
			for (int i = 0, ctr = 0, inc = big * spd; i < bigheight; i++) {
				InputStreams.readFully(is, line);
				for (int j = 0; j + inc <= scanline/* full chunk */; j += inc)
					for (int k = 0; k < spd; k++, ctr++)
						rawdata[ctr + k] = line[j + k];
				InputStreams.skipFully(is, scanline * (big - 1));
			}
			w = bigwidth;
			h = bigheight;
			scanline = bigwidth * spd;
		} else
			rawdata = InputStreams.toByteArray(is, w * h * spd);
		// System.out.println(est+" vs "+rawdata.length+" actual @ "+bpc);

		int reqlen = scanline * h;
		if (rawdata.length < reqlen) { // show data that do have
			if (DEBUG) {
				System.out.println("short data: " + rawdata.length + " < "
						+ reqlen + ": " + w + "x" + h + " * " + spd + " @ "
						+ bpc + " bpp");
				System.out.println(imgdict);
			}
			// byte[] req = new byte[reqlen]; System.arraycopy(rawdata,0, req,0,
			// rawdata.length); rawdata = req;
			h = rawdata.length / scanline;
		}

		// decode matrix
		double[] da = getDecode(imgdict, pdfr);
		if (da != null) {
			// LATER: if IndexColorModel, twiddle map instead
			boolean fid = true, finv = true;
			for (int i = 0, imax = da.length; i < imax; i += 2) {
				if (da[i] != 0.0 || da[i + 1] != 1.0)
					fid = false;
				if (da[i] != 1.0 || da[i + 1] != 0.0)
					finv = false;
			}
			// System.out.println("invert");
			if (finv)
				for (int i = 0, imax = rawdata.length; i < imax; i++)
					rawdata[i] ^= 0xff;
			else if (!fid)
				multivalent.Meta.unsupported("Decode matrix: "
						+ java.util.Arrays.asList((Object[]) pdfr
								.getObject(imgdict.get("Decode"))) + ", len="
						+ da.length);
		}

		/*
		 * if (bpc==8 && cm.getColorSpace() instanceof ICC_ColorSpace) { int[]
		 * hist = new int[256]; for (int i=0,imax=rawdata.length; i<imax; i++)
		 * hist[rawdata[i]&0xff]++; for (int i=0,imax=hist.length; i<imax; i++)
		 * if (hist[i]>0) System.out.print(i+"="+hist[i]+" "); }
		 */

		// Work around Java AffineTransformOp limitation on 4-component color
		// spaces by transcoding data to RGB.
		// Just handles 8-bit CMYK case now, which is all that I've seen.
		if (bpc == 8 && cm.getColorSpace() instanceof ColorSpaceCMYK) {
			byte[] newdata = new byte[w * h * 3];
			for (int i = 0, imax = Math.min(w * h * 4, rawdata.length), j = 0; i < imax; i += 4, j += 3) { // sometimes
				// extra
				// data
				int k = rawdata[i + 3] & 0xff;
				newdata[j] = (byte) (255 - Math.min(255, (rawdata[i] & 0xff)
						+ k));
				newdata[j + 1] = (byte) (255 - Math.min(255,
						(rawdata[i + 1] & 0xff) + k));
				newdata[j + 2] = (byte) (255 - Math.min(255,
						(rawdata[i + 2] & 0xff) + k));
			}
			rawdata = newdata;
			cm = new ComponentColorModel(ColorSpace
					.getInstance(ColorSpace.CS_sRGB), false, true,
					Transparency.OPAQUE, DataBuffer.TYPE_BYTE);
			spd = 3;
		}

		// special case for Type 3: convert data to Fax for good looking
		// scaling: 8x larger, but worth it
		WritableRaster r;
		if (Boolean.TRUE == fmask && Color.BLACK.equals(fillcolor)
				&& w * h < 5 * 1024 /* && fType3 */) {
			assert spd == 1 : spd;
			// System.out.println("converting black ImageMask for Fax scaling: "+w+"x"+h+", bpc="+bpc);
			// byte[] newdata = new byte[(w+1)*(h+1)]; // Fax scaling requires
			// extra row and column
			byte[] newdata = new byte[w * h];
			for (int y = 0, i = 0, base = 0; y < h; y++, base += w/* +1 */) {
				int valid = 0, bits = 0;
				for (int x = 0; x < w; x++) {
					if (valid <= 0) {
						bits = rawdata[i++] & 0xff;
						valid = 8;
					}
					newdata[base + x] = (bits & 0x80) == 0 ? (byte) 1 : 0;
					bits <<= 1;
					valid--;
				}
			}

			rawdata = newdata;
			bpc = 8;
			fmask = Boolean.FALSE;
			// cm = new
			// ComponentColorModel(ColorSpace.getInstance(ColorSpace.CS_GRAY),
			// false, true, Transparency.OPAQUE, DataBuffer.TYPE_BYTE);
			cm = Fax.CM_BLACKWHITE;
			r = Raster.createInterleavedRaster(new DataBufferByte(rawdata,
					rawdata.length), w, h, w/* +1/*stride */, 1,
					new int[] { 0 }, null);

		} else
			r = createRawRaster(rawdata, w, h, bpc, spd);
		// WritableRaster r = createRawRaster(rawdata, w, h, bpc, spd);

		// 3. colorspace + raster = image
		// create image with color model so colors survive affine transform
		BufferedImage img = null;
		try {
			img = new BufferedImage(cm, r, false, new java.util.Hashtable());
		} catch (Exception e) {
			// logger.severe("...");
			// e.printStackTrace();
			System.err.println(e);

			System.out.println("color model = " + cm);
			SampleModel sm = r.getSampleModel();
			System.out.println("sample model = " + sm);
			/*
			 * if (sm instanceof ComponentSampleModel) { if (sm.getNumBands() !=
			 * getNumComponents()) { return false; } for (int i=0;
			 * i<nBits.length; i++) { if (sm.getSampleSize(i) < nBits[i]) {
			 * return false; } } return (raster.getTransferType() ==
			 * transferType); }
			 */
			// from IndexedColorModel.java
			// int size = r.getSampleModel().getSampleSize(0);
			// System.out.println(r.getTransferType()+" ==? "+cm.getTransferType());
			// System.out.println(r.getNumBands()+" ==? 1");
			// System.out.println((1 <<
			// size)+" >=? "+((IndexColorModel)cm).getMapSize());
			// if (DEBUG) System.exit(1);
			System.out.println("sample model instance of ComponentSampleModel "
					+ (sm instanceof ComponentSampleModel));
			System.out.println("num bands = " + sm.getNumBands() + " ==? "
					+ cm.getNumComponents() + " cm num comp");
			int[] nbits = cm.getComponentSize();
			for (int i = 0; i < nbits.length; i++)
				System.out.println("  " + sm.getSampleSize(i) + " >=? "
						+ nbits[i]);
			System.out.println(r.getTransferType() + " ==? "
					+ sm.getTransferType());
		}

		return img;
	}

	private static ColorModel createRawColorModel(Object csref, Boolean fmask,
			int bpc, Color fillcolor, PDFReader pdfr) throws IOException {
		// assert csref!=null; // => image mask
		Object csobj = pdfr.getObject(csref);
		// ColorSpace cs = (csobj!=null/*error, except for image masks*/?
		// getColorSpace(csref): ColorSpace.getInstance(ColorSpace.CS_sRGB));
		ColorSpace cs = pdfr.getColorSpace(csref, null, null);
		assert cs != null || Boolean.TRUE == fmask;

		ColorModel cm;

		// 1: INDEX or GRAY
		if (Boolean.TRUE == fmask) { // as abused in riggs.pdf
			assert bpc == 1;
			// System.out.println("fillcolor = "+fillcolor+"/"+Integer.toHexString(fillcolor.getRGB())+", decode="+decode+", "+w+"x"+h);
			// "unmasked areas will be painted using the current nonstroking color"
			cm = new IndexColorModel(1, 2, new int[] { fillcolor.getRGB(), 0 },
					0, /* false--grr */true, 1, DataBuffer.TYPE_BYTE);
			// cm = new IndexColorModel(1, 2, new int[] { Color.BLUE.getRGB(),
			// Color.GREEN.getRGB() }, 0, false, -1, DataBuffer.TYPE_BYTE);

		} else if (CLASS_ARRAY == csobj.getClass()
				&& ("Indexed".equals(((Object[]) csobj)[0]) || "I"
						.equals(((Object[]) csobj)[0]))) { // indexed special
			// case
			cm = ColorSpaces.createIndexColorModel(csobj, bpc, pdfr);
			// Index works with both Interleaved and Packed Rasters
			// shanghai one color for transparency in case of /Mask

		} else if (cs.getNumComponents() == 1 && bpc < 8) { // grayscale
			// (/Indexed handled
			// above)
			// Java has to have IndexColorModel for Packed Rasters so translate
			// grayscale
			if (bpc == 1)
				cm = new IndexColorModel(1, 2,
						new int[] { 0x000000, 0xffffff }, 0, false, -1,
						DataBuffer.TYPE_BYTE);
			else if (bpc == 2)
				cm = new IndexColorModel(2, 4, new int[] { 0x000000, 0x404040,
						0xc0c0c0, 0xffffff }, 0, false, -1,
						DataBuffer.TYPE_BYTE);
			else
				/* assert bpc==4 */cm = new IndexColorModel(4, 16, new int[] {
						0x000000, 0x111111, 0x222222, 0x333333, 0x444444,
						0x555555, 0x666666, 0x777777, 0x888888, 0x999999,
						0xaaaaaa, 0xbbbbbb, 0xcccccc, 0xdddddd, 0xeeeeee,
						0xffffff }, 0, false, -1, DataBuffer.TYPE_BYTE);

			// 3/4: RGB, CMYK
		} else if (bpc == 8 || bpc == 4) { // 4-bit case split out in
			// readRawData to separate bytes
			cm = new ComponentColorModel(cs, false, true, Transparency.OPAQUE,
					DataBuffer.TYPE_BYTE); // "each sample in a separate data element",
			// that is, interleaved raster

		} else { // fabricate an IndexColorModel so can pack all samples into
			// single data element, 2^(2*4) = 256, but usually
			// 2^(2*3)=64
			// ColorSpace cs = (csobj!=null/*error, except for image masks*/?
			// getColorSpace(csref):
			// ColorSpace.getInstance(ColorSpace.CS_sRGB));
			int spd = cs.getNumComponents();
			assert (spd == 3 || spd == 4) && (bpc == 2 || bpc == 1) : "bpc="
					+ bpc;
			// System.out.println("imgdict = "+imgdict+", colorspace="+csobj+", spd="+spd);

			// color, 3 or 4 components -- compute entries vis-a-vis base
			// colorspace
			int po2 = (1 << bpc);
			byte[] b = new byte[(1 << (bpc * spd)) * spd];
			// System.out.println("bpc="+bpc+", spd="+spd+", b.length="+b.length);
			float red = 0f, green = 0f, blue = 0f, black = 0f, inc = (float) (1f / po2);
			float[] comp = new float[4]; // enough for CMYK
			Color c;
			int i = 0;
			for (int r = 0; r < po2; r++, red += inc, green = 0f, blue = 0f, black = 0f) {
				comp[0] = red;
				for (int g = 0; g < po2; g++, green += inc, blue = 0f, black = 0f) {
					comp[1] = green;
					for (int bl = 0; bl < po2; bl++, blue += inc, black = 0f) {
						comp[2] = blue;
						for (int k = 0; k < po2; k++, black += inc) {
							comp[3] = black;
							c = new Color(cs, comp, 0f);
							b[i++] = (byte) c.getRed();
							b[i++] = (byte) c.getGreen();
							b[i++] = (byte) c.getBlue();
							if (spd == 3)
								break;
						}
					}
				}
			}
			assert i == b.length : i + " vs " + b.length;

			cm = new IndexColorModel(bpc, b.length / spd, b, 0, false);
		}

		return cm;
	}

	private static WritableRaster createRawRaster(byte[] rawdata, int w, int h,
			int bpc, int spd) {
		WritableRaster r;

		int[] offs = new int[spd];
		for (int i = 0; i < spd; i++)
			offs[i] = i;

		// System.out.println("bpc="+bpc+", spd="+spd+", w="+w+", h="+h);
		// if (bpc<8)
		// System.out.println("w="+w+", h="+h+", bpc="+bpc+", spd="+spd+", data buf len = "+rawdata.length+" vs w*h*spd="+(w*h*spd*bpc/8+w*spd));
		// provided SampleModels either force all samples in same element
		// (packed), or sample-per-element (interleaved)
		if (bpc == 8 || (bpc == 2 && spd == 4)) {
			// if (spd==1) PackedRaster; else
			// System.out.println("data len ="+rawdata.length+" vs needed "+(w*h*spd)+"  ("+(rawdata.length/(w*spd))+" rows)");
			r = Raster.createInterleavedRaster(new DataBufferByte(rawdata,
					rawdata.length), w, h, w * spd, spd, offs, null);

		} else if (spd == 1) {
			assert bpc == 4 || bpc == 2 || bpc == 1; // indexed or grayscale:
			// samples fit in a byte
			// System.out.println("spd=1, bpc="+bpc+", rawdata.length="+rawdata.length+", mask="+Integer.toBinaryString((1<<bpc)-1)+", shift="+(8-bpc));//+", bands="+r.getNumBands());
			r = Raster.createPackedRaster(new DataBufferByte(rawdata,
					rawdata.length), w, h, bpc, null);
			// System.out.print("spd==1: "); for (int i=0, imax=Math.min(10,
			// rawdata.length); i<imax; i++)
			// System.out.print(Integer.toHexString(rawdata[i])+" ");
			// System.out.println("   @ "+ctm.getTranslateX()+","+ctm.getTranslateY()+", scale="+ctm.getScaleX()+"x"+ctm.getScaleY());
			/*
			 * if (!(cm instanceof IndexColorModel)) { // Java has to have
			 * IndexColorModel for this Raster class and got here from grayscale
			 * cm = (bpc==1? GRAY1: bpc==2? GRAY2: /*bpc==4* / GRAY4); }
			 */

		} else if (bpc == 4 || (bpc == 1 && spd == 4)) { // split two pairs of
			// 4-bit data per
			// byte into single
			// 4-bits of data
			// per byte. Doubles
			// size for RGB or
			// CMYK samples,
			// which is
			// acceptable
			// expansion
			int scanline = w * spd;
			boolean fodd = (scanline & 1) == 1;
			byte[] newdata = new byte[scanline * h + (fodd ? 1 : 0)];
			// System.out.println("4 bpc x 2 bytes: "+w+"x"+h+", len="+rawdata.length+" => "+newdata.length+", spd="+spd+", offs="+offs);
			for (int y = 0, i = 0, j = 0; y < h; y++) {
				for (int x = 0; x < scanline; x += 2) {
					byte b = rawdata[i++];
					// decode array...
					newdata[j++] = (byte) (b & 0xf0);
					newdata[j++] = (byte) ((b << 4) & 0xf0);
				}
				if (fodd)
					j--;
			}
			r = Raster.createInterleavedRaster(new DataBufferByte(newdata,
					newdata.length), w, h, scanline, spd, offs, null);

		} else if (bpc == 2) { // spd==3: samples smeared across data elements
			byte[] newdata = new byte[w * h];
			int valid = 0, vbpc = 0;
			for (int y = 0, newi = 0, base = 0, stride = (w * spd * bpc + 7) / 8; y < h; y++, base = y
					* stride) {
				for (int x = 0; x < w; x++) { // slow, but unusual
					if (valid < 6) {
						vbpc = (vbpc << 8) | rawdata[base++];
						valid += 8;
					}
					newdata[newi++] = (byte) ((vbpc >> (valid - 6)) & 0x3f);
					valid -= 6;
				}
			}
			// r = Raster.createPackedRaster(new DataBufferByte(newdata,
			// newdata.length), w,h, w, new int[] { 0x30, 0x0c, 0x03 }, null);
			r = Raster.createInterleavedRaster(new DataBufferByte(newdata,
					newdata.length), w, h, w * spd, spd, new int[] { 0 }, null);
			multivalent.Meta.sampledata("2 bpc packed BYTE: " + rawdata.length
					+ " => " + newdata.length);

		} else {
			assert bpc == 1;
			byte[] newdata = new byte[w * h];
			int valid = 0, vbpc = 0;
			for (int y = 0, newi = 0, base = 0, stride = (w * spd * bpc + 7) / 8; y < h; y++, base = y
					* stride) {
				for (int x = 0; x < w; x++) { // slow, but unusual
					if (valid < 3) {
						vbpc = (vbpc << 8) | rawdata[base++];
						valid += 8;
					}
					newdata[newi++] = (byte) ((vbpc >> (valid - 3)) & 7);
					valid -= 3;
				}
			}
			// r = Raster.createPackedRaster(new DataBufferByte(newdata,
			// newdata.length), w,h, w, new int[] { 4,2,1 }, null);
			r = Raster.createInterleavedRaster(new DataBufferByte(newdata,
					newdata.length), w, h, w * spd, spd, new int[] { 0 }, null);
			multivalent.Meta.sampledata("1 bit packed byte: " + rawdata.length
					+ " => " + newdata.length);
		}

		return r;
	}
}
