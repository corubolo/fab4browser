package multivalent.std.adaptor.pdf;

import java.io.IOException;
import java.util.Map;
import java.util.Iterator;
import java.util.Arrays;

import phelps.lang.Integers;
import phelps.util.Version;
import phelps.util.Arrayss;

import static multivalent.std.adaptor.pdf.COS.*;



/**
	Normalize object: update PDF usage, correct, save space.
	Invoked automatically on all objects unless {@link PDFReader#setExact(boolean)}.
	Inexpensive when reading through document, and desired when programmatically processing entire document.

	<ul>
	<li>update to current level of PDF:
		convert /Dests from dictionary to Name tree,
		remove old annotation style if new style present,
		remove ProcSet from Resource dictionary (obsolete in PDF 1.4),
		remove obsolete "Name" in fonts and XObjects

	<!--<li>fill in missing parts: Page, Pages -->

	<li>correct erroneous or verbose usage:
		rewrite simple hyperlink specified as "GoTo" action as opposed to /Dest

	<li>save space:
		remove attributes set to their defaults,
		remove "Type" from XObject and Annot et alia,
		<!-- remove thumbnails as machines fast enough to generate on the fly, -->
		remove non-official dictionary keys put in by apps such as Pit Stop
	</ul>

	@version $Revision: 1.5 $ $Date: 2003/12/29 20:48:32 $
*/
class Normalize {
  private static final Integer INTEGER0=Integers.ZERO, INTEGER1=Integers.ONE;  // in case use == for equality testing
  private static final Double REAL0=PDFReader.getReal(0.0), REAL1=PDFReader.getReal(1.0);

  /*private--PDFWriter, Images*/ static final /*const*/ Object[]
	// default decode matrices
	A01 = { INTEGER0, INTEGER1 }, A10 = { INTEGER1, INTEGER0 },     // monochrome
	A010101 = { INTEGER0, INTEGER1, INTEGER0, INTEGER1, INTEGER0, INTEGER1 },   // RGB
	A01010101 = { INTEGER0, INTEGER1, INTEGER0, INTEGER1, INTEGER0, INTEGER1, INTEGER0, INTEGER1 },   // CYMK
	A01f = { REAL0, REAL1 }, A10f = { REAL0, REAL1 },       // same as above in floating point
	A010101f = { REAL0, REAL1, REAL0, REAL1, REAL0, REAL1 },
	A01010101f = { REAL0, REAL1, REAL0, REAL1, REAL0, REAL1, REAL0, REAL1 },
	// borders
	A001 = { INTEGER0, INTEGER0, INTEGER1 },    // default
	A000 = { INTEGER0, INTEGER0, INTEGER0 },    // common (invisible)
	// color
	A000f = { REAL0, REAL0, REAL0 }, A111f = { REAL1, REAL1, REAL1 }, A100f = { REAL1, REAL0, REAL0 }, A010f = { REAL0, REAL1, REAL0 }, A001f = { REAL0, REAL0, REAL1 },     // RGB black, white, red, green, blue
	// identity matrix
	A100100 = { INTEGER1, INTEGER0, INTEGER0, INTEGER1, INTEGER0, INTEGER0 }, A100100f = { REAL0, REAL0, REAL1, REAL0, REAL0, REAL1 }
  ;

  // Keys for selected dictionaries.  Useful for stripping unfamiliar keys and for writing dictionary in canonical order.
  // Many also take a /Metadata key.  PDF Reference's prohibitions of /Metadata on "implementational artifacts" violated by Adobe's own software, so allow anywhere.
  private static final /*const*/ String[]
	KEYS_TRAILER = { "Size", "Prev", "Root", "Encrypt", "Info", "ID" /*no "Metadata"*/, KEY_COMPRESS },
	//KEYS_XREF = KEYS_TRAILER + { "Type", "Index", "W" }
	KEYS_CATALOG = { "Type", "Version", "Pages", "PageLabels", "Names", "Dests", "ViewerPreferences", "PageLayout", "PageMode", "Outlines", "Threads", "OpenAction", "AA", "URI", "AcroForm", "Metadata", "StructTreeRoot", "MarkInfo", "Lang", "SpiderInfo", "OutputIntents", "PieceInfo", "OCProperties", "Perms" /*no "Metadata"*/},  // seen DefaultRGB, DefaultGray
	//KEYS_INFO = { "Title", "Author", "Subject", "Keywords", "Creator", "Producer", "CreationDate", "ModDate", "Trapped", "Metadata" },
	KEYS_PAGES = { "Type", "Parent", "Kids", "Count",  /*+ inheritable:*/ "Resources", "MediaBox", "CropBox", "Rotate" },
	KEYS_PAGE = { "Type", "Parent", "LastModified", "Resources", "MediaBox", "CropBox", "BleedBox", "TrimBox", "ArtBox", "BoxColorInfo", "Contents", "Rotate", "Group", "Thumb", "B", "Dur", "Trans", "Annots", "AA", "Metadata", "PieceInfo", "StructParents", "ID", "PZ", "SeparationInfo" }
	//KEYS_STREAM = { "Length", "Filter", "DecodeParms", "F", "FFilter", "FDecodeParms" }
  ;
  static {
	Object[] keys = { KEYS_TRAILER, KEYS_CATALOG, /*KEYS_INFO,*/ KEYS_PAGES, KEYS_PAGE/*, KEYS_STREAM*/ };
	for (int i=0,imax=keys.length; i<imax; i++) Arrays.sort((String[])keys[i]);
  }

  // Defaults for normalizeObject/zapDefaults: (key, value) pairs.  value == null implies key is obsolete
  // e.g., /Name useless, takes up space, and would interfere with object equality testing
  private static final /*const*/ Object[]
	DEF_PAGE = { /*"Type",null,=>required "Page"*/ /*"Rotate",INTEGER0,=>may be overriding inherited*/ /*"Thumb",null=>pdf.Massges nulls*/ },
	DEF_PAGES = { /*"Type",null,=>required "Pages"*/ },
	DEF_VIEWER = { "HideToolbar",Boolean.FALSE, "HideMenubar",Boolean.FALSE, "HideWindowUI",Boolean.FALSE,
		"FitWindow",Boolean.FALSE, "CenterWindow",Boolean.FALSE, "DisplayDocTitle",Boolean.FALSE, "NonFullScreenPageMode","UseNone",
		"Direction","L2R", "ViewArea","CropBox", "ViewClip","CropBox", "PrintArea","CropBox", "PrintClip","CropBox" },
	DEF_ANNOT = { "Type",null, /*"P",null,--maybe zap back pointer to page*/ /*"NM",null,--unique name needed for scripts?*/  "Flags",INTEGER0, "Border",A001, "CA", REAL1 },
	DEF_ANNOT_BS = { "Type",null,  "W",INTEGER1, "S","S" },
	DEF_ANNOT_TEXT = { "Open",Boolean.FALSE, "Name","Note" },
	DEF_ANNOT_LINK = { "H","I" },
	DEF_ANNOT_FREETEXT = { "Q",INTEGER0 },
	DEF_ANNOT_LINE = { "LE", new Object[] { "None", "None" } },
	DEF_ANNOT_STAMP = { "Name","Draft" },
	DEF_ANNOT_POPUP = { "Open",Boolean.FALSE },
	DEF_ANNOT_FILEATTACHMENT = { "Name","PushPin" },
	DEF_ANNOT_SOUND = { "Name","Speaker" },
	DEF_ANNOT_MOVIE = { "A",Boolean.TRUE },
	DEF_ANNOT_WIDGET = { "H","P"/*error in manual*//*, "MK",new Dict()*/ },
	DEF_FONT = { /*"Type",null,=>required "Font"*/ "Name",null },
	DEF_FONTDESCRIPTOR = { /*"Type=>required "FontDescriptor",*/ "Leading",INTEGER0, "XHeight",INTEGER0, "StemH",INTEGER0, "AvgWidth",INTEGER0, "MaxWidth",INTEGER0, "MissingWidth",INTEGER0 },
	DEF_XOBJECT = { "Type",null,/*maybe keep: /Image, /Form*/ "Name",null },
	//DEF_IMAGE = { /*"Decode",A01/A01f / A010101/A010101f*/ };
	DEF_IMAGE_FAX = { "K",INTEGER0, "EndOfLine",Boolean.FALSE, "EncodedByteAlign",Boolean.FALSE, "Columns",new Integer(1728), "Rows",INTEGER0, "EndOfBlock",Boolean.TRUE, "BlackIs1",Boolean.FALSE, "DamagedRowsBeforeError",INTEGER0 },
	DEF_IMAGE_DCT = { "ColorTransform",INTEGER1 },
	DEF_FORM = { "Type",null, "FormType",INTEGER1, "Matrix",A100100 },
	DEF_ACTION = { "Type",null },
	DEF_PATTERN = { "Type",null,  "Matrix",A100100 },
	DEF_FILESPEC = { /*"Type",null,=>highly recommended/required "Filespec"*/ "V",Boolean.FALSE },
	DEF_FILE_EMBEDDED = { "Type","EmbeddedFile" },
	DEF_CRYPTFILTER = { "Type","CryptFilter", "CFM","None", "Length",Integers.getInteger(128), "AuthEvent","DocOpen" },
	DEF_CRYPTFILTER_DP = { "Type","CryptFilterDecodeParms" },
	DEF_OCG = { /*"Type","OCG", */ "Intent","View" },
	DEF_OCMD = { /*"Type","OCMD", */ "P","AnyOn" },
	//DEF_ "StructElem"
	//DEF_ = { },
	DEF_CATALOG = { /*"Type",null,=>required "Catalog"*/  "PageLayout","SinglePage", "PageMode","UseNone" };


  private PDFReader pdfr_;

  /*package-private*/ Normalize(PDFReader pdfr) {
	assert pdfr!=null;
	pdfr_ = pdfr;
  }

  private Object getObject(Object o) throws IOException {
	// if (o instanceof IRef) pinObject(...) => pins too much
	return pdfr_.getObject(o);
  }


  /**
	Update older PDF to current standard so other functions don't have to worry about version differences.
	This method updates global aspects that other objects rely on;
	{@link #normalizeObject(Object)} updates objects incrementally.

	<ul>
	<li>convert /Dests from dictionary to Name tree
	<li>Catalog /Version removed in favor of updated doc version in header
	</ul>
  */
  public void normalizeDoc(final Dict cat) throws IOException {
	assert cat!=null;

	PDFReader pdfr = pdfr_;
	pdfr.getVersion().setMin(new Version("1.1"));     // zap /Name's in normalizeObject

	// trailer
	Dict trailer = pdfr.getTrailer();
	Object[] ID = (Object[])pdfr.getObject(trailer.get("ID"));
	if (ID==null) {	// fill in missing ID
		ID = new Object[2];
		//o = getObject(trailer_.get("Info")); Dict info = OBJECT_NULL!=o? (Dict)o: null;   // can't use getInfo() before set ID
		ID[0] = ID[1] = createID(pdfr.getURI(), pdfr.getInfo());  // known not to be encrypted
//System.out.println("new ID");
	}
	trailer.put("ID", ID);	// new or IRef => direct

	// Catalog
	Object val = getObject(cat.get("Version"));
	if (val!=null /*&& CLASS_NAME==val.getClass()*/) { // /Version overrides %PDF-m.n header
		pdfr.getVersion().setMin(new Version((String)val));
		cat.remove("Version");
	}

	// root
	Object o = getObject(cat.get("Pages"));
	if (OBJECT_NULL != o) {
		Dict pages = (Dict)o;
		if (pages!=null && (val = getObject(pages.get("Rotate")))!=null) if ((((Number)val).intValue() % 360) == 0) pages.remove("Rotate");
	}

	//updateDests(cat);
	//updateXXX...
  }



  /**
	Update PDF 1.1's single dictionary to PDF 1.2's name tree (with one big leaf).
	References into the tree are updated incrementally, on demand in {@link Normalize} when the corresponding annotation object is read!
  private void updateDests(Dict cat) throws IOException {
	// old
	IRef iref = (IRef)cat.get("Dests"); if (iref==null) return;   // must be IRef, so don't have to create new object for replacement
	pdfr_.setMin(1,2);
	Dict olddests = (Dict)getObject(iref);
	pdfr_.pinObject(iref.id);	// going to rewrite it
	cat.remove("Dests");

	// new
	Dict names = (Dict)getObject(cat.get("Names"));   // name tree /Dests in catalog's /Names
	if (names==null) { names = new Dict(4); cat.put("Names", names); }   // direct so don't have to create new object
	Dict nametree = new Dict(4);

	// construct new contents
	String[] sorted = olddests.keySet().toArray(new String[0]);  // PDF keys are Java strings
	Arrays.sort(sorted);
	Object[] na = new Object[sorted.length * 2];
	for (int i=0,imax=na.length, j=0; i<imax; i+=2, j++) {
		String key = sorted[j];
		na[i] = new StringBuffer(key);    // PDF Name => String
		na[i+1] = olddests.get(key);
//if (na[i]==null || na[i+1]==null) System.out.println(na[i]+" "+na[i+1]+"    ");;
	}
	nametree.put("Names", na);  // no more balanced than it used to be, but at least of the right type
	//nametree.put("Limits", new Object[] { na[0], na[dests.size()*2-1-1] });   // root of name tree has no limits (everything!)

	// add new
	//objCache_[iref.id] = nametree; -- objCache_ private, so rewrite old object
	olddests.clear(); olddests.putAll(nametree);
	names.put("Dests", iref);
  }
  */



  public void/*Object*/ normalizeObject(final Object obj) throws IOException {
	if (CLASS_DICTIONARY != obj.getClass()) return;// obj;   // other classes simple and normalized in readObject()

	final Dict dict = (Dict)obj;
	String type = (String)getObject(dict.get("Type")), subtype = (String)getObject(dict.get("Subtype"));
	//if (type==null) type = guessType(dict);
	Object reso = null;
	//Dict dictin = new Dict(dict);
	Object o;



	// 1. across dictionaries
	/*
	// X named destinations and GoTo actions => too much to track down: /Dest, /A, /AA, /Next + possible cycles, in annotations, outlines, FDF, ...
	if ("Link".equals(subtype) || ("Annot".equals(type) && !"Movie".equals(subtype))
		|| (dict.get("Parent")!=null && dict.get("Title")!=null && dict.get("Prev")!=null && dict.get("Next")!=null)	// Outline item
		|| FDF) {
		// in cooperation with updateDests(), update dest /Name to (String)
		if ((o=getObject(dict.get("A")))!=null) {   // action
			dict.remove("Dest");    // /A supercedes /Dest
			for (Dict adict = (Dict)o; adict!=null; ) {
				if ("GoTo".equals(adict.get("S")) && (o = adict.get("D"))!=null) {      // /D required
					if (o instanceof String) adict.put("D", (o=new StringBuffer((String)o)));     // update PDF 1.1
					if (adict.size()==2/*no chained action* /) { dict.remove("A"); dict.put("Dest", o); }    // more direct and more compatible; preferred by PDF Reference
				}
				o = adict.get("Next"); adict = o!=null && CLASS_DICTIONARY==o.getClass()? (Dict)o: null;
			}

		} else if ((o=getObject(dict.get("Dest"))) instanceof String) {  // array=direct, string=named, name=key into PDF 1.1 /Dests dict
			dict.put("Dest", new StringBuffer((String)o));     // update PDF 1.1 /Name to (String)
		}
	}*/

	// 2. all streams
	if (dict.get(STREAM_DATA)==null) {  // not a stream
	} else if (dict.get("F")!=null) {  // external ("embedded external" doesn't have "F") -- Would have been better to have "external" flag and use the same names for the same things
multivalent.Meta.sampledata("external file "+obj);
		dict.remove("Filter"); dict.remove("DecodeParms");  // ignore Filter and DecodeParms
		dict.put("Length", INTEGER0);     // "bytes between stream and endstream are ignored"
		simplifyArray1(dict, "FFilter"); simplifyArray1(dict, "FDecodeParms");
		// spec doesn't seem to allow missing external to fail to embedded data

	} else {
		simplifyArray1(dict, "Filter");
		if ((o=dict.remove("DP"))!=null && dict.get("DecodeParms")==null) dict.put("DecodeParms", o);  // expand abbreviation, but "DecodeParms" takes precedence over "DP" (PDFRef1.4 implementation note 7)
		simplifyArray1(dict, "DecodeParms");    // all used up, except for image's
	}


	// 3. type-specific
	if (type==null) {
		// not a target object type

	} else if ("Page".equals(type)) {
		cleanDict(dict, KEYS_PAGE, DEF_PAGE);
		//dict.remove("Thumb");
		reso = dict.get("Resources");

		simplifyArray1(dict, "Contents");

		// CropBox defaults to MediaBox, but also inheritable
		// Rotate same as inherited -- but don't fight getPage(int) + liftPageTree

		// BleedBox, TrimBox, ArtBox default to CropBox; not inheritable

	} else if ("Pages".equals(type)) {
		cleanDict(dict, KEYS_PAGES, DEF_PAGES);
		reso = dict.get("Resources");

		// root pointing to self fixed in global pass

	} else if ("Annot".equals(type)) {
		cleanDict(dict, null, DEF_ANNOT);

		//Dict AP = (Dict)getObject(dict.get("AP"));
		//if (AP != null) reso = AP.get("Resources");
		Object[] border = (Object[])getObject(dict.get("Border"));
		/*if (dict.get("BS")!=null) dict.remove("Border");
		else*/ if (Arrays.equals(A000, border)) dict.put("Border", A000);   // single instance
		if (!"Screen".equals(subtype)) dict.remove("P");	// page ref

		if ("Text".equals(subtype)) cleanDict(dict, null, DEF_ANNOT_TEXT);

		else if ("Link".equals(subtype)) {
			cleanDict(dict, null, DEF_ANNOT_LINK);
			if ((o=getObject(dict.get("A")))!=null) {
				dict.remove("Dest");    // /A supercedes /Dest
				Dict adict = (Dict)o;
				if ("GoTo".equals(adict.get("S")) && (o = adict.get("D"))!=null) {      // /D required
					if (adict.size()==2/*no chained action*/) { dict.remove("A"); dict.put("Dest", o); }    // more direct and more compatible; preferred by PDF Reference
				}
			}

		} else if ("FreeText".equals(subtype)) cleanDict(dict, null, DEF_ANNOT_FREETEXT);
		else if ("Line".equals(subtype)) cleanDict(dict, null, DEF_ANNOT_LINE);
		//else if ("Circle".equals(subtype))
		//else if ("Square".equals(subtype))
		//else if ("Highlight".equals(subtype))
		//else if ("Underline".equals(subtype))
		//else if ("Squiggly".equals(subtype))
		//else if ("StrikeOut".equals(subtype))
		else if ("Stamp".equals(subtype)) cleanDict(dict, null, DEF_ANNOT_STAMP);
		//else if ("Ink".equals(subtype))
		else if ("Popup".equals(subtype)) cleanDict(dict, null, DEF_ANNOT_POPUP);
		else if ("FileAttachment".equals(subtype)) cleanDict(dict, null, DEF_ANNOT_FILEATTACHMENT);
		else if ("Sound".equals(subtype)) cleanDict(dict, null, DEF_ANNOT_SOUND);
		else if ("Movie".equals(subtype)) cleanDict(dict, null, DEF_ANNOT_MOVIE);
		else if ("Widget".equals(subtype)) cleanDict(dict, null, DEF_ANNOT_WIDGET);

	} else if ("Bead".equals(type)) {
		//cleanDict(dict, null, DEF_BEAD);

	} else if ("Font".equals(type)) {
		cleanDict(dict, null, DEF_FONT);

		if ("Type3".equals(subtype)) {
			reso = dict.get("Resources");
		}

		Dict fd = (Dict)getObject(dict.get("FontDescriptor"));    // do this here rather than in a "FontDescriptor" branch so know the type of font
		if (fd!=null) {
			cleanDict(fd, null, DEF_FONTDESCRIPTOR);

			IRef iref; Dict embed;
			if ("Type1".equals(subtype) || "MMType1".equals(subtype)) {
				iref=(IRef)fd.get("FontFile");
				if (iref!=null) { embed=(Dict)getObject(iref); fd.remove("FontFile2"); fd.remove("FontFile3"); pdfr_.setObject(iref.id, embed); }
				else { // "Type1C"
					fd.remove("FontFile"); fd.remove("FontFile2"); iref=(IRef)fd.get("FontFile3");
					embed = (Dict)getObject(iref);
					if (embed!=null) {
						subtype = (String)getObject(embed.get("Subtype"));
						assert "Type1C".equals(subtype);
						embed.remove("Length1"); embed.remove("Length2"); embed.remove("Length3");
						pdfr_.setObject(iref.id, embed);
					}
				}
			} else if ("TrueType".equals(subtype) || "CIDFontType2".equals(subtype)) {
				fd.remove("FontFile"); iref=(IRef)fd.get("FontFile2"); fd.remove("FontFile3"); 
//System.out.println(iref+" => "+getObject(iref));
				embed=(Dict)getObject(iref); if (embed!=null) { embed.remove("Length2"); embed.remove("Length3"); pdfr_.setObject(iref.id, embed); }
			} else /* "CIDFontType0C" or other*/ {
				fd.remove("FontFile"); fd.remove("Fontfile2"); iref=(IRef)fd.get("FontFile3"); 
				embed=(Dict)getObject(iref); if (embed!=null) { embed.remove("Length2"); embed.remove("Length3"); pdfr_.setObject(iref.id, embed); }
			}
			if (embed!=null) pdfr_.objCache_[iref.id] = embed;	// relative normalize so pin vs gc

			if ("Type1".equals(subtype) && embed!=null) {
				//assert fbulk_ == false;
				// Strip off trailing 512 zeros.
				int clen = pdfr_.getObjInt(embed.get("Length1")), elen = pdfr_.getObjInt(embed.get("Length2")), zlen = pdfr_.getObjInt(embed.get("Length3"));
				if (zlen > 0) {
					//System.out.println("Type1 "+zlen+", "+(embed.get(STREAM_DATA) instanceof Number? "fast": "slow"));
					embed.put("Length3", INTEGER0);
					byte[] data = pdfr_.getStreamData(iref, false, true);
					//assert data.length == clen + elen + zlen: // pdfTeX sets /Length3 532 even though doesn't supply that data
					if (data.length > clen+elen) {
						data = Arrayss.resize(data, clen+elen);
						embed.put(STREAM_DATA, data);
					}
				}
			}
		}

	//} else if ("FontDescriptor".equals(type)) { => in "Font"

	} else if ("Type1U".equals(dict.get("Type"))) {	// re-encrypt Type 1, which is necessary for their use
		dict.remove("Type");
		int clen = pdfr_.getObjInt(dict.get("Length1")), elen = pdfr_.getObjInt(dict.get("Length2"));
		byte[] data = pdfr_./*(byte[])dict.get(STREAM_DATA)*/getStreamData(dict, false, true); assert data.length >= clen+elen: data.length+" < "+clen+"+"+elen+" in "+dict;
		//System.out.print("encrypting Type 1 "+fd.get("BaseFont"));
//System.out.print("encrypted "+data.length+" => ");
		data = com.pt.awt.font.NFontType1.toPFB(data);
//System.out.println(data.length);
		//dict.remove("Filter");	// was /FlateDecode

	} else if ("XObject".equals(type)) {
		cleanDict(dict, null, DEF_XOBJECT);

		if ("Image".equals(subtype)) {
			// hardcode /Width, /Height, /BitsPerComponent
			hardcode(dict, "Width"); hardcode(dict, "Height"); hardcode(dict, "BitsPerComponent");

			//if (dict.get("Mask")!=null || dict.get("SMask")!=null) dict.remove("ColorSpace");
			o = getObject(dict.get("ImageMask"));
			if (o == Boolean.TRUE) { dict.remove("BitsPerComponent"); dict.remove("Mask"); dict.remove("ColorSpace"); }
			else dict.remove("ImageMask");

			Object cs = getObject(dict.get("ColorSpace"));

			// remove decode arrays that are same as defaults
			o = getObject(dict.get("Decode"));
			if (cs!=null && o!=null) { Object[] oa = (Object[])o;
				if ( (("DeviceGray".equals(cs) || "CalGray".equals(cs) || "Separation".equals(cs))  &&  (Arrays.equals(A01, oa) || Arrays.equals(A01f, oa)))
					 || (("DeviceRGB".equals(cs) || "CalRGB".equals(cs))  &&  (Arrays.equals(A010101, oa) || Arrays.equals(A010101f, oa)))
					 || (("DeviceCYMK".equals(cs))  &&  (Arrays.equals(A01010101, oa) || Arrays.equals(A01010101f, oa)))
					)
				dict.remove("Decode");
			}

			Dict dp = Images.getDecodeParms(dict, pdfr_);
			String filter = Images.getFilter(dict, pdfr_);
			if ("CCITTFaxDecode".equals(filter)) {
				cleanDict(dp, null, DEF_IMAGE_FAX);
				// check that /BitsPerComponent is INTEGER1
				// check that /ColorSpace is /DeviceGray ?
				o = getObject(dict.get("Decode"));
				if (o==null) {} else if (Arrays.equals(A01,(Object[])o)) dict.remove("Decode"); else if (Arrays.equals(A10, (Object[])o)) dict.put("Decode", A10);    // could have /Decode [1.0 0.0]

			} else if ("DCTDecode".equals(filter)) cleanDict(dp, null, DEF_IMAGE_DCT);

			if (dp!=null && dp.size()==0) dict.remove("DecodeParms");   // if all attrs same as defaults, omit

		} else if ("Form".equals(subtype)) {
			cleanDict(dict, null, DEF_FORM);
			reso = dict.get("Resources");
		}

	} else if ("Border".equals(type)) { // border style
		cleanDict(dict, null, DEF_ANNOT_BS);

	} else if ("Action".equals(type)) {
		cleanDict(dict, null, DEF_ACTION);

	} else if ("Pattern".equals(type)) {
		cleanDict(dict, null, DEF_PATTERN);
		reso = dict.get("Resources");

	} else if ("Filespec".equals(type)) {
		cleanDict(dict, null, DEF_FILESPEC);

	} else if ("EmbeddedFile".equals(type)) {
		cleanDict(dict, null, DEF_FILE_EMBEDDED);

	} else if ("CryptFilter".equals(type)) {
		cleanDict(dict, null, DEF_CRYPTFILTER);
	} else if ("CryptFilterDecodeParms".equals(type)) {
		cleanDict(dict, null, DEF_CRYPTFILTER_DP);

	} else if ("OCG".equals(type)) {
		cleanDict(dict, null, DEF_OCG);
	} else if ("OCMD".equals(type)) {
		cleanDict(dict, null, DEF_OCMD);

	} else if ("XRef".equals(type)) {
		cleanDict(dict, KEYS_TRAILER, null);    // zap "W", "XRef", ...

	} else if ("Catalog".equals(type)) {
		cleanDict(dict, KEYS_CATALOG, DEF_CATALOG);
		o = getObject(dict.get("Outlines"));
		Dict outlines = o!=OBJECT_NULL? (Dict)o: null;
		if (outlines!=null && (o=outlines.get("Count"))!=null && pdfr_.getObjInt(o)==0) { dict.remove("Outlines"); outlines=null; }	// pdfdb/000608
		if ("UseOutlines".equals(getObject(dict.get("PageMode"))) && outlines==null)
			dict.remove("PageMode");    // if /PageMode /UseOutlines but no outlines, zap (afc_docs.pdf by pdfTeX-0.13d)

		Dict vdict = (Dict)getObject(dict.get("ViewerPreferences"));
		cleanDict(vdict, null, DEF_VIEWER);
		if (vdict!=null && vdict.size() == 0) dict.remove("ViewerPreferences");

		// 09_17_03_UB_RO_a.pdf has "/Outlines 492 0 R" but no object 492

		// /Info (no /Type and might not use getInfo(), so piggyback)
		Dict info = pdfr_.getInfo();
		cleanDict(info, /*KEYS_INFO,--custom metadata ok*/null, null);

	} else if (dict.get("FunctionType")!=null) {
	}


	// 4. resource dictionary
	if ((o = getObject(reso))!=null && CLASS_DICTIONARY == o.getClass()) {
		Dict resdict = (Dict)o;
		o = resdict.remove("ProcSet");	// if ProcSet via IRef possible that gc'ed and revert
		if (o!=null) pdfr_.getVersion().setMin(new Version("1.4"));
	}

	//if (dict.size() != dictin.size()) System.out.println(dictin+" => "+dict);
	//if ("Page".equals(dict.get("Type")) && Integers.getInteger(7).equals(dict.get("StructParents"))) System.out.println(dict);

	// hardcode any (remaining) /Type and /Subtype values
	hardcode(dict, "Type"); hardcode(dict, "Subtype");

	//return obj;
  }

  private void hardcode(Dict dict, String key) throws IOException {
	Object o = dict.get(key);
	if (o!=null && CLASS_IREF==o.getClass()) dict.put(key, getObject(o));
  }

  private void simplifyArray1(Dict dict, String key) throws IOException {
	Object ref = dict.get(key), o;
	if (ref!=null && CLASS_ARRAY == (o = getObject(ref)).getClass()) {
		Object[] oa = (Object[])o;
		if (oa.length==1) {     // 1-element array => element
			//if (ref == o) dict.put(key, oa[0]);
			//else { assert CLASS_IREF==ref.getClass();  objCache_[((IRef)ref).id] = oa[0]; }
			dict.put(key, oa[0]);
		}
	}
  }

/*
  public String guessType(Dict dict) {
	String type = (String)getObject(dict.get("Type")); if (type!=null) return type;
	String subtype = (String)getObject(dict.get("Subtype"));

	if (type==null) {
		if (subtype != null) {
			if ("Form".equals(subtype)) || "Image".equals(subtype) || "Group".equals(subtype)) type = "XObject";
			else if ("Link".equals(subtype) || ... ) type = "Annot";

		} else {
			if (dict.get("PatternType")!=null || dict.get("PaintType")!=null || dict.get("TilingType")!=null) type = "Pattern";
		}
	}

	return type;
  }
*/

  /**
	Removes key from dictionary if value is same as default.
	(Special case: if default set to <code>null</code>, always remove.)
	From selected dictionaries, also removes all non-official keys.
  */
  private void cleanDict(Dict dict, String[] keys, Object[] defs) throws IOException {
	//assert defs.length % 2 == 0;
	if (dict==null) return;

	Object o;
	if (keys!=null) for (Iterator<Map.Entry<Object,Object>> i = dict.entrySet().iterator(); i.hasNext(); ) {	// remove non-official keys
		Map.Entry<Object,Object> e = i.next();
		String key = (String)e.getKey();
		if (!"Metadata".equals(key) && Arrays.binarySearch(/*sorted*/keys, key) < 0) i.remove(); /*System.out.println("removed "+key+" "+getFile());*/
	}

	if (defs!=null) for (int i=0,imax=defs.length; i<imax; i+=2) {
		Object key=defs[i], val=defs[i+1];
		if (val==null
			|| val.equals(o = getObject(dict.get(key)))
			|| (val.getClass()==CLASS_ARRAY && Arrays.equals((Object[])val, (Object[])o)))
			dict.remove(key);
	}
  }
}
