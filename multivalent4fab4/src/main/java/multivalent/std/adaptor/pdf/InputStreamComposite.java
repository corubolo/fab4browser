package multivalent.std.adaptor.pdf;

import java.io.*;

import phelps.util.Arrayss;
import phelps.lang.Objects;
import phelps.io.InputStreams;

import com.pt.io.RandomAccess;
//import com.pt.io.InputStreamRandomAccess;

import static multivalent.std.adaptor.pdf.COS.*;



/**
	Merges possibly multiple component {@link java.io.InputStream}s,
	possibly with encodings such as Flate and ASCIIHex,
	possibly with a Predictor (on LZW or Flate),
	possibly encrypted.
	Does not expand out image-specific filters (DCT, FAX, JBIG2).
	Allows one character of {@link #unread(int)} and {@link #peek()}.

	@see DecodeASCIIHex
	@see DecodeRunLength
	@see com.pt.io.InputStreamASCII85
	@see com.pt.io.InputStreamLZW
	@see java.util.zip.InflaterInputStream
	@see DecodePredictor

	@version $Revision: 1.30 $ $Date: 2005/03/22 04:58:19 $
*/
public class InputStreamComposite extends /*Filter?*/InputStream {
  static final boolean DEBUG = false;

  private static final String[] FILTER_SELFDELIMITING = { "FlateDecode", "Fl", "LZWDecode", "LZW", "ASCIIHexDecode", "AHx", "ASCII85Decode", "A85", "RunlengthDecode", "RL" };
  private static final int INVALID_CHAR = -2;	// -1 is EOF

  private PDFReader pdfr_;
  private Object[] sub_;
  private int subi_ = -1;
  private InputStream in_ = InputStreams.DEVNULL;
  private int pushch_ = INVALID_CHAR;
  private boolean isContent_, didBogusSpace_ = true;	// commands split over substreams have implicit separating whitespace


  /**
	Returns stream of buffered, decrypted, uncompressed data; image filters not processed.
	Referenced stream dictionary (via IRef, Object[], or direct {@link Dict}) can supply inline data by setting {@link COS#STREAM_DATA} key with byte[].
	@param obj  can be {@link COS#CLASS_DICTIONARY} for stream with filters, {@link IRef} to stream or array or streams, Object[] of IRef's to stream, or {@link COS#CLASS_DATA} for final data requiring no filters.
	@param iscontent    declare whether stream corresponds to page contents, and if so insert extra space between each pair of substreams, to handle corner case where concatenated streams don't have such space and would concatenate commands or arguments.
  */
  public InputStreamComposite(Object obj, boolean iscontent, PDFReader pdfr) throws IOException {
	Class cl = obj.getClass();
	assert obj!=null && (CLASS_ARRAY==cl || CLASS_IREF==cl || CLASS_DATA==cl || CLASS_DICTIONARY==cl): "unknown stream type: "+(obj!=null? cl.getName(): null);
	assert pdfr!=null;

	isContent_ = iscontent;
	pdfr_ = pdfr;

	//System.out.println(o+" in "+cl.getName());
	Object sub;
//System.out.println("ISC "+obj);
	if (CLASS_ARRAY==cl) {	// composite: array of streams
		sub = obj;

	} else if (CLASS_IREF==cl) {
		Object ant = pdfr_.getObject(obj);
		if (CLASS_ARRAY==ant.getClass()) {	// array of streams
			sub = ant;
		} else { assert CLASS_DICTIONARY==ant.getClass();
			Dict dict = (Dict)ant;
			Object o = "ObjStm".equals(pdfr.getObject(dict.get("Type")))? dict.get("Extends"): null;
//System.out.println("IRef extends "+o);
			sub = o==null? obj: new Object[] { obj, o };	// 'obj' not 'ant' -- keep IRef for decrypting.  order as 'obj' then 'o' to get new objects first, which aren't overridden
			sub = obj;
		}
//if (DEBUG) System.out.println("single stream: "+obj+" => "+pdfr_.getObject(o));

	} else if (CLASS_DATA==cl) {	// some client supplied content, wants to call peek(), unread(), pdf_.readToken()
		Dict fake = new Dict(5);
		fake.put(STREAM_DATA, obj);
		sub = fake;

	} else { assert CLASS_DICTIONARY==cl: cl;
		Dict dict = (Dict)obj;
		Object o = "ObjStm".equals(pdfr.getObject(dict.get("Type")))? dict.get("Extends"): null;
		sub = o==null? obj: new Object[] { obj, o };
	}

	if (CLASS_ARRAY==sub.getClass()) {
		Object[] oa = (Object[])sub;
		sub_ = oa.length>0? (Object[])oa.clone(): oa;	// Ghostscript 6.53 has 0-length
	} else { sub_ = new Object[] { sub }; }	// everybody gets fresh Object[] so we can null out references and help gc

	nextStream();	// X don't want IOException on instantiation => FileInputStream construtor throws exception too
  }


  /** Pushes a new <code>InputStream</code>, reads from it until exhausted, then returns to point at which stream was pushed.
  public void pushStream(InputStream in) {
	// switch sub_/subi_ to go from end down to 0/-1 so can push to end, when allocat sub_ leave from for a couple pushes
  } */


  /**
	Returns next character without advancing file position pointer, so that next peek() or read() returns same character at same position.
	Side effect: pushback character is set.
  */
  public int peek() throws IOException { if (INVALID_CHAR==pushch_) pushch_=read(); return pushch_; }

  /**
	Pushes back one character of stream, so that the next read() will return it.
  */
  public void unread(int c) {
	assert pushch_==INVALID_CHAR: "already have a pushback";
	assert c>=-1 && c<=255: "pushed back "+c+" not -1 (EOF) or in 0..255";

	pushch_ = c;
  }


  public int read() throws IOException {
	int c;

	if (pushch_!=INVALID_CHAR) { c=pushch_; pushch_=INVALID_CHAR; }
//	else if (ra_==null) c=INVALID_CHAR;	// repeated reads at end of file (substreams should handle this)
	else if (/*in_==null ||*/ (c=in_.read())==-1) {
		if (isContent_ && !didBogusSpace_) { c=' '; didBogusSpace_=true; }
		else if (subi_+1 < sub_.length/*exists a next stream*/) { nextStream(); c=/*in_.*/read(); }	// must recurse in case have 0-length stream and immediately get -1 again
	}

	return c;
  }

  public int read(byte[] b, int off, int len) throws IOException {
	assert b!=null && off>=0 && len>=0 && len+off <= b.length: "b="+b+", off="+off+", len+off="+(len+off)+" vs "+b.length;
//System.out.print("read(byte[], "+off+", "+len+") on "+in_.getClass().getName()+":  ");

	if (len==0) return 0;
	if (pushch_!=INVALID_CHAR) { b[off]=(byte)pushch_; pushch_=INVALID_CHAR; /*off++; len--;*/ return 1;/*rarely mix block read and peek()*/ }

	int cnt = in_.read(b, off, len);	// streams already buffered in ByteArrayInputStream
	if (cnt==-1) {
		if (isContent_ && !didBogusSpace_) { b[off] = (byte)' '; didBogusSpace_=true; return 1; }
		else if (subi_+1 < sub_.length /*is a next stream*/) { nextStream(); return /*in_.*/read(b, off, len); }	// must recurse in case have 0-length stream and immediately get -1 again
	}

	return cnt;
  }


  private void nextStream() throws IOException {
//System.out.println("nextStream(), subi_="+subi_+", len="+sub_.length);
	if (subi_+1 >= sub_.length) return;	// don't close final stream -- do that with InputStreamComposite.close()

	// close up old, if any
	if (subi_ >= 0) { in_.close(); sub_[subi_]=null; }	// null out reference because may have ByteArrayInputStream with big byte[] that's not freed on close()

	Object ref = sub_[++subi_];
	boolean fdecrypt = false;
	didBogusSpace_ = false;	// resets when coming back to stream after pushing XObject Form, which is OK

	Dict dict = (Dict)pdfr_.getObject(ref);
//System.out.println("nextStream ref = "+ref+" / "+ref.getClass().getName()+" = "+dict.get(STREAM_DATA));
if (DEBUG) System.out.println("open new substream "+dict);
	boolean fextern = dict.get("F")!=null;

	InputStream is;
	Object data = dict.get(STREAM_DATA);
	if (/*dict.get("Length")==null--not stream w/o /Length ||*/ data==null) {	// Panda v0.2
		is = null;

	} else if (CLASS_DATA==data.getClass()) {	// data inline
		byte[] buf = (byte[])data;
		is = buf.length>0? new ByteArrayInputStream(buf): null;
//System.out.println("inline data, len="+buf.length);
		// don't decrypt as either already decrypted or not from PDF file

	} else if (fextern) {
multivalent.Meta.sampledata("external file");
		InputStream exis = pdfr_.getFileInputStream(dict.get("F"));
		is = (exis instanceof BufferedInputStream? exis:	// embedded -- and already decypted
			new BufferedInputStream(exis, 8*1024));	// file or network -- and not encrypted
		//fwrap = false;?

	} else { assert data instanceof Number: data;	// data at given offset in file
//if (pdfr_.getRA()==null || dict.get("Length")==null) { System.out.println("null: "+data+" "+" "+pdfr_.getRA()+" "+dict.get("Length")); System.exit(1); }
		RandomAccess ra = pdfr_.getRA();
		long off = ((Number)data).longValue();
		int len = pdfr_.getObjInt(dict.get("Length"));
		// read an extra byte, in case /Length too short, as done by pdfFactory Pro v1.53 -- but only if stream is self-delimiting
		Object o = pdfr_.getObject(dict.get("Filter"));
		if (o!=null && len>0) {
			if (CLASS_ARRAY != o.getClass()) o = new Object[] { o };
			for (Object fil: (Object[])o) if (Arrayss.indexOf(FILTER_SELFDELIMITING, fil)>=0) { len++; break; }
		}

		// streams (1) not reliant on RA so can jump to other objects (images, forms), (2) buffered so don't need BufferedInputStream wrapper
		if (ra==null) {	// characters in Type3 fonts getting re-encoded
			is = null;	//throw new IOException("RA closed");

		} else if (len==0) {
			is = null;	// StarOffice 7 (OpenOffice DevelopersGuide.pdf) has 0-length, which InflaterStream doesn't like

/*		} else if (len > 100*1024) {	// incremental from file
//System.out.println("incremental through ISSlice @ "+off+" on "+f);
			is = new phelps.io.InputStreamSlice(new InputStreamRandomAccess(f), off, len); -- need new file descriptor in case seek before fully read
*/
		} else {
			byte[] buf = new byte[len];
			ra.seek(off);
			ra.readFully(buf);
//System.out.println("read fully @ "+data+", len "+len+": "+Integer.toHexString(buf[0])+" "+Integer.toHexString(buf[1])+" "+Integer.toHexString(buf[2])+"..."+Integer.toHexString(buf[len-1]));
			//eatSpace();	// could verify "endstream"
			//for (int i=0,imax="endstream".length(); i<imax; i++) if ("endstream".charAt(i) != ra.read()) { System.out.println("premature end of stream, obj #"+num); break; }
			is = new ByteArrayInputStream(buf);
		}
		fdecrypt = true;	// only decrypt when read from (1) this PDF, (2) for first time (or falls out of cache)
	}

	in_ = is!=null? wrapStream(is, ref, fdecrypt): InputStreams.DEVNULL;
  }

  private InputStream wrapStream(InputStream is, Object ref, boolean fdecrypt) throws IOException {	//-- also fextern for filter selection, buf (buf validity spot checks), ref for error reporting
//System.out.println(ref+" "+dp);
	// filters, possibly chained
	Dict dict = (Dict)pdfr_.getObject(ref);
	boolean fextern = dict.get("F")!=null;
	Object o = pdfr_.getObject(dict.get(fextern? "FFilter": "Filter"));
	Object dp = pdfr_.getObject(dict.get(fextern? "FDecodeParms": "DecodeParms")); if (dp==null) dp = pdfr_.getObject(dict.get("DP"));	// "DP" expanded by PDFReader but maybe had setExact(true)
	Object[] oa; if (o==null) oa=Objects.ARRAY0; else if (o.getClass()==CLASS_NAME) { oa=new Object[1]; oa[0]=o; } else oa=(Object[])o;
	Object[] dpa=new Object[oa.length]; if (dp==null || dpa.length==0) {} else if (dp.getClass()==CLASS_DICTIONARY) dpa[0]=(Dict)dp; else dpa=(Object[])dp;
	Encrypt e = pdfr_.getEncrypt();
	int objnum=-1, gennum=-1; if (CLASS_IREF==ref.getClass()) { IRef iref = (IRef)ref; objnum=iref.id; gennum=pdfr_.getObjGen(iref.id); }
	//assert oa.length == dpa.length;	// can be ok if not true: if don't have LZW/Flate after last dp

	// if no explicit CryptFilter on stream, use default (apply first in chain)
	if (fdecrypt && e!=null && e.getStmF()!=CryptFilter.IDENTITY && !fextern && CLASS_IREF==ref.getClass() && java.util.Arrays.asList(oa).indexOf("Crypt")==-1) {
		assert objnum!=-1 && gennum!=-1;
		//System.out.println("default encryption "+e.getStmF()+" for "+objnum);
		is = new CryptFilter(e.getStmF(), is, objnum,gennum);
	}

//System.out.println("object "+ref);
	for (int i=0,imax=oa.length; i<imax; i++) {
		String f = (String)pdfr_.getObject(oa[i]);	// .intern()
		o = pdfr_.getObject(dpa[i]); if (OBJECT_NULL==o) o=null;	// dict or null
		Dict parms = (Dict)o;
		boolean flzwflate=false;

///*if (DEBUG)*/ System.out.println("\tunwrap filter = "+f);
		// could use Reflection or Class.forName("pdf."+f); new Instance(args[]); but just a limited number (five) of known filters
		if (OBJECT_NULL==f || "None".equals(f)) {
			// nothing
		} else if ("FlateDecode".equals(f) || "Fl".equals(f)) {
			//assert (b0&0xf)==8 || i>0/*encoded by ASCII or encrypted*/: ref+" "+Integer.toHexString(b0);
			is = new java.util.zip.InflaterInputStream(is/*, parms, pdfr_*/);
			if (i+1<imax) // workaround for InflaterInputStream bug tickled by Java's JPEG decoder
				try { is = new ByteArrayInputStream(InputStreams.toByteArray(is)); } catch (IOException ioe) {}
			flzwflate = true;
		} else if ("LZWDecode".equals(f) || "LZW".equals(f)) {
			//assert b0==0x80 || i>0: ref+" "+Integer.toHexString(b0);
			int early = 1;
			if (parms!=null && (o=parms.get("EarlyChange"))!=null) {
				early = pdfr_.getObjInt(o);
				assert early==0 || early==1: early;
				if (DEBUG && early==0) System.out.println("early change = "+early);
			}
			is = new com.pt.io.InputStreamLZW(is, early==1);
			flzwflate=true;
		} else if ("BZip2Decode".equals(f)) {	// Compact stream
			is = new org.apache.tools.bzip2.CBZip2InputStream(is);
			flzwflate = true;

		} else if ("ASCIIHexDecode".equals(f) || "AHx".equals(f)) is = new DecodeASCIIHex(is);
		else if ("ASCII85Decode".equals(f) || "A85".equals(f)) is = new com.pt.io.InputStreamASCII85(is);
		else if ("RunLengthDecode".equals(f) || "RL".equals(f)) is = new DecodeRunLength(is);

		else if ("Crypt".equals(f)) {
			assert "CryptFilterDecodeParms".equals(parms.get("Type"));
			assert !fextern && objnum!=-1 && gennum!=-1;
			CryptFilter cf = e.getCryptFilter((String)parms.get("Name"));
			if (fdecrypt && cf!=CryptFilter.IDENTITY) is = new CryptFilter(cf, is, objnum,gennum);

		} else { assert "CCITTFaxDecode".equals(f) || "CCF".equals(f)
			|| "DCTDecode".equals(f) || "DCT".equals(f) || "JPXDecode".equals(f)
			|| "JBIG2Decode".equals(f): f;
			// no image decompression here -- done by Images
			assert i+1==imax: i+" < "+(imax-1);	// image better be last -- no image wrapping another image or LZW
		}

		// Predictor for Flate or LZW
		if (flzwflate && parms!=null && (o=parms.get("Predictor"))!=null) {
			int pred = pdfr_.getObjInt(o);
			//if (pred>=10 && pred<15) pred = is.read() + 10;	// >=10 "merely indicates PNG ... in use; the specific ... in incoming data" 
			//System.out.println("predictor = "+pred);
			if (pred!=1 && pred!=10) is = new DecodePredictor(is, pred, parms, pdfr_);
		}
	}

	return is;
  }


  // looks like a FilterInputStream, but not because doesn't take another InputStream and filter it... though could interpret broadly as filtering on File
  /** If no filter, true; else false. */
  public boolean markSupported() { return in_.markSupported(); }

  public synchronized void reset() throws IOException { in_.reset(); pushch_=INVALID_CHAR; }
  public synchronized void mark(int readlimit) { in_.mark(readlimit); }
  public void close() throws IOException {
	//assert in_!=null: "stream already closed"; => done by java.awt.Font.createFont()
	if (in_!=null) { in_.close(); in_=null; /*not ra.close()*/ sub_=null; pdfr_=null; }	// allow multiple calls to close
  }
  public int available() throws IOException { return in_.available() /*+ (pushch_==INVALID_CHAR? 0: 1)*/; }
  public long skip(long n) throws IOException { return in_.skip(n); }
}
