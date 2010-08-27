package multivalent.std.adaptor.pdf;

import java.awt.FontFormatException;
import java.awt.geom.Rectangle2D;
import java.io.InputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.regex.*;

import phelps.io.InputStreams;
import phelps.util.Arrayss;
import phelps.lang.Integers;

import com.pt.awt.NFont;
import static com.pt.awt.NFont.*;
import com.pt.awt.font.*;

import static multivalent.std.adaptor.pdf.COS.*;



/**
	Font creation, from PDF description to {@link com.pt.awt.NFont}.

	@version $Revision: 1.41 $ $Date: 2005/07/26 21:01:23 $
*/
public class Fonts {
  static final boolean DEBUG = !false && multivalent.Meta.DEVEL;

  private static final String[] CORE14 = {
	"Times-Roman", "Times-Bold", "Times-Italic", "Times-BoldItalic",
	"Helvetica", "Helvetica-Bold", "Helvetica-Oblique",  "Helvetica-BoldOblique",
	"Courier", "Courier-Bold", "Courier-Oblique", "Courier-BoldOblique",
	"Symbol",
	"ZapfDingbats"
  };
  static { Arrays.sort(CORE14); }

  private static final String[][] EQ_ = {	// PDF Reference 1.4, p.795.  ordered to increasing specialization.  position 0 hold canonical name.
	{ "Times-Roman",  /*unofficial:*/"Times", /*unofficial:*/"Times New Roman","TimesNewRoman",  "TimesNewRomanPS",  "TimesNewRomanPSMT" }, 
	{ "Times-Bold",  "TimesNewRoman,Bold","TimesNewRoman-Bold",  "TimesNewRomanPS-Bold",  "TimesNewRomanPS-BoldMT" },
	{ "Times-Italic", "TimesNewRoman,Italic","TimesNewRoman-Italic",  "TimesNewRomanPS-Italic",  "TimesNewRomanPS-ItalicMT" },
	{ "Times-BoldItalic",  "TimesNewRoman,BoldItalic","TimesNewRoman-BoldItalic",  "TimesNewRomanPS-BoldItalic",  "TimesNewRomanPS-BoldItalicMT" },
	{ "Helvetica",  "Arial",  "ArialMT" },
	{ "Helvetica-Bold","Helvetica,Bold",  "Arial,Bold","Arial-Bold",  "Arial-BoldMT" },
	{ "Helvetica-Oblique", "Helvetica,Italic","Helvetica-Italic",  "Arial,Italic","Arial-Italic",  "Arial-ItalicMT" },
	{ "Helvetica-BoldOblique", "Helvetica,BoldItalic","Helvetica-BoldItalic",  "Arial,BoldItalic","Arial-BoldItalic",  "Arial-BoldItalicMT" },
	{ "Courier",  "CourierNew",  "CourierNewPSMT" },
	{ "Courier-Bold","Courier,Bold",  "CourierNew,Bold","CourierNew-Bold",  "CourierNewPS-BoldMT" },
	{ "Courier-Oblique",  "Courier,Italic", "CourierNew-Italic","CourierNew,Italic",  "CourierNewPS-ItalicMT" },
	{ "Courier-BoldOblique", "Courier,BoldItalic",  "CourierNew-BoldItalic","CourierNew,BoldItalic",  "CourierNewPS-BoldItalicMT" },
	{ "Symbol" }, 
	{ "ZapfDingbats", /*unofficial:*/"Zapf-Dingbats", /*unofficial (URW)*/"Dingbats", /*OS X "ZapfDingbatsITC matched by NFontManger's fuzzy,*/ /*"NOT Monotype Sorts", NOT "Wingdings" -- different characters*/ }
  };

  /** Map named CMap to Unicode mapping.  */
  private static final String[][] TOUNICODE = {
	// format: <canonical> <map1> <map2> ...
	// Chinese (Simplified)
	{ "GBpc-EUC-UCS2",/*"GBpc-EUC-UCS2C",*/  "GBpc-EUC-H", "GBpc-EUC-V" },
	{ "GBK-EUC-UCS2",  "GBK-EUC-H", "GBK-EUC-V" },
	{ "UniGB-UCS2-H",  "GB-EUC-H", "GBT-EUC-H", "GBK2K-H", "GBKp-EUC-H" },
	{ "UniGB-UCS2-V",  "GB-EUC-V", "GBT-EUC-V", "GBK2K-V", "GBKp-EUC-V" },

	// Chinese (Traditional)
	{ "B5pc-UCS2",/*"B5pc-UCS2C",*/  "B5pc-H", "B5pc-V" },
	{ "ETen-B5-UCS2",  "ETen-B5-H", "ETen-B5-V", "ETenms-B5-H", "ETenms-B5-V" },
	{ "UniCNS-UCS2-H",  "HKscs-B5-H", "CNS-EUC-H" },
	{ "UniCNS-UCS2-V",  "HKscs-B5-V", "CNS-EUC-V" },

	// Japanese
	{ "90pv-RKSJ-UCS2",/*"90pv-RKSJ-UCS2C",*/  "90pv-RKSJ-H", "83pv-RKSJ-H" },
	{ "90ms-RKSJ-UCS2",  "90ms-RKSJ-H", "90ms-RKSJ-V", "90msp-RKSJ-H", "90msp-RKSJ-V" },
	{ "UniJIS-UCS2-H",  "Ext-RKSJ-H", "H", "Add-RKSJ-H", "EUC-H" },
	{ "UniJIS-UCS2-V",  "Ext-RKSJ-V", "V", "Add-RKSJ-V", "EUC-V" },
	//{ "UniJIS-UCS2-HW-H" },
	//{ "UniJIS-UCS2-HW-V" },

	// Korean
	{ "KSCms-UHC-UCS2",  "KSCms-UHC-H", "KSCms-UHC-V", "KSCms-UHC-HW-H", "KSCms-UHC-HW-V" },
	{ "KSCpc-EUC-UCS2",/*"KSCpc-EUC-UCS2C",*/  "KSCpc-EUC-H" },
	{ "UniKS-UCS2-H",  "KSC-EUC-H" },
	{ "UniKS-UCS2-V",  "KSC-EUC-V" },
	//{ "UniKS-UTF16-H", "UniKS-UTF16-V" }, => special case

	/* distributed with Acrobat but not defined in PDF Reference
"Adobe-CNS1-3", 
"Adobe-CNS1-UCS2", 

"Adobe-GB1-4", 
"Adobe-GB1-UCS2", 

"Adobe-Japan1-4", 
"Adobe-Japan1-UCS2", 

"Adobe-Korea1-2", 
"Adobe-Korea1-UCS2", 
*/
  };

  private static final Matcher REGEX_SUBSET = Pattern.compile("[A-Z][A-Z][A-Z][A-Z][A-Z][A-Z]\\+.+").matcher("");	// naming convenetion:"SIXCAP+..."


  private Fonts() {}

  public static boolean isCore14(String name) {
	return name!=null && Arrays.binarySearch(CORE14, name)>=0;
  }

  public static String[] getCore14() { return (String[])CORE14.clone(); }

  /** Returns canonical name of font in core 14 font set, or <code>null</code> if none. */
  public static String getCanonical(String name) {
	for (int i=0,imax=EQ_.length; i<imax; i++) {
		String[] eq = EQ_[i];
		int inx = Arrayss.indexOf(eq, name);
		if (inx != -1) return eq[0];
	}
	return null;
  }

  public static boolean isSubset(String name) {
	return name!=null && REGEX_SUBSET.reset(name).matches();
  }

  
  /**
	Constructs {@link NFont} based on font dictionary, scaled to <var>size</var> pixels.
  */
  public static NFont createFont(Dict fd, PDFReader pdfr, PDF pdf) throws IOException {
	assert fd!=null; //&& size>=0f /*&& size<200f*/: fd+", size="+size;	// ==0f seen in riggs.pdf page 20
	assert "Font".equals(fd.get("Type")) || null==fd.get("Type");

	//PDFReader pdfr = pdf.getReader();
	NFontManager fm = NFontManager.getDefault();
	NFont font = null;

	String subtype = (String)pdfr.getObject(fd.get("Subtype"));
	String basefont = (String)pdfr.getObject(fd.get("BaseFont"));
//System.out.println("createFont: fd = "+fd+", subtype="+subtype);
	Dict fdesc = (Dict)pdfr.getObject(fd.get("FontDescriptor"));
	if (fdesc==null && basefont!=null/*Type 3, Type 0*/) fdesc = Core14AFM.getFontDescriptor(basefont);
	//if ((pdf.getFlags() & MediaAdaptor.FLAG_DISPLAY) == 0) ... don't need actual font, just metrics
	String family = fdesc!=null? (String)pdfr.getObject(fdesc.get("Family")): null;	// could have missing core 14 w/o fdesc
	if (family==null && basefont!=null) family = NFontManager.guessFamily(basefont);
	Object o;
	int flags = fdesc!=null && (o=fdesc.get("Flags"))!=null? pdfr.getObjInt(o): FLAG_NONE;
	int weight = fdesc!=null && (o = pdfr.getObject(fdesc.get("FontWeight"))) instanceof Number? ((Number)o).intValue(): NFontManager.guessWeight(basefont);

//System.out.println("unicode: "+fd.get("ToUnicode"));
	CMap touni = pdfr.getCMap(fd.get("ToUnicode"));
//if (DEBUG && touni!=null) System.out.println("unicode: "+touni);

	// 1. instantiate basic font
	Object emref;
	boolean fembedded = false;
	// a. guaranteed embedded: Type 3 and Type 0
	if ("Type3".equals(subtype)) {
		font = new NFontType3(fd, pdfr, pdf);
		//font = (NFontAWT)NFont.getInstance("Times", flags, 1f);	// sometimes look better if ignore Type 3?

	} else if ("Type0".equals(subtype)) {
		o = fd.get("Encoding"); Object o2 = pdfr.getObject(o);	// required in Type 0
		Class cl = o2.getClass();
if (DEBUG) System.out.println("Type 0 "+basefont+", encoding = "+o2);
		CMap e2c = pdfr.getCMap(o);

		if (touni==null && CLASS_NAME==cl) {
			String e = (String)o2; //int bl = e.length();
			if ("Identity-H".equals(e) || "Identity-V".equals(e) || "Identity".equals(e)) touni = e2c;
			else if (e.indexOf("UCS2")>0) touni = CMap.IDENTITY_H;	// UCS2 is from Unicode to CID so already in Unicode
			else if (e.indexOf("UTF16")>0) touni = CMap.IDENTITY_UTF16BE;
			else {	// else pick UCS2 from base
				for (String[] eqclass: TOUNICODE) {
					for (String eq: eqclass) if (eq.equals(e)) { touni = CMap.getInstance(eqclass[0]); System.out.println(e+" => Uni "+eqclass[0]); break; }
				}
			}
			// else translate via java.nio.charset.Charset, maybe
				// somehow map named encoding to font cmap (PDF Ref: "The means by which this is accomplished are implementation-dependent.")
			/*else if (bl>=3 && base.charAt(bl-2)=='-') {	// ends with "-[HV1234]"
				String u = base.substring(0, bl-2);
				if (u.endsWith("-HW")) u = u.substring(0, u.length()-3);
				if (u.length() > 1) touni = CMap.getInstance(u + "-UCS2");
			}*/
//System.out.println("compute toUnicode from "+o2);
		}
		Object[] oa = (Object[])pdfr.getObject(fd.get("DescendantFonts")); assert oa.length==1;
		//NFont[] dfs = new NFont[oa.length];
		//for (int i=0,imax=dfs.length; i<imax; i++) dfs[i] = createFont((Dict)pdfr.getObject(oa[i]), pdf);
		Dict dfdict = (Dict)pdfr.getObject(oa[0]);
		NFont[] dfs = new NFont[] { createFont(dfdict, pdfr, pdf) };
		//dfs[0] = ((NFontSimple)dfs[0]).deriveFont(null/*original*/, null);

		Dict dfdesc = (Dict)pdfr.getObject(dfdict.get("FontDescriptor"));
		if (dfdesc.get("FontFile")==null && dfdesc.get("FontFile2")==null && dfdesc.get("FontFile3")==null) {	// if not embedded, no CID: translate everything to Unicode
			// b. update e2c to forget about CID and go directly to Unicode, which we always compute anyway
			// good luck: source in Unicode, and rely on TrueType font's default Unicode CMap (almost always have Unicode CMap)
			if (touni != null) e2c = touni;
			// we're probably screwed, but give it a try
			else e2c = touni = CMap.IDENTITY_H;
		}

		NFontType0 t0 = new NFontType0(basefont, dfs);
		font = t0.deriveFont(e2c, touni);

	// b. fake out data -- faster for text extraction
	} else if (/*(NFont.FLAG_NO_SHOW&flags)!=0 &&*/ pdf == null && fd.get("Widths")!=null
			   && ("Type1".equals(subtype) || "MMType1".equals(subtype) || "TrueType".equals(subtype))
			   ) {
//System.out.println("cheap "+subtype+": "+basefont);
		font = new NFontInvisible(null, basefont, family);

	// c. possibly embedded
	} else if ("Type1".equals(subtype) || "MMType1".equals(subtype) || "CIDFontType0".equals(subtype)) {
		if (fdesc!=null/*core 14*/ && ((emref=fdesc.get("FontFile"))!=null || (emref=fdesc.get("FontFile3"))!=null)) {	// Type 1 or Multiple Master instance or CFF or OpenType
//if (DEBUG) System.out.println("embedded "+subtype+": "+basefont);
			InputStream in = pdfr.getInputStream(emref);
			try {
				byte[] data = InputStreams.toByteArray(in, 10*1024);
				font = "OpenType".equals(pdfr.getObject(fdesc.get("Subtype")))? (NFont)new NFontOpenType(null, data): new NFontType1(null, data);
//System.out.println(basefont+", embedded, "+font.getName()+", "+((NFontSimple)font).getEncoding());
				fembedded = true;
			}
			catch (FontFormatException failed) { System.err.println("embedded "+subtype+" "+emref+": "+failed); }
			catch (IOException ioe) { System.err.println("embedded "+subtype+" "+emref+": "+ioe); }
			catch (Error e) {
				System.err.println("embedded Type 1 "+emref+": "+e);
				if (DEBUG) e.printStackTrace();
			} finally { in.close(); }
//if (DEBUG) System.out.println("embedded "+subtype+": "+basefont);
		}

	} else if ("TrueType".equals(subtype) || "CIDFontType2".equals(subtype)) {
		if (fdesc!=null && (emref = fdesc.get("FontFile2"))!=null) {
//if (DEBUG) System.out.println("embedded "+subtype+": "+basefont);
			InputStream in = pdfr.getInputStream(emref);
			try { font = /*"NFontOpenType" OK*/new NFontTrueType(null, InputStreams.toByteArray(in, 10*1024)); fembedded = true; }
//System.out.println("created new TrueType font from embedded stream, name = "+basefont+", "+font.getName());
			catch (FontFormatException failed) { System.err.println("embedded "+subtype+" "+basefont+" "+emref+": "+failed); }
			catch (IOException ioe) { System.err.println("embedded "+subtype+" "+basefont+" "+emref+": "+ioe); }
			finally { in.close(); }
		}

	} else assert false: "unknown font type on "+basefont+": "+subtype;

	// d. exact name
	//&& basefont!=null -- only Type 3 and Type 0, but those guaranteed
	if (font==null && fm.isAvailableName(basefont)) try { font = fm.getFont(basefont); } catch (Exception fail) {}

	// e. Symbol or Zapf-Dingbats
	//if (font==null) match on Unicode blocks

	// f. canonicalized to core 14
	if (font==null) {
		String canon = getCanonical(basefont);
		if (canon!=null) for (int i=0,imax=EQ_.length; i<imax; i++) if (EQ_[i][0]==canon) {
			String[] eq = EQ_[i];
			for (int j=eq.length-1; j>=0; j--) if (fm.isAvailableName(eq[j])) try { font = fm.getFont(eq[j]); break; } catch (Exception fail) {}	// tries all in equivalence class until one exists and not bad data
//System.out.println(basefont+" => core14 "+canon+" / font="+font);
			break;
		}
	}

	// g. same family
	//if (font==null) String family = NFontManager.guessFamily(basefont);
	// ...

	// h. non-embedded CID: match based on /Ordering
	if (font==null && ("CIDFontType0".equals(subtype) || "CIDFontType2".equals(subtype))) {
		Dict sysinfo = (Dict)pdfr.getObject(fd.get("CIDSystemInfo"));
		String e = pdfr.getObject(sysinfo.get("Ordering")).toString();

		String fam = 
			e.startsWith("Ident")? null:	// who knows?  maybe even Hebrew or Arabic
			e.startsWith("Japan")? NFontManager.FAMILY_JAPANESE:
			e.startsWith("GB")? NFontManager.FAMILY_CHINESE_SIMPLIFIED:
			e.startsWith("Korea")? NFontManager.FAMILY_KOREAN:
			/*"CNS"*/ NFontManager.FAMILY_CHINESE_TRADITIONAL;
		if (fam!=null) font = NFont.getInstance(fam, weight, flags, 1f);
if (DEBUG) System.out.println("CID not embedded, /Ordering = "+e+" => "+font);//+", ToUnicode = "+touni);
	}

	// i. based on family and flags -- GUARANTEED
	if (font==null) {
//System.out.print("NFont.getInstance("+fam+", "+Integer.toBinaryString(flags)+", 1f)");
		// extended flags
		o = fdesc!=null? fdesc.get("FontStretch"): null;
		if (basefont.indexOf("Cond")>/*=*/0) flags |= FLAG_CONDENSED;

		//assert flags!=-1: family;
		font = NFont.getInstance(family, weight, flags, 1f);
//System.out.println(" => "+font.getName());
	}


	// 2. customize: encoding and widths
	if ("Type3".equals(subtype) || "Type1".equals(subtype) || "MMType1".equals(subtype) || "TrueType".equals(subtype)) {
//System.out.print(font.getName()+", "+font.getNumGlyphs()+"/"+font.getMaxGlyphNum());
		NFontSimple fs = (NFontSimple)font;
		fs = setEncoding(fs, fd, touni, fembedded, pdfr);
//System.out.println(" => "+fs.getNumGlyphs()+"/"+fs.getMaxGlyphNum());
		/*if (!fembedded)--needed by symbol in PDF Ref 1.4*/ fs = setShaping(fs, fd, fdesc, fs.getEncoding(), pdfr);	// PDF Reference 1.5 implementation note 53: "If ... embedded ... widths should exactly match the widths in the font dictionary."
//System.out.println("shaping on "+fs.getName()+", "+fdesc);
		/*else {	// verify that widths all the same
			System.out.println("embedded "+java.util.Arrays.asList((Object[])pdfr.getObject(fd.get("Widths"))));
			int firstch = pdfr.getObjInt(fd.get("FirstChar")), lastch = pdfr.getObjInt(fd.get("LastChar"));
			Object[] oa = (Object[])pdfr.getObject(fd.get("Widths"));
			System.out.println(basefont+" /Widths "+firstch+".."+lastch);
			if (oa.length != lastch - firstch + 1) System.out.println("/Widths length = "+oa.length+" vs last-first="+(lastch-firstch));
			for (int i=firstch,imax=lastch+1; i<imax; i++) {
				double in = font.echarWidth((char)i), em = ((Number)oa[i-firstch]).doubleValue() / 1000.0;
//System.out.println("   "+in+"  vs   "+em);
				if (Math.abs(in - em) > 0.001 && em!=0.0) System.out.println("widths mismatch "+basefont+" @ "+i+": "+in+" => "+em);	// sometimes put 0 in /Widths to fill slot if don't use character(?), sometimes have width for char not in subset
			}
		}*/
		font = fs;

	} else if ("CIDFontType0".equals(subtype) || "CIDFontType2".equals(subtype)) {
		//if (!fembedded) t0 = t0.setShaping(...);

		if (!fembedded) {
			// keep Unicode

		} else if ("CIDFontType2".equals(subtype)) {
			o = fd.get("CIDToGIDMap");
			if (o==null || "Identity".equals(o)) {
				font = ((NFontTrueType)font).deriveFont(CMap.IDENTITY, CMap.IDENTITY);

			} else try {
				StringBuffer sb = new StringBuffer(200);
				InputStream in = pdfr.getInputStream(o); for (int c; (c=in.read())!=-1; ) sb.append((char)((c<<8) | in.read())); in.close();
				//char[] cmap = new char[sb.length()]; for (int i=0,imax=cmap.length; i<imax; i++) cmap[i]=sb.charAt(i);
//if (cmap[i]!=0) System.out.print(" "+i+"->"+(int)cmap[i]);}
				font = ((NFontTrueType)font).deriveFont(new CMap(sb.toString().toCharArray()).reverse(), CMap.IDENTITY);
			} catch (IOException ioe) {}	// don't let error here kill whole font
			// c [w1 w2 .. wn]
			// cfirst clast w

		} else { assert "CIDFontType0".equals(subtype);
			// Type 0 maps to CID, which CIDFont maps to GID
		}
	}

	//if (DEBUG) System.out.println(basefont+" / "+NFont.strFlags(flags)+" / "+pdfr.getObject(fd.get("Encoding"))+" => "+font/*+"/"+font.hashCode()/*+", encoding="+font.getEncoding()*/+" / "+font.getFormat()+" / "+NFont.strFlags(font.getFlags())+", spacech = "+(int)font.getSpaceEchar()+", #glyphs="+font.getNumGlyphs());
	//getLogger().finer(basefont+" "+subtype+" => "+font);
	return font;
  }


  private static NFontSimple setEncoding(NFontSimple font,  Dict fd, CMap touni, boolean fembedded, PDFReader pdfr) throws IOException {
	String basefont = (String)pdfr.getObject(fd.get("BaseFont"));
	Object enobj = pdfr.getObject(fd.get("Encoding"));

	Encoding en;
	if (enobj==null) {	// "built-in encoding" of font
		if (isCore14(basefont)) {
			en =	//fembedded? font.getEncoding() ?
				basefont.startsWith("ZapfD")? Encoding.ZAPF_DINGBATS:
				basefont.startsWith("Symbol")? Encoding.SYMBOL:
				Encoding.ADOBE_STANDARD;	// implicitly Adobe Standard even if substitute font (as in XEP, EPodd).
		} else en = null;	// null means try intrinsic with 8-bit encoding
//System.out.println("encoding "+font.getName()+" = "+en);
	} else if (CLASS_NAME==enobj.getClass()) {
		en = "ZapfDingbats".equals(basefont)? Encoding.ZAPF_DINGBATS: "Symbol".equals(basefont)? Encoding.SYMBOL:	// override FOP's buggy assertion of /StandardEncoding
			Encoding.getInstance((String)enobj);
	} else { assert CLASS_DICTIONARY==enobj.getClass();	// differences
		Dict emap = (Dict)enobj;
		String base = (String)pdfr.getObject(emap.get("BaseEncoding"));
		en = base!=null? Encoding.getInstance(base): 
			fembedded? font.getEncoding():
			basefont==null? Encoding.ADOBE_STANDARD:
			basefont.startsWith/*equals?*/("ZapfD")? Encoding.ZAPF_DINGBATS:
			basefont.startsWith/*equals?*/("Symbol")? Encoding.SYMBOL:
			Encoding.ADOBE_STANDARD;
		//en = base!=null? Encoding.getInstance(base): font.getEncoding();	// find where Adobe Standard not good
//System.out.println("base="+base+", basefont="+basefont+", embed? "+fembedded+" => "+en);
//System.out.println(emap.get("BaseEncoding")+" ->  "+en+" + "+emap.get("Differences"));
		en = new Encoding(en, (Object[])pdfr.getObject(emap.get("Differences")));
	}

//System.out.println(font.getName()+" "+font.getSource()+", encoding = "+en);
	font = font.deriveFont(en, touni);
	return font;
  }

  private static NFontSimple setShaping(NFontSimple font,  Dict fd, Dict fdesc, Encoding en, PDFReader pdfr) throws IOException {
	//if (fdesc!=null) System.out.println(basefont+": FontBBox = "+array2Rectangle((Object[])fdesc.get("FontBBox"), IDENTITY_TRANSFORM)+" vs "+font.getMaxCharBounds());
	Object o = pdfr.getObject(fd.get("Widths"));
	//if (o==null) return font;	// temporary, for Type 0 -- as in RESIDU p51

	int[] widths; int firstch;
	if (o!=null && CLASS_ARRAY==o.getClass()) {
		Object[] wo = (Object[])o;
		widths = new int[wo.length];
		for (int i=0,imax=wo.length; i<imax; i++) widths[i] = pdfr.getObjInt(wo[i]);
		firstch = (o = fd.get("FirstChar"))!=null? pdfr.getObjInt(o): 0;	// required

	} else { assert o==null: o;	// core 14, which don't have /Widths
		widths = Core14AFM.getWidths(getCanonical((String)fd.get("BaseFont")));
		firstch = 32;	// happens to be 32 for entire core 14

		// core widths in AdobeStandard
		if (widths == null) {}	// bad PDF: not core 14 and no widths (see Bagley6.pdf)
		else if (Encoding.ADOBE_STANDARD == en) {}	// matches (rare)
		else {	// sort widths to match font encoding
			int[] ewidths = new int[256-firstch];
			//for (int i=0+firstch,imax=256; i<imax; i++) {
			for (int i=0,imax=256-firstch; i<imax; i++) {
				String cname = en.getName((char)(i+firstch));
				int std = Encoding.ADOBE_STANDARD.getChar(cname) - firstch;
				if (0 <= std&&std < widths.length) ewidths[i] = widths[std];
			}
			widths = ewidths;
		}
	}

/*if (widths!=null && widths.length > firstch+0x92) {
	System.out.println(font.getName()+": font intrinsic encoding = "+font.getEncoding()+" / w0x92 = "+font.charAdvance((char)0x92)+", name = "+font.getEncoding().getName((char)0x92));
	System.out.println("/Widths encoding = "+en+" / Width[0x92] = "+widths[0x92-firstch]+", name = "+en.getName((char)0x92));
}*/
//if (DEBUG) System.out.println("widths for core "+(o = fd.get("BaseFont"))+"/"+getCanonical((String)o)+" = "+widths);

	//else if ("AvgWidth")int AvgWidth = (desc!=null && (o=desc.get("AvgWidth"))!=null? pdfr.getObjInt(o): 0);
	// "FixedPitch"/FLAG_FIXEDPITCH
	// FLAG_ALLCAP just measure uppercase

	//int lastch = ((o = fd.get("LastChar"))!=null? pdfr.getObjInt(o): firstch_ + widths_.length);	// required
	// error: some PDFs don't give /Widths and not standard


	NFontSimple nfont;
	if (fdesc==null && widths==null) nfont = font;	// no change
	else if (fdesc==null) nfont = font.deriveFont(widths, firstch, 0,  0, 0, null);
	else {
		int missing = (o=fdesc.get("MissingWidth"))!=null? pdfr.getObjInt(o): 0;
		int ascent = (o=fdesc.get("Ascent"))!=null? pdfr.getObjInt(o): 0;
		int descent = (o=fdesc.get("Descent"))!=null? pdfr.getObjInt(o): 0;	// negative if exists
		Rectangle2D bbox = null;
		if ((o=fdesc.get("FontBBox"))!=null && CLASS_ARRAY==o.getClass()) {
			float[] f = new float[4]; PDF.getFloats((Object[])o, f, 4); bbox = new Rectangle2D.Double(f[0],f[1],f[2],f[3]);
		}
		nfont = font.deriveFont(widths, firstch, missing,  ascent, descent, bbox);
	}

	return nfont;
  }
}
