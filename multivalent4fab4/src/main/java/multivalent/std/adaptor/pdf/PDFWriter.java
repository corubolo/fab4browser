package multivalent.std.adaptor.pdf;

import java.io.*;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.Observer;
import java.util.List;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.zip.*;
import java.util.Iterator;
import java.util.Arrays;
import java.text.NumberFormat;
import java.net.URI;

import phelps.lang.Integers;
import phelps.lang.Strings;
import phelps.lang.StringBuffers;
import phelps.util.Version;
import phelps.util.Arrayss;
import phelps.io.OutputStreams;

import com.pt.io.OutputUni;
import com.pt.io.OutputStreamTee;
import com.pt.io.RandomAccess;
import com.pt.awt.NFont;
import com.pt.awt.font.NFontType1;
import com.pt.doc.PostScript;

import static multivalent.std.adaptor.pdf.COS.*;
import multivalent.ParseException;



/**
	Write new PDF file from low-level data structures.

	<ul>
	<li>Constructors and file control:
		{@link #PDFWriter(OutputUni, PDFReader) new PDF based on existing}, {@link #PDFWriter(OutputUni, PDFReader, boolean) modify existing PDF incrementally}, 
		{@link #PDFWriter(OutputUni) new PDF from scratch}, 
		{@link #close()}
	<li>Low-level structure:
		{@link #getVersion()}, {@link #getTrailer()}, {@link #getCatalog()},
	<li>Object management:
		{@link #getObjCnt()}, {@link #setObjCnt(int)},
		{@link #setObject(int, Object)}, {@link #addObject(Object)},
		{@link #getObject(int, boolean)}, {@link #getObject(Object)}, {@link #getCache(int)}, {@link #readAllObjects()},
		{@link #getObjects()},
		{@link #getObjGen(int)}, {@link #setObjGen(int,int)}, {@link #getObjType(int)},
		{@link #addFilter(Dict, String, Object)}, {@link #removeFilter(Dict, String)},
		{@link #deflateStream(Dict, int)}, {@link #maybeDeflateData(byte[])}
	<li>Advanced object manipulation:
		{@link #objEquals(Object,Object)}, {@link #connected(Object)},
		{@link #refcnt()}, {@link #refcntRemove()}, {@link #renumber(int[])}, {@link #renumberRemove(int[])},
		{@link #resetPageTree(List)}, {@link #liftPageTree()},
		{@link #convertType1(int)}

	<li>Write:
		{@link #setCompress(boolean)}, 
		{@link #makeObjectStreams(int, int)}, <!-- writeBarrier, -->
		{@link #writeHeader()}, {@link #writeXref(Dict,int,long,  long[],int,int)} / {@link #writeXref(Dict,int,long,  long[],int[],int[])},
		{@link #writeObject(Object, int, int)} and {@link #writeObject(Object, int, int, boolean, Encrypt)} for top-level objects, 
		{@link #writeObject(Object, StringBuffer, boolean)} to content stream,
		<!--{@link #writeObject(Object, int,int, StringBuffer, boolean, Encrypt)} internal,-->
		{@link #writeCommandArray(Cmd[], boolean)}, {@link #writeInlineImage(Dict, byte[], StringBuffer)},
		{@link #writePDF(Observer)}, {@link #writePDF()}<!--, {@link #writeIncremental(Observer)},-->
		{@link #writeFDF()}<!--, {@link #writeReader()}-->
	</ul>


	<h3>How to use this class</h3>

	<p>Either start with an existing PDF or start from stratch with the appropriate construction.
	Then add or modify objects, as informed by the Adobe's PDF Reference.

	<p>When done, simply {@link #writePDF()}.
	Any objects not set in this class will be faulted in from the backing PDFReader, if any.
	Some useful PDF manipulations, such as replacing CCITT FAX images with JBIG2, just need to modify a few
	objects; for this the convenience method {@link #writePDF(Observer)} will invoke the caller before every object, at which time it can be modified
	before writing.

	<p>To encrypt, just set up a PDF encryption dictionary according to the PDF Reference (don't forget point to it in the trailer).
	See {@link tool.pdf.Encrypt} for an example.
	If there is a backing PDF then by default the PDF to be written inherits it encryption settings, if any.
	Encryption must be set up before writing ({@link #writePDF()} or {@link #writeHeader()}).

	<p>To write in Compact format, just set up dictionary according to the
	<a href='http://multivalent.sourceforge.net/Research/CompactPDF.html'>Compact PDF Specification</a>.

	<p>For greater control, as for PDF concatenation and excerpting and other applications, write parts separately:
	{@link #writeHeader()}, {@link #writeObject(Object, int, int)} for each object, {@link #writeXref(Dict,int,long,  long[], int, int)}

	<pre>
	PDFWriter pdfw = new PDFWriter(...);
	pdfw.writeHeader();
	pdfw.writeObject();
	// ...
	pdfw.close();
	</pre>

	<p>Notes
	<ul>
	<li>Newer PDF generators, such as AdlibDocSys, don't fully understand PDF and write buggy documents.
		This class used to reuse delected object numbers, but AdlibDocSys is buggy, so we take this out.
		Besides, according to the PDF Reference 1.6, "Acrobat 6.0 and later do not use the free list to recycle object numbers; new objects are assigned new numbers."
	</ul>

	@version $Revision: 1.72 $ $Date: 2005/07/21 16:03:02 $
*/
public class PDFWriter extends COSSource {
  static final Map<String,String> INLINE_ABBREV = PDFReader.INLINE_ABBREV;

  public static final int PDFOBJ_OVERHEAD = "n m obj\n\nendobj\n".length();	// don't count "n m R " because shared
  public static final int PDFOBJREF_OVERHEAD = " n m R".length();

  private static final int FLATE_OVERHEAD = "/Filter/FlateDecode".length();
  private static final int PREDICT_XREF_OVERHEAD = "/DecodeParms<</Predictor 12/Columns 5>>>>".length();
  private static final int OBJSTMC_MAX = 200;	// same as Acrobat 6.0's limit for non-linearized
  private static final int PAGES_MAX = 20;

  // pretty printing orders dictionary keys
  private static final String[] DICT_KEYS_START = { "Type","Subtype",  "BaseFont","Encoding",  "Filter","Width","Height" };
  private static final String[] DICT_KEYS_END = { "Parent","Count", "Length", "Border","BS" };
  private static final String[] DICT_KEYS_SKIP = new String[DICT_KEYS_START.length + DICT_KEYS_END.length];
  static {
	System.arraycopy(DICT_KEYS_START,0, DICT_KEYS_SKIP,0, DICT_KEYS_START.length);
	System.arraycopy(DICT_KEYS_END,0, DICT_KEYS_SKIP,DICT_KEYS_START.length, DICT_KEYS_END.length);
	Arrays.sort(DICT_KEYS_SKIP);
  }

  /** Floating point formatter that matches PDF limits. */
  public /*private--PostScript*/ static final NumberFormat NF = NumberFormat.getInstance(java.util.Locale.US);
  static {
	NF.setMinimumFractionDigits(0);
	NF.setMaximumFractionDigits(5);		// matches Acrobat 5.0's implementation limit
	NF.setGroupingUsed(false);
	// + no scientific notation
  }

  // for pretty printing content streams
  private static final Set<String>
	BREAK_BEFORE = new HashSet<String>(java.util.Arrays.asList("BT ET  q Q BMC BDC EMC BX EX  BI".split(" +"))),
	BREAK_AFTER = new HashSet<String>(java.util.Arrays.asList("BT ET  q Q  Tj TJ ' \"  s S f f* F b b* B B* n  BMC BDC EMC BX EX  Do".split(" +")));	// not 'W' or 'W*' as followed by 'n'
 /* "DP MP" ? */

  /** Information page for non-Compact-aware applications. */
  private static final /*const*/ byte[] COMPACT_PAGE1 = Strings.getBytes8(
	"BT\n100 792 Td 0 -50 TD  /F1 12 Tf\n"
	+ "(Compact format was used on this PDF in order to save space.) '\n"
	+ "(See "+multivalent.Multivalent.HOME_SITE+") '\n"	//  for viewer and tools to convert to standard PDF.
	+ "ET");


  // INSTANCE VARIABLES
  /** Nominal File to written to. */
  private URI uri_ = null;
  /** Actual output written to, which is different than nominal iff nominal output is same as input. */
  private OutputStreamTee w_;	// maybe create on demand at writeBarrier(), but want to fail fast if bad
  /** If overwriting base, first write to tmp file. */
  private File outfile_ = null;
  /** Base PDF, if any. */
  private PDFReader pdfr_;

  private boolean fcompress_ = true;
  private boolean fmonitor_ = false;
  private boolean fexact_ = false;
  private long startxref_ = -1L;	// exactly one "startxref" written at close
  private boolean finc_ = false;
  private String password_ = "";

  private Encrypt encrypt_ = null;
  /**
	Objects to write plainly, not to compress (not in Compact stream and not /FlateDecode and not ObjStm): 
	/Catalog, /Info, /Metadata, already /FlateDecode (some image raw samples) so not in Compact. 
  */
  private boolean[] fplain_ = null;
  /** Objects not to encrypt: /Encrypt itself. */
  private List<Object> noencrypt_;

  /**
	Primary data structure: array of objects.
	If object has been instantiated objCache_[obj_id] != null (OBJECT_NULL ok).
	Object 0 is not a real object, so objCache_[0] = null always.
	So most iterations go <code>1 .. {@link #getObjCnt()}-1</code>.
  */
  private Object[] objCache_;	// if entry is null, take from shadow
  private short[] objGen_;
  private int[] renum_;	// track renumberings.  renum_[objnum] = original position
  private int objcnt_;	// different than objCache_.length so can add objects without constantly reallocating array

  private Version version_ = new Version("1.2");	// min 1.2 = /Flate, hex in /Name's
  private Dict trailer_;

  private boolean fbulk_ = false;



  /**
	Creates a new PDF based on an existing PDF.

	Modify based on an existing PDF.
	If the source PDF is encrypted, it should have its password set before passing to PDFWriter.
	Objects are shared between this object and its backing PDFReader,
	so mutated objects here are mutated in PDFReader too.

	Writes PDF to <var>ou</var>, with unmutated objects taken from <var>base</var>, 
	writing incrementally iff <var>incremental</var> is <code>true</code> or else to a new file.
	To write to a pipe (that is, a PDFReader) pass <code>null</code> for <var>ou</var>.

	<p>The output is allowed to be the same as the input <var>base</var> if writing to a {@link java.io.File}.
	<!-- in which case the output is written to a temporary file, then renamed. -->
  */
  public PDFWriter(OutputUni ou, PDFReader base, boolean incremental) throws IOException {
	if (ou==null) ou = OutputUni.DEVNULL;
	assert /*ou!=null &&*/ ou.getURI()!=null;
	assert base!=null;	// null => use other constructor

	// copy over metadata.  (objects loaded on demand, if at all)
	URI uri = uri_ = ou.getURI();
	finc_ = incremental;
	PDFReader pdfr = pdfr_ = base;
	new Normalize(pdfr).normalizeDoc(pdfr.getCatalog());	// rely on normalizations
	getVersion().setMin(pdfr.getVersion());

	// fast fail on can't write, before object reading and manipulation
	if (!finc_ && uri.equals(pdfr.getURI())) {
		if ("file".equals(uri.getScheme())) {
			outfile_ = File.createTempFile("pdfw", ".pdf", new File(uri).getParentFile());	// create in same directory so rename doesn't have to copy
			w_ = OutputUni.getInstance(outfile_, uri).getOutputStream(finc_);
		} else throw new IOException("can't overwrite "+pdfr.getURI()+" with protocol "+uri.getScheme());
	} else w_ = ou.getOutputStream(finc_);
//System.out.println("outfile_ = "+outfile_+" in "+file_.getParentFile());

	// cache
	objcnt_ = pdfr.getObjCnt();
	objCache_ = new Object[objcnt_ + 20/*room to grow*/]; objGen_ = new short[objCache_.length]; renum_ = Arrayss.fillIdentity(new int[objCache_.length]);
	objCache_[0] = OBJECT_DELETED; for (int i=0,imax=objcnt_; i<imax; i++) objGen_[i] = (short)pdfr.getObjGen(i);

	// trailer
	trailer_ = new Dict(pdfr.getTrailer());
	IRef iref = (IRef)trailer_.get("Root");
	objCache_[iref.id] = new Dict(pdfr.getCatalog());	// normalizes
	Dict info = pdfr.getInfo();	// normalizes
	//if (!fexact_ && ou != OutputUni.DEVNULL) {
	if (ou != OutputUni.DEVNULL) {	// DEVNULL => client just wants to manipulate objects of PDFReader, as for tool.pdf.Diff
		Object[] ID = (Object[])pdfr.getObject(trailer_.get("ID"));	// update /ID, which is guaranteed to exist by Normalize
		ID[1] = createID(pdfr.getURI(), pdfr.getInfo());	// different version
		if (info==null) { info = new Dict(5); addObject(info); }	// X don't create Info if doesn't exist: if PDFReader fexact then shouldn't and else already created
		//info.put("ModDate", createDate(System.currentTimeMillis()));	// X do this at start so client can modify => rewrites that don't modify content (Compress, Encrypt) shouldn't be considered modifications
	}
  }

  //** Incrementally writes additions to existing PDF. */
  /** Convenience for <code>new PDFWriter(<var>out</var>, <var>base</var>, false). */
  public PDFWriter(OutputUni ou, PDFReader base) throws IOException {
	this(ou, base, false);
  }

  //** Convenience for <code>new PDFWriter(InputUni.getInstance(<var>file</var>, null, null), <var>base</var>, <var>incremental</var>)</code>. */
  public PDFWriter(File file, PDFReader base, boolean incremental) throws IOException {
	this(OutputUni.getInstance(file, null), base, incremental);
  }


  /**
	Creates a PDF from scratch.
	Automatically creates ID in trailer and Catalog and Info dictionaries.
	Existing output file, if any, is overwritten.
  */
  public PDFWriter(OutputUni ou) throws IOException {
	if (ou==null) ou = OutputUni.DEVNULL;
	URI uri = uri_ = ou.getURI();
	w_ = ou.getOutputStream(false);	// fail fast on file-writable check -- up to user to check for overwriting
	pdfr_ = null;
	//getVersion().setMin(1,4);

	// cache
	objcnt_ = 0+1;	// special object 0
	objCache_ = new Object[1000]; objGen_ = new short[objCache_.length]; renum_ = Arrayss.fillIdentity(new int[objCache_.length]);
	objCache_[0] = OBJECT_DELETED; objGen_[0] = (short)GEN_MAX;

	// trailer
	trailer_ = new Dict(5);

	IRef iref;
	Dict cat = new Dict(5); iref = addObject(cat); trailer_.put("Root", iref);
	cat.put("Type", "Catalog");

	Dict info = new Dict(5); iref = addObject(info); trailer_.put("Info", iref);
	info.put("Producer", new StringBuffer("Multivalent "+multivalent.Multivalent.VERSION));
	info.put("CreationDate", createDate(System.currentTimeMillis()));

	Object[] ID = new Object[2]; ID[0] = ID[1] = createID(getURI(), info); trailer_.put("ID", ID);
  }

  /** Creates a PDF from scratch, written to <var>file</var>. 
  public PDFWriter(File file) throws IOException {
	this(OutputUni.getInstance(file, null));
  }*/


  /**
	If false (the default), unpacks objects from objects streams and report object streams themselves as {@link COS#OBJECT_NULL}.
	If true, reports objects as seen from backing {@link PDFReader}, with objects inside object streams given as a {@link java.lang.Long} giving their index within the stream.
	@see PDFReader#setExact(boolean)
  */
  public void setExact(boolean b) { fexact_ = b; }

  /** Provide either owner or user password for encryption, if any. */
  public void setPassword(String password) { password_ = password; }

  public PDFReader getReader() { return pdfr_; }

  public URI getURI() { return uri_; }

  /** For expert use in special cases. */
  public OutputStreamTee getOutputStream() { return w_; }	// getCount(), tool.pdf.Uncompress writes comments


  // METADATA
  public Version getVersion() { return version_; }

  /** Document trailer.  To change the trailer, such as the /ID array, mutate this object. */
  public Dict getTrailer() { return trailer_; }	// besides ID nothing user can usefully modify => need as root to all objects

  /** Returns document <code>/Catalog</code>. */
  public Dict getCatalog() throws IOException { return (Dict)getObject(getTrailer().get("Root")); }
  public Dict getInfo() throws IOException { return (Dict)getObject(getTrailer().get("Info")); }



  // OBJECT MANAGEMENT

  /** Returns number of objects. */
  public int getObjCnt() { return objcnt_; }

  /**
	Truncate object list or allocate space for more ({@link #addObject(Object)} automatically allocate space as needed too).
  */
  public void setObjCnt(int newcnt) {
	assert newcnt>=1: newcnt;

	if (newcnt > objCache_.length) {
		int newcapacity = Math.max(newcnt, objCache_.length + 200);
		objCache_ = Arrayss.resize(objCache_, newcapacity);
		objGen_ = Arrayss.resize(objGen_, newcapacity);
		renum_ = Arrayss.resize(renum_, newcapacity);
	} else if (newcnt < getObjCnt()) {
		Arrays.fill(objCache_, newcnt, getObjCnt()-1, null);	// null out for gc
		// don't resize -- use rest of array as expansion room
	}

	objcnt_ = newcnt;
  }

  /**
	Adds object by reusing number of deleted object if possible, or else appending to end.
	@return object number assigned
  */
  public IRef addObject(Object obj) {
	assert obj!=null;
	//assert fplain_==null;	// write barrier => but don't enforce.  makeObjectStreams adds objects

	int posn;
	posn = objcnt_; setObjCnt(posn+1);
	setObject(posn, obj);
	return new IRef(posn, getObjGen(posn));	// gen not 0 when reuse, and Acrobat is picky about generation numbers
  }

  /**
	Set an object to null to take from base PDFReader.
	Do not use {@link IRef}s as top-level objects.
	Do not set an object beyond {@link #getObjCnt()}, exclusive; first extend the set of objects with {@link #setObjCnt(int)}.
	To delete an object, set it to {@link COS#OBJECT_DELETED}.
	<!-- No bulk replacement of all objects; setObjCnt() then mutate. -->
  */
  public void setObject(int num, Object obj) {
	assert num>0 && num<getObjCnt(): num;
	assert obj!=null && !(obj instanceof IRef): obj;

	//if (num >= getObjCnt()) setObjCnt(num+1); => NO, consider this an error

	objCache_[num] = obj;
	renum_[num] = num;
  }

  /**
	Objects are read on demand from the backing PDFReader, if any.
	If object is a stream, the stream content is read in, uncompressed, and stored under the dictionary key {@link COS#STREAM_DATA}.
	Objects typically requires about 10 times as many bytes in memory as on disk.
	Valid object numbers are 0 .. {@link #getObjCnt()}-1.
	@param fcache   true if should object be cached
  */
  public Object getObject(int objnum, boolean fcache) throws IOException {
	assert objnum>=0 && objnum<getObjCnt(): objnum;
	assert objCache_[0] == OBJECT_DELETED;

	Object o = getCache(objnum);
	if (o==null) try {
		//if (pdfr_==null) throw new IOException("fetching non-existent object");
		o = pdfr_.getObject(objnum);	// cache in PDFReader too so can use its query functions => now PDFReader always caches with SoftReference
		if (fexact_) {	// keep ObjStm component objects in stream
			if (XREF_OBJSTMC == pdfr_.getObjType(objnum)) o = new Long(pdfr_.getObjOff(objnum));
		} else {	// extract objects from ObjStm, but supress ObjStm itself
			if (CLASS_DICTIONARY==o.getClass() && "ObjStm".equals(((Dict)o).get("Type"))) o = OBJECT_DELETED;	// can still get component objects from PDFReader
			else if (XREF_OBJSTMC == pdfr_.getObjType(objnum)) { /*objTyp_[objnum]=XREF_NORMAL -- type computed from object itself*/ objGen_[objnum] = 0; }
		}
		if (fcache) objCache_[objnum] = o;
		assert o!=null: "null @ "+objnum;

		// if stream, read in data
		if (/*fcache &&*/ CLASS_DICTIONARY == o.getClass()) {
			// raw samples considered format of type /FlateDecode just like /DCTDecode, in order to save space
			//Dict dict = (Dict)o;
			//boolean fraw = "Image".equals(dict.get("Subtype")) && "FlateDecode".equals(dict.get("Filter")) && dict.get("DecodeParms")==null && getEncrypt()==null/*.getStmF()==CryptFilter.IDENTITY*/;
			//fraw = false;

//System.out.println("#"+objnum+": "+o);
			// want to be able to scan objects cheaply without dragging in data, but by time go to fetch data may have renumbered object
			//if (dict.get(STREAM_DATA) instanceof Number) LATER: raw data decoded on demand
			/*byte[] data =*/ pdfr_.getStreamData(new IRef(objnum, pdfr_.getObjGen(objnum)), false, true);
			//assert !"FlateDecode".equals(dict.get("Filter")): objnum+" "+dict; => /Filter from Compact

			// if samples were encrypted or wrapped in LZW or ASCII, postprocess to FlateDecode
			/*if ("Image".equals(dict.get("Subtype")) && dict.get("Filter")==null) {
				// compare Adobe Flate to Java Flate, as on rare occasion Adobe Flate with its multiple blocks is better,
				// but on other occasions Java Flate is better, and often they're the same
				byte[] smalldata = maybeDeflateData(data);
				if (smalldata != data) { dict.put(STREAM_DATA, smalldata); dict.put("Filter", "FlateDecode"); }
			}*/
		}

	} catch (IOException ioe) {
		System.err.println(ioe+" while reading object #"+objnum+": "+o);
		// +", boom @ byte #"+boom+", a stream encoded "+fo+", dictionary = "+stream+": "+ioe);
		throw ioe;
	}

	return o;
  }

  /**
	Find <var>obj</var> among instantiated objects and return its index number.
	@return -1 if not found.
  */
  /*public*/ int getObjIndex(Object obj) throws IOException {
	for (int i=0+1, imax=getObjCnt(); i<imax; i++) if (obj == getCache(i/*,false?--already loaded for ==!*/)) return i;
	return -1;
  }

  /** Low-level retrieval from table of instantiated objects: if object is not present, <code>null</code> is returned. */
  public/*private?*/ Object getCache(int objnum) {
	assert objnum>=0 && objnum<getObjCnt(): objnum;
	return objCache_[objnum];
  }

  public Object getObject(Object ref) throws IOException {
	//assert ref!=null;	// PDF objects never null, but convenient to resolve non-existent dictionary key-value
	/*while*/if (ref!=null && CLASS_IREF==ref.getClass()) ref = getObject(((IRef)ref).id, true/*safest to pin*/);
	assert !(ref instanceof IRef);	// ref to ref?  never happens.
	return ref;
  }

  public byte[] getStreamData(Object obj/*, int objnum/*final Object ref, boolean fraw, boolean fcache*/) throws IOException {
	//obj = getObject(obj); => if IRef then objnum wrong
	if (obj==null || CLASS_DICTIONARY!=obj.getClass()) return null;
	Dict dict = (Dict)obj;
	Object o = dict.get(STREAM_DATA);

	byte[] data;
	if (o==null) data = null;
	else if (CLASS_DATA == o.getClass()) {
		//String filter = Images.getFilter(dict, this);
		//if (dict.get("Filter")==null 
		data = (byte[])o;
	} else if (o instanceof File) {	// read in file data now
		data = phelps.io.Files.toByteArray((File)o);
	//else if (o instanceof URI) ... external file?
	} else { data = null; assert false: o; }
	//else { assert o instanceof Number: o; data = pdfr_.getStreamData(new IRef(objnum, pdfr_.getObjGen(objnum)), false, true); } => may have renumbered object so can't get from pdfr_

	if (data!=null && data!=o) dict.put(STREAM_DATA, data);

	return data;
  }

  /**
	Read all remaining objects from backing PDFReader that have not already been read or set by {@link #setObject(int, Object)}.
	Same effect as invoking {@link #getObject(int, boolean)} on all objects in backing PDF.
  */
  public void readAllObjects() throws IOException {
	if (pdfr_ == null) return;

	if (fmonitor_) System.out.print("READ, "+objCache_.length+" objects"+"   ");

	long start = System.currentTimeMillis();
	for (int i=0+1,imax=/*pdfr_.*/getObjCnt(); i<imax; i++) {
		if (fmonitor_) { if (i%1000==0) System.out.print((imax-i)/1000); else if (i%500==0) System.out.print(" "); }	// count down
		Object obj = getObject(i, true);
		getStreamData(obj/*, i*/);	// data too
		assert objCache_[i] != null: i+" of "+imax;
	}
	if (fmonitor_) { System.out.println(); System.out.println((System.currentTimeMillis() - start) + " ms"); }

	getVersion().setMin(pdfr_.getVersion());
	//closeReader();	// content streams
  }

  /**
	Return array of all objects currently read in base PDFReader or explicitly set by client code.
	Usually callers want to first instantiate all objects by invoking {@link #readAllObjects()}.
	Invoking this method takes control of the objects from PDFWriter, and results in losing any deleted object chain.
  */
  public Object[] getObjects() {
	Arrays.fill(objGen_, 0+1, getObjCnt(), (short)0);
	// type computed
	return objCache_;
  }

  public void setObjGen(int objnum, int newgen) {
	assert objnum>=0 && objnum<getObjCnt(): objnum;
	newgen = Math.min(newgen, GEN_MAX);
	objGen_[objnum] = (short)newgen;
  }

  public int getObjGen(int objnum) {
	assert objnum>=0 && objnum<getObjCnt(): objnum;
	//return /*objnum==0? GEN_MAX:*/ deleted_!=null/*!*/ && pdfr_!=null && objnum < pdfr_.getObjCnt()? pdfr_.getObjGen(objnum): 0;
	//assert objGen_[0] == GEN_MAX;
	//return getObjType(objnum)==XREF_OBJSTMC? objGen_[objnum]: 0;
	//return objCache_[objnum]!=null && CLASS_OBJSTMC==objCache_[objnum].getClass()? objGen_[objnum]&0xffff: 0;
	return objGen_[objnum]&0xffff;
  }

  public byte getObjType(int objnum) throws IOException {
	assert objnum>=0 && objnum<getObjCnt(): objnum;
	Object obj = getObject(objnum, false);
	// rather than keeping a type array, determine type dynamically based on actual data.  less bookkeeping
	return OBJECT_DELETED==obj? XREF_FREE: CLASS_OBJSTMC==obj.getClass()? XREF_OBJSTMC: XREF_NORMAL;
  }


  /**
	Insert object (<var>name</var>, <var>val</var>) from name tree,
	and return its old value if any.
  * /
  public Object insertNameTree(Dict node, StringBuffer name, Object val) throws IOException {
  }
  /**
	Delete object <var>name</var> from name tree and return its old value.
  * /
  public Object deleteNameTree(Dict node, StringBuffer name) throws IOException {
  }
*/


// InputStreamComposite gets -- already iterates, Images reports image filter -- special check for any image,
// Decrypt and Uncompress zap /Crypt, PDFWriter adds /Crypt /Identity, Encrypt gets
  /** Prepends filter to stream. */
  public void addFilter(Dict stream, String filter, Object parms) throws IOException {
	Object fo = getObject(stream.get("Filter")), dpo = getObject(stream.get("DecodeParms")); if (dpo==null) dpo=OBJECT_NULL;
	Object nf, ndp;
	if (fo==null) {
		nf = filter; ndp = parms;
	} else if (CLASS_NAME==fo.getClass()) {
		nf = new Object[] { fo, filter }; ndp = new Object[] { dpo, parms };
	} else { assert CLASS_ARRAY==fo.getClass();
		Object[] foa = (Object[])fo; int len = foa.length;
		Object[] nfa = new Object[len]; nfa[0]=filter; System.arraycopy(foa,0, nfa,1, len); nf = nfa;
		Object[] ndpa = new Object[len]; ndpa[0]=parms; if (dpo!=null) System.arraycopy(foa,0, ndpa,1, len); else Arrays.fill(ndpa,1,len, OBJECT_NULL); ndp = ndpa;
	}
	stream.put("Filter", nf); if (OBJECT_NULL!=ndp) stream.put("DecodeParms", ndp);
  }

  /** Removes filter and associated DecodeParms from stream. */
  public void removeFilter(Dict stream, String filter) throws IOException {
	Object o = getObject(stream.get("Filter"));
	if (o==null) {
	} else if (CLASS_NAME==o.getClass()) {
		if (o.equals(filter)) { stream.remove("Filter"); stream.remove("DecodeParms"); }
	} else { assert CLASS_ARRAY==o.getClass();
		Object[] oa = (Object[])o;
		for (int i=0,imax=oa.length; i<imax; i++) {
			if (oa[i].equals(filter)) {
				if (imax==1) { stream.remove("Filter"); stream.remove("DecodeParms"); }
				else {
					Object[] na = new Object[imax-1];
					System.arraycopy(oa,0, na,0, i); System.arraycopy(oa,i+1, na,i+1, imax-i); stream.put("Filter", na);
					if ((oa = (Object[])getObject(stream.get("DecodeParms"))) != null) {
						System.arraycopy(oa,0, na,0, i); System.arraycopy(oa,i+1, na,i+1, imax-i); stream.put("DecodeParms", na);
					}
				}
				break;
			}
		}
	}
  }


  /**
	If data would be smaller with Flate compression applied, apply it, set /Filter /FlateDecode and /Length, and return compressed data.
	If data would be larger, set /Length so don't try and fail to deflate twice, and return original data.
	@return null     if dictionary is not a stream
  */
  public byte[] deflateStream(Dict stream, int objnum) throws IOException {
	byte[] data = getStreamData(stream/*, objnum*/); if (data==null) return null;

	// prepare data first so know output length for inline /Length
	//if ("FlateDecode".equals(/*(String)--guaranteed no arrays?*/omap.get("Filter"))) {
	if (fcompress_ && stream.get("Filter")==null/*not image, which is already compressed*/ && stream.get("Length")==null	// already deflated
		&& data.length > FLATE_OVERHEAD + 25/*possible maximum savings to make it worth the trouble*/) {
//		&& (fplain_==null || objnum<0 || objnum>=fplain_.length/*Merge*/ || !fplain_[objnum])) { => not determined here
		byte[] smalldata = maybeDeflateData(data);
		if (smalldata != data) {
//System.out.println("\treplace "+data.length+" with "+smalldata.length);
			stream.put("Filter", "FlateDecode"); 
			stream.put(STREAM_DATA, smalldata);
			data = smalldata;
			//getVersion().setMin(1,2); -- too late, already wrote header
		}
//System.out.println("flate "+pdfr_.getObject(ra, stream.get("Length"))+"/"+data.length+" => "+bout.size()+" "+fBZip2_);
	}
	stream.put("Length", Integers.getInteger(data.length));	// set /Length before write stream

	return data;
  }


  /**
	Deflates <var>data</var>, if compressed size is smaller than original.
  */
  public static byte[] maybeDeflateData(byte[] data) throws IOException {
	ByteArrayOutputStream bout = new ByteArrayOutputStream(data.length);	// almost always more than long enough
	Deflater def = new Deflater(Deflater.BEST_COMPRESSION, false);
	//def.setStrategy(Deflater.FILTERED);	// worse ratios
	OutputStream zout = new DeflaterOutputStream(bout, def, 8*1024);
	zout.write(data);
	zout.close();
	def.end();
//System.out.println("flate "+data.length+" => "+bout.size());
	bout.close();

	return bout.size() + "/Filter/FlateDecode".length() + 10/*enough to make it worth the effort*/ < data.length? bout.toByteArray(): data;
  }


  // MANIPULATION

  /**
	Deep equality testing, recursing through arrays and dictionarys and <em>one level</em> of indirect references.
  */
  public boolean objEquals(Object o1, Object o2) {
//System.out.println("objEquals |"+o1+"|  *vs*  |"+o2+"|");
	if (o1==o2) return true;
	else if (o1==null || o2==null) return false;	// dict val vs non-existent in other
	Class cl=o1.getClass(), cl2=o2.getClass();
	/*else*/ if (cl == CLASS_IREF && cl2 == CLASS_IREF) {
		IRef or1=(IRef)o1, or2=(IRef)o2;
//if (or1.id>objs_.length || or2.id>objs_.length) System.out.println(or1.id+" or "+or2.id+" > "+objs_.length);
//if (objs_[or1.id]==null || objs_[or2.id]==null) System.out.println("bad iref: "+or1+"=>"+objs_[or1.id]+" and "+or2+"=>"+objs_[or2.id]);
		return getCache(or1.id) == getCache(or2.id);	// for this pass
		//else return objEquals(objs_[or1.id], objs_[or2.id]); => cycles
	} else if (cl == CLASS_IREF) return objEquals(getCache(((IRef)o1).id), o2);	// can't happen anymore, since not compared unless same class
	else if (cl2 == CLASS_IREF) return objEquals(o1, getCache(((IRef)o2).id));
	else if (cl!=cl2) return false;	// checked before start, but recurses
	else if (o1.equals(o2)) return true;	// numbers

	boolean match=true;
	if (CLASS_DICTIONARY == cl) {
		Dict oh1=(Dict)o1, oh2=(Dict)o2;
		match = oh1.size() == oh2.size();
		if (match) for (Iterator<Map.Entry<Object,Object>> i=oh1.entrySet().iterator(); match && i.hasNext(); ) {
			Map.Entry<Object,Object> e = i.next();	// "key must be a name"
			match = objEquals(e.getValue(), oh2.get(e.getKey()));
		}

	} else if (CLASS_ARRAY==cl) {
		Object[] oa1=(Object[])o1, oa2=(Object[])o2;
		match = (oa1.length == oa2.length);
		for (int i=0,imax=oa1.length; i<imax && match; i++) match = objEquals(oa1[i], oa2[i]);	// Arrays.equals() uses ==

	} else if (CLASS_STRING==cl) {	// few (String) objects
		StringBuffer sb1=(StringBuffer)o1, sb2=(StringBuffer)o2;
		match = sb1.length() == sb2.length();
		for (int i=0,imax=sb1.length(); i<imax && match; i++) match = sb1.charAt(i)==sb2.charAt(i);

	} else if (CLASS_DATA==cl) {	// stream data
		match = Arrays.equals((byte[])o1, (byte[])o2);

	} else match = false;

	return match;
  }


  /**
	Reference count PDF objects to see how many times (and if) an object is used.
	@return array such that array[objnum] = reference count.
  */
  public int[] refcnt() {
	int[] cnts = new int[getObjCnt()];
	// would like to iterate over all objects vs adding/removing to List, but that would count all objects even if not used!
	List<IRef> q = new ArrayList<IRef>(cnts.length/10);
	//if (CLASS_IREF==root.getClass()) q.add((IRef)root); else refcnt(cnts, root, q);
	refcnt(cnts, getTrailer(), q);
	while (!q.isEmpty()) {
		IRef iref = q.remove(q.size()-1);	// remove from end so don't have to shift
		//refcnt(cnts, getCache(iref.id), q);	// no IOException
		try { refcnt(cnts, getObject(iref), q); } catch (IOException shouldnthappen) {}	// getObject so don't depend on readAllObjects
	}
	return cnts;
  }

  private void refcnt(int[] cnts, Object o, List<IRef> q) {
	Class cl = o.getClass();

	if (CLASS_IREF==cl) {	// only way to reference!
		IRef iref = (IRef)o;
		int id = iref.id; assert 0<=id && id<=cnts.length: id+" > "+cnts.length;
		if (cnts[id]++ == 0/*don't cycle*/) q.add(iref);	// used to recurse here, but pathological case where single thread through entire document recurses on each bead and exhausts stack!

	} else if (CLASS_ARRAY==cl) for (Object oi: (Object[])o) refcnt(cnts, oi, q);

	else if (CLASS_DICTIONARY==cl) {
		for (Iterator<Object> i=((Dict)o).values().iterator(); i.hasNext(); ) refcnt(cnts, i.next(), q);

	} // else primitive type
  }

  /**
	Reference count and remove unused objects.
	@return number of unused objects
  */
  //public int refcntRemove(PDFWriter pdfw) { return refcntRemove(pdfw.getTrailer(), pdfw); }
  public int refcntRemove(/*Object root*/) {
	//assert freadfull;
//System.out.println("refCntRemove");

	int objcnt = getObjCnt();
	int[] refcnt = refcnt(/*root*/);	// gc root of all references
//System.out.println("REFCNT "+root);

	// object with linearizated information, if any, is zapped, but that's ok since made invalid anyhow

	int[] newnum = new int[objcnt];
//for (int i=1; i<objcnt; i++) if (refcnt[i]==0) System.out.println("X"+i+"  ");   System.out.println();
	for (int i=1; i<objcnt; i++) newnum[i] = (refcnt[i]==0? 0: i);

	renumberRemove(newnum);

//System.out.println("refcnt "+objcnt+"->"+getObjCnt());
	return getObjCnt() - objcnt;
  }



  /**
	Renumbers IRef's according to <var>newnum[]</var>
	by descending through object tree (rooted at <!--<var>o</var>, which initially is--> the Trailer),
	where new numbers can map to any number and no objects are removed from the object table.
	Caller retains responsibility to correctly number the objects (<tt><var>n</var> <var>g</var> obj ... endobj</tt>) on writing a new PDF.
  */
  public void renumber(int[] newnum) {
	int objcnt = newnum.length;
	IdentityHashMap<IRef,IRef> seen = new IdentityHashMap<IRef,IRef>(objcnt);	// other code shares IRefs, so prevent double renumbering

	renumber(newnum, getTrailer(), seen);	// trailer not in xref-numbered objects
	Object[] objs = getObjects();
	for (int i=1; i<objcnt; i++) {
		Object o = objs[i];
		//if (seen.get(o)==null) { seen.put(o,o); renumber(newnum, o); } -- may have seen bogus object, not the one to write out (as when moving up to make space for components?)
//System.out.print(" "+i);
//if ((o!=null && newnum[i]<=0) || (o==null && newnum[i]>0)) System.out.println("mismatch "+o+" vs "+newnum[i]);
		if (o!=null) renumber(newnum, o, seen);	// don't renumber o==o twice -- not idempotent
		//if (newnum[i] > 0) renumber(newnum, o);
	}

	short[] oldgen=objGen_, newgen=new short[objcnt];
	for (int i=1; i<objcnt; i++) {
		newgen[newnum[i]] = oldgen[i];
		renum_[newnum[i]] = i;	// reverse
	}
  }

  private void renumber(int newnum[], Object o, /*, List<IRef> q, boolean[] seen/*, /*Identity*/Map<IRef,IRef> seen) {	// mutate IRefs so can't rely on .equals()
	Class cl = o.getClass();
	if (CLASS_IREF==cl) {	// mutate
		IRef iref = (IRef)o;
//System.out.print(iref.id+"->"+newnum[iref.id]+" ");
//System.out.print("r");
//if (seen_.get(iref)!=null) System.out.println("shared iref "+iref);
		if (seen==null) iref.id = newnum[iref.id];
		else if (seen.get(iref) == null) {
			assert newnum[iref.id]>0 || OutputStreams.DEVNULL==w_/*reversing deleted object*/: iref.id+" => 0 0 R";
			iref.id = newnum[iref.id];
			seen.put(iref, iref);
			iref.generation = 0;	// PDFReader.getGen(int) invalid after renumbering, and have iref=>ref map in Compress.canonicalizeResources()
		}
		// do not recurse to IRef-referenced object

	} else if (CLASS_DICTIONARY==cl) {	// recurse
		///*if (seen!=null)*/ seen.put(o,o);	// can get multiple IRefs
		for (Iterator<Object> hi=((Dict)o).values().iterator(); hi.hasNext(); ) renumber(newnum, hi.next(), seen);

	} else if (CLASS_ARRAY==cl) {	// recurse
		for (Object oi: (Object[])o) renumber(newnum, oi, seen);
	}
  }


  /**
	Descends through object tree (rooted at Trailer), renumbering IRef's according to <var>newnum[]</var>
	and removing unused objects.
	Assumes that object numbers lie within 0..getObjCnt(),
	and that a renumbered object is obsolete and deleted in favor of the referenced object;
	thus some objects will always be removed or there was no use invoking this method.
	Shrinks the object tables by the number of unused objects, and renumbering is adjusted by moved object positions.

	@param newnum is mutated so that object id's point to positions in collapsed array
	@return offsets array updated to match new object numbers so, in addition to objs_, callers can update parallel arrays
		for (int i=0; i<objcnt; i++) { int moveto=i-offs[i]; array[moveto] = array[i]; }
	@see #renumber(int[])
  */
  public int[] renumberRemove(int[] newnum) {
	//assert freadfull;
	//assert newnum.length == objs_.length
	assert newnum[0] == 0: newnum[0];	// don't move object 0

	// throw out unused and update tables
	int cnt = 0;
	int objcnt = getObjCnt();	// = Math.min(getObjCnt(), newnum.length)?
	Object[] objs = getObjects(), newobjs = new Object[objcnt];

	int[] off = new int[newnum.length];
	for (int i=0; i<objcnt; i++) {
		// some remapping, even if newnum[i]==i, since crunching xref table too
		off[i] = cnt;
		//if (newnum[i]==0 || newnum[i]!=i) cnt++;

		if (newnum[i]==i) {	// same as before -- most common case
//System.out.print(i+"="+(i-cnt)+" ");
			//off[i] = cnt; -- ok if don't see trailing removed objects
			newobjs[i-cnt] = objs[i];
			//newnum[i] -= cnt;	// renumber to collapsed position => if moving higher, don't know cnt at that point until get there
		} else {	// assert newnum[i]<=0 || newnum[i]!=i;	// delete (altogether, or same as another canonical object)
			objs[i] = null;	// works but =null in wrong place
			cnt++;
		}
	}

	if (cnt==0) return newnum;

	// consider collapsed positions
	int newobjcnt = objcnt - cnt;
	for (int i=0; i<objcnt; i++) newnum[i] -= off[newnum[i]];
	//for (int i=newobjcnt; i<objcnt; i++) assert objs_[i]==null: newobjcnt+" .. "+i+" .. "+objcnt;
	//Arrays.fill(objs_, newobjcnt,objcnt, null);
//System.out.println("zap "+cnt+", now "+(objcnt-cnt));
//for (int i=1; i<objcnt; i++) System.out.print(newnum[i]+" ");  System.out.println();

	// *** renumber IRefs against implicit oldnum[i]=i, before moving objects ***
	renumber(newnum /*, new HashMap(objcnt)*/);

//System.out.print(i+"->"+(i-cnt)+" "g);
	//System.out.println("\ntotal = "+cnt);
	//if (fmonitor_) System.out.println("\n"+cnt+" duplicate or unused objects");
//for (int i=0,imax=; i<imax; i++) System.out.print(same[i]+" ");  System.out.println(); }

//System.out.println("RENUMBER obj cnt "+objcnt+" -= "+cnt+" => "+newobjcnt);
	//objs_ = new Object[newobjcnt];
	assert newobjcnt <= objcnt: newobjcnt+" > "+objcnt;
	System.arraycopy(newobjs,0, objs,0, newobjcnt);
	setObjCnt(newobjcnt);

	//trailer_.put("Size", new Integer(newobjcnt));

	return off;
  }


  /**
	Rebalances page tree so that each internal node tries to have 20 children and none has no more than 20 children.
	Subsequently, Sometime before writing, the caller probably wants to invoke {@link #liftPageTree}.
	@param leaves	holds all the pages, in sequence, with all of their attributes explicit (not relying on inheritance from a parent).
		A convenient way to accumulate this list is to read {@link IRef}s from {@link PDFReader#getPageRef(int)} 
		and make attributes explicit with {@link PDFReader#getPage(int)}.
  */
  public void resetPageTree(List<IRef> leaves) throws IOException {
	List<IRef> q = new LinkedList<IRef>(); q.addAll(leaves);

	IRef pref = null;
	// Acrobat requires that top be /Pages, not /Page
	for (int mincnt=0; q.size() > mincnt; mincnt=1) {
		// pull of PAGES_MAX at a time, up to previous size of queue
		for (int i=0,imax=q.size(); i<imax; ) {
			Dict p = new Dict(); p.put("Type", "Pages"); int cnt = 0;
			Object[] kidrefs = new Object[Math.min(PAGES_MAX, imax-i)];
			pref = addObject(p);
			for (int j=0,jmax=kidrefs.length; j<jmax; j++) {
				IRef kidref = q.remove(0);
//System.out.println("kid "+kidref+", "+j+"/"+jmax);
				Dict kid = (Dict)getObject(kidref);
				kid.put("Parent", pref);
				cnt += "Pages".equals(kid.get("Type"))? ((Integer)kid.get("Count")).intValue(): 1;
				kidrefs[j] = kidref;
				i++;
			}
			p.put("Kids", kidrefs); p.put("Count", Integers.getInteger(cnt));
//System.out.println("add "+p+" w/"+kidrefs.length+", q.size()="+q.size()+", "+i+"/"+imax);
			q.add(pref);
		}
//System.out.println("q.size()="+q.size());
	}
	Dict cat = getCatalog();
	if (pref!=null) cat.put("Pages", pref); else cat.remove("Pages");
//System.out.println("new top = "+pref);

	//liftPageTree(); => client may want to further manipulate while attributes are explicit
  }

  /**
	Removes unnecessarily duplicated inherited attributes in page tree.
	Removes attributes set to default, such as CropBox same as MediaBox.
	Sometimes makes a big difference, sometimes no difference.
  */
  public void liftPageTree() throws IOException {
	Dict pageroot = (Dict)getObject(getCatalog().get("Pages"));
	if (OBJECT_NULL==pageroot) return;	// possible, valid
	assert pageroot.get("Parent")==null;
	if (pageroot.get("Rotate")==null) pageroot.put("Rotate", Integers.ZERO);

	liftPageTree(pageroot, pageroot);	// sometimes "Parent" is null, which is error, but happens

	// defaults at root
	if (getObjInt(pageroot.get("Rotate")) % 360 == 0) pageroot.remove("Rotate");
  }

  private void liftPageTree(Object o, Dict parent) throws IOException {
	if (CLASS_DICTIONARY != o.getClass()) return;	// such as OBJECT_NULL
	Dict page = (Dict)o;

	String type = (String)getObject(page.get("Type"));
//System.out.println("liftPageTree "+page);
	//if (page.get("Parent")==null) page.put("Parent", IREF);	// LATER: fix missing "Parent"

	// 1. on way down, inherit attrs
	Object[] ival = new Object[PDFReader.PAGE_INHERITABLE.size()];
	for (int i=0,imax=PDFReader.PAGE_INHERITABLE.size(); i<imax; i++) {
		String key= PDFReader.PAGE_INHERITABLE.get(i); Object val=page.get(key);
		if (val==null && (val=parent.get(key))!=null) { ival[i]=val; page.put(key, val); }
	}

	// 2. on Pages, if all children have same value, lift up into parent (even if different than explicit!)
	if ("Pages".equals(type)) {
		Object[] kids = (Object[])getObject(page.get("Kids"));
		// if intermediate with /Kids of length 1, kill it
		// ...

		// normalize children
		for (Object kidref: kids) liftPageTree(getObject(kidref), page);

		for (int i=0,imax=PDFReader.PAGE_INHERITABLE.size(); i<imax; i++) {
			String key = PDFReader.PAGE_INHERITABLE.get(i); Object val=page.get(key);

			// do all children have the same value?
			// LATER: pick most popular value and if cnt>1, percolate up and remove in children.  else if cnt==1 then all different so zap in parent as no use in inheriting
			Object newval = null;
			for (int j=0,jmax=kids.length; j<jmax; j++) {
				Dict kid = (Dict)getObject(kids[j]); Object kval = kid.get(key);
				if (j==0/*newval==null=>NO*/) newval=kval;	// first value seen
				else if ((kval==null && newval!=null) || !objEquals(newval, kval)) { newval=null; break; }	// Object[].equals() uses ==
			}

			// if so, and different than existing value inherited from parent, replace parent's value and update children
			if (newval!=null && (!"Resources".equals(key) || page.get("Parent")!=null)/*bug in Acrobat 6.0.1 doesn't like shared /Resources on /Root*/) {
				page.put(key, newval); ival[i] = newval;	// may have percolated up from children
				for (Object kid: kids) ((Dict)getObject(kid)).remove(key);	// possible that children's values all explicitly set and inherited is null
			}
		}
	}

	// 3. if explicit same as local default, then zap.  (after recurse to children so they can inherit Cropbox)
	Object mb = page.get("MediaBox");
	if (mb != null) {
		Object cb = page.get("CropBox");
		if (cb==null) cb=mb; else if (ival[2]==null && objEquals(mb, cb)) page.remove("CropBox");
		Object box = page.get("BleedBox"); if (box!=null && objEquals(box, cb)) page.remove("BleedBox");
//if (box!=null) System.out.println("bleed "+Arrays.asList((Object[])box)+" "+Arrays.asList((Object[])cb)+", "+objEquals(box,cb));
		box = page.get("TrimBox"); if (box!=null && objEquals(box, cb)) page.remove("TrimBox");
		box = page.get("ArtBox"); if (box!=null && objEquals(box, cb)) page.remove("ArtBox");
	} else assert "Pages".equals(type): type+" "+page+" <- "+getObject(page.get("Parent"))+" / "+ival[2];


	// 4. on way up, if attr same as parent, remove
	if (page!=parent) for (int i=0,imax=PDFReader.PAGE_INHERITABLE.size(); i<imax; i++) {
		String key = PDFReader.PAGE_INHERITABLE.get(i); Object val=page.get(key);
		if (val!=null && objEquals(val, parent.get(key))) page.remove(key);	// objEquals resolves IRefs
	}
  }

	/*
  public void rebalanceNameTree() {
  }*/

  /**
	Convert embedded Type 1 fonts, if any, to a different format.
	For historical reasons Type 1 fonts are encrypted and not compressed.
	They can be written out 
	decrypted ({@link NFontType1#SUBFORMAT_DECRYPTED}) for low-level inspection.
	<!--or compacted ({@link NFontType1#FORMAT_CFF}) to save space.
	The default is {@link NFontType1#FORMAT_CFF}, but {@link #setExact(boolean)} switches to {@link NFontType1#FORMAT}.-->
	This does not affect other font formats (Type 1C, TrueType, ...).
  */
  public void convertType1(String subformat) throws IOException {
	if (NFontType1.SUBFORMAT_DECRYPTED!=subformat) return;	// for now just decrypt for Uncompress

	for (int i=0+1,imax=getObjCnt(); i<imax; i++) {	// maybe scan Page and Form XObject /Resources instead...
		Object o = getObject(i, false); if (CLASS_DICTIONARY!=o.getClass()) continue;
		Dict dict = (Dict)o; if (!"Font".equals(getObject(dict.get("Type"))) || !"Type1".equals(getObject(dict.get("Subtype")))) continue;
		Dict fdesc = (Dict)getObject(dict.get("FontDescriptor")); if (fdesc==null) continue;
		IRef embedref = (IRef)fdesc.get("FontFile");
		Dict embed = (Dict)getObject(embedref); if (embed==null) continue;

		dict = (Dict)getObject(i, true);	// pin
		//int clen = getObjInt(embed.get("Length1")), elen = getObjInt(embed.get("Length2")), zlen = getObjInt(embed.get("Length3"));
		if (NFontType1.FORMAT_CFF==subformat) {
			embed.remove("Length1"); embed.remove("Length2"); embed.remove("Length3");
			embed.put("Subtype", "Type1C");
			// new NFontType1(data); export(NFont.Type1C);

		} else {
			assert NFontType1.SUBFORMAT_DECRYPTED==subformat: subformat;
			if (!"Type1U".equals(embed.get("Type"))) {	// don't double decrypt (IRS p550.pdf)
				byte[] data = getStreamData(embed/*, embedref.id*/); assert data!=null: embed;
//System.out.println("decrypted "+data.length+" (vs clen+elen = "+(clen+elen)+")");
				try {
					data = NFontType1.normalize(data);
					embed.put(STREAM_DATA, data);
					embed.put("Length3", Integers.ZERO);	// never need 512 zeros!

					embed.put("Type", "Type1U");	// mark independently from font descriptor so can thaw independently too
				} catch (java.awt.FontFormatException dontdecrypt) {}
			}
		}
	}
  }



  // WRITE

  /**
	Compress objects, or not (for debugging or pedagogical purposes).
	Also writes COS objects with minimal number of spaces, or to be more readable by humans.
	Images are always compressed in image-specific formats regardless of this setting,
	and compression of other objects, if requested, is always with Flate compression (not LZW).
  */
  public void setCompress(boolean b) { fcompress_ = b; }

  /** Shows status information. */
  public void setMonitor(boolean b) { fmonitor_ = b; }


  /**
	Collect non-stream objects into compressed <dfn>object streams</dfn> (introduced in PDF 1.5),
	in groups of 200 or so.
	Should be last method invoked before starting to write PDF objects to a file.

	<p>Other algorithms that make object streams represent this
	adding the object streams as dictionaries, storing their component objects in the {@link COS#STREAM_DATA} dictionary key,
	and replacing the old copies of component objects with a number of class {@link COS#CLASS_OBJSTMC} that holds the object number of the object stream
	and setting its generation to the index of the object in the object stream.
  */
  public boolean makeObjectStreams(int start, int end) throws IOException {
	writeBarrier();	// need fplain

	// make some object streams, if find enough consecutive objects to make it worth it
	List<Dict> l = new ArrayList<Dict>(20);	// keep track of new ObjStm because zapping old ones
	int[] os = new int[OBJSTMC_MAX]; int osi=0;

	for (int i = start>0? start: 1, cnt=0; i<=/*= to flush*/end; i++) {
		// collect objects
		if (i<end && !fplain_[i]) {
			Object o = getObject(i, true); Class cl = o.getClass();
			// kill existing ObjStm's -- early so can reuse object number for new one
			if (CLASS_DICTIONARY==cl && "ObjStm".equals(((Dict)o).get("Type")) && l.indexOf(o)==-1) setObject(i, OBJECT_DELETED);
			else if (!(CLASS_DICTIONARY==cl && (((Dict)o).get(STREAM_DATA)!=null || "Catalog".equals(((Dict)o).get("Type")) || ((Dict)o).get("Author")!=null || ((Dict)o).get("Creator")!=null))	// not stream, Catalog, Info
				 && OBJECT_DELETED != o
				 ) {
				os[osi++] = i;
			}
		}

		// at flush points, see if saves space to stuff into ObjStm
		if ((osi==OBJSTMC_MAX || i==end) && osi>0) {
			StringBuffer noff = new StringBuffer(OBJSTMC_MAX * 10);
			StringBuffer sb = new StringBuffer(8*1024);
			for (int j=0; j<osi; j++) {
				int n = os[j];
				noff.append(n).append(' ').append(sb.length()).append(' ');
				writeObject(getObject(n,false), sb, true);
				if (j+1<i) sb.append(' ');	// separate objects [only between non-self-delimiting objects?] -- maybe not required in Acrobat
			}
			sb.insert(0, (Object/*Java 1.5*/)noff);
			byte[] data = StringBuffers.getBytes8(sb), newdata = maybeDeflateData(data);

			if (newdata.length + 1024 < data.length + osi*" n obj\nendobj\n".length()) {	// enough to make it worthwhile?  Almost always so, but if so have to compute Flate anyhow so may as well check.
//System.out.println(osi+"?  "+data.length+"=>"+newdata.length/*+" (+ overhead)"*/);
				Dict objstm = new Dict(7);
				objstm.put("Type", "ObjStm");
				objstm.put("N", Integers.getInteger(osi));
				objstm.put("First", Integers.getInteger(noff.length()));
				//objstm.put(STREAM_DATA, data);	// save old data for general compression later
				if (fcompress_) { objstm.put(STREAM_DATA, newdata); objstm.put("Filter", "FlateDecode"); } else objstm.put(STREAM_DATA, data);

				l.add(objstm);
				int inx = addObject(objstm).id;
				setObjGen(inx, 0);	// FIX: ObjStm have gen 0, but should never reuse deleted
				Long pobjstm = new Long(inx); for (int j=0; j<osi; j++) { int n=os[j]; setObject(n, pobjstm); setObjGen(n, j); }	// point components to index in new ObjStm
//System.out.println("added ObjStm @ "+inx);
			}

			osi = 0;
		}
	}

	boolean fobjstm = l.size() > 0;
	if (fobjstm) getVersion().setMin(new Version("1.5"));
	return fobjstm;
  }


  private void writeBarrier() throws IOException {
	if (fplain_ != null) return;	// already

	// 1.compute operational objects from PDF COS descriptions
	// a. encryption
	Dict trailer = getTrailer();
	if (!fcompress_) trailer.remove("Encrypt");	// no use having uncompressed but encrypted

	Object eref = trailer.get("Encrypt");	// /Encrypt can be direct I guess

	// new Encrypt even if same as PDFReader's, else RC4 state mixed up between interleaved reads and writes
	Dict edict = (Dict)getObject(eref);	// faults in component objects
	encrypt_ = new Encrypt(edict, this);

	if (edict != null) {
//System.out.println("edict = "+edict);
//System.out.println("encrypt_ = "+encrypt_);
		SecurityHandler sh = encrypt_.getSecurityHandler();
		boolean nullvalid = sh.isAuthorized() || sh.authUser("") || sh.authOwner("");
		boolean valid = nullvalid || sh.authUser(password_) || sh.authOwner(password_);
		if (!valid) throw new IllegalStateException("must set valid password by now");

		int V = getObjInt(edict.get("V"));
		int pdfv = V>=4? 5: (V==2 || V==3)? 4: edict.get("SubFilter")!=null? 3: 1;
		getVersion().setMin(new Version("1."+pdfv));
		//if (e instanceof SecurityHandlerStandard && e.getLength() > 40) getVersion().setMin("1.4");


		// if encrypted with null passwords and PDF>=1.5, convert to plain text and /Identity so don't waste time encrypting/decrypting
		// Contrariwise, if PDF dropping and /Identity, ...
		if (nullvalid && getVersion().compareTo(1,5)>=0) {
			//edict.put("V", Integers.getInteger(4)); edict.put("R", Integers.getInteger(4));
			// ...
		}
	}

	// b. Compress
	//Dict comp = (Dict)trailer.get(KEY_COMPRESS);
	//if ("Compact") getVersion().setMin("1.6");	// n/a


	// 2. determine special objects
	int objcnt = getObjCnt();
	fplain_ = new boolean[objcnt];

	// b. objects not to encrypt
	if (edict != null) {
		// LATER: if fplain_ && PDF>=1.5 && non-null password, /CryptFilter /Identity
		List<IRef> noeref = connected(eref);
		noencrypt_ = new ArrayList<Object>(noeref.size() + 10);
		for (int i=0,imax=noeref.size(); i<imax; i++) {
			IRef ref = noeref.get(i);
			noencrypt_.add(getObject(ref));
			fplain_[ref.id] = true;
		}
		noencrypt_.add(trailer_);
//System.out.println("noencrypt: "+noencrypt_);
	} else noencrypt_ = new ArrayList<Object>(10);

	// b. plain objects
	fplain_[0] = true;
	//iref = (IRef)trailer_.get("Root"); fplain[iref.id]=true; => references lots of objects.  replace with fake page for non-aware viewers
	IRef iref = (IRef)trailer_.get("Info"); if (iref!=null) fplain_[iref.id]=true;
	Dict cat = getCatalog();
	iref = (IRef)cat.get("Metadata"); if (iref!=null) {	// metadata can be everywhere, so just for document-wide metadata
		Dict dict = (Dict)getObject(iref);
		Object filter = getObject(dict.get("Filter"));
		if (filter==null) fplain_[iref.id]=true;
		if (encrypt_.getCryptFilter(dict) != CryptFilter.IDENTITY && getVersion().compareTo(1,5)>=0) {
			//noencrypt_.add(new IRef(i,getObjGen(i))); => has to be CryptFilter
			//addFilter(dict, "Crypt", "Identity");
		}
	}

/*	if (comp!=null) {
System.out.println("trailer /Compress = "+comp);
		IRef oldroot = (IRef)comp.remove(KEY_COMPRESS_ROOT);
		List<IRef> plaincomp = connected(comp);
		for (int i=0,imax=plaincomp.size(); i<imax; i++) fplain_[plaincomp.get(i).id] = true;
		//for (IRef iref: connected(comp)) fplain_[iref.id] = true;
		if (oldroot!=null) comp.put(KEY_COMPRESS_ROOT, oldroot);
	}*/
	// + trailer, which is not a stream anyhow, which is good because would be special as as trailer doesn't have object number if PDF <= 1.4
  }


  /**
	Writes document header: "%PDF-m.n\n%byte/byte/byte/byte\n".
	After this point, no changes can be made to the encryption settings.
  */
  public void writeHeader() throws IOException {
	writeBarrier();	// last chance to bump version

	OutputStreamTee w = w_;
	w.writeString8(SIGNATURE+getVersion()+'\n');	// "%PDF-m.n"
	w.write('%'); w.write(144); w.write(132); w.write(134); w.write(143); w.write('\n');	// four bytes with high bits on so recognized as binary in file transfers
  }

  /**
	Writes cross reference table and trailer.
	If PDF version is 1.5 or later and trailer can be found among existing objects, then the cross reference table is written as a stream, which is compressible.
	As separate method so can write in chunks for Linearized.
	If an object is a component of an object stream, it should have been set as described in {@link #makeObjectStreams(int,int)}.

	@param size     total number of objects in the file (not just in current xref segment); usually same as {@link #getObjCnt()}
  */
  public void writeXref(Dict trailer, int size, long prev,  long[] offset, int start, int length) throws IOException {
	writeXref(trailer, size, prev,  offset, new int[] { start }, new int[] { length });
  }

  /** Writes cross reference of multiple sections and trailer. */
  public void writeXref(Dict trailer, int size, long prev,  long[] offset, int[] start, int[] length) throws IOException {
	assert start.length == length.length;
	// start[] in ascending order

	OutputStreamTee w = w_;
	startxref_ = w.getCount();	// last one wins

	// update trailer
	if (prev<=0) trailer.remove("Prev"); else trailer.put("Prev", new Integer((int)prev));
	trailer.put("Size", Integers.getInteger(size));

	// we no longer support the deleted object chain, so cauterize list.
	offset[0] = 0L;

	int xnum = getObjIndex(trailer);
//System.out.println("trailer @ "+xnum);
	if (getVersion().compareTo(1,5)>=0 && xnum > 0 && (fcompress_ || fexact_)) {	// write as compressed stream, unless human readable
		trailer.put("Type", "XRef");

		if (start.length==1 && start[0]==0 && length[0]==size) trailer.remove("Index");
		else {
			Object[] oa = new Object[start.length * 2];
			for (int i=0,imax=start.length, j=0; i<imax; i++) { oa[j++]=Integers.getInteger(start[i]); oa[j++]=Integers.getInteger(length[i]); }
			trailer.put("Index", oa);
		}

		// compute max bytes needed for type, offset, gen
		int cnt = 0; long max1=0; int max2=0; boolean fall1 = true;
		for (int i=0,imax=start.length; i<imax; i++) {
//System.out.println("max of "+start[i]+".."+(start[i]+length[i]-1));
			for (int j=start[i],jmax=start[i]+length[i]; j<jmax; j++) {
				if (j==0) { /*start[0]++; length[0]--;*/ continue; }	// object 0 implicit, and save a byte /W [1 3 1] rather than [1 3 2] for gen 65535
				if (fall1 && getObjType(j) != XREF_NORMAL) fall1 = false;
				max1 = Math.max(offset[j], max1);
				max2 = Math.max(getObjGen(j), max2);
			}
			cnt += length[i];
		}

		int byT = fall1? 0: 1;	//del==0? 0: 1;	// if types all == XREF_NORMAL, don't need to write out
		int byO=0; for (int i=0,imax=8; i<imax; i++) if (((0xffL << (i*8)) & max1) != 0) byO=i+1;
//System.out.println("max offset = "+max1+"/"+Long.toHexString(max1)+", requiring "+byO+" bytes");
		int byG = max2==0? 0: max2<=0xff? 1: 2;
//System.out.println("max gen = "+max2+", requiring "+byG+" bytes");
		trailer.put("W", new Object[] { Integers.getInteger(byT), Integers.getInteger(byO), Integers.getInteger(byG) });

		// make sure xref table assigned object number
		// maybe use /PredictDecode like Adobe
		ByteArrayOutputStream bout = new ByteArrayOutputStream(start.length * 10  +  (byT+byO+byG) * cnt);
		for (int i=0,imax=start.length; i<imax; i++) {
			for (int j=start[i],jmax=start[i]+length[i]; j<jmax; j++) {
				// more accurately, fields 1, 2, 3
				byte typ = getObjType(j);
				long off = XREF_OBJSTMC==typ? ((Number)getObject(j,false)).longValue(): offset[j];
				int gen = getObjGen(j);
				if (j==0) { typ=XREF_FREE; off=0L; gen=0; }	// tweak object 0 so doesn't upset what otherwise is gen 0

				if (byT>0) bout.write(typ);
				for (int k=byO-1; k>=0; k--) bout.write((int)(off>>(k*8)));
				for (int k=byG-1; k>=0; k--) bout.write(gen>>(k*8));
			}
		}
		bout.close();
//System.out.println("xref "+(20*cnt)+" => "+bout.size()+" = "+trailer);


		// smaller with Up predictor, sometimes
		byte[] without = bout.toByteArray();
		int linelen = byT + byO + byG, bug = 1;
		byte[] with = new byte[(bug + linelen) * (without.length / linelen)];
		for (int i=without.length-1, j=with.length-1; i > linelen/*not =line 0*/; ) {
			for (int k=0; k<linelen; k++, j--, i--) with[j] = (byte)((without[i] & 0xff) - (without[i-linelen]&0xff));
			if (bug==1) with[j--] = 2;	// Adobe bug: per-line code
		}
		System.arraycopy(without,0, with,0+bug, linelen); if (bug==1) with[0] = 2;

		byte[] withoutdef = maybeDeflateData(without), withdef = maybeDeflateData(with);
		if (withdef.length + PREDICT_XREF_OVERHEAD + 25/*aggravation*/ < withoutdef.length) {
			trailer.put(STREAM_DATA, withdef);
			Dict pre = new Dict(); pre.put("Columns", Integers.getInteger(linelen)); pre.put("Predictor", Integers.getInteger(12));
			trailer.put("DecodeParms", pre);
			if (with!=withdef) trailer.put("Filter", "FlateDecode");
		} else {
			trailer.put(STREAM_DATA, withoutdef);
			if (without!=withoutdef) trailer.put("Filter", "FlateDecode");
		}
		

		// write as ordinary object
		writeObject(trailer, xnum,0, false, null);


	} else {
		/*if (fhead)*/ w.writeString8("xref\n");	// should track when "xref" needed, not just on start==0
		//if (length==0) return 0L;	// to just write "xref"

		for (int i=0,imax=start.length; i<imax; i++) {
			w.writeString8(start[i]+" "+length[i]+"\n");

			for (int j=start[i], jmax=start[i]+length[i]; j<jmax; j++) {
				// take generation from backer
				long off = offset[j]; int gen = getObjGen(j);
				char status = OBJECT_DELETED==getCache(j)? 'f': 'n';
				String soff = Long.toString(off), sgen = gen==0? "0": Integer.toString(gen);	// => there's some format object for this now
				w.writeString8("0000000000".substring(soff.length()) + soff + " " + "00000".substring(sgen.length()) + sgen + " " + status+" \n");
			}
		}

		if (trailer==null) trailer = new Dict(5);
		w.writeString8("trailer\n");
		trailer.remove("Type"); trailer.remove("W"); trailer.remove("Index");
		w.writeString8(writeObject(trailer, new StringBuffer(200), fcompress_).toString());
		w.writeString8("\n");
	}
  }



  /**
	Writes a top-level object: <code>n g obj</code> <i>contents</i> <code>endobj</code>, 
	with applicable encryption, respecting <code>CryptFilter</code>, if any.
  */
  public long writeObject(Object obj, int objnum, int objgen) throws IOException {
	writeBarrier();
	Encrypt encrypt = objnum>0/*content stream==-1*/ && noencrypt_.indexOf(obj)==-1? encrypt_: null;
	boolean fplain = objnum < fplain_.length? fplain_[objnum]: false;
//if (objnum==206) System.out.println("#206 "+encrypt+", "+noencrypt_.indexOf(obj)+", "+encrypt_);
	return writeObject(obj, objnum, objgen, fplain, encrypt);
  }

  /**
	Low-level write of a top-level object: <code>n g obj</code> <i>contents</i> <code>endobj</code>, encrypting according to <var>encrypt</var>, which can be null for no encryption.
	in that follows <var>encrypt</var> setting, ignoring any <code>CryptFilter</code>.
	Content streams should pass their data streams as a <code>byte[]</code> under the {@link COS#STREAM_DATA} key.
	If object is a stream and no filter has been applied, applies Flate compression if that results in a smaller object.
	@return file offset of start of object, or 0 if the object has been deleted or object number is 0 (which is a special number reserved by PDF).
<!-- complete low-level control -->
  */
  public long writeObject(Object obj, int objnum, int objgen, boolean fplain, Encrypt encrypt) throws IOException {
	// writeBarrier(); => not in low-level, in which everything is explicit
	if (objnum==0 || OBJECT_DELETED==obj || CLASS_OBJSTMC==obj.getClass()) return 0L;	// getObjType(objnum)==XREF_OBJSTMC does getObject()

	OutputStreamTee w = w_;
	long posn = w.getCount();
//System.out.println(objnum+" "+posn);

	// 1. massage data
	// compress
	byte[] dataout = null;
	if (CLASS_DICTIONARY==obj.getClass() && ((Dict)obj).get(STREAM_DATA) != null) {
		Dict dict = (Dict)obj;
		// compression
		if ("None".equals(dict.get("Filter"))) { fplain = true; dict.remove("Filter"); }
		if (fplain) {
			dataout = getStreamData(dict/*, objnum*/);
			dict.put("Length", Integers.getInteger(dataout.length));
		} else dataout = deflateStream(dict, objnum);

		// encryption -- last filter
		if (encrypt!=null && dataout!=null) {
			CryptFilter cf = encrypt.getCryptFilter(dict);
			if (cf != CryptFilter.IDENTITY) {
				dataout = (byte[])dataout.clone();	// don't mutate
				cf.reset(objnum, objgen).encrypt(dataout, 0,dataout.length);
			}
		}
	}

	// 2. write to PDF
	w.writeString8(Integers.toString(objnum)); w.writeString8(" "); w.writeString8(Integers.toString(objgen)); w.writeString8(" obj");	// don't need \n after obj and endobj (depending on context)
	if (CLASS_DICTIONARY != obj.getClass()) w.writeString8("\n");
	w.writeString8(writeObject(obj, objnum, objgen, new StringBuffer(1*1024), fcompress_, encrypt).toString());

	// stream content
	if (dataout!=null) {
		w.writeString8("stream\n");
		w.write(dataout);
		w.writeString8("\nendstream");	// always followed by "\nendobj"
	}

	w.writeString8("\nendobj\n");

	return posn;
  }


  /**
	Writes contents of passed PDF object to StringBuffer that represents a content stream.
	@return same StringBuffer passed in
  */
  public /*static*/ StringBuffer writeObject(Object o, StringBuffer sb, boolean fcrunch) {
	return writeObject(o, -1,-1, sb, fcrunch, null);
  }

  /**
	Writes contents of passed PDF object to StringBuffer, recursively writing contents of dictionary, stream, and array.
	@return same StringBuffer passed in
<!-- complete low-level control -->
  */
  private/*public /*static--Compact iref syntax*/ StringBuffer writeObject(Object o, int objnum, int objgen, StringBuffer sb, boolean fcrunch, Encrypt encrypt) {
//System.out.println(o);
	//boolean ftoplevel = objnum>=0;
	Class cl = o.getClass();
	if (CLASS_STRING == cl) {
		StringBuffer sb0 = (StringBuffer)o, sbin = sb0;

		/*if (objnum>0--no, lin ... || DEBUG)*/ for (int i=0,imax=sbin.length(); i<imax; i++) {
			if (sbin.charAt(i)>=256) {
				//assert objnum>0: objnum+" "+o;	// no Unicode in content stream => also object measuring for Linearize
				//System.out.println("Unicode: "+sbin);
				sbin = new StringBuffer(sb0.length() * 2 + 2);	// spread out for writing low bytes only
				if (sb0.charAt(0) != '\ufeff') sbin.append("\u00fe\u00ff");
				for (int j=0,jmax=sb0.length(); j<jmax; j++) {
					char ch = sb0.charAt(j);
					sbin.append((char)((ch>>8)&0xff)).append((char)(ch&0xff));
				}
				break;
			}
		}

		if (encrypt!=null && CryptFilter.IDENTITY != encrypt.getStrF()) {
			if (sbin==sb0) { sbin = new StringBuffer(sb0.length()); sbin.append(sb0); }	// no mutate on write
			encrypt.getStrF().reset(objnum, objgen).encrypt(sbin);
		}
//if (objnum==206) System.out.println("#206c "+sb0+" => "+sbin+", "+encrypt);
/*if (encrypt_==o) {
	StringBuffer osb=(StringBuffer)encrypt_.get("O"); for (int i=0,imax=osb.length(); i<imax; i++) System.out.println(Integer.toHexString(osb.charAt(i))+" ");  System.out.println();
	StringBuffer usb=(StringBuffer)encrypt_.get("U"); for (int i=0,imax=usb.length(); i<imax; i++) System.out.println(Integer.toHexString(usb.charAt(i))+" ");  System.out.println();
}*/

		// count parentheses so know if balanced or not (separate pass from Unicode)
		int rp=0, lp=0;	// unpaired parens: rp starts at max and counts down, lp counts up as encountered
		for (int i=0,imax=sbin.length(); i<imax; i++) if (sbin.charAt(i)==')') rp++;

		// always write as (string), never as <hex> representation.
		// If human readable, then control char just as readable as hex; and if compressed or encrypted, shorter.
		// Although, would be nice to write trailer /ID as hex.
		sb.append('(');
		for (int i=0,imax=sbin.length(); i<imax; i++) {	// escape meta characters
			char ch = sbin.charAt(i);
			if (ch==')') { rp--; if (lp>0) { sb.append(')'); lp--; } else sb.append("\\)"); }
			else if (ch=='(') { if (lp<rp) { sb.append('('); lp++; } else sb.append("\\("); }
			else if (ch=='\\') sb.append("\\\\");
			else if (ch=='\r') sb.append("\\r");	// happens!  don't let it get read back as \n, especially if encrypted!
			else sb.append(ch);
		}
		assert rp==0 && lp==0: sb;	// all paired up when done
		sb.append(')');

	} else if (CLASS_NAME == cl) {
		// names can have spaces and other encoded characters
		sb.append('/');
		String str = (String)o;
		for (int i=0,imax=str.length(); i<imax; i++) {
			char ch = str.charAt(i);
			if (' ' < ch&&ch < 127 && ch!='#' && !PostScript.WSDL[ch]) sb.append(ch);
			else { sb.append("#").append(ch<0x10? "0": "").append(Integer.toHexString(ch)); /*getVersion().setMin("1.2");--too late, after header*/ }	// always two hex digits
		}

	} else if (CLASS_IREF == cl) {
		IRef oref = (IRef)o;
		int id = oref.id; assert id>=1: id;
		//assert getObject(id) != OBJECT_DELETED && !(getObject(id) instanceof IRef) /* but ==null OK*/; => can't in static context
		sb.append(Integers.toString(id));
		if (fbulk_) sb.append("R");
		//else if (oref.generation==0) sb.append(" 0 R");	// shorter to write id+" 0R", which Multivalent allows, but Acrobat doesn't
		else sb.append(' ').append(Integers.toString(oref.generation)).append(" R");	// either keep initial generation, or zero if renumber.  Better than above because still static.
		//sb.append(" ").append(getObjGen(id)).append(" R");	// ignore generation field in IRef in favor of possibly updated value?

	} else if (CLASS_ARRAY == cl) {
		sb.append('[');
		Class lastcl=null;
		for (Object val: (Object[])o) {
			Class vcl = val.getClass();
			if (lastcl!=null/*not first -- was i>0*/ && (!fcrunch ||
				(lastcl!=CLASS_DICTIONARY && lastcl!=CLASS_ARRAY && lastcl!=CLASS_STRING	// previous delimiting after
				&& vcl!=CLASS_NAME && vcl!=CLASS_DICTIONARY && vcl!=CLASS_ARRAY && vcl!=CLASS_STRING)))	// this delimiting before
				sb.append(' ');

			writeObject(val, objnum, objgen, sb, fcrunch, encrypt);

			lastcl=vcl;
		}
		sb.append(']');

	} else if (CLASS_DICTIONARY == cl) {
		Dict dict = (Dict)o;
		sb.append("<<");

		// keys at start: /Type, /Subtype
		for (int i=0,imax=DICT_KEYS_START.length; i<imax; i++) {
			Object key = DICT_KEYS_START[i], val = dict.get(key);
			if (val!=null && val!=OBJECT_NULL) {
				if (!fcrunch) sb.append(' ');
				writeObject(key, objnum, objgen, sb, fcrunch, encrypt);

				Class vcl=val.getClass(); if (!fcrunch || (vcl!=CLASS_NAME && vcl!=CLASS_DICTIONARY && vcl!=CLASS_ARRAY && vcl!=CLASS_STRING)) sb.append(' ');
				writeObject(val, objnum, objgen, sb, fcrunch, encrypt);
			}
		}

		// ordinary keys
		for (Iterator<Map.Entry<Object,Object>> i=dict.entrySet().iterator(); i.hasNext(); ) {
			Map.Entry<Object,Object> e = i.next();
			String key=(String)e.getKey(); Object val=e.getValue(); assert val!=null: key;
			if (key==STREAM_DATA || key==REALIZED || val==OBJECT_NULL || Arrays.binarySearch(DICT_KEYS_SKIP, (String)key) >= 0) continue;

			if (!fcrunch) sb.append(' ');	// "The key must be a name..." so self-delimiting before
			writeObject(key, objnum, objgen, sb, fcrunch, encrypt);

			Class vcl=val.getClass();
			if (!fcrunch || (vcl!=CLASS_NAME && vcl!=CLASS_DICTIONARY && vcl!=CLASS_ARRAY && vcl!=CLASS_STRING)) sb.append(' ');	// no space before self-delimiting
			writeObject(val, objnum, objgen, sb, fcrunch, encrypt);
		}

		// keys at end: /Filter, /Length
		for (int i=0,imax=DICT_KEYS_END.length; i<imax; i++) {
			Object key = DICT_KEYS_END[i], val = dict.get(key);
			if (val!=null && val!=OBJECT_NULL) {
				if (!fcrunch) sb.append(' ');
				writeObject(key, objnum, objgen, sb, fcrunch, encrypt);

				Class vcl=val.getClass(); if (!fcrunch || (vcl!=CLASS_NAME && vcl!=CLASS_DICTIONARY && vcl!=CLASS_ARRAY && vcl!=CLASS_STRING)) sb.append(' ');
				writeObject(val, objnum, objgen, sb, fcrunch, encrypt);
			}
		}

		sb.append(">>");
		// ... write content of stream in caller, who knows object number for encryption

	} else if (CLASS_INTEGER == cl /*|| o instanceof Long--trailer /Prev*/) { sb.append(Integer.toString(((Integer)o).intValue()));

	} else if (CLASS_REAL == cl) {
		double dval = ((Double)o).doubleValue();
//System.out.println(dval+", "+Math.rint(dval));
		if (Math.rint(dval) == dval) sb.append(Math.round(dval));
		else sb.append(NF.format(dval));

	} else if (CLASS_BOOLEAN == cl) { sb.append(o==Boolean.TRUE? "true": "false");

	} else if (OBJECT_NULL == o) { sb.append("null");

	} else if (CLASS_DATA == cl) {	// can happen in command stream inline image data
		byte[] b = (byte[])o;
		for (int i=0,imax=b.length; i<imax; i++) sb.append((char)(b[i]&0xff));

	} else if (OBJECT_DELETED == o) { assert false: objnum;	// shouldn't write deleted objects

	} else assert false: objnum+": "+o+" / "+cl.getName();

	return sb;
  }


  /**
	Writes command array back into a byte stream, skipping commands marked invalid.
	(Doesn't minimize whitespace as done for top-level objects, since compression in streams wipes out advantages.)

	<ul>
	<li>normalize content stream line ends
	<li>optionally pretty print content stream: indent under BT..ET / q..Q
	<li>write floating point numbers compactly and limit resolution to useful range: "0.00" => "0", "5.248549334" => "5.24854"
	<li>write strings compactly: characters rather than hex, escaped '(' ')' only when unpaired
	<li>remove separate LZW or Flate compression on inline images as it's more efficient to compress as part of the overall content stream
	</ul>
  */
  public byte[] writeCommandArray(Cmd[] cmds, boolean prettyprint) {
	StringBuffer out = new StringBuffer(cmds.length * 10/*content.length*/);	// could write over input byte[]
	out.append('\n');	// sentinal

	//ByteArrayOutputStream out = new ByteArrayOutputStream(content.length); => no write(String)
	int newopcnt=0;
	int indent = 0;
	for (int i=0,imax=cmds.length; i<imax; i++) {
		Cmd cmd=cmds[i]; if (!cmd.valid) continue;
		String op=cmd.op; Object[] ops=cmd.ops; int opsi=ops.length;
		newopcnt++;

		if (prettyprint) {
			boolean br = out.charAt(out.length()-1) == '\n';
			if (!br && BREAK_BEFORE.contains(op)) { out.append('\n'); br=true; }
			if ("ET".equals(op) || "Q".equals(op)) indent = Math.max(indent-1, 0);	// stream starts with "Q"
			if (br) for (int j=0,jmax=indent; j<jmax; j++) out.append("   ");
			else out.append("  ");
			if ("BT".equals(op) || "q".equals(op)) indent++;
		}

		// 1. special-cased objects
		if ("%"==op && opsi==1) {
			out.append("%").append(ops[0]).append('\n');

		} else if ("BI"==op) {
			writeInlineImage((Dict)ops[0], (byte[])ops[1], out);

		} else if ("TJ".equals(op) && opsi==1/*p45-zellweger splits across streams*/) {
			out.append('[');
			for (Object oi: (Object[])ops[0]) writeObject(oi, out, false);	// no separating spaces
			out.append("]TJ\n");

		// 2. other objects
		} else {
			//String dl = "()<>[]{}/%" => "()<>[]{}/"; -- could crunch space if previous ends with dl or next begins with dl => prev or cur is string, dict, name, array
			//Class prevcl=null;
			for (int j=0,jmax=opsi; j<jmax; j++) {
				writeObject(ops[j], out, false); out.append(' ');
			}
			if (op!=Cmd.NO_OP) {	// command split over streams
				out.append(op);	//.append(opsi>0? '\n': ' ');
				//out.append(/*prettyprint &&*/ BREAK_AFTER.contains(op)? "\n": "  ");
				if (BREAK_AFTER.contains(op)) out.append('\n'); else if (!prettyprint) out.append(" ");
			} else assert i+1 == imax: i;
		}
	}

	//chash.put(STREAM_DATA, out.toByteArray()); -- NO! runs through an encoding which loses non-ASCII
	return Strings.getBytes8(out.toString());
  }


  /**
	Writes inline image with image <var>data</var> into content stream <var>sb</var>.
	@see PDFReader#readInlineImage(InputStreamComposite)
  */
  public StringBuffer writeInlineImage(Dict params, byte[] data, StringBuffer sb) {
	sb.append("BI  ");
	// X writeObject(ops[0], out, false); => no "<<" and ">>" here + abbreviate
	for (Iterator<Map.Entry<Object,Object>> j=params.entrySet().iterator(); j.hasNext(); ) {
		Map.Entry<Object,Object> e = j.next();
		String key=(String)e.getKey();
		String o = INLINE_ABBREV.get(key); if (o!=null) key = o;	// abbreviate

		writeObject(key, sb, false); sb.append(' ');
		writeObject(e.getValue(), sb, false); sb.append(' ');
	}
	sb.append("\nID ");
	// compress inline images along with rest of stream -- if do so separately, don't gain and in fact gets bigger as kills Flate dictionary
	// LATER: check that no "\nEI" in array
	writeObject(data, sb, false);
	sb.append("\nEI\n");

	return sb;
  }


  /**
	Writes data in memory or base PDF to new PDF file, complete with header, xref table, and trailer.
	If a non-<code>null</code> <var>observer</var> is passed in, it is invoked after fully reading but before writing each PDF object,
	with <var>observer</var> being invoked with a two-element array consisting of the PDF object and the object number as an Integer.
	If asked to write onto existing file, first writes to temporary file, then deletes and renames.
	If an object has not been instantiated, it is instantiated, written, and then cleared in order to free up memory
	and thus allow PDFs of any size to be processed in a limited amount of memory.

	<p>Completely control writing by writing own version of this method with these steps:
	<ol>
	<li>{@link #writeHeader()}
	<li>{@link #writeObject(Object, int, int)}s, keeping track of file offset for xref table
	<li>{@link #writeXref(Dict,int,long,  long[], int, int)} table
	</ol>

	<p>If PDF version is 1.5 or later, writes trailer and xref as a stream, adding an object for it with the highest object number.
	If object streams are desired, they must be created beforehand, as by invoking {@link #makeObjectStreams(int,int)}.

	<p>If Compact PDF writing mode is set
	writes entire PDF in format that puts almost every object in a single BZip2 or Flate stream.
	It is more 30 to 60% more compact on large classes of PDF, but does not conform to the PDF 1.5 specification.
	It is readable with the {@link PDFReader} class, the Multivalent Browser, and the Multivalent tools;
	and it is valid PDF so you can do incremental writes and so on.
	See <a href='http://multivalent.sourceforge.net/Research/CompactPDF.html'>Compact PDF format</a>.

	<p>After writing, objects may have been mutated or deleted, and therefore should not be accessed.

	@return length of new PDF file (as a {@link java.lang.Long}) or other object for special cases
  */
  public Object writePDF(Observer observer) throws IOException {
	int objcnt = getObjCnt();
	if (objcnt==0) { System.out.println("empty PDF -- nothing written"); return null; }
	//else if no /Catalog or ...

	if (fmonitor_) System.out.print("WRITE, "+objcnt+" objects\t");

	Dict comp = (Dict)trailer_.get(KEY_COMPRESS);
	String filter = null;
	if (comp!=null) {
		filter = (String)comp.get(KEY_COMPRESS_FILTER);
		// clean up
		comp.remove(KEY_COMPRESS_COMPACT);
		// update
		//LengthO, SpecO -- unknown here
	}
	//if (incremental) ...
	Object ret = null;
	if (fmonitor_) System.out.print(", filter "+filter);
	if (OutputStreams.DEVNULL==w_) ret = writeReader(observer);
	else if ("Compact".equals(filter)) writeCompact(observer); 
	// else if (future method) ...
	else writeStandard(observer);


	long newlen = w_.getCount();
	//close(); => client()
	if (fmonitor_) System.out.print(", wrote to "+getURI()+" / "+outfile_+" / "+fcompress_+" @ "+newlen);

	return ret!=null? ret: new Long(newlen);
  }

  /** Convience method for <code>writePDF(null)</code>. */
  public Object writePDF() throws IOException { return writePDF(null); }

  /**
	Writes contents in Forms Data Format (FDF).
  */
  public void writeFDF(/*Observer observer*/) throws IOException {
	writeBarrier();
	OutputStreamTee w = w_;
	w.writeString8("%FDF-1.2\n");
	int objcnt = getObjCnt();
	for (int i=0+1; i<objcnt; i++) writeObject(getObject(i,false), i,0);	// gen 0
	// no xref
	w.writeString8("\ntrailer\n"); w.writeString8(writeObject(trailer_, new StringBuffer(200), false).toString()); w.writeString8("\n%%EOF\n");
  }


  private void writeStandard(Observer observer) throws IOException {
	writeHeader();

	int objcnt = getObjCnt();
	//if (compareVersion(1,5)>=0) makeObjectStreams(0, objcnt); => zap old, make new => external control
	long[] off = new long[objcnt + 1];	// +1 for PDF 1.5 xref

	for (int i=0+1/*0 is head of deleted chain*/; i<objcnt; i++) {
		if (fmonitor_) { if (i%1000==0) System.out.print((objcnt-i)/1000); else if (i%500==0) System.out.print(" "); }
		//boolean fempty = objCache_[i]==null;
		boolean fempty = getCache(i)==null;
		Object obj = getObject(i, false);	// don't cache now: after manipulations so too late, and conserve memory so can process arbitrarily large PDFs
		//Object dataval = CLASS_DICTIONARY==obj.getClass()? ((Dict)obj).get(STREAM_DATA): null;
		byte[] data = getStreamData(obj/*, i*/);
//if (data!=null) System.out.println("stream data #"+i+": ["+data.length+"]");
		if (fempty) {
			//if (dataval!=null) ((Dict)obj).put(STREAM_DATA, dataval);	// could be set elsewhere => always read from content stream
			objCache_[i] = null;	// zap what wasn't cached before to preserve incremental processing of long files
		} else if (data!=null) ((Dict)obj).put(STREAM_DATA, data);
		//if (CLASS_DICTIONARY==obj.getClass() && "DCTDecode".equals(((Dict)obj).get("Filter"))) obj=OBJECT_NULL; -- for experiment on RWGL6.pdf

		if (observer!=null) observer.update(null, new Object[] { obj, Integers.getInteger(i) });

		off[i] = writeObject(obj, i, getObjGen(i));
//if (i==96) System.out.println("#96: "+obj+" vs "+OBJECT_NULL+", off[i]="+off[i]);
		//assert !fempty || objCache_[i]==null: i;
	}
	if (fmonitor_) System.out.println();


	// if PDF 1.5 add trailer as object
	if (getVersion().compareTo(1,5)>=0 && getObjIndex(trailer_)==-1) {
//System.out.println("writing trailer @ "+objcnt);
		// xref is an object, and practically should come after objects it indexes
		off[objcnt] = w_.getCount();
		objcnt++; setObjCnt(objcnt); setObject(objcnt-1, trailer_);
	}

	//trailer_.put("Size", new Integer(objcnt)); trailer_.remove("Prev");
	writeXref(trailer_, objcnt, -1L,  off,0,objcnt);
  }


  private void writeCompact(Observer observer) throws IOException {
	readAllObjects();
	int objcnt = getObjCnt();
	long olength = pdfr_!=null? pdfr_.getRA().length(): 4*1024*1024;

	// 1. set /Compact dictionary and fake page
//System.out.println("sorted["+bulkid+"/"+objcnt+"]="+sorted[bulkid]);
	Dict trailer = new Dict(trailer_);
	Dict comp = (Dict)trailer.get(KEY_COMPRESS); assert comp!=null;

	// fake first page for non-aware viewers
	if (true/*comp.get(KEY_COMPRESS_ROOT)==null*/) {	// LATER: respect (and don't duplicate) existing fake
		Dict contents = new Dict(); IRef contentsref = addObject(contents); contents.put(STREAM_DATA, COMPACT_PAGE1);
		//if (trailer.get("Encrypt")!=null && getVersion().compareTo(1,5)>=0) contents.put("CryptFilter", "Identity"); -- when respect CryptFilter during writing
		Dict cat = new Dict(); IRef catref=addObject(cat); cat.put("Type", "Catalog");
		Dict pages = new Dict(); IRef pagesref=addObject(pages); pages.put("Type", "Pages"); pages.put("Count", Integers.ONE); cat.put("Pages", pagesref);
		Dict page1 = new Dict(); IRef page1ref=addObject(page1); page1.put("Type", "Page"); page1.put("Parent", pagesref); page1.put("Contents", contentsref);
		page1.put("MediaBox", new Object[] { Integers.ZERO, Integers.ZERO, Integers.getInteger(612), Integers.getInteger(792) });
		pages.put("Kids", new Object[] { page1ref });
		Dict res = new Dict(); page1.put("Resources", res);
		Dict fontres = new Dict(); res.put("Font", fontres);
		Dict fontdict = new Dict(); IRef fontref=addObject(fontdict); fontdict.put("Type","Font"); fontdict.put("Subtype","Type1"); fontdict.put("BaseFont","Times-Roman");	// core 14
		fontres.put("F1", fontref);

		comp.put(KEY_COMPRESS_ROOT, trailer.get("Root"));
		trailer.put("Root", catref);
	} //else { foreach in connected(comp) fplain_[i]=true; } -- could lead to long chain
	Dict bulkobj = new Dict(5); IRef bulkref = addObject(bulkobj);
	comp.put(KEY_COMPRESS_COMPACT, bulkref);
	comp.put("Producer", new StringBuffer("Multivalent "+multivalent.Multivalent.VERSION));


	writeHeader();

	// 2. compute compact stream, choosing smaller of Flate and BZip2
	fbulk_ = true;	// special writing of indirect references: m n R => mR

	// prepare objects
	convertType1(NFontType1.SUBFORMAT_DECRYPTED);	// FORMAT_CFF ?

	for (int i=0+1; i<objcnt; i++) {
		Object o = getCache(i);
		//if (observer!=null) observer.update(null, obj);

		if (CLASS_DICTIONARY==o.getClass()) {
			Dict dict = (Dict)o;
			byte[] data = getStreamData(dict/*, i*/);

			// certain images never in Compact: large raw image samples, already FlateDecode, JPEG2000, JBIG2
			if ("Image".equals(dict.get("Subtype"))) {
				String filter = (String)getObject(dict.get("Filter")), imgfilter = (String)Images.getFilter(dict, null);
//if (filter==null) System.out.println("raw samples "+data.length);
//if (filter!=null) System.out.println(" "+filter);
//if ("FlateDecode".equals(filter)) System.out.println("skip "+i);
				if ("FlateDecode".equals(filter)) fplain_[i] = true;	// from Predictor
				else if (/*false &&--FL4prnt.pdf takes 36min!*/ imgfilter==null && data.length > 200*1024) {	// raw samples kill BWT sorting time, but do get better compression ratios
					deflateStream(dict, -1);
					fplain_[i] = true;
//if (data.length > 10*1024) System.out.print(data.length+"/"+dict.get("Subtype")+"/"+dict.get("Filter")+" ");
				} else if ("JBIG2Decode".equals(imgfilter) || "JPXDecode".equals(imgfilter)) fplain_[i] = true;
				// but try compressing JPEG (color maps), FAX (Group 3)
			}

			// stuff in /Length because needed for reading back and to prevent deflateStream from deflating objects in Compact stream
			if (data!=null && !fplain_[i]) dict.put("Length", Integers.getInteger(data.length));
		}
	}
	for (int i=0+1; i<objcnt; i++) if (!fplain_[i]) freezeCompact(/*getObject(i,true)*/getCache(i));
	int[] sorted = sortCompact(objCache_, objcnt);

	// a. Flate first
	ByteArrayOutputStream bout = new ByteArrayOutputStream((int)olength);
	Deflater def = new Deflater(Deflater.BEST_COMPRESSION, false);
	OutputStream zout = new DeflaterOutputStream(bout, def, 8*1024);
	writeCompactStream(sorted, zout);
	def.end();
	byte[] zdata = bout.toByteArray();	// shrink

	// b. BZip2, which mutates objects
	//if (olength > 50*1024) ... BZip2 needs to get warmed up, but small data fast anyhow
	bout = new ByteArrayOutputStream((int)olength);
	zout = new org.apache.tools.bzip2.CBZip2OutputStream(bout);
	//zout = new com.colloquial.arithcode.ArithCodeOutputStream(bout, new com.colloquial.arithcode.AdaptiveUnigramModel());
	//zout = new com.colloquial.arithcode.ArithCodeOutputStream(bout, new com.colloquial.arithcode.PPMModel(4)); -- slower and bigger than BZip2
	int ocnt = writeCompactStream(sorted, zout);

	fbulk_ = false;


	boolean fBZip2 = bout.size() + 1024/4/* enough to pay for slower uncompression */ < zdata.length;
	//if (!fBZip2) System.out.println(bout.size()+" > "+fout.size()+" -- Flate wins");
	byte[] data = fBZip2? bout.toByteArray(): zdata;
//if (!fBZip2) System.out.println(", zlen="+zdata.length+" vs bzip2="+bout.size());
	zdata=null; bout=null;	// gc
	//System.out.println("num+off="+bout.size()+" for "+ocnt);
//System.out.println("bulkout.size() = "+bulkout.size()+", bout.size()="+bout.size());

	bulkobj.put("N", Integers.getInteger(ocnt));
	bulkobj.put(STREAM_DATA, data);
	//bulkobj.put("LengthU", new Integer(bout.size()));
	bulkobj.put("Filter", fBZip2? "BZip2Decode": "FlateDecode");
//System.out.println("ulen = "+bout.size()+" vs olen = "+olength);


	// 3. write objects and xref
	int objcntC = getObjCnt();
	long[] off = new long[objcntC];

	// /Compact and fake page
	for (int i=objcnt; i<objcntC; i++) off[i] = writeObject(getCache(i), i,0);	// /Compact and fake page

	// plain
	for (int i=0+1; i<objcnt; i++) {
//System.out.println(i+"=>"+sorted[i]+": "+getObject(sorted[i],false));
		int newi = sorted[i]; 
		Object obj = getObject(newi, false); if (!fplain_[newi] || obj==OBJECT_DELETED) continue;

		if (observer!=null) observer.update(null, obj);
		off[newi] = writeObject(obj, newi, getObjGen(newi));
	}
//System.out.println("bulkid = "+bulkid+" vs  objcnt="+objcnt+", fplain_["+bulkid+"]="+fplain_[bulkid]);

	if (fmonitor_) System.out.println();


	// write xref for objects not in Compact stream: Compact itself, fake page, fplain objects
	List<Integer> sl = new ArrayList<Integer>(10);
	for (int i=0; i<objcnt; i++) {
		if (fplain_[i]) {
			int j=i+1;
			while (j<objcnt && fplain_[j]) j++;
			sl.add(Integers.getInteger(i)); sl.add(Integers.getInteger(j-i));
			i = j - 1;
		}
	}
	sl.add(Integers.getInteger(objcnt)); sl.add(Integers.getInteger(objcntC-objcnt));	// /Compact and fake page 1
	int[] start = new int[sl.size()/2], length = new int[start.length];
	for (int i=0,imax=start.length; i<imax; i++) { start[i] = ((Integer)sl.get(i*2)).intValue(); length[i] = ((Integer)sl.get(i*2+1)).intValue(); }
	writeXref(trailer, objcntC, -1L,  off,start,length);
  }

  private int writeCompactStream(int[] sorted, OutputStream zout) throws IOException {
	byte[] sObj = Strings.getBytes8(" 0 obj\n"), sEndobj = Strings.getBytes8("\nendobj\n"), sStream = Strings.getBytes8("\nstream\n"), sEndstream = Strings.getBytes8("endstream");
	//sObj = sEndobj = sStream = sEndstream = new byte[0];	//156772-155444 = 1328 = ~1%
	sObj = sEndstream = new byte[] { (byte)' ' }; sEndobj = new byte[] { (byte)'\n' }; sStream = new byte[] { (byte)'s' };
	boolean fbzip2 = !(zout instanceof DeflaterOutputStream);

	StringBuffer sb = new StringBuffer(10*1024);
	int ocnt=0;
	for (int i=0+1, objcnt=sorted.length; i<objcnt; i++) {
		if (fmonitor_) { if (i%1000==0) System.out.print((objcnt-i)/1000); else if (i%500==0) System.out.print(" "); }
		int newi = sorted[i];
		Object obj = getObject(newi, false); if (fplain_[newi] || OBJECT_DELETED==obj) continue;
//System.out.println(i+" "+obj);
		//bout.write(Strings.getBytes8(newi+" "+bulkout.size()+" "));

		ocnt++;
		zout.write(Strings.getBytes8(Integer.toString(newi))); zout.write(sObj);
		writeObject(obj, sb, true);
		//unlen += perobj + sb.length();
		if (fbzip2) setObject(newi, OBJECT_NULL);	// wipe objects to conserve memory

		zout.write(StringBuffers.getBytes8(sb)); sb.setLength(0);
		byte[] data = getStreamData(obj/*, i*/);
		if (data!=null) { zout.write(sStream); zout.write(data); zout.write(sEndstream); }
		zout.write(sEndobj);
	}

	zout.close();

	return ocnt;
  }

  /**
	Rewrite objects so as to compress better: decrypt Type 1, maybe remove implicit /Type, remove /Parent from page tree.
	Dual of {@link PDFReader#thawCompact(Object)}.
  */
  private void freezeCompact(Object o) throws IOException {
//System.out.print(newi+" ");
	//Object o = getObject(i, false);	//if (fplain_[i] || OBJECT_DELETED==obj) continue;

  }

  /** Clusters objects by type to improve compression. */
  private int[] sortCompact(Object[] objs, int objcnt) {
	int[] sorted=new int[objcnt]; boolean[] moved = new boolean[objcnt];
	sorted[objcnt-1]=objcnt-1; moved[objcnt-1]=true;	// last is Bulk
	int j=0+1; Object o;

	// page contents
	for (int i=0+1; i<objcnt; i++) {	// NO: if (moved[i]) continue;
		o = objs[i]; assert o!=null: i;
		if (CLASS_DICTIONARY!=o.getClass()) continue;
		Dict dict = (Dict)o;
		if ("Page".equals(dict.get("Type")) || "Template".equals(dict.get("Type"))) {
			o = ((Dict)o).get("Contents"); if (o==null) continue;
			Object[] irefs = CLASS_ARRAY==o.getClass()? (Object[])o: new Object[] { o };
			for (int k=0,kmax=irefs.length; k<kmax; k++) {
//System.out.println("#"+k+": "+o+" "+o.getClass());
				int id = ((IRef)irefs[k]).id;
				if (/*iref!=null &&*/ !moved[id]) { sorted[j++] = id; moved[id]=true; }
			}

		// XObject Form also command stream
		} else if ("Form".equals(dict.get("Subtype"))) {
			if (!moved[i]) { sorted[j++] = i; moved[i]=true; }
		}
	}
//System.out.print("(p"+j+")");


	// group by /Type and /Subtype
	String[] types = { "Page","Pages","Font","FontDescriptor","ExtGState","Encoding","Annot" };
	for (int k=0,kmax=types.length; k<kmax; k++) {
		for (int i=0+1; i<objcnt; i++) { if (moved[i]) continue;
			o = objs[i];
			if (CLASS_DICTIONARY==o.getClass() && types[k].equals(((Dict)o).get("Type"))) { sorted[j++] = i; moved[i]=true; }
		}
	}
	String[] subtypes = { "Link" };
	for (int k=0,kmax=subtypes.length; k<kmax; k++) {
		for (int i=0+1; i<objcnt; i++) { if (moved[i]) continue;
			o = objs[i];
			if (CLASS_DICTIONARY==o.getClass() && subtypes[k].equals(((Dict)o).get("Subtype"))) { sorted[j++] = i; moved[i]=true; }
		}
	}

	// non-streams
	for (int i=0+1; i<objcnt; i++) { if (moved[i]) continue;
		o = objs[i];
		if (CLASS_DICTIONARY!=o.getClass() || ((Dict)o).get("Length")==null) { sorted[j++] = i; moved[i]=true; }
	}

	// everything that doesn't have data-specific compression (not JPEG or FAX)
	for (int i=0+1; i<objcnt; i++) { if (moved[i]) continue;
		o = objs[i];
		boolean fignore = false;
		if (CLASS_DICTIONARY==o.getClass()) {
			Dict dict = (Dict)o; Object filter = dict.get("Filter");
			fignore = "Image".equals(dict.get("Subtype")) && ("DCTDecode".equals(filter) || "CCITTFaxDecode".equals(filter));
		}
		if (!fignore) { sorted[j++] = i; moved[i]=true; }
	}

	// images are all that's left
	for (int i=0+1; i<objcnt; i++) if (!moved[i]) sorted[j++]=i;
	j++; /*Bulk*/ assert j==objcnt: j+" != "+objcnt;
//System.out.println("sorted["+(objcnt-1)+"]="+sorted[objcnt-1]);

	return sorted;
  }


  /*
	Append new and changed objects to existing PDF of backing PDFReader with an incremental update.
	(If you're writing annotations, consider writing an FDF file instead.)
	In fact, this appends all objects that have been read through this PDFWriter object, even if they are not changed.
	Thus in order to append exactly those objects that are new or changed,
	clients should inspect a PDF by reading objects through a PDFReader.
	On the other hand, all objects that refer to modified objects should be written as well.
  */
  private/*for now, LATER public*/ void writeIncremental(Observer observer) throws IOException {
	PDFReader pdfr = pdfr_;
	if (pdfr == null) { writePDF(observer); return; }

	// update /Version in /Catalog?
	// ...

	int objcnt = getObjCnt();
	long[] off = new long[objcnt];

	// scan for modified (that is, non-null) objects
	List<Integer> mods = new ArrayList<Integer>(100);
	//int objcnt0 = pdfr!=null? pdfr.getObjCnt(): 0;
	for (int i=0+1, imax=objcnt; i<imax; i++) {
		Object obj = getCache(i);
		if (obj != null) {
			if (observer!=null) observer.update(null, obj);
			off[i] = writeObject(obj, i, getObjGen(i));
			mods.add(Integers.getInteger(i));
		}
	}

	if (mods.size() > 0) {
		long startxref = w_.getCount();

		// chop into hunks
/*UPDATE		if (getObject(1) == null) writeXref(null, 0, 0, true);	// header
		for (int i=1; i<objcnt; i++) {
			while (getObject(i) == null && i<objcnt) i++;
			int start = i;
			while (getObject(i) != null && i<objcnt) i++;

			writeXref(off, start, i-start, false);
		}

		writeTrailer(trailer_, startxref, objcnt, pdfr!=null? pdfr.getTrailerOffset(): -1L);
*/
	}
  }


  /** Write directly as PDFReader at Java object level, without intervening translation to and from PDF file format. */
  private PDFReader writeReader(Observer observer) throws IOException {
	// check that PDFReader is authorized... (writeBarrier?)
	//SecurityHandler sh = pdfr.getEncrypt().getSecurityHandler();
	//if (!sh.isAuthorized()) throw new ParseException("must set valid password by now");

	int objcnt = getObjCnt();
	for (int i=0+1; i<objcnt; i++) {
		Object obj = getCache(i);
		if (obj==null) obj = pdfr_.getObject(i);
		if (CLASS_DICTIONARY==obj.getClass()) {
			Dict dict = (Dict)obj;
			Object data = dict.get(STREAM_DATA);
			if (data!=null) data = pdfr_.getStreamData(dict, true, true);		// don't expand data -- would just use up memory and time
		}

		if (observer!=null) observer.update(null, new Object[] { obj, Integers.getInteger(i) });

		objCache_[i] = obj;
	}
	return new PDFReader(Arrayss.resize(objCache_, objcnt), trailer_);
  }


  /* Once fully read all objects (streams too), close it to release lock on RA. => may want to share PDFReader across PDFWriters, mindful of object mutation
  private void closeReader() {
	if (pdfr_==null) return;
	try { pdfr_.close(); } catch (IOException ioe) {}	// keep on truckin'
	pdfr_ = null;
  }*/

  /**
	Closes PDFWriter and associated {@link java.io.File} or {@link com.pt.io.OutputStreamTee}.
	If there was a backing PDFReader, any mutated objects are mutated in PDFReader as well, 
	and therefore in most cases that PDFReader instance should be {@link PDFReader#close() closed} as well.
	After closing, the PDFWriter is invalid and should not be used or queried.
  */
  public void close() throws IOException {
	//closeReader();	// X first close backing PDFReader => client
	OutputStreamTee w = w_;
	if (w!=null/*multiple close*/ && w.getCount() > 0) {	// wrote something
		// Every PDF gets exactly one "startxref".
		// In fact, optional for /Linearized, and not used for incremental update, append new trailer.
		if (startxref_ > 0) {
			//w.seek(w.length());	// should be there already
			//assert w.getFilePointer() == w.length(); => same for OutputStream
			w.writeString8("startxref\n");
			w.writeString8(Long.toString(startxref_));
			w.writeString8("\n%%EOF\n");
		}
		w.close();
		w_=null;

		if (outfile_ != null) {
			File f = new File(pdfr_.getURI());
			f.delete();	// maybe reading from earlier and not available to delete
			outfile_.renameTo(f);
		}
	}

	// restore object IRef IDs to original for reuse by PDFReader
	renumber(renum_);
  }
}
