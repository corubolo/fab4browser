package multivalent.std.adaptor.pdf;

import java.awt.color.*;
import java.io.IOException;
import java.io.InputStream;
import java.awt.image.IndexColorModel;

import phelps.awt.color.*;

import static multivalent.std.adaptor.pdf.COS.*;



/**
	Returns Java color space that matches PDF specification.
	Used by ordinary drawing (<code>cs</code>, <code>CS</code>) and images (<code>/ColorSpace</code>).

	<ul>
	<li>PDF DeviceRGB and DeviceGray are mapped into equivalent CIE-based via {@link ColorSpace#getInstance(int)}.
	<!-- <li>CalRGB is mapped to {@link ColorSpaceCalRGB} and CalGray to {@link ColorSpaceCalGray}, => bug -->
	<li>CalRGB and CalGray are mapped likewise.
	<li>DeviceCMYK is mapped to {@link ColorSpaceCMYK}.
	<li>Lab is mapped to {@link ColorSpaceLab}.
	<li>Separation is mapped to {@link ColorSpaceSeparation}.
	<li>DeviceN is mapped to {@link ColorSpaceDeviceN}
	<li>Pattern unsupported; returns RGB color space.
	<li>Indexed color space returns base color space for further processing by images.
	<li>Embedded ICC Profile data is supported via {@link ICC_Profile#getInstance(InputStream)}.
	</ul>

	@version $Revision: 1.8 $ $Date: 2004/04/26 21:27:59 $
*/
public class ColorSpaces {
  private ColorSpaces() {}

  /**
	Interpret PDF color space represented by <var>csref</var> and return a {@link java.awt.color.ColorSpace} that implements it.
	@param csref    IRef or Dict
  */
  public static ColorSpace createColorSpace(Object csref, PDFReader pdfr) throws IOException {
//System.out.println("getColorSpace "+csref+" / "+getObject(csref));
	if (csref==null) return null;    // image mask

	// from content stream, csobj name of built-in parameter-free color space or key into ColorSpace entry of page resrouces => now built-in or array
	// from other objects, cs name of built-in or array
	Object csobj = pdfr.getObject(csref);

	// skip over parts Java calls ColorModel (vs ColorSpace)
	String name = null;
	if (csobj.getClass()==COS.CLASS_ARRAY) {   // [name ...]
		Object[] oa = (Object[])csobj;
		name = (String)pdfr.getObject(oa[0]);
//System.out.println("["+name+"]");
		if (oa.length==1) csobj = name;     // can put simple name in array
		else if ("Indexed".equals(name) || "I".equals(name)) return createColorSpace(oa[1], pdfr);    // map to base -- IndexedColorModel for image, but no IndexedColorSpace
	}
	/*not else*/if (csobj.getClass()==COS.CLASS_NAME) name = (String)csobj;    // built-in name or resource (could come from alt or base of special color space)
	//else name=null;


//System.out.println("getColorSpace() = "+name+": |"+csobj+"|");
	ColorSpace cs;
	if (csobj instanceof ColorSpace) {  // cached -- can be nested in non-cached and built by prior reference in different context, e.g., [/Indexed <ref> ...]
		cs = (ColorSpace)csobj;

	} else if (name==null/*should never happen*/ || "DeviceRGB".equals(name) || "RGB".equals(name)) {
		cs = ColorSpace.getInstance(ColorSpace.CS_sRGB);

	} else if ("DeviceGray".equals(name) || "G".equals(name)) {
		cs = ColorSpace.getInstance(ColorSpace.CS_GRAY);
//System.out.println("DeviceGray: "+cs);

	} else if ("DeviceCMYK".equals(name) || "CMYK".equals(name)) {
		cs = ColorSpaceCMYK.getInstance();

/*	} else if (name instanceof String) {    // inline image
System.out.println("fetching colorspace from resources: "+name);
		return (csres!=null? getColorSpace(csres.get(name), null): null);
*/
	} else if ("Separation".equals(name)) {
		// [/Separation name alternateSpace tintTransform], e.g., [ /Separation /All /DeviceCMYK <function IRef> ]
		Object[] oa = (Object[])csobj;
		String rep = (String)pdfr.getObject(oa[1]);
		ColorSpace alt = createColorSpace(oa[2], pdfr);
		Function tint = Function.getInstance(oa[3], pdfr);
		cs = ColorSpaceSeparation.getInstance(rep, alt, tint);

	} else if ("DeviceN".equals(name)) {
		// [/DeviceN names alternateSpace tintTransform <attrs>], e.g., "/DeviceN[/gris-fonc#8E#20]/DeviceCMYK 90 0 R]"
		Object[] oa = (Object[])csobj;
		ColorSpace alt = createColorSpace(oa[2], pdfr);
		Function tint = Function.getInstance(oa[3], pdfr);
		cs = ColorSpaceDeviceN.getInstance((Object[])oa[1], alt, tint);

	} else if ("Pattern".equals(name)) {
/* e.g.,
46 0 obj << /Type /Pattern /PatternType 1 /PaintType 1 /TilingType 1 /BBox [ 0 0 1200 90 ]
/Resources << /ProcSet [ /ImageI ] /XObject << /BGIm 2446 0 R >> >>
/XStep 1200 /YStep 90 /Matrix [ 0.81491 0 0 0.81491 0 63.15579 ]
/Length 45 0 R >> stream <data> endstream endobj
*/
		//Object[] oa = (Object[])csobj;
		//if (oa.length > 1) cs = createColorSpace(oa[1], pdfr);   // => not alternative, supplies color for uncolored
		cs = ColorSpace.getInstance(ColorSpace.CS_sRGB);

	} else { assert COS.CLASS_ARRAY==csobj.getClass(): "not CIE: "+csobj;       // CIE-based
		Object[] oa = (Object[])csobj;
		Dict csdict = (Dict)pdfr.getObject(oa[1]);    // [name dict]
//System.out.println("create new color space "+name+"=>"+csname+" / "+csdict.get(name)+" from "+o);

		Object attr;
		if ("CalRGB".equals(name)) {
//System.out.println("*** CalRGB color space / "+name);
			float[] abc = new float[9];
			PDF.getFloats((Object[])csdict.get("WhitePoint"), abc, 3);
			ColorSpaceCalRGB calrgb = new ColorSpaceCalRGB(abc[0], abc[1], abc[2]);
			if ((attr=csdict.get("Gamma"))!=null) { PDF.getFloats((Object[])attr, abc, 3); calrgb.setGamma(abc[0], abc[1], abc[2]); }   // LATER: match with monitor's gamma (else colors for print too dark)
			if ((attr=csdict.get("BlackPoint"))!=null) { PDF.getFloats((Object[])attr, abc, 3); calrgb.setBlackPoint(abc[0], abc[1], abc[2]); }
			if ((attr=csdict.get("Matrix"))!=null) { PDF.getFloats((Object[])attr, abc, 9); calrgb.setMatrix(abc); }
			cs = calrgb;    // slow on images
			// LATER: if parameters match sRGB, use that
			if (PDF.GoFast || true) cs = ColorSpace.getInstance(ColorSpace.CS_sRGB);    // much faster for images

		} else if ("CalGray".equals(name)) {
//System.out.println("*** CalGray color space / "+name);
			float[] abc = new float[3];
			PDF.getFloats((Object[])csdict.get("WhitePoint"), abc, 3);
			ColorSpaceCalGray calgray = new ColorSpaceCalGray(abc[0], abc[1], abc[2]);
			if ((attr=csdict.get("Gamma"))!=null) calgray.setGamma(((Number)attr).floatValue());  // LATER: match with monitor's gamma
			if ((attr=csdict.get("BlackPoint"))!=null) { PDF.getFloats((Object[])attr, abc, 3); calgray.setBlackPoint(abc[0], abc[1], abc[2]); }
			cs = calgray;
			cs = ColorSpace.getInstance(ColorSpace.CS_GRAY);

		} else if ("Lab".equals(name)) {
//System.out.println("*** Lab color space / "+name);
			float[] abc = new float[4];
			PDF.getFloats((Object[])csdict.get("WhitePoint"), abc, 3);
			ColorSpaceLab lab = new ColorSpaceLab(abc[0], abc[1], abc[2]);
			if ((attr=csdict.get("BlackPoint"))!=null) { PDF.getFloats((Object[])attr, abc, 3); lab.setBlackPoint(abc[0], abc[1], abc[2]); }
			if ((attr=csdict.get("Range"))!=null) { PDF.getFloats((Object[])attr, abc, 4); lab.setRange(abc[0], abc[1], abc[2], abc[3]); }
			cs = lab;

		} else { assert "ICCBased".equals(name): name+" / "+oa[1];   // can handle the hardest! but very slow for images, so substitute
			// AffineImageOp won't work on this or CMYK!
			Dict datadict = (Dict)pdfr.getObject(oa[1]);
			Object alt = datadict.get("Alternate");
//System.out.print("*** embedded ICC Profile!  "+alt+", "+PDF.GoFast);
			if (alt!=null) {
//System.out.println("alt "+alt);
				cs = createColorSpace(alt, pdfr);
			} else if (PDF.GoFast) {
				int N = pdfr.getObjInt(datadict.get("N"));
				if (N==1) cs = ColorSpace.getInstance(ColorSpace.CS_GRAY);
				else if (N==3) cs = ColorSpace.getInstance(ColorSpace.CS_sRGB);
				else { assert N==4: N; cs = ColorSpaceCMYK.getInstance(); }

			} else {    // AffineImageOp won't work and things slow
//System.out.println("ICC data length = "+pdfr.getObject(datadict.get("Length")));
				InputStream csin = pdfr.getInputStream(oa[1]);
				cs = new ICC_ColorSpace(ICC_Profile.getInstance(csin));
				csin.close();
			}
		}
	}

	assert cs!=null;
	return cs;
  }


  public static IndexColorModel createIndexColorModel(Object csref, int bpc, PDFReader pdfr) throws IOException {
	Object[] oa = (Object[])pdfr.getObject(csref);   // [/Indexed base hival samples]
	int hival = pdfr.getObjInt(oa[2]);
	Object cmap = pdfr.getObject(oa[3]);  assert CLASS_DICTIONARY==cmap.getClass() || CLASS_STRING==cmap.getClass();

	byte[] samp;
	if (CLASS_DICTIONARY==cmap.getClass()) {
		samp = pdfr.getStreamData(oa[3]/*NOT cmap-- could be encrypted*/, false, false);
//Sytem.out.println("index data "+oa[3]+"/"+pdfr.getObject(oa[3])+" => size="+samp.length+" vs hival="+hival+"/"+((hival+1)*3));

	} else { assert CLASS_STRING==cmap.getClass();
		StringBuffer sb = (StringBuffer)cmap;
		samp = new byte[sb.length()];
		for (int i=0,imax=sb.length(); i<imax; i++) samp[i] = (byte)sb.charAt(i);
	}

	// Java's IndexColorModel requires base RGB space, so convert colors from current color space
	Object base = pdfr.getObject(oa[1]);     // "array or name"
	if (null==base || "DeviceRGB".equals(base) || "RGB".equals(base)) {}  // ok as is
	else {  // "any device or CIE-based color space or (in PDF 1.3) a Separation or DeviceN space, but not a Pattern space or another Indexed space."
		ColorSpace bcs = pdfr.getColorSpace(base, null, null);
		int spd = bcs.getNumComponents(), sampcnt = samp.length / spd;
		byte[] brgb = new byte[sampcnt*3];  // RGB
		float[] fsamp = new float[spd];
//System.out.println("converting "+base+"/"+oa[1]+"/"+bcs+", spd="+spd+" to RGB, length "+samp.length+"=>"+brgb.length+", spd="+spd+", sampcnt="+sampcnt);
		for (int i=0, is=0, id=0; i<sampcnt; i++) {
			for (int j=0; j<spd; j++) fsamp[j] = (samp[is++]&0xff) / 255f;  // GIVE US UNSIGNED BYTES!
			float[] rgb = bcs.toRGB(fsamp);
//if(id<10*spd || i>250) System.out.println(fsamp[0]+" "+fsamp[1]+" "+fsamp[2]+" "+fsamp[3]+" => "+rgb[0]+" "+rgb[1]+" "+rgb[2]);
			for (int j=0; j<3; j++) brgb[id++] = (byte)(rgb[j] * 255f);
//if (i<10 || i>250) System.out.println(i+": "+fsamp[0]+" "+fsamp[1]+" "+fsamp[2]+" "+fsamp[3]+" => "+brgb[id-3]+" "+brgb[id-2]+" "+brgb[id-1]);
		}
		samp = brgb;
	}
//for (int i=0,imax=samp.length; i<imax; i+=3) System.out.println((i/3)+": "+Integer.toHexString(samp[i]&0xff)+" "+Integer.toHexString(samp[i+1]&0xff)+" "+Integer.toHexString(samp[i+2]&0xff));
//System.out.println("bpc = "+bpc+", hival="+hival);

	// cache conversion...

	// Java bug: IndexColorModel.isCompatibleRaster() checks (1<<raster_bpc)>=map_size should be (1<<raster_bpc)<=map_size
	//assert b.length == hival+1: bpc+" vs "+(hival+1);
	// workaround Java bug: make equal (ColorModels not cached).  Tickled by pdfTeX-0.14d.  RESTORE.
//System.out.println("indexed size = "+(hival+1)+", bpc="+bpc+", samp.length="+samp.length);
	return new IndexColorModel(bpc, Math.min(1<<bpc, hival+1), samp, 0, false);
  }
}
