package multivalent.std.adaptor.pdf;

import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.io.File;
import java.net.URI;
import java.util.Calendar;
import java.util.Iterator;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.pt.doc.PostScript;



/**
	Translations between Java and PDF "COS" (Carousel Object System) data types.

	<ul>
	<li>Correspondence between PDF and Java object types:
		{@link #CLASS_IREF}, {@link #CLASS_DICTIONARY}, {@link #CLASS_ARRAY}, {@link #CLASS_NAME}, {@link #CLASS_STRING},
		{@link #CLASS_INTEGER}, {@link #CLASS_REAL}, {@link #CLASS_BOOLEAN},
		{@link #CLASS_DATA}, {link #CLASS_COMMENT}
	<li>{@link #OBJECT_NULL}, {@link OBJECT_DELETED}
	<li>extended types
	<li>{@link #parseDate(StringBuffer)}, {@link #createDate(long)},
		{@link #array2Rectangle(Object[], AffineTransform, boolean)},
		{@link #createID(URI, Dict)}
	</ul>
<!-- add findNameTree and findNumberTree? -->

	@version $Revision: 1.8 $ $Date: 2005/02/20 05:57:57 $
*/
public final class COS {
  private static Calendar cal_ = null;
  private static int offsetZone_, offsetDST_;
  private static Pattern pat_ = null;

  private static final String DATE_PATTERN = "D:YYYYMMDDHHmmSSOHH'mm'";


  /** Java type ({@link IRef}) of PDF indirect reference data type (e.g., <code>15 0 R</code>). */
  public static final Class CLASS_IREF=IRef.class;

  /**
	Java type ({@link Dict}) of PDF dictionary data type (e.g., <code>&lt;&lt; /Length 705 /Filter /FlateDecode &gt;&gt;</code>).
	Dictionary keys are of type {@link #CLASS_NAME}.
  */
  public static final Class CLASS_DICTIONARY=Dict.class;	// different from PostScript

  /**
	Java type (<code>Object[]</code>) of PDF array data type (e.g., <code>[ 41 63 572 729 ]</code>).
	Arrays can be heterogeneous.
	When creating new PDF array objects, always make a <code>Object[]</code> even though the array may be homogeneous of another type.
  */
  public static final Class CLASS_ARRAY=Object[].class;
  //public static final int ARRAY_MAX = 8192; -- not respected in practice

  /** Java type (<code>String</code>) of PDF name data type (e.g., <code>/Title</code>). */
  public static final Class CLASS_NAME = PostScript.CLASS_NAME;

  /**
	Java type (<code>StringBuffer</code>) of PDF string data type (e.g., <code>(PDF Reference)</code> and <code>&lt;a47cc386aea60669950095583bec355d&gt;</code>).
	Note that PDF strings are not Java {@link java.lang.String}s.
  */
  public static final Class CLASS_STRING = PostScript.CLASS_STRING;    // maybe switch to char[].class, which avoids charAt overhead?

  /**
	Java type (<code>Integer</code>) of PDF integer number data type (e.g., <code>93515</code>).
	Note PDF considers both integer and floating point the same type.
	When is possible to represent a number without lose of accuracy as an integer, this is preferred; this is what {@link #readObject()} does.
  */
  public static final Class CLASS_INTEGER = PostScript.CLASS_INTEGER;    // same 32 bits as Acrobat
  /** Java type (<code>Double</code>) of PDF real number data type (e.g., <code>0.9505</code>). */
  public static final Class CLASS_REAL = PostScript.CLASS_REAL;    // Acrobat uses a 32-bit fixed-point (1 bit sigin, 15 bits whole number, 16 bits fraction), but Java geometry classes take Double's.

  /** Java type (<code>Boolean</code>) of PDF boolean data type (<code>true</code> or <code>false</code>). */
  public static final Class CLASS_BOOLEAN = PostScript.CLASS_BOOLEAN;

  /** Java type (<code>char[]</code>) of PDF comment data type (e.g., <code>% comments run from '%' to end of line</code>). */
  public static final Class CLASS_COMMENT = PostScript.CLASS_COMMENT;

  //public static final Class CLASS_NULL=OBJECT_NULL.getClass(); => just use OBJECT_NULL


  /**
	Java type (<code>byte[]</code>) for raw data, which is <em>not a PDF data type</em>, but which is sometimes mixed with them.
	Some applications cache the contents of a stream in the stream dictionary under the key {@link #STREAM_DATA},
	and if so remove the <code>Length</code> key in favor of taking this information from <code>byte[].length</code>.
	{@link InputStreamComposite} accepts such streams, and internally inline images are processed into such streams to make them identical to image XObjects for subsequent merged transformation.
  */
  public static final Class CLASS_DATA = PostScript.CLASS_DATA;

  /**
	Singleton PDF NULL object (which is not Java <code>null</code>).
	Equality can use <code>==</code> as well as {@link Object#equals(Object)}. <!-- Object.equals() uses only = = equality, so OK but slower. -->
	The PDF NULL object is referred to as "PDF NULL" or <code>OBJECT_NULL</code>; otherwise the term "null" refers to the Java <code>null</code>.
	Note that there is no <code>CLASS_NULL</code>; instead compare the object itself to this instance.
  */
  public static final Object OBJECT_NULL = PostScript.OBJECT_NULL;

  /**
	Singleton object corresponding to freed/deleted objects, which can be created when a PDF is incrementally updated.
	Equality can use <code>==</code> as well as {@link Object#equals(Object)}. <!-- Object.equals() uses only = = equality, so OK but slower. -->
  */
  public static final Object OBJECT_DELETED = new Object() { public String toString() { return "DELETED"; } };

  /** String at start of file (usually byte 0) that identifies as PDF: <code>%PDF-<var>m.n</var></code>". */
  public static final String SIGNATURE = "%PDF-";
  /** String at start of file (usually byte 0) that identifies as FDF: <code>%FDF-</code>. */
  public static final String SIGNATURE_FDF = "%FDF-";

  /** String at end of trailer: "%%EOF". */
  public static final String EOF = "%%EOF";

  //** Minimum possible length of a valid PDF. */
  //public static final int MIN_VALID_LENGTH = "%PDF-1.5\n1 0 obj\nendobj\nxref\n0 1\ntrailer<<>>startxref\n0\n%%EOF".length() + 40/*xref*/;


  /** Cross reference type 0. */
  public static final byte XREF_FREE = 0;
  /** Cross reference type 1. */
  public static final byte XREF_NORMAL = 1;
  /** Cross reference type 2 (<em>component</em> of <code>/Type /ObjStm</code>). */
  public static final byte XREF_OBJSTMC = 2;

  public static final int GEN_MAX = 0xffff;


  // extensions

  public static final java.io.FileFilter FILTER = new phelps.io.FileFilterPattern("(?i)\\.pdf$");


  /** If move object into a ObjStm, replace it with the object number of the ObjStm of this class (<code>java.lang.Long</code>) . */
  public static final Class CLASS_OBJSTMC=Long.class;

  /** For stream dictionary, offset of data in file if of type <code>Long</code>, or data itself if of type <code>byte[]</code>. */
  public static final String STREAM_DATA = "DATA";
  /** Injected key for objects cached within dictionaries, such as fonts, images. */
  public static final String REALIZED = "REALIZED";
  static { assert CLASS_NAME==STREAM_DATA.getClass() && CLASS_NAME==REALIZED.getClass(); }  // dictionary keys must be names

  /** 
	Key in trailer for <em>direct</em> dictionary with compression information.
	Within dictionary, {@link #KEY_COMPRESS_LENGTHO}, {@link #KEY_COMPRESS_FILTER}, {@link #KEY_COMPRESS_VERSION}, {@link #KEY_COMPRESS_COMPACT}.
  */
  public static final String KEY_COMPRESS = "Compress";
  /** Key in compression dictionary that gives original length of PDF. */
  public static final String KEY_COMPRESS_LENGTHO = "LengthO";
  /** Key in compression dictionary that gives compression method. */
  public static final String KEY_COMPRESS_FILTER = "Filter";
  /** Key in compresion dictionary that gives specification version of compression filter.  Optional.  Default: 1.0. */
  public static final String KEY_COMPRESS_VERSION = "V";
  /** Key in compression dictionary with indirect reference to Compact stream. */
  public static final String KEY_COMPRESS_COMPACT = "Compact";
  /** Key in compression dictionary with <var>major</var>.<var>minor</var> of PDF specification compatibility modulo compression (Compact, ObjStm, ...). */
  public static final String KEY_COMPRESS_SPECO = "SpecO";
  /** Key in compression dictionary that points to original <code>/Catalog</code>. */
  public static final String KEY_COMPRESS_ROOT = "Root";



/*
  public static int
	CC_w=0, CC_J=1, CC_j=2, CC_M=3, CC_d=4, CC_ri=5, CC_i=6, CC_gs=7,
	CC_q=8, CC_Q=9, CC_cm=10,
	CC_m=11, CC_l=12, CC_c=13, CC_v=14, CC_y=15, CC_h=16, CC_re=17,
	CC_S=18, CC_s=19, CC_f=20, CC_F=21, CC_fstar=22, CC_B=23, CC_Bstar=24, CC_b=25, CC_bstar=26, CC_n=27,
	CC_W=28, CC_Wstar=29,
	CC_BT=30, CC_ET=31,
	CC_Tc=32, CC_Tw=33, CC_Tz=34, CC_TL=35, CC_Tf=36, CC_Tr=37, CC_Ts=38,
	CC_Td=39, CC_TD=40, CC_Tm=41, CC_Tstar=42,
	CC_Tj=43, CC_TJ=44, CC_squote=45, CC_=dquote=46,
	CC_d0=47, CC_d1=48,
	CC_CS=49, CC_cs=50, CC_SC=51, CC_SCN=52, CC_sc=53, CC_scn=54, CC_G=55, CC_g=56, CC_RG=57, CC_rg=58, CC_K=59, CC_k=60,
	CC_sh=61,
	CC_BI=62, CC_ID=63, CC_EI=64,
	CC_Do=65,
	CC_MP=66, CC_DP=67, CC_BMC=68, CC_BDC=69, CC_EMC=70,
	CC_BX=70, CC_EX=71
	;
*/
  private COS() {}

  /**
	Converts PDF rectangle array (llx lly urx ury) to Java Rectangle (ulx,uly, width, height), normalized to have positive width and height.
	Java rectangle in PDF coordinate space as described in passed AffineTransform.
  */
  public static Rectangle array2Rectangle(Object[] oa, AffineTransform at) {
//for (int i=0,imax=mb.length; i<imax; i++) System.out.println(mb[i]+"    "+mb[i].getClass().getName());
	Point2D p1 = new Point2D.Double(((Number)oa[0]).doubleValue(), ((Number)oa[1]).doubleValue()),
			p2 = new Point2D.Double(((Number)oa[2]).doubleValue(), ((Number)oa[3]).doubleValue());
	if (at!=null) { at.transform(p1,p1); at.transform(p2,p2); }
	//double x1=Math.max(p1.getX(), 0.0), y1=Math.max(p1.getY(), 0.0), x2=Math.max(p2.getX(), 0.0), y2=Math.max(p2.getY(), 0.0);  // hyper/2002/p76...#page=2 has y coord far into negative space
	double x1=p1.getX(), y1=p1.getY(), x2=p2.getX(), y2=p2.getY();  // hyper/2002/p76...#page=2 has y coord far into negative space
//System.out.println("("+x1+" "+y1+"), ("+x2+","+y2+")");
	if (x1>x2) { double tmp=x1; x1=x2; x2=tmp; }   // left
	if (y1>y2) { double tmp=y1; y1=y2; y2=tmp; }   // top (PDF space -- upside down in Java space)
	//assert x2-x1>=0 && y2-y1>=0: (x2-x1)+" "+(y2-y1);
	return new Rectangle/*2D?*/((int)x1,(int)y1, (int)Math.ceil(x2-x1),(int)Math.ceil(y2-y1));
  }

/*  public static Rectangle array2Rectangle(Object[] oa) {
	return array2Rectangle(oa, ctm_);
  }*/

  /**
	Returns Java time of PDF Date.
	PDF dates have the form <code>(D:20010406143021)</code>.
	@param local  convert time to local time
	@see #createDate(long)
  */
  public static long parseDate(StringBuffer sb) throws InstantiationException/*ParseException*/ {
	assert sb!=null; // && sb.length()/*==23*/>=4;    // "All fields after the year are optional. (The prefix D:, although also optional, is strongly recommended.)"
	if (pat_==null) pat_ = Pattern.compile("(?:D:)?(\\d\\d\\d\\d)(\\d\\d)?(\\d\\d)?(\\d\\d)?(\\d\\d)?(\\d\\d)?(.)?(\\d\\d')?(\\d\\d')?");

	String s = sb.toString();
	if (s.startsWith("D:191") && s.length()%2==1) s = "D:20" + s.substring("D:191".length());	// Y2K bug in Distiller 3.01 ("D:191001220180401" => "2000/12/20 10:04:01 am")
	Matcher m = pat_.matcher(s);
	if (!m.find()/*match()?*/) throw new InstantiationException/*ParseException*/("not a valid PDF date: ("+DATE_PATTERN+")");

	Calendar cal = getCalendar();

	long date = 0L;
	try {
	int gi=1;	// X gcnt=m.groupCount()+1 => number of capturing groups, regardless if all populated

	cal.set(Calendar.YEAR, Integer.parseInt(m.group(gi++)));
	if ((s = m.group(gi++)) != null) cal.set(Calendar.MONTH, Integer.parseInt(s) - 1);
	if ((s = m.group(gi++)) != null) cal.set(Calendar.DAY_OF_MONTH, Integer.parseInt(s));
	if ((s = m.group(gi++)) != null) cal.set(Calendar.HOUR_OF_DAY, Integer.parseInt(s));
	if ((s = m.group(gi++)) != null) cal.set(Calendar.MINUTE, Integer.parseInt(s));
	if ((s = m.group(gi++)) != null) cal.set(Calendar.SECOND, Integer.parseInt(s));

	String utc = m.group(gi++);
	int utcHH = 0; if ((s = m.group(gi++)) != null) utcHH = Integer.parseInt(s.substring(0,2)/*excluding trialing tick mark*/);
	int utcMM = 0; if ((s = m.group(gi++)) != null) utcMM = Integer.parseInt(s.substring(0,2));
	cal.setTimeZone(TimeZone.getTimeZone("GMT"));
	int off = (utc==null || utc.equals("Z")? 0: utc.equals("-")? -1: 1) * ((utcHH*60 + utcMM)*60) * 1000;	// if missing UTC is "undefined", so assume GMT
//System.out.println(m.group(7)+" "+m.group(8)+" => "+off);
	date = cal.getTimeInMillis() - off;	// long always in GMT

	} catch (NumberFormatException canthappen) {
		canthappen.printStackTrace();
	}

	return date;
  }

  /**
	Convert a Java time (since Jan 1 1970 GMT) into a PDF date in the current time zone.
	@see #parseDate(StringBuffer, boolean)
  */
  public static StringBuffer createDate(long javatimemillis) {
	Calendar cal = getCalendar(); cal.setTimeZone(TimeZone.getTimeZone("GMT"));

	int off = offsetZone_ + offsetDST_;
	cal.setTimeInMillis(javatimemillis + off);

	StringBuffer sb = new StringBuffer(DATE_PATTERN.length());
	sb.append("D:");
	int d;
	sb.append(cal.get(Calendar.YEAR));
	d=1+cal.get(Calendar.MONTH); if (d<10) sb.append('0');  sb.append(d);
	d=cal.get(Calendar.DAY_OF_MONTH); if (d<10) sb.append('0');  sb.append(d);
	d=cal.get(Calendar.HOUR_OF_DAY); if (d<10) sb.append('0');  sb.append(d);
	d=cal.get(Calendar.MINUTE); if (d<10) sb.append('0');  sb.append(d);
	d=cal.get(Calendar.SECOND); if (d<10) sb.append('0');  sb.append(d);

//System.out.println((utc/1000)+" "+(dst/1000));
	if (off == 0) sb.append('Z');
	else {
		sb.append(off<0? '-': '+'); off = Math.abs(off);
		int min = off / (1000 * 60);
		d = min / 60; if (d<10) sb.append('0');  sb.append(d).append('\'');
		d = d*60 - min;  if (d<10) sb.append('0');  sb.append(d).append('\'');
	}

	//assert sb.length() == DATE_PATTERN.length(): sb; -- can end in Z or OHH'mm'
	return sb;
  }

  private static Calendar getCalendar() {
	if (cal_==null) {
		cal_ = Calendar.getInstance();
		offsetZone_ = cal_.get(Calendar.ZONE_OFFSET); offsetDST_ = cal_.get(Calendar.DST_OFFSET);
	}
	cal_.clear();
	return cal_;
  }

  /**
	Computes value for trailer /ID, for initializing both if /ID doesn't exist or for updating the second value.
	Done on reads rather than writes so that guaranteed valid for subsequent operations.
	Returned StringBuffer 16 characters (8-bit bytes) long.
	<!-- would be in class COS but have to extract objects from <var>info</var>. -->
  */
  public static StringBuffer createID(URI uri, Dict info) {
	java.security.MessageDigest md5 = null;
	try { md5 = java.security.MessageDigest.getInstance("MD5"); } catch (java.security.NoSuchAlgorithmException builtin) {}
	assert md5.getDigestLength()>=16: md5.getDigestLength();

	// current time
	md5.update(new java.util.Date().toString().getBytes());     // .getBytes() encodes, which elsewhere in PDF we want to control, but here anything goes

	// pathname
	if (uri.getPath()!=null)
		md5.update(uri.getPath().getBytes());

	// length
	long length = "file".equals(uri.getScheme())? new File(uri.getPath()/*strip any fragment*/).length(): System.currentTimeMillis();
	for (int i=0; i<8; i++) { md5.update((byte)length); length>>=8; }

	// values of /Info --  doesn't matter if encrypted
	if (info!=null) for (Iterator<Object> i=info.keySet()/*should be values*/.iterator(); i.hasNext(); ) md5.update(((String)i.next()).getBytes());


	byte[] digest = md5.digest();
	StringBuffer sb = new StringBuffer(16);
	for (int i=0; i<16; i++) sb.append((char)(digest[i] & 0xff));

	assert sb.length() == 16: sb;
	return sb;
// /ID[<9b5f38229c72b7c64978cd23b524d341><9b5f38229c72b7c64978cd23b524d341>]
  }
}
