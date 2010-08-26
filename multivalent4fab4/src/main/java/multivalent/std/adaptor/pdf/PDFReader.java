package multivalent.std.adaptor.pdf;

import java.io.*;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.awt.color.ColorSpace;
import java.awt.geom.AffineTransform;
import java.net.URI;
import java.net.URISyntaxException;
import java.lang.ref.SoftReference;

import phelps.io.InputStreams;
import phelps.lang.Integers;
import phelps.lang.Strings;
import phelps.lang.Floats;
import phelps.lang.Bytes;
import phelps.util.Arrayss;
import phelps.util.Version;
import phelps.net.URIs;

import com.pt.io.InputUni;
import com.pt.io.OutputUni;
import com.pt.io.RandomAccess;
import com.pt.io.RandomAccessByteArray;
import com.pt.awt.NFont;
import com.pt.awt.font.CMap;
import com.pt.doc.PostScript;

import multivalent.ParseException;

import static multivalent.std.adaptor.pdf.COS.*;



/**
        Parse Adobe's Portable Document Format (PDF) and construct low-level objects (COS in Adobe terminology: string, number, dictionary)
        and high-level Java objects (Font, Image).
        Also parses Forms Data Format (FDF), which can hold form and annotation data.
        Based on Adobe's PDF 1.5 Reference, available online.
        This class provides easy access to all parts of a PDF for developers familiar with the PDF Reference.
        At this time PDF files of up to 4GB in length are supported.

        <ul>
        <li>Constructors and file control:
                {@link #PDFReader(File,boolean)}, {@link #PDFReader(InputUni,boolean)},
                {@link #getURI()}, {@link #getRA()}, {@link #close()}

        <li>Low-level structure:
                {@link #getLinearized()},
                {@link #getTrailer()}, {@link #getStartXRef()},
                {@link #getObjCnt()}, {@link #getObjOff(int)}, {@link #getObjGen(int)}, {@link #getObjType(int)}

        <li>Bytes on disk to PDF/Java data structure:
                {@link #getObject(int)}, {@link #getObject(Object)},
                {@link #fault()},
                {@link #readObject()} (using {@link #eatSpace(RandomAccess)} and {@link #readInt(RandomAccess)}),
                {@link #getInputStream(Object, boolean)} / {@link #getInputStream(Object)}, {@link #getStreamData(Object, boolean, boolean)},
                {@link #countCached()}

        <li>Structure:
                {@link #getCatalog()}, {@link #getInfo()}, {@link #getMetadata(Object)},
                {@link #getEncrypt()}, {@link #setPassword(String)}, {@link #isAuthorized()},
                {@link #getPageCnt()}, {@link #getPageRef(int)}, {@link #getPage(int)}, {@link #getPageNum(Dict)}

        <li>Page content stream:
                {@link #readObject(InputStreamComposite)} (using {@link #eatSpace(InputStreamComposite)}),
                {@link #readCommand(InputStreamComposite)}, {@link #readInlineImage(InputStreamComposite)},
                {@link #readCommandArray(Object)}

        <li>Higher level Java objects:
                {@link #getFileSpecification(Object)}, {@link #getFileInputStream(Object)},
                {@link #getColorSpace(Object, Dict, Dict)}, {@link #getImage(IRef, AffineTransform, Color)},
                {@link #getFont(Dict, float, AffineTransform, PDF)}, {@link #getCMap(Object)},

        <li>Query:
                {@link #findNameTree(Dict, StringBuffer)}, {@link #findNumberTree(Dict, Number)}

        <li>Repair:
                If an error is found in the xref table, an attempt to repair it is made by sequentially reading the entire PDF
                looking for objects as indicated "<code>m n obj</code>" ... "<code>endobj</code>" at the start of lines.
                If needed, this is done automatically and transparently.
                {@link #isModified()}, {@link #isRepaired()}

        <li>Modernization:
                Older versions of PDF are updated to the current specification (presently 1.5);
                for instance, older PDF stored all named destinations in a single dictionary, whereas current PDF uses a name tree.
                Turned off with {@link #setExact(boolean)}.
        </ul>


        <h3>How to use this class</h3>

        A PDF file is a series of numbered "COS" objects (strings, integers, dictionaries, streams, references to other objects, ...)
        that can be interpreted as images, streams of page-building commands, annotations, and so on, as described in the PDF Reference.
        To access these objects, first create an instance on a <tt>.pdf</tt> file with a constructor.
        Now all the PDF's objects are available by number via {@link #getObject(int)} from 1 to {@link #getObjCnt()}.
        If an object refers to another by an indirect reference ({@link IRef}), {@link #getObject(Object)} will follow the reference to the actual object.
        PDF objects are represented with basic Java data types, e.g., PDF dictionaries as Java Map's, with the complete correspondence given by CLASS_* constants.
        The data for images and page contents are kept in <dfn>streams</dfn>, readable uncompressed and decrypted with {@link #getInputStream(Object)}.

        <p>At a higher level, you can ask this API for pages and to transform them into Java images, fonts, colorspaces, and so on.
        Get a particular page's dictionary by number from <em>1</em> (not 0) to {@link #getPageCnt()}, <em>inclusive</em>, with {@link #getPage(int)}.
        Pages' content streams that describe page appearance can be parsed into individual commands with {@link #readCommand(InputStreamComposite)}.
        High-level versions of images, colorspaces, and fonts are available by passing the PDF object to the appropriate method.



        <h3>See Also</h3>

        <ul>
        <li>Source code {@link tool.pdf.Info} and {@link tool.pdf.Validate} for examples of use.
        <li>{@link PDF} to display pages
        <li>{@link PDFWriter} to write new PDF data format from Java data structures
        </ul>

<!--
        <p>Other PDF manipulation libraries:
        <ul>
        <li>Adobe's <a href='http://partners.adobe.com/asn/developer/acrosdk/docs.html'>Acrobat Core API</a>
                has various layers of interfaces, including a high level one
                that has many functions for specific actions like adding annotations.
                An advantage of this is that the programming language helps ensure correctness of the PDF
                by enforcing fixed sets of type-checked arguments.
                A disadvantage is that you have nearly 3000(!) pages of API to master,
                as compared to about 1000 for the PDF Specification itself.
                In contrast, the Multivalent PDF API is, relatively, just a handful of methods
                and furthermore uses common Java objects ({@link java.util.Map} for PDF dictionary) that you likely already know.
                On the other hand, it does not aid correctness with type checking
                or provide high level interfaces to complex operations.
        <li>lots of others
        </ul>
-->

        @version $Revision: 1.103 $ $Date: 2005/07/26 19:38:02 $
*/
public class PDFReader extends COSSource {
  private static final boolean DEBUG = false;// && multivalent.Meta.DEVEL;
  static final boolean DEBUG_DAMAGED = false;// && DEBUG;

  private static final char[] OBJECT_COMMENT = new char[0];

  /** Alternative signature: "%!PS-Adobe-N.n PDF-M.m".  Rarely seen; archaic. */
  /*public*/ static final String SIGNATURE_ALT = "%!PS-Adobe-";

  //int dhist = 0, dnot = 0;
  private static final int INT_ACCUM_MAX = Integer.MAX_VALUE / 10 - 9;
  private static final AffineTransform TRANSFORM_IDENTITY = new AffineTransform();
  private static final IRef IREF0 = new IRef(0,0);

  /** Canonical copies of common <code>/Name</code>s.  Don't intern because that would put every name into similar table. */
  private static final Map<String,String> NAME_CANONICAL = new HashMap<String,String>(101);
  static {
        String[] names = {
                "Type", "Subtype", "Name",
                "XRef", "ObjStm", "First", "N",
                "Page", "Parent", "Contents", "Kids", "Count", "Rotate", "MediaBox", "CropBox", "BleedBox", "TrimBox", "ArtBox",
                "Length", "Filter", "FlateDecode", "DecodeParms", "LZWDecode", "DCTDecode", "JPXDecode", "CCITTFaxDecode", "PredictorDecode", "ASCII85Decode", "ASCIIHexDecode", "RunLengthDecode",
                "Resources", "XObject", "ExtGState", "Properties", "Group", "ProcSet","Text","ImageC","PDF", "Im0","Im1", "Fm0","Fm1",
                "Font", "Type1", "Type1C", "TrueType", "CIDFontType0C", "XHeight", "StemH", "AveWidth", "MaxWidth", "Leading", "MissingWidth",
                "Form", "FormType", "Matrix", "BBox",
                "ColorSpace", "DeviceRGB", "DeviceCMYK", "Decode",
                "Width", "Height", "BitsPerComponent", "Columns", "Rows",
                "Metadata", "XML",
                "CryptFilter", "CF"
        };
        for (String name: names) NAME_CANONICAL.put(name, name);
  }

  /** List of attributes inheritable in Page tree. */
  /*package private*/ static final /*const*/ List<String> PAGE_INHERITABLE = Collections.unmodifiableList(Arrays.asList(new String[] { "Resources", "MediaBox", "CropBox", "Rotate" }));


  private static final char[] ESCAPE = PostScript.ESCAPE;
  private static final boolean[] WHITESPACE = PostScript.WHITESPACE;
  private static final boolean[] WSDL = PostScript.WSDL;
  /** Array position at character index (0-255) is true iff character is PDF operator. */
  /*package-private--PDF stream*/ static final boolean[] OP=new boolean[0x100];
  static {
        String op = "<[(/0123456789-+."; for (int i=0,imax=op.length(); i<imax; i++) OP[op.charAt(i)] = true;
  }

  /** Translations from abbreviations in inline images to full image. */
  static final /*const*/ Map<String,String> INLINE_ABBREV, INLINE_EXPAND;
  static {
        String[] abbrev = { "BitsPerComponent","BPC",  "ColorSpace","CS",  "Decode","D",  "DecodeParms","DP",  "Filter","F",   "Height","H",  "ImageMask","IM", "Interpolate","I", "Width","W" };
        INLINE_ABBREV = new HashMap<String,String>(abbrev.length); INLINE_EXPAND = new HashMap<String,String>(abbrev.length);
        for (int i=0,imax=abbrev.length; i<imax; i+=2) {
                String exp=abbrev[i], abb=abbrev[i+1];
                INLINE_EXPAND.put(abb, exp);
                INLINE_ABBREV.put(exp, abb);
        }

        // => color space abbreviations accepted by ColorSpaces
        //String[] abbrev_cs = { "DeviceGray","G",  "DeviceRGB","RGB",  "DeviceCMYK","CMYK",  "Indexed","I" };

        // filter abbreviations handled in InputStreamComposite
  }

  private static final String[] STREAM_CMDS = {
        "w", "J", "j", "M", "d", "ri", "i", "gs", "q", "Q", "cm","m","l","c","v","y","h","re","S","s","f","F","f*","B","B*","b","b*","n","W","W*",
        "BT", "ET", "Tc", "Tw", "Tz", "TL", "Tf", "Tr", "Ts", "Td", "TD", "Tm", "T*", "Tj", "TJ", "'", "\"", "d0", "d1",
        "CS","cs","SC","SCN","sc","scn","G","g","RG","rg","K","k","sh","BI","ID","EI","Do",
        "MP","DP", "BMC", "BDC", "EMC", "BX", "EX", "%",
  };
  static { Arrays.sort(STREAM_CMDS); }


  // INSTANCE VARIABLES
  private Dict trailer_ = null; private long startxref_;
  private Dict Catalog_ = null;
  private Dict Info_ = null;
  private Encrypt encrypt_ = null;
  private String magic_;
  private boolean idCheck_;
  private Version version_;
  private int linid_ = -1;

  private int[] objOff_ = null;
  private short[] objGen_;      // used only by encryption => taken from IRef.  PDF Ref: "The maximum generation number is 65,535"
  private byte[] objType_;
  //byte[] objValid_; => represented by special case of objOff_[] == 0
  /** Cached instantiations of objects.  Same size as <var>objOff_</var>. */
  /** Object cache.  <code>null</code> if not cached, or hard ref to small objects, or SoftReference to large objects (dict, array, string). */
  /*package private -- Normalize*/ Object[] objCache_;
  private int[] pageObjNum_ = null;     // fast page # => page object #
  private boolean ffdf_ = false;
  private boolean fexact_ = false;
  private boolean fbulk_ = false;
  private Normalize norm_;
  /** Executed {@link #readStructure2()}. */
  private boolean fstruct_ = false;

  private RandomAccess ra_;
  private long ralen_;
  private URI uri_;
  /** If PDF rewritten from Compact format, points to uncompacted file. */
  private File fileb_ = null;
  private boolean modified_ = false;
  /** <code>true</code> if reconstructed damaged xref or if modified PDF annotations. */
  private boolean repaired_ = false;

  private Map<IRef,AffineTransform> imgctm_ = new HashMap<IRef,AffineTransform>(10);    // save AffineTransform of images in case zoom or reuse image with different transform
  private Map<Object,SoftReference<ColorSpace>> cscache_ = new HashMap<Object,SoftReference<ColorSpace>>(10);   // ColorSpace not a dictionary so can't stuff REALIZED


  /** Constructs new instance corresponding to the <tt>.pdf</tt> <var>file</var>. */
  public PDFReader(File file, boolean idCheck) throws IOException, ParseException {
        this(InputUni.getInstance(file.getCanonicalFile(), null, null), idCheck);       // getCanonical/getAbsolute here so getParent() on plain tail works (Hilfinger)
        assert file!=null;
  }

  /** Constructs new instance corresponding to the {@link com.pt.io.InputUni}. */
  public PDFReader(InputUni iu, boolean idCheck) throws IOException, ParseException {
        assert iu!=null;
        uri_ = iu.getURI();
        ra_ = iu.getRandomAccess(); ralen_ = ra_.length();
        idCheck_ = idCheck;
        try {
                norm_ = new Normalize(this);
                readStructure();
        } catch (IOException ioe) {
                // don't leave RandomAccess open -- caller has no instance to close (Aleksei (kasf))
                close();
        }
  }

  /** Constructs new instance given the data structures of a PDF (for experts). */
  public PDFReader(Object[] objs, Dict trailer) throws IOException {
        assert objs!=null && trailer!=null;
        uri_ = URI.create("data:///"+objs.length/*title*/); ra_=null; ralen_=0L;
        objCache_ = objs;       // objOff_ and objGen_ and objType_ left as null as should never be accessed
        trailer_ = trailer;
        encrypt_ = new Encrypt((Dict)getObject(trailer_.get("Encrypt")), this);
        norm_ = null;
  }


  /**
        Close use and free up resources, including file descriptors.
        This class should be closed when no longer used.
  */
  public void close() throws IOException {
        if (ra_!=null) {
                ra_.close(); ra_ = null;
                if (fileb_!=null) fileb_.delete();
        }
//getLogger() System.out.println("PDFReader.close() "+getURI());
  }


  /**
        Returns associated {@link com.pt.io.RandomAccess}.
        Clients should not cache the return value since the RandomAccess can change.
  */
  public RandomAccess getRA() { return ra_; }

  /** Returns associated URI. */
  public URI getURI() { return uri_; }

  /**
        As a PDF is read in, COS objects are {@link Normalize normalized}.
        Set exact to <code>true</code> to prevent this.
        This should be set <em>immediately</em> after instantiation and setting the encryption password if any;
        after this point is cannot be changed, because that would leave some parts updated and others not and there would be conflicts.
  */
  public void setExact(boolean b) {
        if (Catalog_==null) fexact_ = b;
  }

  /** Modified, perhaps because repaired or annotated. */
  public boolean isModified() { return modified_; }

  public boolean isRepaired() { return repaired_; }

  public String getMagic() { return magic_; }



//+ structure: readXref, readStructure, repair

  /**
        Read parts common to all pages: header, trailer, xref, encrypt.
        If the trailer has no ID array, one is created.
        Password for encrypted files is set through {@link #getEncrypt()}, except that the null user password is tried automatically.
  */
  private void readStructure() throws IOException, ParseException {
        //if (structure is good) return; => allow rereading in case file changed

        // null out previous in case fail
        trailer_ = Catalog_ = null;
        encrypt_ = null;        // null while reading trailer /ID and /Encrypt itself
        reallocObj(1);
        RandomAccess ra = getRA();

        // 1. header "%PDF-m.n" or "%!PS-Adobe-N.n PDF-M.m", somewhere in first 1K
        int offset0 = -1;
        String sig = SIGNATURE;
        for (int i=0,imax=1024, c; i<imax; i++) {
                ra.seek(i);

                sig = SIGNATURE;
                boolean fmatch = true;
                for (int j=0,jmax=sig.length(); j<jmax; j++) {
                        c = ra.read();
                        if (sig.charAt(j) != c) {
                                if (j==1 && c=='F') { sig=SIGNATURE_FDF; imax=sig.length(); ffdf_=true; }
                                else if (j==1 && c=='!' && ra.read()=='P') { sig=SIGNATURE_ALT; j=3-1; jmax=sig.length(); }     // matched initial '%' and already ate '!'
                                else { fmatch=false; break; }
                        }
                }
                if (fmatch) { offset0 = i; break; }
        }
        if (offset0==-1) throw new ParseException("not a PDF: doesn't start with '"+SIGNATURE+"'", 0);
        else if (offset0 > 0) {
                ra.slice(offset0, ra.length()); ralen_ = ra.length();
                // LATER: also handle case where does not affect xref offsets
        }
        ra.seek(sig.length());
        magic_ = (ffdf_ ? SIGNATURE_FDF : sig);
//System.out.println("'"+sig+"' @ "+offset0+" / "+ra.getFilePointer());
//for (int i=0; i<10; i++) System.out.println((char)ra.read());  System.out.println();  ra.seek(0L);
      if (SIGNATURE_ALT == sig) {
          while (!Character.isWhitespace((char) ra.read())) /*skip over "M.n "*/
              ;
          for (int i = 0, imax = "PDF-".length(); i < imax; i++)
              ra.read();
      }
      int major = readInt(ra); ra.read();/*'.'*/ int minor = readInt(ra);       version_ = new Version(major+"."+minor);        // overridden by possible /Version in doc catalog.  Set as String so know exactly when extract when write.
//System.out.println("major="+major+", minor="+minor+" / "+ra.getFilePointer());
        if (idCheck_)
            return;

        assert major >= 1: major;

        Object o;
        if (SIGNATURE_FDF==sig) {
                reallocObj(100);
                int maxm = 0;

                // read all objects
                eatSpace(ra);
                while (true) {
                        long off = ra.getFilePointer();
                        o = readObject();
//System.out.println("new obj '"+o+"', type="+o.getClass().getName());
                        if (o==null) break;     // error
                        else if ("trailer".equals(o)) {
                                trailer_ = (Dict)readObject();
//System.out.println("trailer = "+trailer_);
                                break;
                        } else assert o instanceof Number;
                        int m = ((Number)o).intValue(); if (m>maxm) { maxm=m; if (maxm>=getObjCnt()) reallocObj(getObjCnt()*2); }

                        o = readObject(); assert ((Number)o).intValue()==0;
                        o = readObject(); assert "obj".equals(o);
                        o = readObject(); if (o==null) break;
                        //X objCache_[m] = o;  => could be encrypted?
//System.out.println("obj#"+m+" = "+o);
                        objOff_[m]=(int)off; objGen_[m]=0; objType_[m]=XREF_NORMAL;
                        if (CLASS_DICTIONARY==o.getClass()) getStreamData(new IRef(m,0), true, false);  // skip over stream data
                        o = readObject(); assert "endobj".equals(o): o;
                }

//System.out.println("maxm = "+maxm);
                reallocObj(maxm+1);

        } else {
                // 2. Linearized -- read very first object in file
                ra.seek(0L); eatSpace(ra);      // skips over comments too
                Object numo = readObject(), geno = readObject(), obj=readObject();
                if (CLASS_DICTIONARY==(o=readObject()).getClass() && ((Dict)o).get("Linearized")!=null && CLASS_INTEGER==numo.getClass()) {
                        linid_ = getObjInt(numo);
                        // set offset so can getObject(getLinearized())
                } else linid_ = 0;

                // 3. xref and primary trailer
                try {
                        readXref();
                } catch (ParseException pe) {   // recover from bad/missing EOF, startxref, xref internals
                        if (!repair(pe.getMessage())) throw pe;
                }
//System.out.println("Size = "+getObjCnt()+", OBJECT_NULL="+OBJECT_NULL);

                assert trailer_!=null;  // trailer set by xref15, xref10, or repair.  Have to get a trailer before update so have /Type /Catalog (LATER: scan, though could get confused)
                trailer_.put("Type", "XRef"); normalizeObject(trailer_);
        }

        // 4. Encryption dictionary, if any -- before any other objects, which would be encrypted
//if (DEBUG) System.out.println("trailer: "+trailer_);
        o = trailer_.get("Encrypt");
        // tree walk to read all parts of encryption, which themselves aren't encrypted, before switching on (encrypt_ = e).  pin via-a-vis gc so PDFWriter doesn't re-read
        /*1.5: for (iref: connected(o));*/List<IRef> l=connected(o); for (int i=0,imax=l.size(); i<imax; i++) { IRef iref=l.get(i); objCache_[iref.id] = getObject(iref); }
        Dict edict = (Dict)getObject(o);
        assert edict==null || trailer_.get("ID")!=null; // ID is part of encryption key
        Encrypt e = new Encrypt(edict, this);
        encrypt_ = e;

        readStructure2();
//System.out.println("trailer = "+trailer_+", cat = "+getCatalog());
  }

  /** Read rest of structure, after password has been supplied. => no chance for setExact, and don't want to pass exact in a constructor
  private void readStructure2() throws IOException, ParseException {
        // unbundle embedded files => on demand, and maybe not have to unbundle
  }*/
  /**
        After authorized and possible {@link #setExact()}, setups before making objects available.
        Invoked after readStructure()/setPassword() and after repair().
  */
  private void readStructure2() throws IOException {
        //if (fexact_) return;  // these fixes so important do no matter what
        if (fstruct_ || !isAuthorized()) return;
        fstruct_ = true;

        // objgen[0] = 0xffff done in readXref

        // if in compact bulk format, convert to standard PDF for random access
        //if (!fexact_) => too late
        Dict comp = (Dict)trailer_.get/*direct*/(KEY_COMPRESS);
//System.out.println(KEY_COMPRESS+" = "+comp);
        if (comp!=null) {
                if (comp.get(KEY_COMPRESS_COMPACT)!=null) rewriteCompact(comp);
                Object o = comp.get/*remove*/(KEY_COMPRESS_ROOT); if (o!=null) trailer_.put("Root", o);
                assert Catalog_ == null: Catalog_;
        }

        pageObjNum_ = null;     // triggers page-related setup on demand
  }


  /** Positions before primary trailer, reads via {@link #readXref(boolean)}, sets primary trailer. */
  private void readXref() throws IOException, ParseException {
        RandomAccess ra = ra_;
        // scan from end for "%%EOF"
        long flen = ralen_;
        ra.seek(flen);
        boolean foundEOF=false;
        for (int i=EOF.length(); i<1024/*Adobe limit*/*2; i++) {
                ra.seek(flen-i);
                foundEOF=true; for (int j=0,jmax=EOF.length(); j<jmax; j++) if (ra.read() != EOF.charAt(j)) { foundEOF=false; break; }
                if (foundEOF) { ra.seek(flen-i+1); break; }
        }
        if (!foundEOF) throw new ParseException("can't find '"+EOF+"'", ra.getFilePointer());


        // eol
        int c;
        do { ra.seek(ra.getFilePointer()-1-1); c = ra.read(); } while (c!=0 && WHITESPACE[c]);  // \n, \r, ' ' in Crystal Reports
        ra.read();      // invariant for "back up 2"

        // xref offset
        long off = 0;
        for (long mul=1; true; mul*=10L) {      // read int backwards
                ra.seek(ra.getFilePointer()-1-1);
                if ((c = ra.read())>='0' && c<='9') off += (c-'0')*mul;
                else break;
        }
        // previous line: "startxref"
//System.out.println("xref table offset = "+off); ra.seek(off); for (int i=0; i<20; i++) System.out.print((char)ra.read()); System.out.println();
        if (off<=0) throw new ParseException("no 'startxref' value", ra.getFilePointer());
        else if (off >= ra.getFilePointer()) throw new ParseException("'startxref' in trailer >= file length", off);

        startxref_ = off;       // in /Linearized, last start points to top, then top's /Prev back down.  In incremental, always point up.
        ra.seek(off);
        trailer_ = readXref(true);

        // lots of people get /Size wrong: pdfTeX, Panda, Oracle, so just treat it as a hint
        //assert getObjCnt() == getObjInt(trailer_.get("Size")): getObjCnt()+" vs Size "+trailer_.get("Size");  // guaranteed integer -- don't have xref yet for indirect objects
        //trailer_.remove("Prev");      // => needed for Undo
  }

  /**
        Reads cross-reference table and returns its trailer.
        If <var>all</var> flag is <code>true</code>, read entire table, chaining from trailer to trailer via <code>/Prev</code>.
        Precondition: file pointer is at start of xref table, at the start of the <code>xref</code> keyword.
        Usually the cross-reference table is read automatically at startup.

        @see #getObjOff(int)
        @see #getObjGen(int)
        @see #getObjCnt()

        @return PDF trailer dictionary
  */
  public/*for Undo*/ Dict readXref(boolean all/*for linearized just read first hunk*/) throws IOException, ParseException {
        assert objOff_!=null && ra_!=null;

        RandomAccess ra = ra_;
        eatSpace(ra);   // in practice this happens in damaged PDFs, for some reason
        //o = readObject();     // TS-1169.pdf misaligned, to "ref"
        Dict trailer;
        Object o = readObject(), xnumo;
        if (CLASS_INTEGER == (xnumo=o).getClass() && CLASS_INTEGER==readObject().getClass() && "obj".equals(readObject())       // "m n obj"
                && CLASS_DICTIONARY == (o = readObject()).getClass() && "XRef".equals(((Dict)o).get("Type"))) { // xref stream? one of this if it looks like a duck type deals
//System.out.println("xref stream @ ");
                trailer = (Dict)o;
                checkStream(ra, trailer);
                readXref15(trailer);
                objCache_[getObjInt(xnumo)] = OBJECT_NULL;      // zap xref itself: read in full so not needed in read, and write has to compute from scratch => falls out in refcnt()
        } else if ("xref".equals(o)) trailer = readXref10();
        else throw new ParseException("expected: 'xref' but saw '"+o+"'", ra.getFilePointer());

        // chain of deleted objects not used here.  see PDFWriter
        // ...

        // chained xref
        if (all && (o = trailer.get("Prev"))!=null) {
//System.out.println("Prev = "+o);
                long prev = ((Number)o).longValue() & Long.MAX_VALUE;
                if (prev < ralen_) {
                        ra.seek(prev);
                        readXref(all);  // support possible mix of old and new styles
                } else throw new ParseException("bad Prev", prev);
        }

        // always establish invariant.  should be able to assert, but errors
        //assert objGen_[0] == GEN_MAX: objGen_[0];
        objGen_[0] = (short)GEN_MAX;    // "Sony Electronic Publishing Services" sets to 0
        objType_[0] = XREF_FREE;
        objCache_[0] = OBJECT_DELETED;  // so get 'f' in xref

        return trailer;
  }

  /** Read cross reference stream (introduced in PDF 1.5). */
  private void readXref15(Dict trailer) throws IOException {
        int size = getObjInt(trailer.get("Size"));
        Object[] ol = (Object[])trailer.get("Index");
        if (ol==null) ol = new Object[] { Integers.ZERO, Integers.getInteger(size) };
        Object[] W = (Object[])trailer.get("W");
        int byT = getObjInt(W[0]), byO = getObjInt(W[1]), byG = getObjInt(W[2]);

        InputStream in = getInputStream(trailer);
//for (int i=0; i<20; i++) System.out.print(Integer.toHexString(in.read())+" "); System.exit(0);
        for (int i=0,imax=ol.length; i<imax; i+=2) {
                int start = getObjInt(ol[i]), cnt = getObjInt(ol[i+1]);
                //System.out.println("reading "+start+".. +"+cnt+" "+byT+"/"+byO+"/"+byG);

                int objcnt = getObjCnt(), minobjcnt = start+cnt;
                if (minobjcnt >= objcnt) reallocObj(minobjcnt); // only happens with incremental that reused deleted objects, which is rare => ERROR?
//System.out.println("expanded xref to "+start+" + "+cnt+" = "+minobjcnt);

//System.out.println("subsection start="+start+", cnt="+cnt+", objoff len="+objOff_.length);
                int[] objOff=objOff_; short[] objGen=objGen_; byte[] objType=objType_; Object[] objCache=objCache_;

                for (int j=start,jmax=start+cnt; j<jmax; j++) {
                        byte typ = byT>0? (byte)in.read(): XREF_NORMAL;
                        //for (int k=0; k<5; k++) System.out.print(in.read()+" ");   System.out.println();
                        int off = 0; for (int k=0; k<byO; k++) off = (off<<8) | in.read();
                        int gen = 0; for (int k=0; k<byG; k++) gen = (gen<<8) | in.read();
//if (jmax-j<10) System.out.println("#"+j+": "+typ+" "+off+" "+gen);
                        if (typ >= 2) gen = 0;  // implicitly 0

                        if ((objOff[j]==0 && objGen[j]==0) || gen >/*NOT >=*/ (objGen[i]&0xffff)) {
                                objOff[j]=off; objGen[j]=(short)gen;
                                if (XREF_FREE <= typ&&typ <= XREF_OBJSTMC) {
                                        objType[j] = typ;
                                        objCache[j] = typ==XREF_FREE? OBJECT_DELETED: null;
                                } else { objType[j] = XREF_NORMAL; objCache[j] = OBJECT_NULL; } // unknown type
                        }
                }
        }
        in.close();
  }

  /** Reads old style xref table: <code>xref  first cnt ... <var>20-byte-lines</var></code>. "*/
  private Dict readXref10() throws IOException, ParseException {
        RandomAccess ra = ra_;

        // subsections
        for (int c; (c=ra.read())>='0' && c<='9'; ) {   // can have subsections -- don't trust trailer's /Size
                ra.seek(ra.getFilePointer()-1);
                int start = readInt(ra), cnt = readInt(ra);

                int objcnt = getObjCnt(), minobjcnt = start+cnt;
                if (minobjcnt >= objcnt) reallocObj(minobjcnt);

//System.out.println("subsection start="+start+", cnt="+cnt+", objoff len="+objOff_.length);
                int[] objOff=objOff_; short[] objGen=objGen_; byte[] objType=objType_; Object[] objCache=objCache_;
                final long OFF_MIN = "%PDF-m.n\n".length(), OFF_MAX = ralen_ - "1 0 obj\nendobj\nxref\ntrailer<<>>\nstartxref\n0\n%%EOF".length();

                // entries: exactly 20 bytes each: 10 5 1<space-or-cr><lf>.  e.g., "0000057693 00000 n \n"
                // Important for this to be fast for fast startup on long documents (PDF Reference 1.3b has 15K objects!), so special case reading, which is much faster than you'd guess.
                for (int i=start,imax=start+cnt; i<imax; i++) {
                        c = ra.read();
                        // to get offsets up to 4GB: collect as long, store bits in int, retrieve and mask with & 0xffffffffL back to long
                        long off = ((c-'0')*1000000000L + (ra.read()-'0')*100000000L + (ra.read()-'0')*10000000L + (ra.read()-'0')*1000000L + (ra.read()-'0')*100000L + (ra.read()-'0')*10000L + (ra.read()-'0')*1000L + (ra.read()-'0')*100L + (ra.read()-'0')*10L + (ra.read()-'0')); // readLong() too slow
                        if (c<'0' || c>'9' /*|| off<0L <8L for %PDF-m.n\n" || off>length -- but deleted obj chain*/) { repair("xref"); return trailer_; }
                        ra.read();      // space
                        int gen = (ra.read()-'0')*10000 + (ra.read()-'0')*1000 + (ra.read()-'0')*100 + (ra.read()-'0')*10 + (ra.read()-'0');    // need for encryption
                        ra.read();      // space
                        boolean fvalid = ra.read() != 'f';
                        ra.read();      // eol: " \n" or "\r\n"
                        c = ra.read(); if ('0'<=c && c<='9') ra.seek(ra.getFilePointer()-1);    // if transfered in ASCII mode, may have dropped one byte ("\r\n"="\n")
                        //c = ra.read(); if (!Character.isWhitespace((char)c)) ra.seek(ra.getFilePointer()-1);  // or added one byte (" \n"=>" \r\n")

                        if ((objOff[i]==0 && objGen[i]==0) || gen >/*NOT >=*/ (objGen[i]&0xffff)) {     // first object pointer seen is latest generation, but be safe by checking generation number
//if (objOff[i]>0 || objGen[i]!=0) System.out.println("#"+i+": off="+objOff[i]+" "+objGen[i]+" => "+off+" "+gen);
                                objOff[i] = (int)off;
                                objGen[i] = (short)gen; // X redundant with object description, so simply take from that => needed by PDFOptimizer when encrypting
                                if (fvalid /*&& off>0L*/ && i>0) {
                                        objType[i] = XREF_NORMAL;
                                        objCache[i] = OFF_MIN <= off && off < OFF_MAX? null: OBJECT_NULL;       // gentlesgml.pdf has referenced but missing #557 @ 0
//if (off < OFF_MIN || OFF_MAX < off) System.out.println("invalid offset "+off+" in "+ralen_);
                                } else {
                                        objType[i] = XREF_FREE;
                                        objCache[i] = OBJECT_DELETED;   // set OBJECT_DELETED in case try to read free object by number
                                }
                        } //else System.out.println("#"+i+": off="+objOff[i]+" "+objGen[i]+"  fights off  "+off+" "+gen);
                }
        }

        // followed by trailer -- need for Prev and XRefStm, otherwise ignore attributes in older xrefs (Size wrong, ...)
        ra.seek(ra.getFilePointer()-1); eatSpace(ra);
        long tposn = ra.getFilePointer();
        if (!"trailer".equals(readObject())) throw new ParseException("No trailer after xref table", ra.getFilePointer());      // must be followed by trailer
        Dict trailer = (Dict)readObject();

        // hybrid?
        Object o = trailer.get("XRefStm");
        if (o!=null) { ra.seek(((Number)o).longValue() & Long.MAX_VALUE); readXref(true); }

        return trailer;
  }

  /** Reallocates cross reference type, offset, generation; and object cache. */
  private void reallocObj(int newsize) {
        int n = Math.min(objOff_!=null? getObjCnt(): 0, newsize);
        assert n>=0;

        objOff_ = Arrayss.resize(objOff_, newsize);
        objGen_ = Arrayss.resize(objGen_, newsize);
        objType_ = Arrayss.resize(objType_, newsize);   // + fill expansion with XREF_NORMAL (which is != 0)?
        objCache_ = Arrayss.resize(objCache_, newsize);
        //objCache_ = new Object[objcnt]; => fixed aka non-SoftReference objects
        //if (n < newsize) Arrays.fill(objCache_, n,newsize, OBJECT_NULL);
  }



  /**
        Rewrites Compact format into random-access PDF objects.
        If PDF is small read all objects into memory;
        if PDF is large write external file.
  */
  private void rewriteCompact(Dict comp) throws IOException {
        long start = System.currentTimeMillis(), oldlen = ralen_;

        IRef binref = (IRef)comp.get/*remove*/(KEY_COMPRESS_COMPACT);
        int LengthO = comp.get(KEY_COMPRESS_LENGTHO)!=null? getObjInt(comp.get(KEY_COMPRESS_LENGTHO)): Integer.MAX_VALUE;
        int objcnt = getObjCnt();
        //System.out.println("LengthO = "+LengthO);
        Dict bulk = (Dict)getObject(binref);
        //int version = bulk.getObjInt("Version");
        int N = getObjInt(bulk.get("N"));
        boolean fmem = LengthO < 2*1024*1024;


        // setup
        PDFWriter pdfw=null;
        long[] off=null;
        if (fmem) {
        } else {
                // point to new
                fileb_ = File.createTempFile("pdfb", ".pdf");

//System.out.println("rewriting "+binref+" to "+fileb_);
                pdfw = new PDFWriter(OutputUni.getInstance(fileb_/*, this=>premature getCatalog() */, null));
                pdfw.setObjCnt(getObjCnt());
                Dict tr = getTrailer(), tw = pdfw.getTrailer();
                Object o = tr.get("Encrypt"); if (o!=null) tw.put("Encrypt", o);        // keep encrypted even here!  Could omit for faster reading.
                List<IRef> elist = connected(o); for (int i=0,imax=elist.size(); i<imax; i++) { IRef iref=elist.get(i); pdfw.setObject(iref.id, getObject(iref)); }
                o = tr.get("ID"); if (o!=null) tw.put("ID", o); // used by standard security handler
                pdfw.getVersion().setMin(getVersion());
                pdfw.setObjCnt(objcnt);
                off = new long[objcnt];


                pdfw.writeHeader();
        }


        // read /Compact objects
        InputStreamComposite bin = getInputStream(binref, false);
        Deflater def = new Deflater(Deflater.BEST_SPEED, false);
        fbulk_ = true;  // reading top-level objects from stream
        for (int i=0, c; i<N; i++) {
                eatSpace(bin);
                int num = 0; while ((c=bin.read())>='0' && c<='9') num = num*10 + c - '0';      // else space which we just ate

                // save space by parsing object rather than storing offsets or lengths.  Have to rewrite IRef's anyhow.
                //System.out.println(i+" #"+num/*+" "+o*/);
                Object o = readObject(bin);

                if (bin.peek()=='s') {  // else number for start of new object.  Maybe /Length implies stream, but risky.
                        bin.read();     // eat 's'
                        Dict dict = (Dict)o;
                        int togo = getObjInt(dict.get("Length"));
                        ByteArrayOutputStream bout = new ByteArrayOutputStream(togo);   // uncompressed size so almost never realloc

                        boolean fcomp = dict.get("Filter")==null && !"Type1U".equals(dict.get("Type")) /*&& !fmem*/ && togo>256;
                        // how fast is the fastest compression?  can we buy compression time for less data writing?  YES
                        OutputStream zout = fcomp? (OutputStream)new DeflaterOutputStream(bout, def, 8*1024): bout;
                        InputStreams.copy(bin, zout, false, togo);
                        zout.close();

                        dict.put(STREAM_DATA, bout.toByteArray()); bout=null;   // gc
                        dict.remove("Length");  // but keep remaining filters, as won't be Flate or LZW or ASCII, but rather image
                        //System.out.println(num+"/"+bout.size()+" "+o);
                        if (fcomp) {
                                dict.put("Filter", "FlateDecode");      //dict.put("Length", Integers.getInteger(bout.size()));
                                def.reset();
                        }
                }

                o = thawCompact(o);

                if (getObjOff(num) != 0) {}     // don't overwrite incremental
                else if (fmem /*|| small or ref to interned object...*/) objCache_[num] = o;
                // can't use PDFWriter.writePDF() because have special read and don't want to read in all before writing
                // unfortunately can't save time by shoveling bytes since (1) reading from stream without backtracking, (2) have to rewrite special IRef syntax, (3) rewrite /Length on streams
                else off[num] = pdfw.writeObject(o, num,0);     // updates /Length and encrypts as applicable
/*if (CLASS_DICTIONARY==o.getClass()) {
byte[] data = (byte[])((Dict)o).get(STREAM_DATA);
if (data!=null) System.out.println("#"+num+": "+/*objCache_[num]* /data.length+" "+fmem+" "+getObjOff(num)+" "+((Dict)o).get("Filter"));}*/
        }
        def.end();
        bin.close();
        fbulk_ = false;
        objCache_[binref.id] = OBJECT_NULL;


        // objects outside of bulk segment
        long newlen;
        if (fmem) {
                newlen = 0L;
                // read rest of objects on demand

        } else {
                // rest of objects (ObjStm, annotations, incremental updates)
                for (int i=0+1; i<objcnt; i++) {
                        if (getObjOff(i) == 0L) continue;       // skip bulk itself
                        Object o = getObject(i); getStreamData(new IRef(i,getObjGen(i)), false, true);  //o = thawCompact(o); => NO
//System.out.println("#"+i+": "+o);
                        off[i] = pdfw.writeObject(o, i, getObjGen(i));  // have to copy since can't take from two different files
                }

                Dict trailer = new Dict(trailer_);
                pdfw.writeXref(trailer, objcnt, -1, off, 0,objcnt);
                newlen = pdfw.getOutputStream().getCount();
                pdfw.close();

                InputUni iu = InputUni.getInstance(fileb_, null, null);
                ra_ = iu.getRandomAccess(); ralen_ = ra_.length();
                //file_ = fileb_;               // needed by InputStreamComposite incremental
                //uri_ = iu.getURI(); -- keep original
                //reallocObj(1); try { readXref(); } catch (ParseException canthappen) {}       // set directly from off[]? same # of objects and obj numbers ... different types
                for (int i=0+1; i<objcnt; i++) objOff_[i] = (int)off[i]; Arrays.fill(objType_, 1,objcnt, XREF_NORMAL);
                fileb_.deleteOnExit();  // be safe
                // major_/minor_ & Encrypt the same
                //System.out.println("expanded to "+fileb_); System.exit(0);
        }


        long elapsed = System.currentTimeMillis() - start;
        //if (multivalent.Meta.MONITOR) System.out.println("expansion time = "+elapsed+" ms, length "+oldlen+" => "+newlen+", "+objcnt+" objects");
  }

  /** Dual of {@link PDFWriter#freezeCompact(Object)}. */
  private Object thawCompact(Object o) throws IOException {
        if (CLASS_DICTIONARY!=o.getClass()) return o;

        Dict dict = (Dict)o;
        Object type = getObject(dict.get("Type")), subtype = getObject(dict.get("Subtype"));

        // Can't generally normalizeObject() here because that reads in other objects, which may not be available yet --
        // and don't need to because objects already normalized before going into Compact stream.
        // But keep Type 1U => Type 1 in Normalize so results of Uncompress -fonts get normalized too.
        if ("Type1U".equals(type)) normalizeObject(o);

        return o;
  }



  /**
        Tries to repair xref table and locate trailer.
        Scans file from beginning looking for "m n obj" .. "endobj".
        Sets trailer to first trailer with maximum size: first so picks up /Linearization's, with longest size so picks up incremental additions.
        Computes length of uncompressed streams to enable editing by user.
        @return true if repaired something and thus caller should try previously failed operation again
  */
  private boolean repair(String msg) throws IOException {       // "Reconstruction fails if any object identifiers do not appear at the start of a line or if the endobj keyword does not appear at the start of a line."
        if (isModified()) return false; // already tried to repair

        modified_ = true;       // whether succeed or fail, don't try again
        // maybe throw up an alert box, but this is pretty fast
        if (multivalent.Meta.MONITOR/*DEBUG_DAMAGED*/) System.out.println("repairing: "+msg);   // => Log.  can't be System.err because Tcl test scripts interpret as return code -1

        RandomAccess ra = getRA();
        ra.seek(0L);

        int maxm=Integer.MIN_VALUE, savedcnt=0;
        int objcnt = 2000;
        reallocObj(objcnt);
        objOff_[0] = 0; objGen_[0] = (short)0xffff; objCache_[0] = OBJECT_DELETED; objType_[0] = XREF_FREE;
        Arrays.fill(objOff_, 0); Arrays.fill(objGen_, (short)0); /*Arrays.fill(objCache_, null);--keep good objects, in cast of fault()*/ Arrays.fill(objType_, XREF_NORMAL);   // don't trust existing
        int size = -1;
        int cat = -1;

        for (int c; (c=ra.read())!=-1; ) {
                // (blows past header: "%PDF-1.2")

                if (c!='\r'&& c!='\n') continue;        // start of line
                while ((c=ra.read())=='\r' || c=='\n') {}
                long objstart = ra.getFilePointer() - 1L;


                // 1. "trailer"?
                if (c=='t') {
                        // first (/Lin), biggest (incremental) /Size wins
                        // remove /Prev, since that's wrong, collecting xref anyhow, and the trailer we keep has all, correct information
                        if (ra.read()=='r' && ra.read()=='a' && ra.read()=='i' && ra.read()=='l' && ra.read()=='e' && ra.read()=='r') {
                                //long newtraileroff = ra.getFilePointer() - "trailer".length();
if (DEBUG_DAMAGED) System.out.println("\ttrailer @ "+(ra.getFilePointer()-"trailer".length()));
                                eatSpace(ra);
                                Object o = readObject();
                                if (CLASS_DICTIONARY == o.getClass()) {
                                        Dict dict = (Dict)o;
                                        Object sizeo = dict.get("Size");
                                        if (sizeo != null && CLASS_INTEGER==sizeo.getClass() && ((Number)sizeo).intValue() > size) {
                                                startxref_ = -1;
                                                size = getObjInt(sizeo);
                                                trailer_ = dict;
                                                trailer_.remove("Prev");
if (DEBUG_DAMAGED) System.out.println("\tnew trailer w/size = "+size+": "+dict);        // last trailer wins
                                        }
                                }
                        }
                        continue;
                }


                // 2a. "m n obj" .. "endobj" ?
                if (c<'0' || c>'9') continue;   // "m<whitespace+>"
//System.out.print(" "+objstart);
                int m = c-'0';
                while ((c=ra.read())>='0' && c<='9') m = m*10 + c-'0';
//System.out.println("m="+m);
                if (m==0 || !Character.isWhitespace((char)c)) continue; // could re-set read position but what already read couldn't be part of "m n obj"
                while (Character.isWhitespace((char)c)) c=ra.read();

                if (c<'0' || c>'9') continue;   // "n<whitespace+>"
                long mark = ra.getFilePointer();
                int n = c-'0';
                while ((c=ra.read())>='0' && c<='9') n = n*10 + c-'0';
//System.out.println("n="+n);
                if (!Character.isWhitespace((char)c)) continue;
                while (Character.isWhitespace((char)c)) c=ra.read();

                if (c!='o') { ra.seek(mark); continue; }        // "obj<whitespace+>"
                if (ra.read()!='b' || ra.read()!='j') continue;
//System.out.println(objstart+" .. obj");
                c=ra.read();
                //if (!Character.isWhitespace((char)c)) continue;       // unlikely => not required
                while (Character.isWhitespace((char)c)) c=ra.read();
                int typec = c;
                long contentstart = ra.getFilePointer()-1;

                mark = contentstart; ra.seek(mark);     // if fail below, don't rescan the above but do rescan to look for "m n obj"
                boolean fobj = false;
                while ((c=ra.read())!=-1) {
                        if (c=='e'/*'\r' || '\n'?*/ && ra.read()=='n' && ra.read()=='d' && ra.read()=='o' && ra.read()=='b' && ra.read()=='j') {        // no repeated letters so no tricky backing up
                                fobj = true;
                                mark = ra.getFilePointer();
                                break;
                        }
                }
//System.out.println("endobj, "+fobj); System.exit(1);

                // 2b. found object -- keep?
                if (fobj && (/*m>0 &&--already checked &&*/ m>=objcnt || n>=getObjGen(m))) {    // later or same generation number
                        if (m >= objcnt) { objcnt *= 2; reallocObj(objcnt); }

//System.out.println(m+" "+n+" @ "+objstart+", "+(char)typec+", "+objCache_[m]);
                        objOff_[m] = (int)objstart;
                        objGen_[m] = (short)n;
                        // if (Type /ObjStm) ...
                        //      foreach (num) objType_[num] = XREF_OBJSTMC
                        objType_[m] = XREF_NORMAL;
                        // else objType_[m] = XREF_NORMAL;
                        // if (Type /Catalog && Catalog_==null) trailer.put("Root", new IRef(m,n));

                        if (m >= maxm) maxm = m;
                        savedcnt++;


                        // read and process selected objects: /Catalog, uncompressed streams [LATER: Info]
                        if (typec == '<') {     // hex string, dict, or stream
                                ra.seek(contentstart);
                                Object o = readObject(); Class cl = o.getClass();

                                if (CLASS_DICTIONARY==cl && "Catalog".equals(((Dict)o).get("Type"))) cat = m;   // last /Catalog wins

                                else if (CLASS_DICTIONARY==cl && ra.read()=='s' && ra.read()=='t' && ra.read()=='r' && ra.read()=='e' && ra.read()=='a' && ra.read()=='m') {    // is a stream, not just a dictionary with a /Length key
                                        //eatSpace(ra); => NO!
                                        c=ra.read();
                                        long start = ra.getFilePointer();
                                        if (c=='\r') { c=ra.read(); if (c=='\n') start++; }

                                        Dict dict = (Dict)o;
                                        Object leno = dict.get("Length");
                                        // if uncompressed stream (or no /Length), data not self-limiting and user may have edited it, so look for 'endstream' and update /Length
                                        // could fix on demand when object is read, but that's messy and this is fast anyhow
                                        if (leno==null || dict.get("Filter")==null)
                                        /* find "endstream" by scanning backward.  More efficient and safer than scanning through stream. => easier for random data to trick "endobj" than "endstream"
                                        for (long m = mark - "endstreamendobj".length(); m >= start; m--) {
                                                ra.seek(m);
                                                boolean fend = true;
                                                for (int i=0,imax="endstream".length; i<imax && fend; i++) fend = ra.read()==endstream.charAt(i);
                                                if (fend) ...
                                        }*/
                                        for (int prevc=c; (c = ra.read()) != -1; prevc=c) {
                                                if (/*(c!='\r' && c!='\n'--not required) &&*/ c!='e' || ra.read()!='n' || ra.read()!='d' ||
                                                        ra.read()!='s' || ra.read()!='t' || ra.read()!='r' || ra.read()!='e' || ra.read()!='a' || ra.read()!='m') continue;
                                                long end = ra.getFilePointer() - "endstream".length() - (prevc=='\r'||prevc=='\n'? 1: 0);
                                                Integer length = Integers.getInteger((int)(end - start));
                                                if (!length.equals(leno)) {     // leno maybe IRef and can't getObject() because if so probably written after stream
                                                        //System.out.println("\tfixed length of uncompressed stream "+m+": "+leno+" => "+length);
                                                        dict.put("Length", length);
                                                        objCache_[m] = dict; dict.put(STREAM_DATA, new Long(start));    // no gc of corrected
                                                }
                                                if (mark < end) mark = end;     // validity check.  Should check for all streams.
                                                break;
                                        }
                                }
                        }
                }

                ra.seek(mark);  // at "endobj"
        }


        // Done scanning.  Postpass
        if (trailer_==null && cat>=1) { trailer_=new Dict(5); trailer_.put("Root", new IRef(cat, 0)); }
        // check streams: /Length vs stream..endobj
        // ...

        if (savedcnt==0 || trailer_==null) return false;

        // Must shrink arrays to fit for getObjCnt().
        objcnt = maxm + 1/*obj 0*/;
        reallocObj(objcnt);
        assert objOff_[0]==0: objOff_[0];

        trailer_.put("Size", Integers.getInteger(objcnt));

        // nonexistent or deleted => OBJECT_NULL
        for (int i=0; i<objcnt; i++) if (objOff_[i]==0 || objType_[i]==XREF_FREE) objCache_[i]=OBJECT_NULL;

        // chain of free objects => don't try to reuse in this emergency state
        //for (int i=0, prev=0; i<objcnt; i++) if (objType_[i]==XREF_FREE) { objOff_[prev]=i; prev=i; } // and last one left at 0, which is correct
        objOff_[0] = 0; objGen_[0] = (short)GEN_MAX;

        // reconstruct "Catalog", "Pages", "Root", ...
        // ...

        //if (DEBUG_DAMAGED) for (int i=0,imax=objcnt; i<imax; i++) System.out.print(" "+i+"="+objOff_[i]);
        if (multivalent.Meta.MONITOR) System.out.println("repaired");

        repaired_ = true;
        encrypt_ = new Encrypt((Dict)getObject(trailer_.get("Encrypt")), this);
        readStructure2();

        return true;
  }



//+ structural metadata: version, trailer, catalog, info, encrypt, object offsets and generation

  public Version getVersion() { return version_; }

  /** If document is linearized, returns integer > 0 that is object number of linearization dictionary. */
  public int getLinearized() { return linid_; }

  /**
        Document trailer.
        Required keys: Size, Root (to catalog), ID.  (If no ID exists, one is created.)
        Optional keys: Encrypt, Info.
  */
  public Dict getTrailer() { return trailer_; }

  /** File offset of (last) trailer, which is needed for incremental updates. */
  public long getStartXRef() { return startxref_; }


  /**
        Returns Document catalog.
        <p>Required keys: Type (=='Catalog'), Pages (dictionary),
        <p>Optional keys: PageLabels (number tree),
                Names (dictionary), Dests (dictionary), ViewerPreferences (dictionary),
                PageLayout (name), PageMode (name), Outlines (dictionary), Threads (array), OpenAction (array or dictionary),
                URI (dictionary), AcroForm (dictionary), StructTreeRoot (dictionary), SpiderInfo (dictionary)
  */
  public Dict getCatalog() throws IOException/*, ParseException*/ {
        // create on demand because may be encrypted
        if (Catalog_==null) {
                if (!isAuthorized()) throw new IOException("decrypt first!");   // kinda IOException, which doesn't add a new exception type to many callers, should be assert by my rules
                IRef iref = (IRef)getTrailer().get("Root");
//for (int i=0+1,imax=getObjCnt(); i<imax; i++) System.out.println("#"+i+" @ "+objOff_[i]+" "+objGen_[i]+" "+objType_[i]);
//System.out.println(iref+" "+objOff_[483]+" w/type="+objType_[483]+", "+objCache_[483]);
                Object o = getObject(iref);
//System.out.println(trailer_+", "+iref+", "+objOff_[iref.id]+", "+objCache_[iref.id]+" => "+o);
                if (o==null || OBJECT_NULL==o) throw new IOException("No document catalog.");   // no repair
                try {
                        Catalog_ = (Dict)o;     // must be indirect ref; dates are strings to decrypt
                        assert Catalog_!=null;
                        //assert "Catalog".equals(Catalog_.get("Type")); => not in FDF
                        //assert Catalog_.get("Pages")!=null;

                        // global error correcting HERE because: after chance to setExact(), after Catalog_ made which updatePDF needs, and before anything else happens as need catalog to do anything productive
                        if (!fexact_ && norm_!=null) norm_.normalizeDoc(Catalog_);

                        objCache_[iref.id] = Catalog_;  // no gc

                } catch (Exception e) { // ClassCastException, AssertionError
                        //System.out.println(e);
                        e.printStackTrace();
                        if (repair("getCatalog()")) return getCatalog();
                        //else throw new IOException("corrupt PDF");
                }
        }
        return Catalog_;
  }

  /**
        Returns /Info dictionary from trailer.  Normalizes to remove 0-length values.
        <p>Optional keys: Title (string), Author (string), Subject (string), Keywords (string), Creator (string), Producer (string),
                CreationDate (date), ModDate (date), Trapped (name).
        @return <code>null</code> if no /Info.
  */
  public Dict getInfo() throws IOException {
        if (Info_==null) {
                //if (!isAuthorized()) throw new IOException("decrypt first!");
                Object o = getObject(getTrailer().get("Info")); // must be indirect ref; dates are strings to decrypt
                Info_ = OBJECT_NULL==o? null: (Dict)o;  // set to null rather than new Dict() so forces user to set in trailer before writing (or... new Dict() so don't have to check for null)

                if (Info_!=null) {
                        String[] keys = Info_.keySet().toArray(new String[0]);
                        // "Any entry whose value is not known should be omitted from the dictionary, rather than included with an empty string as its value."
                        for (String key: keys) {        // can't use iterator because want to remove and don't want concurrent mod ex
                                Object val=Info_.get(key);      // getObject(...)
                                /*if (NULL_OBJECT==val) info.remove(key);       // it's possible => handled by parser
                                else*/ if (CLASS_STRING==val.getClass() && ((StringBuffer)val).length()==0) Info_.remove(key);
                        }
                }
        }
        return Info_;
  }

  /**
        Returns metadata associated with object, or return 0-length String if none.
        To obtain the metadata for the document as a whole, pass the document catalog.
  */
  public String getMetadata(Object o) throws IOException {
        String meta = "";

        o = getObject(o);
        if (o!=null && CLASS_DICTIONARY==o.getClass()) {
                Dict dict = (Dict)o;
                IRef iref = (IRef)dict.get("Metadata");
                o = getObject(iref);
                if (o!=null && CLASS_DICTIONARY==o.getClass()) {
                        byte[] data = getStreamData(iref, false, false);
                        meta = new String(data/*, ASCII CharSet*/);
                }
        }

        return meta;
  }


  /**
        Returns document-wide encryption manager.
        User of class should set the password, if any, through this object.
        If the password is null/empty, the password is automatically set.

        @see SecurityHandler#authUser(String)
        @see SecurityHandler#isAuthorized()
  */
  public Encrypt getEncrypt() { return encrypt_; }

  /**
        Set password, returning true if document can be read unencrypted.
        Document may be unencryted for several reasons: not encrypted, password is null and so automatically unlocked, <var>password</var> is correct, password correctly set earlier.
        If password is the owner then all manipulations of the document are permitted; if the password is the user, then it may be restricted.
        <!--Once the password is correctly set, it may not be unset.-->
  */
  public boolean setPassword(String password) throws IOException {
        SecurityHandler sh = getEncrypt().getSecurityHandler();
        boolean valid = sh.isAuthorized();
        if (password!=null) {   // set password even if authorized because may be upgrading to owner password
                valid = sh.authOwner(password) || sh.authUser(password);        // if password is both owner and user, set owner first so get full permissions
                if (valid) readStructure2();
//System.out.println(getURI()+": password = "+password+", valid? "+valid+", authorized now? "+sh.isAuthorized());
        }
        return sh.isAuthorized();
  }

  public boolean isAuthorized() {
        Encrypt e = getEncrypt();
        assert e!=null;
        return e.getSecurityHandler().isAuthorized();
  }


  /** Returns number of pages in document. */
  public int getPageCnt() throws IOException {
        if (pageObjNum_!=null) return pageObjNum_.length;

        // all object-qua-page come here, where we do lazy init
        Dict cat = getCatalog();
        IRef iref = (IRef)cat.get("Pages");
        Object o = getObject(iref);
        int pagecnt = 0;
        if (o!=null && CLASS_DICTIONARY==o.getClass()) {
                Dict root = (Dict)o;
                root.remove("Parent");  // root.get("Parent")==null, not itself
                objCache_[iref.id] = root;      // no gc
                o=getObject(((Dict)root).get("Count")); if (o instanceof Number) pagecnt = getObjInt(o);
        } // else possible to use PDF as data container without pages

        pageObjNum_ = new int[pagecnt];
        Arrays.fill(pageObjNum_, -1);

        return pagecnt;
  }

  /**
        Given page number, finds corresponding a page object.
        Pages are numbered PDF-style: <b>1</b>..{@link #getPageCnt()}, <b>inclusive</b>.
        If object in that position is not a /Type /Page, returns <code>null</code> (not {@link COS#OBJECT_NULL}).
  */
  public IRef getPageRef(int pagenum) throws IOException {
        int pagecnt = getPageCnt();
        assert 1 <= pagenum && pagenum <= pagecnt: pagenum+ " > "+pagecnt+" (1-based)";

        pagenum--;      // internally treat as 0-based
        int id = pageObjNum_[pagenum];
        if (id>=0) return new IRef(id, getObjGen(id));

        // find page
        IRef pageref=null; Dict page=null;
        Object[] kids = new Object[1]; kids[0] = (IRef)getCatalog().get("Pages");

        // fault in as seen so can read one page of 10K page document efficiently
        // PageTree: Type (name='Pages'), Parent (dictionary), Kids (array), Count (integer)
        // find kid
        Dict p = null;  // PageTree or Page dictionary
//System.out.println("getPageRef "+pagenum);
//System.out.println("child #"+kidi+" = "+p);
        for (int kidi=0, base=0; kidi<kids.length/*kids changes as descend*/; ) {
//System.out.println("kids["+kidi+"] = "+kids[kidi]+", type="+kids[kidi].getClass().getName());
                IRef ref = (IRef)kids[kidi];
                Object o = getObject(ref);      // PageTree or Page dictionary
                String type;
                if (CLASS_DICTIONARY==o.getClass()) { p=(Dict)o; type=(String)p.get("Type"); } else type = null;        // invalid treated as /Page (seen in jdj/7-05)

                // could fill missing/bad but computable parts of page tree: "Type", "Parent", "Count", "Kids"
                //IRef[] k2p = new IRef[objcnt];

                if ("Pages".equals(type)) {
                        int count = getObjInt(p.get("Count"));
                        if (pagenum >= base + count) {
//System.out.println("skipping "+base+"+"+count);
                                base += count;
                                kidi++;
                        } else {
                                kids = (Object[])getObject(p.get("Kids"));
                                if (kids.length < count || isModified()) kidi = 0;      // at least one child is /Pages, but don't know which one or don't trust /Count, so descend
                                else { assert kids.length == count;
                                        // X fast path: all IRefs are /Page leaves, so cache all without needing to construct and inspect => /Pages with /Count 1
//System.out.print(" fastpath "+base+".."+(base+kids.length-1));
                                        // cache so that can handle very long /Kids
                                        for (int i=0,imax=kids.length; i<imax; i++) {
                                                IRef r = (IRef)kids[i];
                                                for (o = getObject(r); CLASS_DICTIONARY==o.getClass() && "Pages".equals(((Dict)o).get("Type")); ) {
                                                        o = ((Dict)o).get("Kids");
                                                        if (o!=null && CLASS_ARRAY==o.getClass()) { r = (IRef)((Object[])o)[0]; o = getObject(r); }
                                                        else { o = OBJECT_NULL; r = IREF0; }
                                                }
                                                pageObjNum_[base+i] = r.id;
                                                kids[i] = r;    // fix screwy
                                        }
                                        pageref = (IRef)kids[pagenum-base]; page = (Dict)getObject(pageref);
                                        assert pageref!=null && pageref.id == pageObjNum_[pagenum];
                                        break;
                                }
//System.out.println("descending "+base+".."+(base+count));
//System.out.println("kids = "+p.get("Kids"));
                        }

                } else if ("Page".equals(type)) {
//System.out.println("Page "+base);
                        //assert false; // should always be superceded by fastpath in parent
                        if (base == pagenum) { pageref = ref; page=p; pageObjNum_[pagenum]=ref.id; break; }
                        base++;
                        kidi++;

                } else {        // error (see jdj/7-05)
                        assert type==null: type;
                        if (CLASS_DICTIONARY==o.getClass()) {   // try to correct
                                if (p.get("Kids")!=null) p.put("Type", "Pages");
                                else /*if (p.get("Contents")!=null)--NO, infinite loop*/ p.put("Type", "Page");
                        } else if (base == pagenum) break;      // error: not valid part of page tree -- treated as bogus page -- seen in Panda 0.2
                        else kidi++;

                        // kidi = kidi; -- unchanged so try same object again
                }
        }


        // verify that didn't have erroneous /Count by checking that object is /Type /Page, or rather not /Type /Pages
        if (page!=null && "Pages".equals(page.get("Type")) && !isModified()) { modified_ = true; Arrays.fill(pageObjNum_, -1); return getPageRef(pagenum+1); }

        //assert pageref!=null: pagenum;
        return pageref; // null if bogus object
  }

  /**
        Given page number, finds corresponding a page dictionary.
        Populates inheritable attirbutes by climbing parents as necessary.
        To get page dictionary without inheriting attributess, use <code>getObject(getPage<b>Ref</b>(<var>pagenum</var>))</code>.
        Pages are numbered 1..{@link #getPageCnt()}, inclusive.
        Reverse of {@link #getPageNum(Dict)}.
  */
  public Dict getPage(int pagenum) throws IOException {
        //assert !fexact_;      // mutates
        Object o = getObject(getPageRef(pagenum));
        if (o.getClass() != CLASS_DICTIONARY) return null;
        Dict page = (Dict)o;

        // Page: Type (name='Page'), Parent (dictionary), Resources (dictionary)
        // climb tree for inheritable attributes
        Object[] ival = new Object[PAGE_INHERITABLE.size()];
        for (int i=0,imax=PAGE_INHERITABLE.size(); i<imax; i++) {
                String attr = PAGE_INHERITABLE.get(i);
                // want result to be writable, so always determine inherited value so can see if ok to remove
                for (Dict p=page; (p=(Dict)getObject(p.get("Parent"))) != null; ) {
                        Object val = getObject(p.get(attr));
                        if (val!=null) {
                                ival[i] = val;
                                if (page.get(attr)==null/* or p!=page*/) page.put(attr, val);   // no explicit setting in page itself, so stuff in inherited
                                break;
                        }
                }
        }

        // delete defaults
        // if inherited (CropBox, Rotate), inherited value must be null or equal to explicit
        Object[] mb = (Object[])getObject(page.get("MediaBox"));
        Object[] cb = (Object[])getObject(page.get("CropBox"));
        if (cb==null) cb = mb; else if (Arrays.equals(mb, cb) && (ival[2]==null || Arrays.equals(cb, (Object[])ival[2])/*could have been pruned eariler*/)) page.remove("CropBox");     // seldom have IRefs in coords
        Object[] oa = (Object[])getObject(page.get("BleedBox")); if (Arrays.equals(cb, oa)) page.remove("BleedBox");
        oa = (Object[])getObject(page.get("TrimBox")); if (Arrays.equals(cb, oa)) page.remove("TrimBox");
        oa = (Object[])getObject(page.get("ArtBox")); if (Arrays.equals(cb, oa)) page.remove("ArtBox");
        o = getObject(page.get("Rotate")); if (o!=null && getObjInt(o)==0 && (ival[3]==null || o.equals(ival[3]))) page.remove("Rotate");

//System.out.println("getPage #"+pagenum+" = "+page);
        return page;
  }



  /** Reverse of {@link #getPage(int)}. */
  public int getPageNum(Dict page) throws IOException {
        assert page!=null;
        //if (page==null) return 0;

        // can use == getObject() because if passing reference, then haven't gc'ed it
        // this doesn't scale
        //for (int i=0,imax=getPageCnt(); i<imax; i++) if (page == getPage(i)) return i+1;
        //return -1;

        int num = 0;
        Dict p = (Dict)getObject(page.get("Parent"));
        if (p!=null) {  // root has no parent
                Object[] kids = (Object[])getObject(p.get("Kids"));
                for (int i=0,imax=kids.length; i<imax; i++) {
                        Dict kid = (Dict)getObject(kids[i]);
//System.out.println("step over "+kid);
                        if (page == kid) {      // spend time to construct kid, but cheap and cached
                                num += getPageNum(p);
                                break;
                        } else {
                                if ("Page".equals(getObject(kid.get("Type")))) { num++; /*System.out.print(num+" ");*/}
                                else { num += getObjInt(kid.get("Count")); /*System.out.print(num+" ");*/}
                        }
                }
        } else num++;   // 1-based

        return num;
  }


  /**
        Returns object's byte offset in file.
        In PDF 1.5 this is the cross reference table's field 1.
        N.B. Points to the object header <code><var>n</var> <var>g</var> obj</code>, not to start of content.
        <!-- If offset is 0, object is free; except in the case of object 0, whose offset points to the first free object. => use getObjType() -->
  */
  public long getObjOff(int objnum) {   // return <code>long</code> for compatibility with future PDFs > 2GB in size
        assert objnum>=0 && objnum<getObjCnt(): objnum+" not in [0.."+getObjCnt()+">";
        return objOff_[objnum] & Long.MAX_VALUE;
  }
  /**
        Returns object's generation number.  Generations are used in incremental writing and encryption.
        In PDF 1.5 this is the cross reference table's field 1.
  */
  public int getObjGen(int objnum) {
        assert objnum>=0 && objnum<getObjCnt(): objnum+" not in [0.."+getObjCnt()+">";
        return objGen_[objnum] & 0xffff;        // shorts always signed
  }
  /**
        Returns object's type, which is one of {@link COS#XREF_FREE}, {@link COS#XREF_NORMAL}, or {@link COS#XREF_OBJSTMC}.
        In PDF 1.5, this is the cross reference table's field 0.
  */
  public byte getObjType(int objnum) {
        assert objnum>=0 && objnum<getObjCnt(): objnum+" not in [0.."+getObjCnt()+">";
        return objType_[objnum] /*& 0xff*/;
  }

  /** Returns number of objects, numbered from 0. */
  public int getObjCnt() { return objCache_.length; }



//+ bytes from disk => Java object: getInputStream, readObject (including eatEOL, eatSpace, readInt), getObject
  /**
        Given indirect reference to stream dictionary or array of such references,
        returns stream of uncompressed and decrypted data.
        (Images are not uncompressed here.)
  */
  public InputStreamComposite getInputStream(Object o, boolean iscontent) throws IOException {
        assert o!=null;
        InputStreamComposite cis;
        try {
                cis = new InputStreamComposite(o, iscontent, this);
        } catch (EOFException e) {      // bogus /Length
                if (repair(e.toString())) cis = new InputStreamComposite(o, iscontent, this);
                else throw e;
        }
        return cis;
  }
  /** Same as {@link #getInputStream(Object, boolean)}, assuming not a content stream. */
  public InputStreamComposite getInputStream(Object o) throws IOException { return getInputStream(o, false); }

  /**
        Returns entire content of input stream.
        For a stream use {@link #getInputStream(Object, boolean)} and {@link java.io.InputStream#read()} out the data.
        @return null if dictionary is not a stream
        @param ref      stream dictionary, or indirect ref to stream dictionary.  If PDF is encrypted, must be indirect ref (perhaps freshly created for this purpose, with the right generation number).
        @param fraw             raw data, not passed through filters
        @param fcache   if true save data under {@link COS#STREAM_DATA} key, remove PDF <code>/Length<code> key, and strip out non-image filters from <code>Filter</code> value
  */
  public byte[] getStreamData(final Object ref, boolean fraw, boolean fcache) throws IOException {
        //assert /*getEncrypt()==null ||*/ CLASS_IREF==ref.getClass(); -- also inline image dict

        Object o = getObject(ref); if (o==null || o.getClass()!=CLASS_DICTIONARY) return null;
        Dict dict = (Dict)o;
        //if ("ObjStm".equals(dict.get("Type"))) fcache = false;        // don't cache ObjStm because of double decryption errors
        o = dict.get(STREAM_DATA);
        if (o==null) return null;       // dictionary is not a stream

        String filter = Images.getFilter(dict, this); Object o2 = getObject(dict.get("Filter"));
        boolean filtered = o2!=null && (CLASS_ARRAY==o2.getClass() || filter==null) /*&& not encrypted*/;
        o2 = dict.get("Length"); int Length = o2!=null? getObjInt(o2): 0;       // 2GB max per stream.  Java can't allocate bigger array anyhow.

//if (CLASS_IREF==ref.getClass() && ((IRef)ref).id==30443)
//System.out.println(data+" "+fraw+" "+filtered+", ref="+ref+", o="+o);
        // 1. source of data
        byte[] data;
        if (CLASS_DATA==o.getClass()) { // cached -- not encrypted
                data = (byte[])o;
                if (!fraw && filtered) data = InputStreams.toByteArray(getInputStream(ref, false), data.length);        // can be cached compressed
                // else ok as is, don't refilter

        } else if (o instanceof File) { // external file -- not compressed or encrypted
                data = phelps.io.Files.toByteArray((File)o);

        } else if (Length <= 0) {       // bad PDF (Compact doesn't store /Length but don't get here)
                data = Bytes.ARRAY0;

        } else if (fraw /*|| !filtered*/) { assert o instanceof Long;
                // can be encrypted
                data = new byte[Length];
                getRA().seek(((Number)o).longValue());
                getRA().readFully(data);

        } else {        //assert data==null || filtered; -- could be encrypted
                // more memory efficient to keep content streams compressed => need uncompressed to concatenate content streams + doesn't save much (maybe length of file, say 15MB max)
                // pass original object IRef in case encrypted!
                data = InputStreams.toByteArray(getInputStream(ref, false), 2*Length);
        }


        // 2. cache and update /Filter
        if (fcache) {
                dict.put(STREAM_DATA, data);
                dict.remove("Length");

                if (!fraw) {
                        dict.remove("Filter");  // consumed ASCII/LZW/Flate/Crypt so strip those out, but keep image filters (CCITT/DCT/JBIG2)
                        Object dp = dict.remove("DecodeParms"); // filters for ASCII/compression/Predictor consumed in reading
                        if (filter!=null) {     // keep for image
                                dict.put("Filter", filter);
                                if (dp!=null) {
                                        if (CLASS_ARRAY==dp.getClass()) dp = ((Object[])dp)[((Object[])dp).length - 1];
                                        if (dp!=OBJECT_NULL) dict.put("DecodeParms", dp);
//System.out.println("DP = "+filter+" / "+dp);
                                }
                        }
                }
        }

        assert data!=null;
        return data;
  }

  /** Eat whitespace between tokens in COS object. */
  public void eatSpace(RandomAccess ra) throws IOException {
        int c; while ((c=ra.read())!=-1 && WHITESPACE[c]) {/*eat*/}
        if (c!=-1) ra.seek(ra.getFilePointer()-1);
        if (c=='%') { readObject(); eatSpace(ra); }     // comments can pop up in the middle of objects, and they are rarely used, so completely ignore them
        //return c; --return first char after whitespace?
  }

  /** Read positive integer from file. */
  public int readInt(RandomAccess ra) throws IOException {
        int val=0, c;
        while ((c=ra.read())>='0' && c<='9') val = val*10 + (c-'0');    // val = (val<<3)+(val<<2) + (c - '0')  faster?
        /*if (c!=-1--end of entire PDF never happens && !WHITESPACE[c])*/ ra.seek(ra.getFilePointer()-1);       // may have stopped at delimiter, not whitespace
        eatSpace(ra);
        return val;
  }

  public static Double getReal(double val) { return PostScript.getReal(val); }
  static Double getReal(int whole, int fract, int pow) { return PostScript.getReal(whole, fract, pow); }

  /**
        Returns next COS object from current file position, which may not be a top-level object starting with <code><var>m</var> <var>n</var> obj</code>.
        Ordinarily you want {@link #getObject(int)} or {@link #getObject(Object)} instead.
        Comments, which are rare, are lost; the following object is returned.
        Keywords that are not boolean or null are returned as {@link java.lang.String}s.

        <p>Precondition: file pointer at start of token. <br />
        Postcondition: Eat following whitespace to bring file pointer to start of next token.
  */
  public Object readObject() throws IOException { return readObject(ra_, -1,-1); }
  private Object readObject(RandomAccess ra, int num, int gen) throws IOException {
        assert ra!=null;
        //if (fmonitor_) System.out.print(" o"+num);

        StringBuffer sb=null;

        // should never get ra.read()==-1 since at worst have "%%EOF" at very end, which known to exist by this point
        Object obj;
        int c=ra.read();
        switch (c) {
        case '+': case '-': case '.':   // [different from content stream in that number may be part of IRef]
        case '0': case '1': case '2': case '3': case '4': case '5': case '6': case '7': case '8': case '9':
                // try integer until forced to floating point
                int val=0, whole=0; int div=-100; boolean fneg=false;
                if (c=='+') fneg=false; else if (c=='-') fneg=true; else if (c=='.') div=0; else val=c-'0';     // first char of num
                while ((c=ra.read())!=-1) {     // rest of chars
                        if ('0'<=c && c<='9') { if (val<INT_ACCUM_MAX) { val = val*10 + c-'0'; div++; } }       // catastrophically lose most sig dig of lots of precision!
                        else if (c=='.' && div<0) { div=0; whole=val; val=0; }  // also disallows second '.'.  Set aside whole part so as to avoid overflow with lots of trailing 0s as in "3233.000000"
                        //else if (WSDL[c]) break;      // without backing up... but backing up is cheap with BufferedRA
                        else if (c=='-') {}     // FineReader puts negative sign in middle of number!  Does not delimit number, does not make negative.
                        else { ra.seek(ra.getFilePointer()-1); break; } // avoid if followed immediate by non-whitespace?
                }

                if (fneg) {
                        whole=-whole; val = -val;
                        obj = div<0? (Object)Integers.getInteger(val): getReal(whole, val, div);
                        // avoid probing for possible IRef

                } else if (/*sgn!=0 ||*/ div>=0) {      // decimal point => double.  rare in object structure, but seen in /BBox
                        obj = getReal(whole, val, div);

                } else {        // int or indirect reference -- if looks like indirect reference, it is!
                        eatSpace(ra);   // !
                        obj = null;
                        long branch = ra.getFilePointer();
                        if ('0'<= (c=ra.read())&&c <= '9') {    // iref vs another number
                                ra.seek(branch);        // for now we're buffered so could peek() above and avoid this, but may switch later
                                int n2 = readInt(ra);
                                c = ra.read();
                                if (c=='R') obj = val!=0? new IRef(val, n2): OBJECT_NULL/*OBJECT_DELETED?  error--seen in langspec-2.0*/;       // e.g., "1932 0 obj" or "/Border [0 0 0]"
//if (obj==OBJECT_NULL) { System.out.println("0 0 R @ "+branch); System.exit(1); }
                        }
                        if (obj==null) {        // wasn't indirect reference
                                ra.seek(branch);
                                obj = Integers.getInteger(val); // not a long: Acrobat on 32-bit architectures limit to 2^31 -1
                        }
                }

                break;

        case '(':       // '(' string ')' -- don't see many in objects (vs content streams)
                sb = new StringBuffer(20);      // TUNE
                for (int nest=1; nest>0 && (c=ra.read())!=-1; ) {
//System.out.print(c+"/"+(char)c+" ");
                        if (c=='\\') {
                                c = ra.read();
                                if (c>='0' && c<='7') { // 1, 2, or 3 digits of octal
                                        int octval = (c-'0');
                                        if ((c=ra.read())>='0' && c<='7') {     // peek() would be nice
                                                octval = octval*8 + (c-'0');
                                                if ((c=ra.read())>='0' && c<='7') octval = octval*8 + (c-'0'); else ra.seek(ra.getFilePointer()-1);
                                        } else ra.seek(ra.getFilePointer()-1);
                                        sb.append((char)octval);
                                } else if (c=='\n' || c=='\r') {        // continuation -- don't add to string
                                        if (c=='\r' && (c=ra.read())!='\n') ra.seek(ra.getFilePointer()-1);
                                } else sb.append(ESCAPE[c]);
                        } else if (c=='\n' || c=='\r') {
                                sb.append('\n');        // normalized
                                if (c=='\r' && (c=ra.read())!='\n' && c!=-1) ra.seek(ra.getFilePointer()-1);
                        } else if (c=='(') { nest++; sb.append('('); }
                        else if (c==')') { nest--; if (nest>0) sb.append(')'); }
                        else sb.append((char)c);
                }

                if (getEncrypt()!=null && num>0/*no double decrypt of obj stream*/) getEncrypt().getStrF().reset(num, gen).decrypt(sb);
                obj = decodeUTF16(sb);
                break;

        case '<':       // hex or dictionary
                if ((c=ra.read())=='<') {       // '<<' dictionary '>>' -- "the main building blocks of a PDF document"
                        eatSpace(ra);
                        Dict dict = new Dict(7);        // TUNE
                        while ((c=ra.read())!='>' && c!=-1) {
                                ra.seek(ra.getFilePointer()-1);
                                Object key=readObject(ra, num,gen);
                                Object dval=readObject(ra, num,gen);
//System.out.println(key+" = "+dval);
                                //if (CLASS_STRING==dval.getClass() && ((String)dval).length()>500) {} else
                                if (key.getClass()!=CLASS_NAME) {} //throw new ParseException("Dictionary key is not name: "+key);      // error -- skip
                                else if (dval!=OBJECT_NULL) dict.put((String)key, dval); else dict.remove(key); // null value is same as not existing
                                //if (CLASS_DICTIONARY==dval.getClass() && ((Dict)dval).size()==0) ...
                        }
                        c = ra.read(); if (c==-1) dict.clear(); else assert c=='>': dict;       // closing '>' of pair
                        obj = dict;

                } else {        // '<' string-in-hex '>'
                        ra.seek(ra.getFilePointer()-1); eatSpace(ra);
                        sb = new StringBuffer(50);
                        int hval,hval2;
                        while ((c=ra.read())!='>') {
                                hval=Integers.parseInt(c); if (hval==-1) continue;      // ignore whitespace and foreign characters between pairs
                                hval <<= 4;     // always high nybble even if no pair

                                c = ra.read();
                                if (c==-1 || c=='>') { sb.append((char)hval); break; }
                                else if ((hval2=Integers.parseInt(c))!=-1) sb.append((char)(hval | hval2));
                                else sb.append((char)hval);     // else foreign char, so skip to next
                        }
//System.out.println("HEX = "+sb);

                        if (getEncrypt()!=null && num>0) getEncrypt().getStrF().reset(num, gen).decrypt(sb);
                        obj = decodeUTF16(sb);
                }
                break;

        case '/':       // '/' name
                sb = new StringBuffer(40/*128--max*/);  // could allocate array[128] and throw away immediately -- wouldn't have to check if size ok
                while ((c=ra.read())!=-1) {
                        if (c=='#') sb.append((char)Integers.parseHex(ra.read(), ra.read()));
                        else if (!WSDL[c]) sb.append((char)c);
                        else { ra.seek(ra.getFilePointer()-1); break; }
                }
                obj = Strings.valueOf(sb);
                String canonical = NAME_CANONICAL.get((String)obj); if (canonical!=null) obj = canonical;
                break;

        case '[':       // '[' array ']'
                eatSpace(ra);
                //Object[] buf = new Object[ARRAY_MAX]; // object alloc has stack efficiency in HotSpot... but cloudy waters with creating of contained objects, and want to handle cheaters, so make List -- TUNE
                List<Object> al = new ArrayList<Object>(100);
                while ((c=ra.read())!=-1 && c!=']') {
                        ra.seek(ra.getFilePointer()-1); // could readObject() and have "]" return mean it's done, without backing up all the time
                        al.add(readObject(ra, num,gen));        // nesting -- that was easy
                }
//System.out.println("array["+i+"] = "+buf[i]);
                obj = al.toArray();
                break;

        case '%':       // '%' comment (until <eol>) -- shouldn't see this here
                while ((c=ra.read())!=-1) if (c=='\n' || c=='\r') break;
                obj = OBJECT_COMMENT;
                break;
                //return readObject(ra, num,gen);

        case '\0': case '\t': case '\r': case '\f': case '\n': case ' ': case -1:
                // error!
                //if (DEBUG_DAMAGED) { System.err.println("obj "+num); new Exception().printStackTrace(); System.exit(1); }
                //assert !DEBUG_DAMAGED: num+" "+(int)c;
                //if (num<=0 || !repair("whitespace in COS "+num)) eatSpace(ra);        // try repair, else eat space
                //return readObject(ra, num,gen);
                //throw new Exception...
                //eatSpace(ra); return readObject();
                obj = null;     // let caller handle, with higher level tactic like use repair()'s xref
                break;

        default:        // keyword [different from content stream -- used for xref/[t]railer/obj]
                sb = new StringBuffer(8);
                sb.append((char)c);     // first letter of keyword is unique, except for 'true' and 'trailer': o/t/f/n/x/t[/r]
                while ((c=ra.read())!=-1 && !WSDL[c]) sb.append((char)c);
                ra.seek(ra.getFilePointer()-1);

                obj = sb.toString();    // could have all keywords in a table
                if ("true".equals(obj)) obj=Boolean.TRUE; else if ("false".equals(obj)) obj=Boolean.FALSE;
                else if ("null".equals(obj)) obj=OBJECT_NULL;
                //else if ("endobj".equals(obj)) obj=OBJECT_NULL;       // pathological: "3 0 obj/endobj"
                else assert !DEBUG || "xref".equals(obj) || "trailer".equals(obj) /*|| "stream".equals(obj) => in getObject()*/ || "obj".equals(obj) || !DEBUG_DAMAGED: obj;

                break;
        }

        eatSpace(ra);
        assert obj!=null || !DEBUG_DAMAGED: c;
//System.out.println(obj);

        return obj;
  }

  // "the special conventions for interpreting the values of string objects apply only to strings outside content streams" (PDF Reference section 3.8)
  private static StringBuffer decodeUTF16(StringBuffer sb) {
        StringBuffer usb = sb;
        if (sb.length()>=2 && sb.charAt(0)==0xfe && sb.charAt(1)==0xff) {       // UTF-16
                //assert sb.length() % 2 == 0: sb+" "+sb.length(); => seen trailing '\0' by PDFMaker 5.0
                usb = new StringBuffer((sb.length()/*-2*/) /2); // keep byte order mark because that indicates Unicode not PDFDocEncoding.  Would like to translate, but sometimes strings used for data storage (e.g., /ID) so don't modify
                for (int i=0/*2*/,imax=sb.length(); i+1<imax; i+=2) usb.append((char)((sb.charAt(i)<<8) | sb.charAt(i+1)));
        }
        // also 00 1b <lang code> <country code> 00 1b

        return usb;
  }


  /**
        Returns referenced object, following any indirect references to concrete objects.
        In contrast to other methods, <var>ref</var> can be a Java <code>null</code>,
        so one can easily fully resolve an object that may or may not be present in a dictionary with a <code>getObject(dict.get("key"))</code>.
  */
  public Object getObject(Object ref) throws IOException {
        //assert ref!=null;     // PDF objects never null, but convenient to resolve non-existent dictionary key-value
        //if (ref==null) ref = OBJECT_NULL; => NO, want to cast result and can't cast OBJECT_NULL
        /*while*/if (ref!=null && CLASS_IREF==ref.getClass()) ref = getObject(((IRef)ref).id/*, true*/);
        assert !(ref instanceof IRef): ref;     // ref to ref?  never happens.
        return ref;
  }


  /**
        Returns object from xref table offset at point <i>num</i>, from 0 to {@link #getObjCnt()}, taking from cache if available.
        Object is decrypted if necessary.
        All objects are cached, with {@link java.lang.ref.SoftReference}s so they are automatically garbage collected when memory is tight.
        If the object is a stream, its contents are not read, but the file position of the data (a {@link java.lang.Long}) is stored under a new injected key {@link COS#STREAM_DATA}.
        If the object has been freed ('f' in xref table), {@link COS#OBJECT_DELETED} is returned -- the old object is not available.
        Object number is an <code>int</code> not a <code>long</code>, so it can handle only 2,147,483,647 of the possible 9,999,999,999 objects in a PDF,
        but even very large PDFs seldom have more than 100,000 objects.
  */
  public Object getObject(int num/*, boolean fstreamtoo -- not right place*/) throws IOException {
        assert num>=0;  // && num<getObjCnt(): "invalid object number: "+num+" not in [0.."+(getObjCnt()-1)+">";
        assert getEncrypt()==null || isAuthorized();

//System.out.println(objOff_.length+" "+objGen_.length+" "+objCache_.length);
        // 1. validity check
        if (num<=0 || num >= getObjCnt()) return OBJECT_NULL;   // PDF Ref 1.5 p.91

//if (objCache_[num]==null) System.out.println("\tgetting #"+num+" of "+getObjCnt()+" @ off="+objOff_[num]+", data="+objCache_[num]+", gen="+objGen_[num]);
        // 2. cached or deleted
        Object obj = objCache_[num];
        if (obj instanceof SoftReference) obj = ((SoftReference)obj).get();     // can cache direct objects too for permanent cache (repair does this)
        if (obj != null) return obj;

//if (objCache_[num] instanceof SoftReference) System.out.println("getObject("+num+")");
        // 3. fetch
        byte typ = getObjType(num); long off = getObjOff(num);
//System.out.println("\tcreating #"+num+" @ off="+objOff_[num]+", data="+objCache_[num]+", gen="+objGen_[num]);
        if (XREF_NORMAL==typ) {
                // num, generation, 'obj', content
//System.out.println("getobj "+num+" seek "+objOff_[num]);
                assert off >= 9L;
                RandomAccess ra = getRA();
                ra.seek(off);
                eatSpace(ra);   // needed by apple2_zip, and done by Acrobat without triggering repair
                int onum=readInt(ra);
                assert onum == num: "#"+num+" != "+onum+" @ "+off;      // happens in bad PDFs!
                int ogen=readInt(ra);   // redundant with xref table
                assert ogen == objGen_[num]: "#"+num+": "+objGen_[num]+" != "+ogen;
                Object keyword = readObject();

                if (onum==num && ogen==getObjGen(num) && "obj".equals(keyword)) {
                        //try {
                                obj = readObject(ra, num,ogen);
                        //} catch (IOException ioe) {
                        //      if (DEBUG) System.err.println(ioe+" while reading object #"+num); => getObject is too low level for this
                        //      throw ioe;
                        //}
                }
                if (obj==null) {
                        if (repair("getObject "+num+"/"+getObjGen(num)+" @ "+off+", found "+onum+"/"+ogen+" / "+keyword)) return getObject(num);
                        //else return null;     // throw new ParseException("bad object reference"); => always keep on truckin'
                        else obj = OBJECT_NULL; // "An indirect reference to an undefined object is not an error; it is simply treated as a reference to the null object."
                }

                checkStream(ra, obj);
                // Page streams not cached; page tree and catalog objects have hard links anyhow; font and images cached in Resources... which are referred to only by objects here?
                Class cl = obj.getClass();
                assert CLASS_IREF!=cl: num;     // no IRefs in toplevel objects
//System.out.println("xref "+num+" = "+obj);
//if (CLASS_DICTIONARY==cl) System.out.println("X: "+((Dict)obj).get("Type")+", fexact_="+fexact_);
                normalizeObject(obj);   // before wrapped in SoftReference
                objCache_[num] = CLASS_DICTIONARY==cl || CLASS_ARRAY==cl || CLASS_STRING==cl? new SoftReference<Object>(obj): obj;      // just as cheap to store simple objects as empty SoftReferences
                // if ("ObjStm") return null? OBJECT_OBJSTM?

        } else if (XREF_OBJSTMC==typ) {
                int osnum = (int)off;
//System.out.println("reading from ObjStm #"+off+" = "+getObject(osnum)+" for #"+num+": ");
                Dict objstm = (Dict)getObject(osnum); assert "ObjStm".equals(objstm.get("Type")): objstm;
                int N = getObjInt(objstm.get("N")), First = getObjInt(objstm.get("First"));
//System.out.print("\t"+objstm.toString().substring(0,20));

                RandomAccess bra = new RandomAccessByteArray(getInputStream(new IRef(osnum, getObjGen(osnum))), "r");   // reads full stream, which it shouldn't...

                // using value in xref field 2 could directly read desired object,
                // but have to uncompress data anyhow,
                // so speculatively read other objects since fast, and objects gc-able so no worry about running out of memory
                int[] noff=new int[N*2]; for (int i=0,imax=noff.length; i<imax; i++) noff[i]=readInt(bra);

                Object o;
                //if (noff[0]==0) { for (int i=0; i<20; i++) System.out.println(readObject(bra,-1,-1)+" "); System.exit(1); }
                for (int i=0, speccnt=0; i<N; i++) {
                        int n = noff[i*2];
                        o=objCache_[n]; if (o instanceof SoftReference && ((SoftReference)o).get()==null) o = objCache_[n] = null;      // not already cached
                        if ((speccnt<1000 || num==n) && o==null) {      // speculating...  Cap to guard against pathological cases.
                                //assert i==getObjGen(n): i+" "+getObjGen(n); => stopped storing obj num in gen slot
                                bra.seek(First + noff[i*2+1]);  // always seek since skip objects already cached
                                o = readObject(bra, -1,-1);     // must not be separately encrypted!
//System.out.println(" "+n+"="+o);
                                if (num==n) obj = o;    // guarantee requested object not knocked out by gc after reading lots of subsequent speculatives
                                else speccnt++;

                                Class cl = o.getClass();
//System.out.println("ObjStm "+n+" = "+o);
                                objCache_[n] = CLASS_DICTIONARY==cl || CLASS_ARRAY==cl || CLASS_STRING==cl? new SoftReference<Object>(o): o;

                        } else noff[i*2] = 0;   // mark so don't re-normalize
                }

                // normalize AFTER read all in stream or else possible cycles
                for (int i=0; i<N; i++) {
                        int n = noff[i*2]; if (n==0) continue;
                        o = objCache_[n]; if (o instanceof SoftReference) o = ((SoftReference)o).get();
                        if (o!=null) normalizeObject(o);        // could already be gc'ed!
                }

                bra.close();
//if (DEBUG) System.out.println();
        } else { assert XREF_FREE==typ: "#"+num+" = "+typ+", obj = "+objCache_[num];    // XREF_FREE should have already returned OBJECT_DELETED, but 11409.pdf points to freed object
                obj = OBJECT_NULL;
        }

//System.out.println("#"+num+", type = "+obj.getClass().getName()+": "+obj);
        assert obj!=null: num;
        return obj;
  }

  /** Pin object <var>num</var> to prevent losing any mutations on it to garbage collection. =>
  /*package-private for Normalize, Action* / void pinObject(int num) {  // maybe getObject(int num, boolean pin) to avoid gc race condition (gc thread ever preempt if not forced by low memory?)
        Object o = objCache_[num];
        if (o instanceof SoftReference) {
                o = ((SoftReference)o).get();
                if (o!=null) objCache_[num] = o;
        }
  }*/

  /**
        Ordinarily PDFReader read-only, but there are special cases.
        {@link Normalize} wants to pin vis-a-vis garbage collection by stripping SoftReference.
        {@link Forms} wants to null object so reread after mutating.
  */
  /*package-private*/ void setObject(int num, Object obj) {
        objCache_[num] = obj;
  }

  /** If object is a stream, stuff file position into {@link COS#STREAM_DATA} for future. */
  private boolean checkStream(RandomAccess ra, Object obj) throws IOException {
        boolean isstream = false;
        if (CLASS_DICTIONARY==obj.getClass() && ((Dict)obj).get("Length")!=null /*-- can non-streams have Length attribute?  don't need a guarantee*/) {
                // since indirect, don't need to maintain seek position
                //if ("stream".equals(readObject())) { NO. read only one or two bytes after "stream", as stream can start with whitespace
                //RandomAccess ra = ra_;
                isstream=true; for (int i=0,imax="stream".length(); i<imax; i++) if ("stream".charAt(i) != ra.read()) { isstream=false; break; }
                if (isstream) {
                        int c=ra.read();
                        long posn = ra.getFilePointer();
                        if (c=='\r') { c=ra.read(); if (c=='\n'/*bug in gobeProductive 3.0d1*/) posn++;  }      // "\r\n" or "\n", not "\r" alone
                        // don't cache stream content, as not used again (page content) or transformed (Font, Image)
                        ((Dict)obj).put(STREAM_DATA, new Long(posn));   // stuff file pointer in synthetic attribute so can seek() to it later
//System.out.println("start @ "+ra.getFilePointer()+", first char = "+ra.read());
                }
                //readObject(ra); -- verify "endstream"?
        }
        return isstream;
  }

  private void normalizeObject(final Object obj) throws IOException {
        if (!fexact_ && norm_!=null) norm_.normalizeObject(obj);
  }


  /**
        Faults into cache all objects reachable in document (starting from trailer),
        and sets unreachable objects (that have not been previously read by the caller) to {@link COS#OBJECT_NULL}.
        Some bad PDFs have cross reference entries to non-existent objects,
        but these objects aren't referenced by other objects so viewing works fine.
        This method ensures that a loop over all objects won't encounter an error either.
  */
  public void fault() throws IOException {
        connected(getTrailer());

        // for objects not referenced and therefore not read, null => OBJECT_NULL
        for (int i=0+1,imax=getObjCnt(); i<imax; i++) if (objCache_[i]==null) objCache_[i]=OBJECT_NULL; // if object gc'ed, still have SoftReference
  }

  /**
        For performance tuning,
        teturns count of different objects that have been cached (but may have been subsequently garbage collected).
  */
  public int countCached() {
        int cnt = 0;
        for (int i=0+1,imax=getObjCnt(); i<imax; i++) if (objCache_[i]!=null) cnt++;
        return cnt;
  }

  /** Clears all cached objects, which may have been mutated. */
  /*
        Reset for reuse after possible mutation of PDF objects by {@link PDFWriter}.
        <!-- This recycles data streams -- you promise not to have modified! -->
  */
  public void reset() {
        //Arrays.fill(objCache_, 1,getObjCnt(), null);
        Catalog_ = null; Info_ = null;
        for (int i=0+1,imax=getObjCnt(); i<imax; i++) {
                Object o = objCache_[i];
                if (o!=null && o instanceof SoftReference) o = ((SoftReference)o).get();
                if (o!=null && (CLASS_DICTIONARY==o.getClass() || CLASS_ARRAY==o.getClass())) objCache_[i] = null;
                // maybe preserve streams, which are expensive, if requested by flag
        }
  }



//+ content stream: readObject(InputStreamComposite), eatSpace(InputStreamComposite)
  /**
        Reads a complete object from a content stream: int, string, dictionary, map, ..., including all subparts.
        <!--
        Different (if only slightly) than {@link #readObject()} in structure, even allowing for RandomAccess vs InputStream.  Only about 100 lines that you'd like to share.
        Has to be recursive so can read array and dictionary.
        -->
  */
  public Object readObject(InputStreamComposite in) throws IOException {
        assert in!=null;

        Object obj;     // set by each branch
        StringBuffer sb;

        int c=in.read();
        switch (c) {

        case '/':       // name
                sb = new StringBuffer(30/*10?*/);       // usually very short, like /Im1 or /F12 or /BitsPerComponent
                while ((c=in.read())!=-1) {
                        if (c=='#') sb.append((char)Integers.parseHex(in.read(), in.read()));
                        else if (!WSDL[c]) sb.append((char)c);
                        else { in.unread(c); break; }
                }
                //obj = sb.substring(0);        // avoidable copy but save space
                obj = sb.toString();    // objects in content stream thrown away quickly as commands are processed, so emphasize speed
                //hist['/'].update(sb.length());
                break;

        case '(':       // string
                sb = new StringBuffer(16/*small because return this*/); // running out of Eden space vs doubling

                for (int nest=1; nest>0 && (c=in.read())!=-1; ) {
                        if (c=='\\') {
                                c = in.read();
                                if (c>='0' && c<='7') { // 1, 2, or 3 digits of octal
                                        int octval = (c-'0');
                                        if ((c=in.read())>='0' && c<='7') {
                                                octval = octval*8 + (c-'0');
                                                if ((c=in.read())>='0' && c<='7') octval = octval*8 + (c-'0'); else in.unread(c);
                                        } else in.unread(c);
//System.out.println("octal => "+(char)octval+"/"+Integer.toOctalString(octval)+"(decimal="+octval+")");
                                        sb.append((char)octval);
                                } else if (c=='\n' || c=='\r') {        // continuation -- don't add to string
                                        if (c=='\r' && (c=in.read())!='\n') in.unread(c);
                                } else sb.append(ESCAPE[c]);
                        } else if (c=='\n' || c=='\r') {
                                sb.append('\n');        // normalized
                                if (c=='\r' && (c=in.read())!='\n') in.unread(c);
                        } else if (c=='(') { nest++; sb.append('('); }
                        else if (c==')') { nest--; if (nest>0) sb.append(')'); }
                        //else if (c==-1) throw new IOException("string spit across streams");
                        else sb.append((char)c);        // [different from File]
                }
                //c=in.read(); -- not invariant anymore
                obj = sb;       // usually further process vis-a-vis encoding
                if (fbulk_) obj = decodeUTF16(sb);
//System.out.println("string: |"+obj+"|);       //next char="+(char)c*/);
                //hist['('].update(sb.length());
                break;

        case '[':       // array
                eatSpace(in);
                List<Object> al = new ArrayList<Object>(100);   // usually TJ so would init to 10, but copy at end anyhow so avoid doubling
                while ((c=in.peek())!=']' && c!=-1) al.add(readObject(in));
                if (in.read()!=']') throw new IOException/*ParseException*/("array spit across streams");       // ... but just reading this object
                obj = al.toArray();
                //hist['['].update(al.size());
                break;

        case '<':       // dictionary or hex
                if ((c=in.read())=='<') {       // second '<' => dictionary
                        Dict dict = new Dict(5);        // used by BMC
                        eatSpace(in);
                        while ((c=in.read())!='>') {
                                if (c==-1) throw new IOException("dictionary spit across streams");
                                in.unread(c);
                                Object key=readObject(in);      //System.out.print(key+" ");
                                if (in.peek()==-1) throw new IOException("dictionary split across streams");
                                Object dval=readObject(in);     //System.out.print(dval+" ");
                                //if (CLASS_NAME!=key.getClass()) System.out.println("key not name: "+key+" in "+dict);
                                if (dval!=OBJECT_NULL) dict.put((String)key, dval); else dict.remove(key);
                        }
                        c=in.read(); assert c=='>';     // second '>' of closing '>>'
                        obj = dict;
                        //hist['<'].update(dict.size());

                } else {        // hex, which are actually PDF strings, which are Java StringBuffer.  rare in content stream
                        in.unread(c); eatSpace(in);
                        sb = new StringBuffer(100);
                        int hval, hval2;
                        while ((c=in.read())!='>') {
                                hval=Integers.parseInt(c); if (hval==-1) continue;
                                hval <<= 4;

                                c = in.read();
                                if (c==-1 || c=='>') { sb.append((char)hval); break; }
                                else if ((hval2=Integers.parseInt(c))!=-1) sb.append((char)(hval | hval2));
                                else sb.append((char)hval);
                        }
//System.out.println("HEX = "+sb+", len="+sb.length()+", "+Integer.toHexString(sb.charAt(0))+" "+Integer.toHexString(sb.charAt(1)));
                        //hist['('].update(sb.length());
                        obj = sb;
                        //c=in.read();  // invariant
                        if (fbulk_) obj = decodeUTF16(sb);
                }
                break;

        case '+': case '-': case '.':   // int or float, actually return int or double objects.
        case '0': case '1': case '2': case '3': case '4': case '5': case '6': case '7': case '8': case '9':
                int/*never long in content stream*/ val=0, whole=0, div=-100; boolean fneg=false;
                if (c=='+') fneg=false; else if (c=='-') fneg=true; else if (c=='.') div=0; else val = c-'0';
                while ((c=in.read())!=-1) {
                        if ('0'<=c && c<='9') { if (val<INT_ACCUM_MAX) { val = val*10 + c-'0'; div++; } }
                        else if (c=='.' && div<0) { div=0; whole=val; val=0; }
                        else if (c=='-') {}
                        else if (c==-1) throw new IOException("number split across streams");
                        else break;
                }
                if (c=='R' && fbulk_) { obj = new IRef(val, 0); break; }        // special syntax for IRef that can immediately distinguished from Integer; illegal in official PDF
                else in.unread(c);

                if (fneg) { val = -val; whole=-whole; }

                // we make lots of new objects in order to put into dictionaries and heterogeneous arrays
                if (div<0/*<=0*/) obj = Integers.getInteger(val)/* "1." not considered an int*/; else obj = getReal(whole, val, div);
//System.out.println((div<0? ""+val: whole+" + "+val+"*10^-"+div)+" = "+obj);
//System.out.println(" n"+obj);
                break;

        case '%':       // comment: "%..<eol>", treated as single space, no semantics
                while ((c=in.read())!=-1) if (c=='\n' || c=='\r') break;
                obj = OBJECT_COMMENT;
                break;

        case '\0': case '\t': case '\r': case '\f': case '\n': case ' ': case -1:
                assert false: c;        // comments processed in caller, and should see no whitespace
                obj = null;
                break;

        default:        // keyword -- could determine outcome by first letter and then just check, but these not common
                sb = new StringBuffer(20);
                sb.append((char)c);
                while ((c=in.read())!=-1 && !WSDL[c]) sb.append((char)c);
                in.unread(c);

                obj = sb.toString();
                if ("true".equals(obj)) obj=Boolean.TRUE; else if ("false".equals(obj)) obj=Boolean.FALSE;
                else if ("null".equals(obj)) obj=OBJECT_NULL;
                //else assert false: obj; -- NFontType3 uses to read operator
        }

        eatSpace(in);

        assert obj!=null;       // null (not the OBJECT_NULL)
        return obj;
  }

  /** Eat whitespace between tokens in content stream. */
  public void eatSpace(InputStreamComposite in) throws IOException {
        assert in!=null;

        int c;
        while ((c=in.read())!=-1 && WHITESPACE[c]) {}
        in.unread(c);
        if (c=='%') { readObject(in); eatSpace(in); }
  }


  /**
        Parses content stream into array of commands.
        If PDF is encrypted, <var>contentstream</var> must be {@link IRef}.

        @see PDFWriter#writeCommandArray(Cmd[], boolean)
        @see Cmd
  */
  public Cmd[] readCommandArray(Object contentstream) throws IOException {
        List<Cmd> opl = new ArrayList<Cmd>(1000);

        InputStreamComposite in = getInputStream(contentstream, true);
        for (Cmd cmd; (cmd = readCommand(in))!=null; ) opl.add(cmd);
        in.close();

//System.out.println("opl len="+opl.size());
        return opl.toArray(new Cmd[opl.size()]);
  }

  /**
        Parse next command from content stream, or return <code>null</code> if no more.
        For inline images (BI..ID..EI), expands abbreviations in dictionary (in ops[0]) and strips non-image filters from data (in ops[1]).
  */
  public Cmd readCommand(InputStreamComposite in) throws IOException {
        Object[] ops=new Object[6]; int opsi=0;
        String op = null;

        eatSpace(in);
        for (int c; (c=in.peek())!=-1; ) {
                if (OP[c]/*!Character.isLetter((char)c) && c!='\'' && c!='"' && c!='%'*/) {     // OPERAND
                        if (opsi==6) throw new IOException("array split across streams");       // happens if all of the following: arrays of streams, object split across streams, just reading single stream
                        ops[opsi++] = readObject(in);

                } else {        // OPERATOR
                        //in.unread('/');       // fake out to get name/java.lang.String
                        //String op = (((char)c) + (String)readObject(in)).intern();    // hope intern is faster now
                        char[] or = new char[3];
                        for (int i=0,imax=3+1; i<imax; i++) {
                                int ch = in.read();
                                if (ch==-1 || WSDL[ch]) {
                                        if (i==0) throw new IOException("null command");
                                        op = new String(or, 0, i); in.unread(ch);
                                        int inx = Arrays.binarySearch(STREAM_CMDS, op); // faster than intern()
                                        if (inx>=0) op = STREAM_CMDS[inx];
                                        break;
                                } else {
                                        //assert i<3: "bad command: "+new String(or)+(char)ch;
                                        or[i] = (char)ch;
                                }
                        }
                        assert op!=null;
//if ("BI".equals(op)) System.out.println("BI, opsi="+opsi+": "+ops[0]+", "+ops[1]);

                        // special cases: comment, inline image, corrections for buggy PDF generators
                        // Always check opsi because may have been split across streams.
                        if ("%".equals(op)) {
                                //System.out.println("comment");
                                StringBuffer sb = new StringBuffer(80);
                                while ((c=in.read())!=-1 && c!='\r' && c!='\n') sb.append((char)c);     // comment
                                ops[0]=sb.substring(0); opsi=1;

                        } else if ("BI".equals(op) /* && opsi>=0*/) {
                                eatSpace(in);
                                Dict iidict = readInlineImage(in); assert iidict.get("F")==null: iidict;

                                // Data occasionally compressed.  If so, eradicate that LZW or Flate, as more efficient not to compress separately from rest of stream.
//System.out.println(iidict);
                                String filter = Images.getFilter(iidict, null);
                                Object f = iidict.get("Filter");        // "F" canonicalized by readlineImage() to "Filter"
                                Object comp = f==null? null: CLASS_ARRAY==f.getClass()? ((Object[])f)[0]: /*f is string*/!f.equals(filter)? f: null;
//System.out.println("inline "+f+", compression = "+comp);
                                if ("LZW".equals(comp) || "LZWDecode".equals(comp)) {   // X f!=null => Flate on erroneous Ghostscript
                                        iidict.put(STREAM_DATA, /*bout.toByteArray()*/getStreamData(iidict, false, false));     // strip filters
                                        //REPLACE WITH: getStreamData(iidict, false, true);

                                        Object o = iidict.remove("DecodeParms");
                                        if (filter!=null) { iidict.put("Filter", filter); if (o instanceof Object[]) { Object[] oa = (Object[])o; iidict.put("DecodeParms", oa[oa.length-1]); } }
                                        else { iidict.remove("Filter"); iidict.remove("DecodeParms"); }
                                }
//System.out.println("filter = "+filter+", "+((byte[])iidict.get(STREAM_DATA)).length+", => "+iidict);
                                byte[] data = (byte[])iidict.remove(STREAM_DATA);

                                // if raw image data short, shrink height.
                                // Acrobat apparently has specific hack for Ghostscript 5.50, which report one too many lines AND uses Flate.  If uncompress this, then Acrobat hack doesn't kick in.  So fix here.
                                /*if (filter==null) {
                                        int w = ((Number)iidict.get("Width")).intValue(), h = ((Number)iidict.get("Height")).intValue(), bpc = ((Number)iidict.get("BitsPerComponent")).intValue();
                                        Object cs = iidict.get("ColorSpace"); int spd = "G".equals(cs) || "DeviceGray".equals(cs) || "I".equals(cs) || "Indexed".equals(cs)? 1: "CMYK".equals(cs) || "DeviceCMYK".equals(cs): 4: 3;
                                        int scanline = (w*spd * bpc + 7) / 8;
                                        if (data.length < scanline * h) iidict.put("Height", Integers.getInteger(data.length / scanline));
                                }*/

                                ops[0]=iidict; ops[1]=data; opsi=2;

                        } else if ("TJ".equals(op) && opsi>=1) {        // FOP 0.20.3 has bug that inserts bogus "Tc" into "[..]TJ"
                                Object[] oa = (Object[])ops[0];
                                int i=0, j=0;
                                for (int imax=oa.length; i<imax; i++) {
                                        Object o = oa[i]; Class cl = o.getClass();
                                        if (CLASS_STRING==cl) oa[j++] = o;
                                        else if (CLASS_INTEGER==cl && ((Number)o).intValue()!=0) oa[j++] = o;   // 0 has no effect whatever the source
                                        else if (CLASS_REAL==cl && ((Number)o).doubleValue()!=0.0) oa[j++] = o; // 0 has no effect whatever the source
                                        // maybe check if previous same class and if so concatenate strings or add numbers
                                        // else ignore
                                }
                                if (j<i) ops[0] = Arrayss.resize(oa, j);
                        }

                        eatSpace(in);
                        break;
                }
        }

//System.out.print(" "+op+"/"+opsi);
        return op!=null || opsi>0? new Cmd(op, ops, opsi): null;        // opsi>0 && op!=null => args split over substreams
  }


  /**
        Parse inline image from stream into a dictionary with its attributes
        and the data in a {@link COS#CLASS_DATA} under key {@link COS#STREAM_DATA}.
        Abbreviated keys (but not values) are expanded
        (e.g., <code>/F</code> => <code>/Filter</code>, but not the color space value <strike><code>G</code> => <code>DeviceGray</code></strike>),
        and non-image filters on the data (such as LZW) are removed.
        On entry input stream should be placed after the <code>BI</code> and following whitespace;
        on exit input stream is immediate after closing <code>EI</code>.
  */
  public Dict readInlineImage(/*imgdict created here,*/ InputStreamComposite in) throws IOException {
        int c;

        // dictionary w/o << and >> so can't reuse code
        Dict iidict = new Dict(7);

        //eatSpace(in);
        while ((c=in.peek())=='/') {
                Object key = readObject(in), val = readObject(in);
                String full = INLINE_EXPAND.get((String)key); if (full!=null) key = full;
                if (val!=OBJECT_NULL) iidict.put(key, val);
        }

        c=in.read(); assert c=='I'; c=in.read(); assert c=='D'; // 'ID'
        c=in.read(); assert Character.isWhitespace((char)c);    // followed by a single white-space character; the next character after that is interpreted as the first byte of image data
//System.out.println(iidict+", ID .. EI = "+c+" // "+in.peek()+" vs "+(int)'\r'+"/"+(int)'\n');
        if (c=='\r' && in.peek()=='\n') in.read();      // EPodd FAX by Distiller 2.0 for Windows
//System.out.println("first = "+in.peek()+"/"+(char)in.peek());

        // image data until 'EI' -- not correct because doesn't decode, but still could have EI in image data.  (xpdf lets image building pull data.)
        ByteArrayOutputStream bout = new ByteArrayOutputStream(4 * 1024);
        for (int prev=-1; c!=-1; prev=c) {
                c = in.read();
                if (c=='E' && in.peek()=='I') {
                        c = in.read();  // 'I'
                        if ((prev=='\r' || prev=='\n')  // usual case: \nEI
                                || in.peek()==' ')      // hack for "PDFOUT v3.6o by GenText": <FF>'EI'<space>.  Accepted by Acrobat; perhaps valid PDF.
                                // watch out for 'EI' as data
                                break;
                        else { bout.write('E'); bout.write('I'); }
                } else bout.write(c);
        }
//System.out.println("len = "+bout.size());

        // decode LZW/Flate here?  done by getInputStream() / getStreamData() when read

        iidict.put(STREAM_DATA, bout.toByteArray());

        eatSpace(in);

        return iidict;
  }



//+ higher level objects: getPageCnt(), getColorSpace [+ ColorSpaces], getImage [+ Images], getFont
  /**
        Converts simple or full file specification into a platform-independent URI.
  */
  public URI getFileSpecification(Object spec) throws IOException {
        spec = getObject(spec);
        if (spec==null || spec==OBJECT_NULL) return null;

        String F;
        String FS = null;       // can be "URL"
        if (CLASS_STRING==spec.getClass()) {
                F = spec.toString();

        } else { assert CLASS_DICTIONARY==spec.getClass();
                Dict dict = (Dict)spec;
                String hostOS = File.separatorChar=='/'? "Unix": File.separatorChar=='\\'? "DOS": "Mac";        // Mac File.separatorChar undefined by Sun

                // "F" ("DOS", "Mac", "Unix")
                Object o;
                boolean ok = (o=dict.get(hostOS)/*preferred*/)!=null || (o=dict.get("F"))!=null || (o=dict.get("Unix"))!=null || (o=dict.get("Mac"))!=null || (o=dict.get("DOS"))!=null;        // Unix syntax matches URI, and Mac OS X is Unix
                F = (String)getObject(o);

                FS = (String)getObject(dict.get("FS"));
        }

        // interpret F according to OS and <hex>
        // LATER

        // File or URL => URI
        URI uri;
        if ("URL".equals(FS)) try { uri = new URI(F); } catch (URISyntaxException fail) { uri=null; }
        else uri = getURI().resolve(F);

        return uri;
  }


  /**
        Given a PDF external file specification, which can be a local file or network URI, returns a stream of data.
        This may involve fetching the file over the network and writing files to the file system.
        Files may happen to have their data embedded.
        Client may want to wrap return value in a {@link java.io.BufferedInputStream}.
        If file is not found, returns <code>null</code>.
  */
  public InputStream getFileInputStream(Object spec/*, boolean asFile*/) throws IOException {
        spec = getObject(spec);
        URI uri = getFileSpecification(spec);

        InputStream in;
        File exfile = new File(uri.getPath());

        if (CLASS_STRING==spec.getClass() || ("file".equals(uri.getScheme()) && exfile.exists())) {     // (1) simple or actual external or cached there by PDFReader
                in = new FileInputStream(exfile);

        } else { assert CLASS_DICTIONARY==spec.getClass();      // (2) embedded or network
                Dict dict = (Dict)spec;
                String hostOS = File.separatorChar=='/'? "Unix": File.separatorChar=='\\'? "DOS": "Mac";        // Mac File.separatorChar undefined by Sun

                Object o;
                Object[] ID = (Object[])getObject(dict.get("ID"));      // can check against external PDF's ID
                boolean V = Boolean.TRUE == getObject(dict.get("V"));
                Dict EF = (Dict)getObject(dict.get("EF"));      // embedded files -- legal to be embedded and volatile?
                Dict RF = (Dict)getObject(dict.get("RF"));      // related files

                if (EF!=null/*&& !new File(getFile(), F).exists()*/) {  // (2a) embedded
multivalent.Meta.sampledata("embedded external");
                        Dict names = (Dict)getObject(getCatalog().get("Names"));
                        Dict eftree = (Dict)getObject(names.get("EmbeddedFiles"));
                        boolean ok = (o=EF.get(hostOS))!=null || (o=EF.get("F"))!=null || (o=EF.get("Unix"))!=null || (o=EF.get("Mac"))!=null || (o=EF.get("DOS"))!=null;
                        StringBuffer name = (StringBuffer)getObject(o);
                        Dict stream = (Dict)findNameTree(eftree, name);

                        String subtype = (String)getObject(stream.get("Subtype"));
                        Dict params = (Dict)getObject(stream.get("Params"));
                        Dict macdict = (Dict)(params!=null? getObject(params.get("Mac")): null);

                        if (/*subtype is streamable MIME && */ RF==null && macdict==null) {     // directly read embedded data
                                in = getInputStream(stream);

                        } else {        // have to write file
                                int size = (params!=null && (o=getObject(params.get("Size"))) instanceof Number? ((Number)o).intValue(): -1);
                                long create=-1L, mod=-1L;
                                try {
                                        if (params!=null && (o=getObject(params.get("CreationDate"))) instanceof StringBuffer) create = parseDate((StringBuffer)o);
                                        if (params!=null && (o=getObject(params.get("ModDate"))) instanceof StringBuffer) mod = parseDate((StringBuffer)o);
                                } catch (InstantiationException ignore) {}
                                //long cksum = (params!=null && (o=getObject(stream.get("CheckSum"))) instanceof StringBuffer? parseDate((StringBuffer)o): -1L);

                                //X if (macdict!=null) ...      // Subtype, Creator, ResFork => not on Mac OS X

                                //iterate over related files
                                        InputStream is = getInputStream(stream);
                                        OutputStream os = new FileOutputStream(exfile);
                                        //md5 = ...
                                        // write to file system if file, or Multivalent cache if URL
                                        // ...
                                        os.close();
                                        is.close();
                                        // verify checksum
                                // set create, mod & Mac subtype, creator, res when Java supports setting file system metadata

                                in = new FileInputStream(exfile);
                        }

                } else if ("file".equals(uri.getScheme())) {    // "file" doesn't exist and not embedded
                        in = new FileInputStream(exfile);       // will throw FileNotFound exeception

                } else {        // (2b) network
                        if (!V /*&& cache*/) {}
                        //in = Multivalent.getCache().getInputStream()
                        in = URIs.toURL(uri).openStream();      // real external or network
                }
        }

        return in;
  }

  /*public File getExternalFile(Object spec) throws IOException { => FileInputStream.getFD()
        InputStream in = getFileInputStream(spec);
        // copy to file
        // ...
  }*/


/* would resolve to array or name, time consuming to map back to page
  public Object resolveDest() {
        // if first object is Integer, treat as page number
  }
*/

  /**
        {@link ColorSpaces#createColorSpace(Object, PDFReader)} with cacheing.
  */
  public ColorSpace getColorSpace(Object csref, Dict csres, Dict patres) throws IOException {
        // csref can be literal name or may be key into /ColorSpace or /Pattern resource dictionaries
        if (csres!=null && csres.get(csref)!=null) csref = csres.get(csref);
        else if (patres!=null && patres.get(csref)!=null) csref = patres.get(csref);

        // ColorSpace not a PDF object, so don't have REALIZED key for storage
        SoftReference<ColorSpace> ref = cscache_.get(csref);
        ColorSpace cs = ref!=null? ref.get(): null;

        if (cs==null) {
//System.out.println("new cs "+csref); if (CLASS_ARRAY==csref.getClass()) { Object[] oa=(Object[])csref; for (int i=0,imax=oa.length; i<imax; i++) System.out.print(" "+oa[i]); }
                cs = ColorSpaces.createColorSpace(getObject(csref), this);
                // X to cache, destructively replace array with ColorSpace object => NO, interferes with PDFWriter
                //if (csref!=null && CLASS_IREF==csref.getClass()) objCache_[((IRef)csref).id] = cs;    //else lose, but that's bad PDF -- not so many color spaces that want soft ref
                cscache_.put(csref, new SoftReference<ColorSpace>(cs));
        }

        return cs;
  }


  /**
        {@link Images#createImage(Dict, InputStream, AffineTransform, Color, PDFReader)} with cacheing (under key {@link COS#REALIZED}).
        <!-- maybe destructively replace definition in objCache_ -->
  */
  public BufferedImage getImage(IRef imgdictref, AffineTransform ctm, Color fillcolor) throws IOException {
        assert imgdictref!=null && ctm!=null/* && fillcolor!=null -- ok if not mask */;

        BufferedImage img;
        Dict imgdict = (Dict)getObject(imgdictref);
//System.out.println("img "+imgdict.get("Name"));

        // copy: (a) (x,y) doesn't matter, (b) ctm usually subsequently mutuated
        AffineTransform ctmcmp = new AffineTransform(ctm.getScaleX(), ctm.getShearY(), ctm.getShearX(), ctm.getScaleY(), 0.0,0.0);

        img = (BufferedImage)imgdict.get(REALIZED);
        if (img==null || !ctmcmp.equals(imgctm_.get(imgdictref))) {
//System.out.println(" NEW");
                InputStream in = getInputStream(imgdictref);    // possibly encrypted
                img = Images.createImage(imgdict, in, ctm, fillcolor, this);
                in.close();

                imgdict.put(REALIZED, img);     // cache whatever you get, even if error, since you'll just get the same the next time
                imgctm_.put(imgdictref/*not img or get memory leak on resized images*/, ctmcmp);
        }

        return img;
  }


  /**
        {@link Fonts#createFont(Dict,float.AffineTransform,Dict,PDF,PDFReader)} with cacheing and scaling.
        Created font stored font dictionary in SoftReference under key {@link #REALIZED}.
  */
  public NFont getFont(Dict fd,  float size, AffineTransform Tm,  PDF pdf) throws IOException {
        NFont font = (NFont)fd.get(REALIZED), font0 = font;
        if (font==null) {
//System.out.println("make font "+fd);
                font = Fonts.createFont(fd, this, pdf);
        }

//System.out.println("cached "+fd.get("BaseFont")+" @ "+font.getSize()+", want "+size+", diff="+(font.getSize()-size));
        // just scaling or general AffineTransform?
//System.out.println(fd.get("BaseFont")+" @ "+size+" / "+Tm);
        // sometimes people change font pointsize by changing Tm only (s 0 0 s 0 0 Tm /Tf 1 /font-name...), which we corrent (... => /Tf s /font-name)
        double sx = Tm.getScaleX(), sy = -Tm.getScaleY();       // Tm turned upside down to match Java ATM
        if (size < 0f) { size=-size; sy=-sy; }  // idiocy from PDFlib 3.01
//System.out.println("scale "+sx+", "+sy);
        if (sx == sy && Tm.getShearX()==0.0 && Tm.getShearY()==0.0) {
                float newsize = (float)(size * sx);
//if (font instanceof phelps.awt.font.NFontType0)
//System.out.println("font "+font.getSize()+" => "+newsize);
                if (Math.abs(newsize - font.getSize()) > Floats.EPSILON) font = font.deriveFont(newsize);
                if (font.isTransformed()) font = font.deriveFont(TRANSFORM_IDENTITY);   // clear existing transform

        } else {        // transform not identity and legitimate
                if (Math.abs(size - font.getSize()) > Floats.EPSILON) font = font.deriveFont(size);
                double shx = Tm.getShearX(), shy = -Tm.getShearY();
                AffineTransform at = font.getTransform();
                //newsize = Math.min(shx, shy); + Affine is deformation in larger?
                if (sx!=at.getScaleX() || sy!=at.getScaleY() || shx!=at.getShearX() || shy!=at.getShearY()) {
                        AffineTransform newat = new AffineTransform(sx, shy, shx, sy, 0.0,0.0);
                        font = font.deriveFont(newat);
//if (at.getScaleY()<0) System.out.println(Tm+" * "+size+" = "+newat+" => "+font.getTransform());
                }
//if (font instanceof phelps.awt.font.NFontType1) System.out.println("font tranform = "+font.getSize()+", "+font.getTransform());
        }

        if (font!=font0) {
                fd.put(REALIZED, font); // ok to cache here since font dictionary itself is gc'ed
                //if (font0!=null && MONITOR) System.out.println("create "+font);
        }

        return font;
  }

  /** Returns CMap for Encoding or ToUnicode. */
  public CMap getCMap(Object ref) throws IOException {
        CMap cmap = null;

        Object o = getObject(ref);
        if (o==null) {}
        else if (CLASS_NAME==ref.getClass()) cmap = CMap.getInstance((String)ref);
        else { assert CLASS_DICTIONARY==o.getClass(): o.getClass().getName()+" "+ref;
                Dict dict = (Dict)o;
                if ((o = dict.get(REALIZED)) != null) cmap = (CMap)o;
                else {
                        CMap usecmap = getCMap(dict.get("UseCMap"));    // should be null for /Encoding
                        cmap = new CMap(usecmap, getInputStream(ref, false));
                        dict.put(REALIZED, cmap);
                }
        }
        return cmap;
  }



//+ Query: findNameTree, findNumberTree, getPage, getPages, getPageNum,

  /**
        Find <var>name</var> in name tree rooted at <var>root</var> and return its associated value.
        Used for Dests, AP, JavaScript, Pages, Templates, IDS, URLS.
        Yes, keys of <i>name</i> tree are <i>(String)'s</i>.
        @return null <var>name</var> is not found in tree or if <var>root</var> is <code>null</code>
  */
  public Object findNameTree(Dict root, StringBuffer name) throws IOException { // Almost the same as the Pages and Numbers trees, but not close enough.
        // invariant: subtree rooted at node contains name, if name anywhere in whole tree
//if (node==null || name==null) System.out.println("NULL: root="+node+", name="+name);
        if (root==null || name==null) return null;      // not assert
        String sname = name.toString(); // Contents of <i>name<i> tree are <i>strings</i>, actually, but we have to convert all StringBuffers (in which we're stuffing PDF strings, as Java Strings hold PDF names) to Java String's to compare since Java StringBuffer inherit Object equals() definition of ==.

        Dict node = root;
        for (Object[] kids; node!=null && (kids = (Object[])getObject(node.get("Kids"))) != null; ) {
                node = null;
                for (int i=0,imax=kids.length; i<imax; i++) {
                        Object o = getObject(kids[i]); if (OBJECT_NULL==o) continue;
//System.out.println("fNT: "+kids[i]+" / "+o);
                        Dict kid = (Dict)o;
                        Object[] limits = (Object[])getObject(kid.get("Limits"));
//System.out.println(name+" in "+Arrays.asoList(limits)); System.out.println(limits[0]+".."+limits[1]);
                        if (/*limits!=null--bug && */limits[0].toString().compareTo(sname)<=0 && sname.compareTo(limits[1].toString())<=0) { node = kid; break; }
//System.out.println("recurse "+limits[0]+".."+limits[1]);
                }
        }

        if (node != null) {     // leaf that contains hit, if any
                Object[] names = (Object[])getObject(node.get("Names"));
                for (int i=0,imax=names.length; i<imax; i+=2) { // (name, object) pairs
                        if (sname.equals(names[i].toString())) {        // conversions actually cheap (true?), since share char[] and throw away right away for HotSpot has stack-based object allo
                                return names[i+1];      // don't resolve any IRef's
                        }
                }
        }

        return null;    // fail
  }


  /**
        Find <var>number</var> in number tree rooted at <var>root</var> and return its associated value.
        Used for PageLabels, ParentTree in structure tree root.
        @return null if <var>number</var> is not found in tree.
  */
  public Object findNumberTree(Dict root, int number) throws IOException {
//if (node==null || number==null) System.out.println("NULL: root="+node+", name="+name);
        if (root==null) return null;    // not assert

        Dict node = root;
        for (Object[] kids; node!=null && (kids = (Object[])getObject(node.get("Kids"))) != null; ) {
                node = null;
                for (int i=0,imax=kids.length; i<imax; i++) {
                        Dict kid = (Dict)getObject(kids[i]);
                        Object[] limits = (Object[])getObject(kid.get("Limits"));
                        if (getObjInt(limits[0]) <= number && number <= getObjInt(limits[1])) { node = kid; break; }
                }
        }

        if (node != null) {
                Object[] nums = (Object[])getObject(node.get("Nums"));
                for (int i=0,imax=nums.length; i<imax; i+=2) if (number == getObjInt(nums[i])) return nums[i+1];
        }

        return null;
  }
}
