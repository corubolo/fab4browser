package edu.berkeley.adaptor;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.MalformedURLException;
import java.text.DecimalFormat;
import java.awt.*;
import java.awt.image.FilteredImageSource;
//import java.awt.image.*;
import java.util.Map;

import phelps.awt.image.TransparentFilter;
import phelps.lang.Integers;
import phelps.lang.Booleans;
import phelps.lang.Strings;
import phelps.net.URIs;

import com.pt.awt.NFont;

import multivalent.*;
import multivalent.node.FixedLeafOCR;
import multivalent.node.FixedI;
import multivalent.node.Fixed;
import multivalent.node.FixedIVBox;
import multivalent.node.FixedIHBox;
import multivalent.node.FixedLeafImage;
import multivalent.std.OcrView;
import multivalent.node.LeafUnicode;
import multivalent.gui.VMenu;
import multivalent.gui.VCheckbox;



/** @ deprecated in favor of spans -- otherwise would make an inner class. */
class FontXdoc {
  public NFont font;
  public int capheight;
  public int descend;	// amount descender goes below line

  FontXdoc(NFont font, int capheight, int descend) {
	this.font = font;
	this.capheight = (capheight>0? capheight : (int)(font.getSize()*1.25));
	this.descend = (descend>0? descend : 2);
//System.out.println("capheight="+this.capheight+", descend="+this.descend);
  }

  //public int wordcnt=0;	// words in this style -- for profiling if makes sense to set main font with spans for variations
  static final String[] sstyle = { "plain", "bold", "italic", "bi" };
  public String toString() { return ""+font+" "+capheight+"/"+descend; }
}


/**
	Media adaptor for ScanSoft XDOC files (.xdc => document tree).
	<blockquote>
	XDOC version 9.0 described in XDOC Data Format: Technical Specification, Febuary 1993<br />
	XDOC version 10.0 described in XDOC DATA FORMAT, Version 3.0, March 1995<br />
	XDOC version 12.0 described in XDOC Data Format, Version 4.0, May 1999
	</blockquote>

	<p>Written against documentation for XDOC versions 9.0, 10.0, and 12.0.<br />
	Tested against versions 7.2, 9.0, 10.0, 12.0, and 12.5

	<p>Furthermore, in the absence of any standard way of structuring the images and corresponding XDOC
	for all the pages that comprise a book, we use that developed by the Berkeley Digital Library Project,
	namely, for document <i>ID</i>, XDOC in the directory <tt>.../<i>ID</i>/OCR-XDOC/</tt>,
	GIF images suitable for display onscreen in  <tt>.../<i>ID</i>/GIF-INLINE/</tt>,
	and bibliographic data in the file <tt>.../<i>ID</i>/BIB/bib.rfc1357</tt>.


<!--
	<hr>To do
	recover old attributes: URI, XDOCURI, IMAGEURI?
	X fix letter clipping on left edge => need uniform scaling and deskewed images
	X adjust for skew => should deskew when ocr
	runnable as own application so can test correctness on random Xdoc files
	add images (x) and rules (r) using ImageFixed
	see how well latest XDOC/analysis engine identifies tables
	more info in XDOC to take advantage of?
	need documentation for XDOC 12.5
-->

	@see <a href='http://documents.cfar.umd.edu/'>CFAR</a>
	@see berkeley.node.FixedLeafOCR
	@see berkeley.adaptor.PDA

	@version $Revision: 1.20 $ $Date: 2005/01/03 10:04:21 $
*/
public class Xdoc extends MediaAdaptor {
  private /*X final => available to command line*/ boolean DEBUG = false;
  public boolean Test = false;	// used when running as application

  //public static final String CANONICAL = "ALLPAGES.xdc"; => that files doesn't exist.  instead use 00000001.xdc

  /**
	Set showing verbose XDOC information: on/off as parsed by {@link Booleans#getBoolean(String, boolean)}, or <code>null</code> to toggle.
	<p><tt>"verboseXDOC"</tt>.
  */
  public static final String MSG_SET_VERBOSE = "verboseXDOC";

  public static final String ATTR_VERBOSE = "verboseXDOC";

  public static final String ATTR_SCALE = "scale";


  // assume 300 ppi, adjust by scale
  private static final double MM2PPI = 300.0/254.0;

  private static final DecimalFormat DLFORMAT = new DecimalFormat("00000000");

  // word character translation
  private static String transString[] = new String[256];
  //static final String specialChar[] = { String.valueOf((char)8216)/*`*/, String.valueOf((char)8217)/*'*/, String.valueOf((char)8220)/*``*/, String.valueOf((char)8221)/*''*/, String.valueOf((char)8212)/*"--"*/, String.valueOf((char)8226)/*bullet*/, "\207", "\210", "\211", "\212" };
  private static final String specialChar[] = { "`", "'", "``", "''", "--", "*"/*bullet*/, "\207", "\210", "\211", "\212" };
  static {
	for (int i=0; i<transString.length; i++) transString[i] = String.valueOf((char)i);
	for (int i=0; i<specialChar.length; i++) transString[i+129] = specialChar[i];
  }

  private static NFont smallFont__ = NFont.getInstance("Serif", NFont.WEIGHT_NORMAL, NFont.FLAG_SERIF, 10f);


  private PushbackInputStream is_;

  private double scale = 3.335;	//1.0; 3.31
  //private Image fullpage_ = null;
  private FixedLeafImage full_ = null;
  // ugly hack for stupid OCR software that doesn't report all ink on page
  // later: put in a signal that can be set by anyone
  //public int aswordscnt = 0;	// number of behaviors that depend on image being displayed as words
  private int charcnt_ = 0;
  public int getCharCnt() { return charcnt_; }
  private int qcharcnt_ = 0;	// count questionable characters to find bad scans, math
  public int getQuestionableCharCnt() { return qcharcnt_; }

  private URI xdcuri_=null;

  private String vinfo_ = null;



  /**
	<ul>
	<li>Sets normalized URI for annotations, base for XDOC and image URIs: if filename of form <i>number</i>.xdc, use grandparent directory name, else take from filename.
		<em>Resets Document to this URI.</em>
	<li>Loads bib.rfc1357, if any
	<li>Sets {@link Document#ATTR_PAGE} attribute from filename, sets {@link Document#ATTR_PAGECOUNT} from bib
	</ol>
  */
  public void restore(ESISNode n, Map<String,Object> attr, Layer layer) {
	super.restore(n, attr, layer);

	// pointed directly to .xdc file, compute image and bib locations
	// input: URI=http://elib.cs.berkeley.edu/data/docs/xxxx.xdc

	// if pointing directly at an .xdc file, e.g., ".../OCR-XDOC/00000011.xdc", extract page number
	Document doc = getDocument();
	String suri = doc.getAttr(Document.ATTR_URI);
	URI nuri=doc.getURI();
	xdcuri_ = nuri;	// pointed to .xdc, so ok as is (though could optionally remove filename)
	//imageuri_ = nuri;	// assume image in same directory, unless detect UCB convention
//System.out.println("restore URI="+suri);

	// 1. Compute page number and normalize URI, if in UCB format ".../OCR-XDOC/000...<num>.xdc",
	// e.g., "http://elib.cs.berkeley.edu/docs/data/0600/620/OCR-XDOC/00000009.xdc",
	// and normalize URI to ".../ALLPAGES.xdc"
	int pageinx=suri.lastIndexOf('/'), inx=suri.lastIndexOf('.');
//System.out.println("sinx="+sinx+", dinx="+dinx);
//System.out.println("suri="+suri+", nuri="+nuri);
//System.exit(1);
	if (pageinx!=-1 && inx-pageinx==8+1) {
		try {
			int page = Integer.parseInt(suri.substring(pageinx+1,inx));
			doc.putAttr(Document.ATTR_PAGE, Integer.toString(page));

			String ref = nuri.getFragment();	// preserve ref
			nuri = nuri.resolve("00000001.xdc"+(ref==null?"":"#"+ref));	// no matter which page start at, annotations saved to bundle.  use page one vs, say, "ALLPAGES.xdc" because want file to exist so get back to Xdoc rather than 404
			suri = nuri.toString();
			doc.putAttr(Document.ATTR_URI, suri); doc.uri=nuri;
//System.out.println("normalized URL to "+suri);

		} catch (NumberFormatException okIfNone) {}
		//} catch (MalformedURLException shouldntHappen) {}
		//} catch (URISyntaxException shouldntHappen) {}
//System.out.println("extracting Xdoc page #"+page);
	}


	// 2. read in bib, if any, and set PAGECOUNT
	try {
		//URL biburl = new URL(xdcuri_, "../BIB/bib.rfc1357");
		URI biburi = xdcuri_.resolve("../BIB/bib.elib");

		BufferedReader r = new BufferedReader(new InputStreamReader(URIs.toURL(biburi).openStream()));
		String line;
		while ((line=r.readLine())!=null) {
//System.out.println("bib  |"+line+"|");
			// <attr>::<val>
			int spliti=line.indexOf(':'); if (spliti==-1) continue;	// blank line, bad data, ...
			String name=line.substring(0,spliti);
			spliti++; spliti++;	// skip "::"
			while (spliti<line.length() && Character.isWhitespace(line.charAt(spliti))) spliti++;
			String val=line.substring(spliti);
//System.out.println("name="+name+", val="+val);
			doc.putAttr(name,val);
		}
		r.close();
	} catch (IOException bibe) {
		System.err.println("couldn't read bib "+bibe);
	}


	// 3. Scale, estimated if no bib and is UCB ID'rg
	if (doc.getAttr(ATTR_SCALE)!=null) {
		try { scale = Double.valueOf(doc.getAttr(ATTR_SCALE)).doubleValue(); } catch (NumberFormatException e) { /*keep default*/ }
//System.out.println("scale from bib = "+scale);
	}
  }

  public void buildBefore(Document doc) {
	if (doc.getAttr(Document.ATTR_PAGE)==null) { new LeafUnicode(""/*"Loading XDOC..."/*no PAGE attr for XDOC*/, null, doc); return; }

	// for now, hardcoded location of image, bib (and that there is bib)
	// later, take from attributes (again, which we did originally)
	int page = Integers.parseInt(doc.getAttr(Document.ATTR_PAGE, "1"), 1);

	URI xdcuri, imageuri;
	try {
		xdcuri = xdcuri_.resolve(DLFORMAT.format(page)+".xdc");
		//xdcurl = new URL(uri, "../OCR-XDOC/"+DLFORMAT.format(page)+".xdc");
		imageuri = xdcuri_.resolve("../GIF-INLINE/"+DLFORMAT.format(page)+".gif");	// yay, Java handles ".."
	} catch (IllegalArgumentException e) {
		// display error info line
		System.err.println("*** error making URIs: "+e);
		return;
	}


	// 1. image, loaded first as that's what's usually visible
	Browser br = getBrowser();
	Image src = null; try { src = br.getToolkit().getImage(URIs.toURL(imageuri)); } catch (MalformedURLException male) {}
//System.out.println("start TransparentFilter "+System.currentTimeMillis());
	Image fullpage = br.createImage(new FilteredImageSource(src.getSource(), new TransparentFilter(Color.WHITE)));
//System.out.println("done  TransparentFilter "+System.currentTimeMillis());
//System.out.println("fullpage_ class="+fullpage_.getClass().getName()+", volatile? "+(fullpage_ instanceof VolatileImage); => neither BufferedImage nor VolatileImage
	full_ = new FixedLeafImage("full",null, null, fullpage);
	doc.putVar(OcrView.VAR_FULLIMAGE, full_/*fullpage_*/);
	//fullpage_ = src;
	//br.prepareImage(fullpage_, this);	// load image on demand, as may be quickly paging through OCR


	// 2. XDOC
	/*"... since the recognition software maintains XDOC page coordinates in
	  absolute units, and does not record the image resolution in XDOC Text, you
	  must keep track of the scale of the text relative to the sacle of the image
	  display"	XDOC DATA FORMAT, Version 3.0, March 1995, page 4-3  */
//	if ((att=/*new*/doc.getAttr("mode"))!=null && att.equals("ASCII")) active=1;
	doc.removeAttr(Fixed.ATTR_REFORMATTED);

	try {
		//Document doc = new Document(null,null, docroot);
		//this.setInputStream(URIs.toURL(xdcuri).openStream());
		this.setInput(com.pt.io.InputUni.getInstance(xdcuri, xdcuri, getGlobal().getCache()));
		/*ocrroot_ = (FixedI)*/parse(/*URIs.toURL(xdcuri).openStream(),*/ doc);
	} catch (FileNotFoundException fnfe) {
		new LeafUnicode("no such page",null, doc);
		return;
	} catch (Exception e) {
		System.out.println("couldn't parse .xdc "+e);
		e.printStackTrace();
		return;
	}

//	super.buildBefore(doc);	// have to set ocrroot first
  }


  /** Switch for verbose information. */
  public boolean semanticEventBefore(SemanticEvent se, String msg) {
	if (super.semanticEventBefore(se, msg)) return true;
	else if (VMenu.MSG_CREATE_VIEW==msg) {
		INode menu = (INode)se.getOut();
		VCheckbox ui = (VCheckbox)createUI("checkbox", "Verbose XDOC Info", "event "+MSG_SET_VERBOSE, menu, VMenu.CATEGORY_MEDIUM, false);
		ui.setState(getRoot().getAttr(ATTR_VERBOSE)!=null);
	}
	return false;
  }

  /** Switch for verbose information. */
  public boolean semanticEventAfter(SemanticEvent se, String msg) {
	if (MSG_SET_VERBOSE==msg) {
		// toggle and redraw
		INode root = getRoot();
		root.putAttr(ATTR_VERBOSE, Booleans.parseBoolean(se.getArg(), !Booleans.parseBoolean(root.getAttr(ATTR_VERBOSE), false))? "true": null);
		getBrowser().repaint();
	}
	return super.semanticEventAfter(se, msg);
  }

  /** Draw verbose information. */
  public boolean paintAfter(Context cx, Node node) {
	// already attached as observer
	if (getRoot().getAttr(ATTR_VERBOSE)!=null) {
		Graphics2D g = cx.g;
		g.setColor(Color.BLACK);
		int x = getDocument().bbox.width - 100, h = (int)smallFont__.getAscent()/*+2*/;
		for (int sep=0,lastsep=0, y=h*2; (sep=vinfo_.indexOf('|', sep+1))!=-1; lastsep=sep+1, y+=h) {
			smallFont__.drawString(g, vinfo_.substring(lastsep, sep), x, y);
		}
	}
//System.out.println("full image = "+fullpage_.getClass().getName()+" instanceof Buffer "+ (fullpage_ instanceof java.awt.image.BufferedImage)); => FALSE
	return super.paintAfter(cx, node);
  }


  /* *************************
   * PARSING
   **************************/

  // shared class globals to reduce sending as parameters
  // don't have to be careful about synchronization because don't have multiple threads in same object
  //char cmd;	// cmd available to error messages

  // parser helper methods

  private boolean checkParm() throws IOException {
	int c = is_.read();
	is_.unread(c);
	return (c==';');
  }

  private int readIntParam() throws IOException {
	int c = is_.read();	// initial ';'
	int val=0, sign=1;

	c=is_.read();
	if (c=='-') sign=-1; else if (c=='+') {} else is_.unread((char)c);

	for (c=is_.read(); c>='0' && c<='9'; c=is_.read()) val = val*10 + c-'0';
	is_.unread(c);

	return (sign>=0? val: -val);
  }

  private char readCharParam() throws IOException {
	int c = is_.read();	// semicolon
	assert c==';': "invariant: (char) parameters start with leading `;' not "+((char)c)+" in cmd="+c;
	return (char)is_.read();
  }

  private String readStringParam() throws IOException {
	int c = is_.read();	// semicolon
	assert c==';': "invariant: (String) parameters start with leading `;' not "+((char)c)+" in cmd="+c;

	StringBuffer sb = new StringBuffer(20);	// per page, so can be a little generous
	c = is_.read();
	// read quoted string or until ';' or ']'
	if (c=='"') {
		while ((c=is_.read())!='"') sb.append((char)c);
		// throw away closing quote
	} else {
		sb.append((char)c);
		while ((c=is_.read())!=';' && c!=']') sb.append((char)c);
		is_.unread(c);
	}

	return Strings.valueOf(sb);
  }



  private void adjustLeaf(FixedLeafOCR n) {
	double factor = MM2PPI / scale;	// just make scale equal to this

	Rectangle bbox = n.getIbbox()/*ibbox*/;

	// need to adjust for skew as well
	bbox.x = (int)(((double)bbox.x) * factor) ;//+ 1 /*+1 for roundoff error*/;
	bbox.y = (int)(((double)bbox.y) * factor);
	bbox.width = (int)(((double)bbox.width) * factor) + 2 /*+ fuzz2*/;
	bbox.height = (int)(((double)bbox.height) * factor) + 2 /*+ fuzz2*/;

	n.ibaseline = (int)((double)n.ibaseline * factor) + 1/*+1 due to fuzz in bbox*/;
  }


  /** For rules and text */
  private void addWord(FixedI tome, String name, Rectangle bbox, FontXdoc xfont) {
	FixedLeafOCR l = new FixedLeafOCR(name,null, null, full_/*fullpage_*/, bbox);

/*
	if (name.equals("")) {
		System.out.println("empty word");	// return?  special cases like rules and images use following method <= not true
	}
*/

	//l.ibaseline = baseline;
	l.ibaseline = xfont.capheight + 2;	// not ibbox.y; -- baseline relative to top of ibbox
	l.getIbbox().y -= xfont.capheight;	// computed on the fly from baseline
	l.getIbbox().height = xfont.capheight + xfont.descend + 2;
	l.font = xfont.font;

	adjustLeaf(l);	// adjust before adding
	tome.appendChild(l);
  }

  /** For rules and images */
  private void addWord(FixedI tome, String name, Rectangle bbox) {
	tome.appendChild(new FixedLeafOCR(name,null, null, full_/*fullpage_*/, bbox));
	//new FixedLeafOCR(name,null, tome, this, bbox); -- have to use above so set ibbox
//System.out.println(name+" "+bbox.x+","+bbox.y);
  }

  public Object parse(INode parent) throws Exception {
//System.out.println("******** XDOC PARSE");// #"+(++parsecnt));
	//FixedIVBox page = null;
	FixedIVBox region[] = new FixedIVBox[1000];
	//FixedIVBox rulereg = new FixedIVBox("OCR",null, parent);ihbox
	//FixedIVBox imagereg = new FixedIVBox("OCR",null, parent);
	int regionorder[] = new int[1000];
	for (int i=0; i<regionorder.length; i++) regionorder[i]=-1;
	int regioni = 0;
	int regionmax = -1;
	FontXdoc fontlist[] = new FontXdoc[200/*100 too small!*/];	// can almost use this
	fontlist[0] = new FontXdoc(NFont.getInstance("Times", NFont.WEIGHT_NORMAL, NFont.FLAG_SERIF, 12f), 0, 0);	// sometimes this is missing
	FixedIVBox para = null;	// words added to paragraphs sensibly?
	FixedIHBox line = null;
	FixedI root = null;
	int regionnum = 0;
	double xdocvers = 9.0;	// should just go with this, since will want things like "if (v>9)" now that have at least four versions of XDOC
	boolean sub=false, sup=false, under=false;
	int fontid=-1;	// safer to set to 0
	FontXdoc curfont = null;
	int c; char ch, cmd;
	int x=0,y=0,w=0,h=0,tmp;
	//int bb=0;	// bx,by,bw,bh
	int rowcnt=0, colcnt=0;
	StringBuffer sb = new StringBuffer(30);
	String word=null;
	char styletag = 'p';
	int tilt=0, untilt=0;
	String dotxdc = " must start with \"[a;...]\" -- is it an .xdc file?";

	int wordcnt=0, regcnt=0, tblcnt=0, imgcnt=0; char recmode='?', pageorient='?';
	charcnt_ = qcharcnt_ = 0;



	// first meaningful content must be start of document (`a') command
	boolean firstcmd = true;
	INode page = new INode("xdoc",null, parent);
	root = new FixedI("regions",null, page);	// always return this, maybe with no children


	is_ = new PushbackInputStream(getInputUni().getInputStream(), 1);
	while ((c=is_.read())!=-1) {
		ch = (char)c;
		if (Character.isWhitespace(ch) || ch=='\015') continue;	// strip leading whitespace and newlines

		//if (firstcmd && ch!='[') error("[ != |"+ch+"| "+in+dotxdc);

		if (ch=='[') {
			// in escape sequence
			cmd = (char)is_.read();	// command character
//System.out.println("cmd = "+cmd);
			if (cmd=='[') { sb.append('['); continue; }	// the way to put "[" in a word?

			c=is_.read(); ch=(char)c; is_.unread(c);	// parameters eat initial ';', or leave trailing char for next iteration
			boolean ops = (ch==';');	// trailing semicolon iff has parameters
			if (DEBUG) System.out.println("command "+cmd+", ops="+ops);
			if (firstcmd && cmd!='a') throw new ParseException(""+is_+dotxdc, 0); else firstcmd=false;	// could take this out of loop

			switch (cmd) {

			case 'a':	// start of document
				para = null;
				word = readStringParam();	// add version of XDOC as an attribute
				xdocvers = Double.valueOf(word.substring(5)).doubleValue();
//System.out.println("xdoc version = "+xdocvers);
				// gotta do different things for different versions
				/*if (xdocvers!=9.0 && xdocvers!=10.0 && xdocvers!=12.0 && xdocvers!=12.5) {
					  System.err.println("XDOC versions 9.0, 10.0, 12.0, 12.5 supported -- try "+xdocvers+" at your own risk");
				}*/
				if (xdocvers>=10.0) {
				  readCharParam();	// XDOC flavor - 'E'nhanced or 'L'ite
				  readStringParam();	// version of XDOC engine
				}
				break;

			case 'A':	// end cell table (new in v10.0)
				break;

			case 'b':	// word bounding box (new in v10.0)
				readIntParam();	// left -- meant something else in 7.2
				if (xdocvers>=9.0) {	// don't seem to be generated in 9.0 or 10.0
					readIntParam();	// top
					readIntParam();	// right
					readIntParam();	// bottom
					if (xdocvers>=12.0) {
						readIntParam();	// baseline
						//wordbbox = new Rectangle(bx,by, bw,bh); -- makes for uneven selection on line
						readIntParam();	// "leader?"
					}
				}
				break;

			case 'B':	// shift to/from subscript
				// later, set span
				sub = !sub;
				break;

			case 'c':	// change font
				fontid = readIntParam();
				curfont = fontlist[fontid];	// only valid for >=10.0
				break;

			case 'd':	// internal document name, set by user
				//root.setName(readStringParam());
				readStringParam();	// set title?
				break;

			case 'E':	// unrecognized character
				//sb.append('?');
				sb.append('X');
				break;

			case 'e':	// change region
				//assert readIntParam()==regionnum: "here's a case where e's region differs from s's";
				///*regionnum =*/ readIntParam();	// redundant with that set by 's'?
				readIntParam();
				break;

			case 'f':	// font information
				// Java standard font list:
				// all 'f's at bottom of file until >=10.0
				fontid = readIntParam();	// font id
				String fontname = readStringParam();	// font name, empty? -- compute from F,V
				char style = readCharParam();	// face style: T,R,I,B
				readCharParam();	// serif style: s,
				readIntParam();	// average character width, e.g., 1748
				char fv = readCharParam();	// fixed or variable character width: F,V
				int capheight = readIntParam();	// average height of uppercase characters, 0 if unknown
				int lowerw = readIntParam();	// average height of lowercase with descenders
				int lowerwo = readIntParam();	// average height of lowercase without descenders
				int points = readIntParam();	// typographic point size, e.g., 10
				if (xdocvers>=10.0) readIntParam();	// font width, e.g., 100%=normal, 80%=compressed, 120%=expanded

				// convert to NFont information
				int jweight = style=='B'||style=='T'? NFont.WEIGHT_BOLD: NFont.WEIGHT_NORMAL;
				int jflags = style=='I'||style=='T'? NFont.FLAG_ITALIC: NFont.FLAG_NONE;
				jflags |= (fv=='F'? NFont.FLAG_FIXEDPITCH: NFont.FLAG_SERIF);
				//curfont/*don't always get a 'c' first*/ => taken from 's'
//if (fontid>=fontlist.length) System.out.println("BIG FONTID! "+fontid); else
				fontlist[fontid] = new FontXdoc(NFont.getInstance(fontname, jweight, jflags, points), capheight, lowerw-lowerwo);
				if (DEBUG) System.out.println("for #"+fontid+" chose font "+fontlist[fontid]);
				break;

			case 'g':	// page information
				tilt = readIntParam();	// cosecant of angular tilt
				readIntParam(); readIntParam();	// (x,y) of top left corner
				readIntParam(); readIntParam();	// (x,y) of bottom right corner
				// tilt about center wrt upperleft
				// maybe add dummy node at lower left
				if (xdocvers>=12.0) readCharParam();	// required or optional page breaks: 'R'
				break;

			case 'H':	// optional (soft) hyphen
				sb.append('-');
				break;

			case 'h':	// nonprinting whitespace -- ends word
				w = (tmp=readIntParam()) - x + 1;
				// close up previous word (same code as 'y')
				// (font id temporarily stored in height)
				//addWord(para, sb.substring(0), new Rectangle(x,y, w,fontid));
				//if (xdocvers>=12.0) addWord(line, sb.substring(0), wordbbox, bb, curfont); else
				if (xdocvers>=10.0) addWord(line, sb.substring(0), new Rectangle(x,y, w,h), curfont); else addWord(line, sb.substring(0), new Rectangle(x,y, w,fontid));
				charcnt_+=sb.length(); wordcnt++;
				sb.setLength(0);

				x = tmp + readIntParam();
				if (checkParm()) readIntParam();	// unique ID of next word
				if (styletag=='t' && checkParm()) readIntParam();	// space character count
				if (xdocvers>=12.0) {
					if (checkParm()) readIntParam();	// tab advance character count
//		    		if (checkParm()) readIntParam();	// not needed
//if (checkParm()) System.out.println("styletag = "+styletag);
				}
				break;

			case 'j':	// new table (new in v10.0)
				readIntParam();	// uid
				readIntParam();	// left
				readIntParam();	// top
				readIntParam();	// right
				readIntParam();	// bottom
				colcnt = readIntParam();	// number of columns
				rowcnt = readIntParam();	// number of rows
				readIntParam();	// position of table on page
				for (int i=0; i<colcnt; i++) {
					if (!checkParm()) break;
					readIntParam();	// left
					readIntParam();	// right
					if (xdocvers>=12.0) {
if (!checkParm()) System.out.println("v12.0 without top,bottom in table");
						readIntParam();	// top
						readIntParam();	// bottom
					}
				}
				tblcnt++;
				break;

			case 'J':	// end dropcap
				break;

			case 'k':	// section change (new v10.0)
				readCharParam();	// type: column, header, footer, caption, timestamp ... T
				colcnt = readIntParam();	// number of columns
				readIntParam();	// if header or footer, position: left, right, center; if caption, the pic id
				readIntParam();	// if column, vertical lines; if caption, caption expands up or down
				if (xdocvers>=12.0) {
					readIntParam();	// number of vertical half lines to output in the default font if not caption or inset
					readIntParam();	// use balanced columns for word processors that can handle balanced cols and hard col breaks (such as MS Word)
					readIntParam();	// use balanced cols and hard col breaks for word processors that do not properly handle this (such as WordPerfect)
				}
				for (int i=0; i<colcnt; i++) {
				  if (!checkParm()) break;
				  readIntParam();	// left
				  readIntParam();	// right
				}
				break;

			case 'l':	// printing whitespace (leader characters)
				/*String repeated =*/ readStringParam();	// repeated characters in the leader

				if (sb.length()>0) {
					int leaderx = readIntParam();
					//addWord(para, sb.substring(0), new Rectangle(x,y, leaderx-x,fontid));
					//if (xdocvers>=12.0) addWord(line, sb.substring(0), wordbbox, bb, curfont); else
					if (xdocvers>=10.0) addWord(line, sb.substring(0), new Rectangle(x,y,leaderx-x,h), curfont); else addWord(line, sb.substring(0), new Rectangle(x,y, leaderx-x,fontid));
					charcnt_+=sb.length(); wordcnt++;
					sb.setLength(0);	// this can (always does?) result in empty words
					x = leaderx;
				} else x = readIntParam();	// left edge

				w = readIntParam();
				if (xdocvers>=12.5 && checkParm()) readIntParam();	// unique ID of next word -- seems misdocumented
				if (checkParm()) readIntParam();	// space character count (iff>1)
				if (styletag=='t' && checkParm()/* not in every table, apparently*/) readIntParam();	// tab advance character
				break;

			case 'M':	// numeric word (new in 9.0) => redefined to start/stop headline (new in 12.0)
				if (xdocvers>=12.0) {
				} else if (xdocvers>=9.0) {
					// same a usual word
					while ((c=is_.read())!='[') sb.append((char)c);
					// then convert?
					is_.unread(c);
				}
				break;

			case 'n':	// start table cell
				readIntParam();	// table uid
				readIntParam();	// col #
				readIntParam();	// colspan
				readIntParam();	// rowspan
				readIntParam();	// continuation cell?
				readIntParam();	//	horizontal alignment: left
				readIntParam();	//	# decimal placed for alignment
				readIntParam();	//	vertical alignment: top
				break;

			case 'o':	// start new table row
				readIntParam();	// row height: 0
				for (int i=0; ; i++) {	// border codes for each cell in row
					if (!checkParm()) break;
					readIntParam();	// top
					readIntParam();	// left
					readIntParam();	// bottom
					readIntParam();	// right
				}
				break;

			case 'O':	// language (new in v12.0)
				readIntParam();	// MS Windows code page
				readIntParam();	// language
				break;

			case 'p':	// start of page
				//page = new FixedIVBox("page",null, null);
				//page = new INode("page",null, null);
				para = null;
				readIntParam();	// logical page number
				pageorient = readCharParam();	// page orientation: 'P'=portrait, 'L'?=landscape
				readIntParam();	// number of zones of text (do we care?) => recomp on/off in 12.0
				if (xdocvers>=10.0) {
					recmode = readCharParam();	// recognition mode: 'S'=?
					readIntParam();	// image skew
				}
				// untilt about center w/r/t center
				untilt = readIntParam();	// page untilt -- cosecant of angular correction already applied
				if (xdocvers>=12.0) {
					readIntParam(); readIntParam();	// x, y resolution
				}
				readIntParam(); readIntParam();	// (x,y) topleft corner of page
				readIntParam(); readIntParam();	// width and height of page, if known
				if (xdocvers>=12.0) {
					readIntParam();	// user set or ok'd zones (M_USER_SPECIFIED_REGIONS) was set
					readIntParam();	// user set or ok'd zone order (M_USER_SPECIFIED_ORDER) was set
					readIntParam();	// word box units
				}
				break;

			case 'Q':	// questionable character
				qcharcnt_++;
				break;

			case 'q':	// character confidence
				readIntParam();	// 0-999
				break;

			case 'r':	// ruling descriptor.  ELIB243 has some of these
				// later add as picture, for now masquerade as FixedLeafOCR node
				x = readIntParam(); y = readIntParam();	// (x,y) of midpoint
				char orient = readCharParam();	// code for orientation: 'h' or 'v'
				w = readIntParam();	// total length
				/*char sdt =*/ readCharParam();	// style: single, double, triple
				h = readIntParam();	// thickness, 0 if unknown (assume horizontal)
				readIntParam();	// interval, 0 if unknown
				if (checkParm()) readIntParam();	// mystery undocumented number -- ID in 12.0
				/*
				// add directly under root?  separate layer?
				para = null;
				*/
				if (orient=='V') { tmp=w; w=h; h=w; }
				x -= (w/2); y -= (h/2);
				if (region[regionnum]==null) {
					region[regionnum] = new FixedIVBox("region",null, null);
					para = null;
					if (xdocvers<=9.0) regionorder[regionmax = regioni++] = regionnum;
				}
				if (xdocvers>=12.0) {
					readCharParam();	// type (recognition or IP)
					readIntParam();	// "celltable ruling?"
				}
				// NO!	XDOC puts all rules all come together at end, so don't know what region they belong to
				// maybe put all rules under common node, which is added to ocr root
				//addWord(region[regionnum], ""/*"-----" causes null pointer exception, perhaps because of following null*/, new Rectangle(x,y, w,h));
				break;

			case 'R':	// reverse video (new in 12.0)
				// later, set span
				readIntParam();	// start or stop -- inconsistent with others
				if (checkParm()) {
					readIntParam();	// rgb colors for foreground and background? (new in 12.5)
					readIntParam();
					readIntParam();
					readIntParam();
					readIntParam();
				}
				break;

			case 'S':	// shift to/from superscript
				// later, set span
				sup = !sup;
				break;

			case 's':	// start of text line
				regionnum = readIntParam();	// zone number
				if (region[regionnum]==null) {
					region[regionnum] = new FixedIVBox("region",null, null);
					para = null;
					if (xdocvers<=9.0) regionorder[regionmax = regioni++] = regionnum;
				}

				x = readIntParam();
				x += readIntParam();
				if (xdocvers>=12.0) readIntParam();	// unique id of next word
				y = readIntParam();	// baseline
				styletag = readCharParam();	// current style tag: 'p'=paragraph, 't'=table, center line
				fontid = readIntParam();	// identifier of primary font
				curfont = fontlist[fontid];
				if (checkParm()) {	// space count for indent off left margin: interpret as indent at start of new paragraph
					if (readIntParam()>0) para=null;	// indent=>start of new paragraph
				}
				if (/*styletag=='t' && not just tables*/ checkParm()/*not all tables, apparently*/) readIntParam();	// tab advance character

				//line = new FixedIHBox(null, null, this);	// null name means nonstructural (just better balancing for trees)
				line = new FixedIHBox(null,null, null);
				// start a new paragraph if some condition saw need for new one
				// works ok, though many splits from display math, column breaks
				if (para==null) {
					para = new FixedIVBox/*vbox*/("para",null, null);
//			        para.breakbefore = para.breakafter = true;
					region[regionnum].appendChild(para);
				}
				para.appendChild(line);
				//line = para;
				break;

			case 't':	// text zone descriptor
				regionnum = readIntParam();	// zone identifier
				if (xdocvers>=10.0) {	// output order
					regionorder[readIntParam()] = regionnum;
					regionmax = Math.max(regionmax, regionnum);
				}
				y = readIntParam();	// top of zone
				readIntParam();	// height of zone
				readCharParam();	// constraints on contents (obsolete; use lexical class); in 10.0, created automatically or manually; in 12.0 plain text or table
				if (xdocvers<=9.0) {
					readIntParam();	// id of sibling zone, always 0
					readIntParam();	// reserved for future use
					//readIntParam();	// reserved for future use
				} else if (xdocvers>=10.0) {
					readStringParam();	// prefix
					readStringParam();	// suffix
				}
				readStringParam();	// zone name, as defined by user
				if (xdocvers>=10.0) {
					readIntParam();	// left of region frame
					readIntParam();	// top
					readIntParam();	// right
					readIntParam();	// bottom
					if (xdocvers>=12.0) {
						readIntParam();	// top border visible?
						readIntParam();	// left
						readIntParam();	// bottom
						readIntParam();	// top
						readIntParam();	// inverse video?
					}
					while (checkParm()) readIntParam();	// language
				}
				break;

			case 'U':	// shift to/from underline
				// later, set span
				// system always records last node made
				// here, if unode==null, record last node made; if unode!=null, add span from unode.getNextLeaf() to last node, null out unode
				under = !under;
				break;

			case 'u':	// drop caps (new in v10.0)
				readIntParam();	// top
				readIntParam();	// left
				readIntParam();	// bottom
				readIntParam();	// right
				readIntParam();	// word partial or complete
				if (xdocvers>=12.0) {
					readIntParam();	// partial word or complete word
				}
				readIntParam();	// number of lines to the right
				break;

			case 'V':	// web links (new in v12.0)
				while (checkParm()) readIntParam();
				break;

			case 'v':	// lexical class
				if (xdocvers>=10.0) {
					while (checkParm()) readIntParam();
				} else if (xdocvers>=9.0) {
				  for (int i=0; i<16; i++) readIntParam();
				}
				break;

			case 'W':	// unknown (new in 12.5)
				readStringParam();
				readIntParam();
				readIntParam();
				break;

			case 'w':	// word confidence -- could maybe use this
				readIntParam();	// 0-999
				break;

			case 'x':	// image zone descriptor.  ELIB627 has some of these
				readIntParam();	// zone identifier
				if (xdocvers>=12.0) readIntParam();	// zone order
				readIntParam();	// code of graphics compression scheme used for data
				x = readIntParam();	// left edge
				if (xdocvers>=10.0) {
					y = readIntParam();	// top
					w = readIntParam()-x;	// right
					h = readIntParam()-y;	// bottom
					readStringParam();	// zone name, as defined by user
				} else if (xdocvers>=9.0) {
					w = readIntParam();	// width of picture
					y = readIntParam();	// top
					h = readIntParam();	// height
				}
				word = readStringParam();	// zone name, as defined by user
				if (xdocvers>=12.0 && checkParm()) {
					readIntParam();	// left expanded frame
					readIntParam();	// top
					readIntParam();	// right
					readIntParam();	// bottom
				}
				// later add to doc tree as picture, for now as empty text region

				if (region[regionnum]==null) {
					region[regionnum] = new FixedIVBox("region",null, null);
					para = null;
					if (xdocvers<=9.0) regionorder[regionmax = regioni++] = regionnum;
				}
				addWord(region[regionnum], "", new Rectangle(x,y, w,h)); imgcnt++;
				// show outline -- maybe XDOC 12.0-level ScanWorX does better
				break;

			case 'X':	// column break (new in 10.0)
				if (xdocvers>=12.0) readIntParam();	// hard or soft
				break;

			case 'y':	// end of text line information -- mostly redundant with 's'
				w = readIntParam() - readIntParam() - x + 1;	// x coordinate of right edge of text zone at this line
				// close up previous word (now that know x extent)
				//addWord(para, sb.substring(0), new Rectangle(x,y, w,fontid));
				//if (xdocvers>=12.0) addWord(line, sb.substring(0), wordbbox, bb, curfont); else
				if (xdocvers>=10.0) addWord(line, sb.substring(0), new Rectangle(x,y,w,h), curfont); else addWord(line, sb.substring(0), new Rectangle(x,y, w,fontid));
				charcnt_ += sb.length(); wordcnt++;
				sb.setLength(0);

				readIntParam();	// y coordinate of baseline
				if (readIntParam()>1) para=null;	// number of line advances
				readCharParam();	// 'H'=hard or 'S'=soft line ending -- use later to make paragraph units => looks unreliable
				break;

			case 'Y':	// character box (new in v12.0)
				readIntParam();	// left
				readIntParam();	// top
				readIntParam();	// right
				readIntParam();	// bottom
				break;

			case 'Z':	// end of document -- often missing
				break;


			// MY EXTENSIONS
			case '/':	// comment/ignore to end of line -- not part of XDOC spec
				while ((c=readCharParam())!=-1) if (c=='\n') break;
				break;

			case '*':	// comment/ignore to next [* -- not part of XDOC spec
				while (true) {
					if (readCharParam()=='[') {
						if ((ch=readCharParam())=='*') break; else is_.unread(ch);
//System.out.println("ch = "+ch);
					}
				}
				break;


			default:
				//throw new ParseException("unknown command: "+(char)cmd, cmd);
				// recover below rather than failing -- get quick diagnostics this way too
				//System.err.println("*** XDOC unknown command: "+(char)cmd);
				assert false: (char)cmd;
			}

			if (ops) {
				// XDOC has new versions and variations in the documented versions all the time,
				// so be robust and gobble everything up to and including closing ']'
				if ((c=is_.read())!=']') {	// closing ']' iff had operands
					System.err.print("*** XDOC ignored parameters for command "+(char)cmd+": "+(char)c);
					while ((c=is_.read())!=']') System.err.print((char)c);
					System.err.println();
				}
			}

		} else /* word */ {
			// just add to string buffer, it's added to line when can determine width

			is_.unread(c);
			// want to read bytes not characters, as xdoc sometimes has some weird ones
			while ((c=is_.read())!='[' && c!=-1) sb.append((c>255?"X":transString[c]));

			if (c==-1) throw new ParseException("end of file on word \""+sb.substring(0)+"\" -- XDOC apparently truncated", 0);

//System.out.println("word = "+sb.substring(0));
			is_.unread(c);
		}
	}

	is_.close();


	/*
	 * check for valid parse
	 */
	if (firstcmd) {
		if (DEBUG || Test) System.err.println("empty -- that's OK");
		return page/*root*/;	// not null because could maybe do something interesting with document -- OCR maybe just a piece of compound
	}


	/*
	 * postpass to sort regions, deskew, set leaf height using font info
	 */

	if (DEBUG) System.out.println("postpass: adding regions, setting bounding boxes, deskewing");

	// now have to scan over accumulated data and create tree
	// put this in switch and generalize to parse multiple pages
	//assert regionmax!=-1: "no regions found!";
	// can find text region but no text in it!
	if (regionmax==-1) {
		if (DEBUG || Test) System.err.println("region without contents -- that's OK");
		return page/*root*/;
	}
/* internal node coordinates not set yet! => but current versions of XDOC report fonts first, so if/when reprocess could do this here
	=> can use system sort by defining a comparable as preferring primarily upper, secondarily left (vice-versa?)
	// sort regions by y so general painting can stop after screen-y > next-region-y
	// insertion sort OK
	FixedIVBox regiony[] = new FixedIVBox[1000];
for (int i=0; i<=regionmax; i++) System.out.print(" "+region[regionorder[i]].bbox.y); System.out.println("	unsorted");
	for (int i=0,j=0; i<=regionmax; i++) if (regionorder[i]!=-1 && region[regionorder[i]]!=null) regiony[j++] = region[regionorder[i]];
	for (int i=0; i<=regionmax; i++) {	// foreach element
		FixedIVBox jregion = regiony[i];
		int jy = jregion.bbox.y;
		int j=i;
		for (; j>0; j--) {	// place in correct spot relative to other sorted ones
			if (jy < regiony[j-1].bbox.y) regiony[j]=regiony[j-1];
			else break;
		}
		regiony[j] = jregion;
	}
for (int i=0; i<=regionmax; i++) System.out.print(" "+regiony[i].bbox.y); System.out.println("	sorted");
*/

	for (int i=0; i<=regionmax; i++) {
		if (regionorder[i]!=-1) { root.appendChild(region[regionorder[i]]); regcnt++; }
//		if (regionorder[i]!=-1) root.appendChild(regiony[i]);
	}


	// previous to 10.0, font information reported after text,
	// and as word dimensions based on font metrics, have to postprocess leaves
	if (xdocvers<10.0) {
		for (FixedLeafOCR n=(FixedLeafOCR)root.getFirstLeaf(), endn=(FixedLeafOCR)root.getLastLeaf().getNextLeaf(); n!=endn && n!=null; n=(FixedLeafOCR)n.getNextLeaf()) {
			Rectangle ibbox = n.getIbbox();

			// non-text leaves: rules and images
			if (n.getName().equals("")) { adjustLeaf(n); continue; }
			// else text...

			// set bounding box (height based on font)
			// temporarily stored font id in height => later set font using spans
			FontXdoc xfont = fontlist[ibbox.height];
			//assert xfont!=null: "word "+l.getName()+": font #"+ibbox.height+" is null!";
			n.font = xfont.font;
			n.ibaseline = xfont.capheight + 2;	// not ibbox.y; -- baseline relative to top of ibbox
			ibbox.y -= xfont.capheight;	// computed on the fly from baseline
			ibbox.height = xfont.capheight + xfont.descend + 2;

			// if adding another to same line, align baselines by adjusting current word's if necessary
	/* maybe XDOC ensures this (defines lines by this)
			INode p = n.getParentNode();
			int absbase = ibbox.y + n.ibaseline;
			if (p!=lastp) lastbase=absbase;	// first word on line set baseline, for better or worse
			else if (p.getName()==null && absbase!=lastbase)
				n.ibaseline += (lastbase-absbase);
			lastp=p;
	*/

			adjustLeaf(n);
		}
	}

	if (DEBUG) System.out.println("returning root of OCR = "+root);


	sb.setLength(0);
	sb.append("xdocvers = ").append(xdocvers).append('|')
	  .append("scale = ").append(scale).append('|')
	  .append("recog mode = ").append(recmode).append('|')
	  .append("page orient = ").append(pageorient).append('|')
	  .append("# regions = ").append(regcnt).append('|')
	  .append("# words = ").append(wordcnt).append('|')
	  .append("# chars = ").append(charcnt_).append('|')
	  .append("# ? chars = ").append(qcharcnt_).append('|')
	  .append("# tables = ").append(tblcnt).append('|')
	  .append("# images = ").append(imgcnt).append('|')
	  ;
	vinfo_ = sb.substring(0);

	return page/*root*/;

//	} catch (ParseException e) {
//	  throw e;	// throw it up -- default
//	} catch (IOException e) {
//	  throw new ParseException("I/O error "+e, 0/*could keep track of line number*/);
	  //error("unexpected exception during parse: "+e);
//	} catch (Exception e) {
	  //error("unexpected exception during parse: "+e);
//	}

//	return null;
  }
}
