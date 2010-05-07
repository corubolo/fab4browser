package multivalent.std.adaptor;

import java.io.*;
import java.net.URI;
//import java.net.MalformedURLException;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.Iterator;
import java.util.ArrayList;

import phelps.Utility;
import phelps.lang.Integers;
import phelps.lang.Maths;
import phelps.io.InputStreams;

import com.pt.io.InputUni;
import com.pt.io.InputUniURI;

import multivalent.*;
import multivalent.node.LeafUnicode;
import multivalent.node.Root;
import multivalent.std.Outliner;
import multivalent.std.span.OutlineSpan;



/**
	Media adaptor for GNU's <a href="http://www.texinfo.org/">Texinfo format</a>.
	Modeled after TkMan's Texinfo reader high quality display and outline navigation, vs nodes of ASCII.
	Internally, build index into parts of Texinfo source, fault in source and format on demand.

	<p>To do
	translate to XML so can markup right for stylesheet control
	architect so that easy to write translator to HTML (no outlining)
	generalize outlining used for man page, apply to man page, man page volumes (if all vols), Texinfo, local directory (tree)

	@version $Revision: 1.6 $ $Date: 2005/01/01 12:58:41 $
*/
public class Texinfo extends MediaAdaptor {
  static final boolean DEBUG = false;

  static class IndexRec {
	public String title;
	//public String file;
	public URI uri;
	public int offset;	// want long but arrays indexed by int's
	public String hier;
	public String font;
	int sectnum;
	public IndexRec(String ti, URI u, int off, String misc) {
		title=ti; uri=u; offset=off; hier=font=misc;
//System.out.println("added "+this);
	}
	public String toString() { return title+" in "+uri+" @"+offset; }
  }


  static final String FAULT_ID = "[FAULT]";

  static final int
	BASE=256,	// use character values some places too
	CHAPTER=BASE, CENTERCHAP=CHAPTER+1, CHAPHEADING=CHAPTER+2,
	HEADING=CHAPHEADING+1, SUBHEADING=HEADING+1, SUBSUBHEADING=HEADING+2, MAJORHEADING=HEADING+3,
	SECTION=MAJORHEADING+1, SUBSECTION=SECTION+1, SUBSUBSECTION=SECTION+2,
	TITLE=SUBSUBSECTION+1, TITLEFONT=TITLE+1, SUBTITLE=TITLE+2, TITLEPAGE=TITLE+3, SHORTTITLEPAGE=TITLE+4, AUTHOR=TITLE+5,
	UNNUMBERED=AUTHOR+1, UNNUMBEREDSEC=UNNUMBERED+1, UNNUMBEREDSUBSEC=UNNUMBERED+2, UNNUMBEREDSUBSUBSEC=UNNUMBERED+3,
	APPENDIX=UNNUMBEREDSUBSUBSEC+1, APPENDIXSEC=APPENDIX+1, APPENDIXSECTION=APPENDIX+2, APPENDIXSUBSEC=APPENDIX+3, APPENDIXSUBSUBSEC=APPENDIX+4,
	LOWERSECTIONS=APPENDIXSUBSUBSEC+1, RAISESECTIONS=LOWERSECTIONS+1,

	TEX=RAISESECTIONS+1, HTML=TEX+1, IFTEX=TEX+2, IFHTML=TEX+3, IFINFO=TEX+4, IFNOTTEX=TEX+5, IFNOTHTML=TEX+6, IFNOTINFO=TEX+7,
	BYE=IFNOTINFO+1, C=BYE+1, COMMENT=BYE+2, IGNORE=BYE+3, END=BYE+4, INCLUDE=BYE+5, MENU=BYE+6,
	SET=MENU+1, CLEAR=SET+1, IFCLEAR=SET+2, IFSET=SET+3, VALUE=SET+4,

	CINDEX=VALUE+1, FINDEX=CINDEX+1, KINDEX=CINDEX+2, PINDEX=CINDEX+3, TINDEX=CINDEX+4, VINDEX=CINDEX+5,
	PRINTINDEX=CINDEX+6, DEFINDEX=CINDEX+7, DEFCODEINDEX=CINDEX+8, SYNCODEINDEX=CINDEX+9, SYNINDEX=CINDEX+10,

	ALIAS=SYNINDEX+1, DOCUMENTENCODING=ALIAS+1, DOCUMENTLANGUAGE=DOCUMENTENCODING+1,
	MACRO=DOCUMENTLANGUAGE+1, DEFINFOENCLOSE=MACRO+1,
	DIRCATEGORY=DEFINFOENCLOSE+1, DIRENTRY=DIRCATEGORY+1,

	EXDENT=DIRENTRY+1, NOINDENT=EXDENT+1, FOOTNOTE=NOINDENT+1, FOOTNOTESTYLE=FOOTNOTE+1,
	FLUSHLEFT=FOOTNOTESTYLE+1, FLUSHRIGHT=FLUSHLEFT+1, SP=FLUSHRIGHT+1, CENTER=SP+1,

	R=CENTER+1, B=R+1, I=B+1, T=I+1, SC=T+1, W=SC+1,
	ACRONYM=W+1, CODE=ACRONYM+1, COMMAND=CODE+1, CITE=COMMAND+1, DFN=CITE+1, DMN=DFN+1, EMAIL=DMN+1, EMPH=EMAIL+1, STRONG=EMPH+1,
	ENV=STRONG+1, FILE=ENV+1, KBD=FILE+1, KBDINPUTSTYLE=KBD+1, KEY=KBDINPUTSTYLE+1, OPTION=KEY+1, SAMP=OPTION+1, VAR=SAMP+1, ASIS=VAR+1,
	DISPLAY=ASIS+1, SHORTDISPLAY=DISPLAY+1, FORMAT=SHORTDISPLAY+1, SMALLFORMAT=FORMAT+1,
	EXAMPLE=SMALLFORMAT+1, SMALLEXAMPLE=EXAMPLE+1, LISP=SMALLEXAMPLE+1, SMALLLISP=LISP+1, MATH=SMALLLISP+1, QUOTATION=MATH+1, CARTOUCHE=QUOTATION+1,

	TABLE=CARTOUCHE+1, FTABLE=TABLE+1, VTABLE=TABLE+2, MULTITABLE=TABLE+3, TAB=TABLE+4,
	ENUMERATE=TAB+1, ITEMIZE=ENUMERATE+1, ITEM=ITEMIZE+1, ITEMX=ITEMIZE+2,

	ANCHOR=ITEMX+1, PXREF=ANCHOR+1, REF=PXREF+1, XREF=REF+1, INFOREF=XREF+1,
	URL=INFOREF+1, UREF=URL+1, IMAGE=UREF+1,

	TIEACCENT=IMAGE+1, RINGACCENT=TIEACCENT+1, DOTACCENT=TIEACCENT+2, UBARACCENT=TIEACCENT+3, UDOTACCENT=TIEACCENT+4,
	HUMLAUT=UDOTACCENT+1, HACEK=HUMLAUT+1, BREVE=HACEK+1, DOTLESS=BREVE+1,
	CHAR_AA=DOTLESS+1, CHAR_aa=CHAR_AA+1, CHAR_AE=CHAR_aa+1, CHAR_ae=CHAR_AE+1, CHAR_OE=CHAR_ae+1, CHAR_oe=CHAR_OE+1,
	CHAR_L=CHAR_oe+1, CHAR_l=CHAR_L+1, CHAR_O=CHAR_l+1, CHAR_o=CHAR_O+1, CHAR_ss=CHAR_o+1,

	BULLET=CHAR_ss+1, COPYRIGHT=BULLET+1, MINUS=COPYRIGHT+1, EXCLAMDOWN=MINUS+1, QUESTIONDOWN=EXCLAMDOWN+1,
	DOTS=QUESTIONDOWN+1, ENDDOTS=DOTS+1, POUNDS=ENDDOTS+1, TeX=POUNDS+1,
	EQUIV=TeX+1, ERROR=EQUIV+1, EXPANSION=ERROR+1, POINT=EXPANSION+1, PRINT=POINT+1, RESULT=PRINT+1, TODAY=RESULT+1
/* handle these with a table
	DEFCV=XREF+1, DEFCVX=DEFCV+1, DEFFN=DEFCV+2, DEFFNX=DEFCV+3, DEFIVAR=DEFCV+4, DEFIVARX=DEFCV+5, DEFMAC=DEFCV+6, DEFMACX=DEFCV+7, DEFMETHOD=DEFCV+8, DEFMETHODX=DEFCV+9,
	DEFOP=DEFCV+10, DEFOPX=DEFOP+11, DEFOPT=DEFCV+12, DEFOPTX=DEFCV+13, DEFSPEC=DEFCV+14, DEFSPECX=DEFCV+15, DEFTP=DEFTP+16, DEFTPX=DEFCV+17, DEFTYPE=DEFCV+18, DEFTYPEX=DEFCV+9,
	DEFTYPEFUN=DEFCV+21, DEFTYPEFUNX=DEFCV+1, DEFFN=DEFCV+2, DEFFNX=DEFCV+3, DEFIVAR=DEFCV+4, DEFIVARX=DEFCV+5, DEFMAC=DEFCV+6, DEFMACX=DEFCV+7, DEFMETHOD=DEFCV+8, DEFMETHODX=DEFCV+9, */
  ;

  static Map<String,Integer> scmd2cmd0_;
  static {
	String[] cmds = {
	//@whitespace -- An @ followed by a space, tab, or newline produces a normal, stretchable, interword space. See Multiple Spaces.
	// \input macro-definitions-file

	// structural
	"chapter", "centerchap", "chapheading",
	"heading", "subheading", "subsubheading", "majorheading",
	"section", "subsection", "subsubsection",
	"title", "titlefont", "subtitle", "titlepage", "shorttitlepage", "author",
	"unnumbered", "unnumberedsec", "unnumberedsubsec", "unnumberedsubsubsec",
	"appendix", "appendixsec", "appendixsection", "appendixsubsec", "appendixsubsubsec",
	"lowersections", "raisesections",

	// control flow + variables
	"tex", "html", "iftex", "ifhtml", "ifinfo", "ifnottex", "ifnothtml", "ifnotinfo",
	"bye", "c", "comment", "ignore", "end", "include",
	"menu", // region, so can't ignore line.  treat like @ignore
	"set", "clear", "ifclear", "ifset", "value",

	// index
	"cindex", "findex", "kindex", "pindex", "tindex", "vindex",
	"printindex", "defindex", "defcodeindex", "syncodeindex", "synindex",

	// special
	"alias", "documentencoding", "documentlanguage",
	"macro", "definfoenclose",
	"dircategory", "direntry",

	// formatting
	// spacing
	"exdent", "noindent", "footnote", "footnotestyle",
	"flushleft", "flushright", "sp", "center",

	// *** inline commands follow ***
	// spans
	"r", "b", "i", "t", "sc", "w",
	"acronym", "code", "command", "cite", "dfn", "dmn", "email", "emph", "strong", "env", "file", "kbd", "kbdinputstyle", "key", "option", "samp", "var", "asis",
	// blocks (can be put in middle of line)
	"display", "shortdisplay", "format", "smallformat", "example", "smallexample", "lisp", "smalllisp", "math", "quotation", "cartouche",
	"table", "ftable", "vtable", "multitable", "tab",
	"enumerate", "itemize", "item", "itemx",

	"anchor", "pxref", "ref", "xref", "inforef",
	"url", "uref", "image",
	// special characters and accents
/* non-alpha/unbraced handles specially
	"!", "\"", "'", "*", ",", "-", ".", ":", "=", "?", "@", "^", "`", "{", "}", "~", */
	"tieaccent", "ringaccent", "dotaccent", "ubaraccent", "udotaccent",
	"AA", "aa", "AE", "ae", "OE", "oe", "H", "v", "L", "l", "O", "o", "ss", "u",
	"bullet", "copyright", "minus", "exclamdown", "questiondown", "dots", "enddots", "pounds", "TeX",
	"equiv", "error", "expansion", "point", "print", "result", "today",

	// stylized formats
/* handle these in bulk?
	"defcv", "defcvx", "deffn", "deffnx", "defivar", "defivarx", "defmac", "defmacx", "defmethod", "defmethodx",
	"defop", "defopx", "defopt", "defoptx", "defspec", "defspecx", "deftp", "deftpx", "deftypefn", "deftypefnx",
	"deftypefun", "deftypefunx", "deftypeivar", "deftypeivarx", "deftypemethod", "deftypemethodx", "deftypeop",
	"deftypeopx", "deftypevar", "deftypevarx", "deftypevr", "deftypevrx", "defun", "defunx", "defvar", "defvarx", "defvr", "defvrx",
*/
/*
	// ignored info only: nodes
	"setfilename", "node", "top", "menu", "detailmenu", "novalidate",
*/
/*
	// ignored in HTML -- if support, pick up in header
	"paragraphindent", "exampleindent",
	// ignored printed only: paper, headers & footers
	"hyphenation", "refill",
	"headings", "evenfooting", "evenheading", "everyfooting", "everyheading", "oddfooting", "oddheading", "settitle",
	"thischapter", "thischaptername", "thisfile", "thispage", "thistitle",
	"finalout", "need", "group", "page", "vskip",
	"setchapternewpage", "setcontentsaftertitlepage", "setshortcontentsaftertitlepage",
	"contents", "shortcontents", "summarycontents",	// outline functions as table of contents
	"afourlatex", "afourpaper", "afourwide", "pagesizes", "smallbook",
*/
	};

	scmd2cmd0_ = new HashMap<String,Integer>(2 * cmds.length);
	for (int i=0,imax=cmds.length; i<imax; i++) scmd2cmd0_.put(cmds[i], new Integer(i+BASE));  // check that command not already in hash (redundant)

	// not static because of macros
	//assert getCmd("lowersections") == LOWERSECTIONS;
	//assert getCmd("ifclear") == IFCLEAR;
	//assert getCmd("synindex") == SYNINDEX;
	//assert getCmd("menu") == MENU;
	//assert getCmd("cite") == CITE;
  }

  static Map<String,String> accent2glyph_;
  static {
	String[] accents = {
	"`A", "&Agrave;", "'A", "&Aacute;", "^A", "&Acirc;", "\"A", "&Auml;", "~A", "&Atilde;",
	"`a", "&agrave;", "'a", "&aacute;", "^a", "&acirc;", "\"a", "&auml;", "~a", "&atilde;",
	"`E", "&Egrave;", "'E", "&Eacute;", "^E", "&Ecirc;", "\"E", "&Euml;",
	"`e", "&egrave;", "'e", "&eacute;", "^e", "&ecirc;", "\"e", "&euml;",
	"`I", "&Igrave;", "'I", "&Iacute;", "^I", "&Icirc;", "\"I", "&Iuml;",
	"`i", "&igrave;", "'i", "&iacute;", "^i", "&icirc;", "\"i", "&iuml;",
	"`O", "&Ograve;", "'O", "&Oacute;", "^O", "&Ocirc;", "\"O", "&Ouml;", "~O", "&Otilde;",
	"`o", "&ograve;", "'o", "&oacute;", "^o", "&ocirc;", "\"o", "&ouml;", "~o", "&otilde;",
	"`U", "&Ugrave;", "'U", "&Uacute;", "^U", "&Ucirc;", "\"U", "&Uuml;",
	"`u", "&ugrave;", "'u", "&uacute;", "^u", "&ucirc;", "\"u", "&uuml;",
	"'Y", "Yacute", "'y", "yacute", "\"", "yuml",
	",C", "Ccedil", ",c", "ccedil", "~N", "Ntilde", "~n", "ntilde",
	//"THORN", "thorn", "ETH", "eth",	-- not in Texinfo 3.0, oddly
	};
	accent2glyph_ = new HashMap<String,String>(/*2 *=> name,val in array*/ accents.length);
	for (int i=0,imax=accents.length; i<imax; i+=2) accent2glyph_.put(accents[i], accents[i+1]);
  }

  //static String[] inxtitle_ = { "c", "Concept", "f", "Function", "v", "Variable", "k", "Keystroke", "p", "Program", "t", "Data Type" };
  //static Map inx2title_ = new HashMap(inxtitle_.length);
  //static { for (int i=0,imax=inxtitle_.length; i<imax; i+=2) inx2title_.put(inxtitle_[i], inxtitle_[i+1]); }

/*
  static int[] struct2level_ = new int[cmds_.length];
  static int[] snl_ = {
	CHAPTER,0, CENTERCHAP,0, UNNUMBERED,0, APPENDIX,0, CHAPHEADING,0,
	SECTION,1, UNNUMBEREDSEC,1, APPENDIXSEC,1, APPENDIXSECTION,1,
	SUBSECTION,2, UNNUMBEREDSUBSEC,2, APPENDIXSUBSEC,2,
	SUBSUBSECTION,3, UNNUMBEREDSUBSUBSEC,3, APPENDIXSUBSUBSEC,3
  };
  static { for (int i=0,imax=snl_.length; i<imax; i+=2) struct2level_[snl_[i]]=snl_[i+1]; }

  // chapter, section, subsection, subsubsection
  static String[] int2String_ = { "0", "1", "2", "3", "4" };
*/

  // instance var
  List<IndexRec> Index_ = new ArrayList<IndexRec>(50);

  private int leveldelta_;
  // persistent
  private Map<String,String> vars_;
  private Map<String,Integer> scmd2cmd_;
  private Map<String,List<IndexRec>> indices_;
  //private String title_=null, subtitle_=null, author_=null;	// may not be set
  // macros

  /** Cache last file seen in case compressed or over http so don't have to read past first bytes. */
  private URI cacheuri_=null;
  private byte[] cachefile_=null; // should be weak ref (additional strong ref while marking up, but purgable when move to other documents)
  private int argc_;
  private String[] args_ = new String[10];
  // end persistent



  String ilev2tag(int ilev) {
	int lev = Maths.minmax(0, ilev+leveldelta_, 3);
	return (lev==0?"chapter": lev==1?"section": lev==2?"subsection": "subsubsection");
	//if (lev==0) return "chapter"; else if (lev==1) return "section"; else if (lev==2) return "subsection"; else return "subsubsection";
  }

  /** Read in index, creating on demand. */
  public Object parse(INode parent) throws IOException {
	Browser br = getBrowser();
	//URL url = br.getCurDocument().getURI();	// should fetch this from... something else
	Document doc = parent.getDocument();
	URI uri = doc.getURI();

	//if (!"file".equals(url.getProtocol())) { new LeafUnicode("Can only show Texinfo on local file system",null, parent); return parent; }

	// mapto
	// index already exists and is still valid?
	// no, compute new one
	// if took awhile to compute, save it --- DDD 450K 160-220ms on 500MHz Pentium III
	//		=> check emacs and elisp to see if worth cacheing any!	speed if compressed?  cache needed over http?
	long pre = System.currentTimeMillis();
	index(uri);
	long post = System.currentTimeMillis();

	StringBuffer sb = new StringBuffer(10000);
	sb.append("<html>\n<head>");
	sb.append("\t<title>").append("Texinfo ").append(uri.getPath()).append("</title>\n");
	//sb.append("\t<base href='").append(uri).append("'>\n");
	sb.append("</head>\n");
	sb.append("<body>\n");

	sb.append("<p>Index built in <b>").append((post-pre)).append("ms</b></p>");
	boolean[] pending = new boolean[4];
	int cnt=0;
	for (Iterator<IndexRec> i=Index_.iterator(); i.hasNext(); cnt++) {
		IndexRec rec = i.next();
		int lev;  if (rec.hier=="chapter") lev=0; else if (rec.hier=="section") lev=1; else if (rec.hier=="subsection") lev=2; else lev=3;
		//String htag; if (rec.hier=="chapter") htag="h2"; else if (rec.hier=="section") htag="h3"; else if (rec.hier=="subsection") htag="h4"; else htag="h5";
		for (int j=3,jmin=lev; j>=jmin; j--) if (pending[j]) { sb.append("</span>"); pending[j]=false; }
		sb.append("\n<span behavior='OutlineSpan' id=").append(cnt).append(" level=").append(lev).append('>'); pending[lev]=true;
		sb.append("\n<h").append((lev+2)).append(" class='").append(rec.hier).append("'>").append(rec.title).append("</h").append((lev+2)).append('>');
		sb.append("<p>").append(FAULT_ID).append("</p>");
	}
	for (int j=3; j>=0; j--) if (pending[j]) sb.append("</span>");	// close up everybody at end
	sb.append("\n</body></html>\n");


	parseHelper(sb.toString(), "HTML", getLayer(), parent);

	br.eventq(Outliner.MSG_MADE, parent);

	return parent;
  }


  private int getCmd(String scmd) {
	//if (scmd==null) return -1;
	Integer icmd = scmd2cmd_.get(scmd); // on copy with possible aliases
	return icmd!=null? icmd.intValue(): -1;
  }

  public void index(URI uri) throws IOException {
	leveldelta_=0;
	vars_ = new HashMap<String,String>(20);
	scmd2cmd_ = scmd2cmd0_;	  // alias may augment but few use it so copy on demand
	indices_ = new HashMap<String,List<IndexRec>>(10);
	try { index2(uri); } catch (IOException ioe) {}
  }

  /** Recurse on @include. */
  public void index2(URI uri) throws IOException {
	// doesn't use supplied InputStream
	byte[] buf = readFile(uri); // local pointer so can recurse and so faster (function call=>array index)
//System.out.println("buf = "+new String(buf,0,1024));
	// can't use BufferInputStream readLine() because have to keep track of byte offsets and need to know if line end is one or two bytes
	//PushbackInputStream in = new PushbackInputStream(new BufferedInputStream(new FileInputStream(f)));

	String end=null;

	byte c;
	int inx;
	// end of buf has '\n'
	for (int boff=0,bmax=buf.length; boff<bmax; ) {
		c = buf[boff++];
		// ignore non-command lines while indexing
//System.out.println("main loop, c="+(char)c);
		if (c!='@' || Character.isWhitespace((char)buf[boff]) || (end!=null && (buf[boff]!='e' || buf[boff+1]!='n' || buf[boff+2]!='d'))) {
//System.out.println("skipping line w/end="+end);
			while (c!='\n' && c!='\r') c=buf[boff++];
			if (c=='\r' && buf[boff]=='\n') boff++;
			continue;
		}

		// collect command
		String scmd=null, arg=null;
		int cmdoff=boff, argoff;
		while (!Character.isWhitespace((char)(c=buf[boff]))) boff++;
		//if (boff-cmdoff>=2)
		scmd=new String(buf, cmdoff, boff-cmdoff);	   // min 2-character command to be valid during indexing
//System.out.println("len="+(boff-cmdoff)+", "+scmd);
		while (((c=buf[boff])==' ' || c=='\t')) boff++;
		// collect arg, if any (and eat to end of line)
		argoff=boff;
		while ((c=buf[boff])!='\n' && c!='\r') boff++;
		if (argoff<boff) arg=new String(buf, argoff, boff-argoff);
		if (c=='\r' && buf[boff+1]=='\n') boff++;

		// quick exits
//System.out.println("cmd=|"+csb.substring(0)+"|");
		//if (scmd==null) continue;
		//if (csb.length()==0 && (csb.length()==1 && csb.charAt(0)=='c')) continue;  // quick path for comment

		// process commands
		int cmd = getCmd(scmd);

		if (end!=null) {	// end!=null => in elided region
//System.out.println("end="+end+" vs cmd="+scmd);
			if (cmd==END && end.equals(arg)) end=null;
		} else /*if (on)*/ switch (cmd) {
		// structural for outline
		case TITLEPAGE: case SHORTTITLEPAGE: Index_.add(new IndexRec("Title Page", uri, boff, ilev2tag(0))); break;
		case CHAPTER: case CENTERCHAP: case CHAPHEADING: case MAJORHEADING: case UNNUMBERED: case APPENDIX:
			Index_.add(new IndexRec(arg, uri, boff, ilev2tag(0)));
			break;
		case SECTION: case HEADING: case UNNUMBEREDSEC: case APPENDIXSEC:
			Index_.add(new IndexRec(arg, uri, boff, ilev2tag(1)));
			break;
		case SUBSECTION: case SUBHEADING: case UNNUMBEREDSUBSEC: case APPENDIXSUBSEC:
			Index_.add(new IndexRec(arg, uri, boff, ilev2tag(2)));
			break;
		case SUBSUBSECTION: case SUBSUBHEADING: case UNNUMBEREDSUBSUBSEC: case APPENDIXSUBSUBSEC:
			Index_.add(new IndexRec(arg, uri, boff, ilev2tag(3)));
			break;

//System.out.println("|"+scmd+"|	 |"+arg+"|	@"+boff);
		case LOWERSECTIONS: leveldelta_--; break;
		case RAISESECTIONS: leveldelta_++; break;

		//case TITLE: title_=arg; break;	-- not necessarily set, so can't rely on for other uses, as in title
		//case AUTHOR: author_=arg; break;

		// control flow
		case SET:
			String name=arg, val=name;
			inx=arg.indexOf(' '); if (inx==-1) inx=arg.indexOf('\t');
			if (inx!=-1) { name=arg.substring(0,inx); val=arg.substring(inx+1).trim(); }
			vars_.put(name, val);
			break;
		case CLEAR: vars_.remove(arg); break;
		case IFCLEAR: if (vars_.get(arg)!=null) end="ifclear"; break;
		case IFSET: if (vars_.get(arg)==null) end="ifset"; break;
		case IFINFO:	// pretend we're Info: TeX grungy and old doc don't know about HTML (may revise this later)
		case IFNOTTEX:
			break;
		case TEX: case HTML: case MENU:
		case IFTEX:	case IFHTML:
		case IGNORE: case IFNOTHTML: case IFNOTINFO:
			end=scmd;
			break;
		case INCLUDE:
			// can do this over http, actually
			if (arg!=null) {
				try {
					index2(uri.resolve(arg));
				} catch (IllegalArgumentException male) {
					System.err.println("bad URI: "+uri+" + "+arg+" => "+male);
				}
			}
			break;
		case BYE:
			return;

		case SYNCODEINDEX:
			break;
		case SYNINDEX:
			break;

		// special cases
		case ALIAS:
			if (arg!=null && (inx=arg.indexOf('='))!=-1) {
				if (scmd2cmd_==scmd2cmd0_) scmd2cmd_=new HashMap<String,Integer>(scmd2cmd0_);	// copy on demand
				String newcmd=arg.substring(0,inx), oldcmd=arg.substring(inx+1);
				Integer curint = scmd2cmd_.get(oldcmd);
				if (curint!=null) scmd2cmd_.put(newcmd, curint);
			}
			break;

		default:
			// can create new indexes so can't use case statements exclusively
			// ok not be maximally fast here since just making one-time (or so) index
			if (scmd.endsWith("index") && scmd.length()>5 /*&& cmd!=PRINTINDEX && cmd!=SYNCODEINDEX && cmd!=SYNINDEX -- can't happen as these are cases above*/) {
				String inxname = scmd.substring(0,scmd.length()-5);
				List<IndexRec> index = indices_.get(inxname);
				if (index==null) { index=new ArrayList<IndexRec>(25); indices_.put(inxname, index); }
				index.add(new IndexRec(arg, uri, boff, "chapter"));
			}

			//ignore @comment, ... everything else
		}
	}

/* need to save to disk for Emacs (4.5MB) and Elisp (2.5MB)?
			try {
				Multivalent control = getGlobal();
				Cache cache = control.getCache();
				URI uri = pendingdoc_.getURI();
				File mapto = cache.mapTo(uri,"fused.html", Cache.COMPUTE);
System.out.println("writing to "+mapto);
				BufferedOutputStream cacheout = new BufferedOutputStream(new FileOutputStream(mapto));
				cacheout.write(newdoc.getBytes());
				cacheout.close();
			} catch (IOException ioe) {
System.out.println("ERROR WRITING "+ioe);
			}
*/
  }


  /** Markup. */
  public Node markup(URI uri, int offset, int endoff) throws IOException {
	byte[] buf = readFile(uri); // local pointer so can recurse and so faster (function call=>array index)
	if (endoff==-1) endoff=buf.length;
//System.out.println("markup = "+new String(buf,offset,1024));

	StringBuffer sb = new StringBuffer(2 * (endoff-offset));
	sb.append("<html>\n<body>\n<div>\n");	// no HEAD--just throw away here

	markup2(buf, offset, endoff, sb);

	sb.append("</div>\n</body>\n</html>\n");
//System.out.println(sb);

	Browser br = getBrowser();
	//Root root = new Root(null, br);
	Document doc = new Document(null,null, null, br);

	Node root = (Node)parseHelper(sb.toString(), "HTML", doc.getLayer(Layer.BASE), doc);

	return root;
  }

  /** @return count of args. */
  int parseArgs(byte[] buf, int boff, int endoff, StringBuffer sb) {
	// recursively process arg string (maybe not necessary)
	int sblen = sb.length();
	int delta = markupLine(buf, boff, endoff, sb);

	for (int i=sblen,imax=sb.length(),lastargi=sblen,argc_=0; i<=imax; i++) {
		if (i==imax || sb.charAt(i)==',') {
			args_[argc_++] = (lastargi<i? sb.substring(lastargi,i-lastargi): "");
			lastargi=i+1;
		}
	}
	sb.setLength(sblen);	// restore sb to entry length
	return delta;
  }

  /**
	Process inline spans, usually braced.
	Recursive for nested markup.
	Used by indexing (titles can have markup) as well as faulted-in contents.
	@return bytes consumed
  */
  int markupLine(byte[] buf, int offset, int endoff, StringBuffer sb) {
	int paccent=-1; // pending accent, brace

	int boff=offset;
	byte c = buf[boff++];	// eat at least one char.  invariant boff points to one past current char
	for (int bmax=endoff; boff<endoff && c!='\n' && c!='\r'; c=buf[boff++]) {

		if (c=='}') {	 // end braced (use '@}' for literal '}')
			return (boff-offset);

		} else if (c!='@') {   // most commonly just a normal character
			//if ((c=='\n' || c=='\r') && pbrace==-1) break;  // braced across lines?
			if (paccent!=-1) {
				// if have glyph, use it.  else write accent then character separately
				String key = ""+((char)paccent)+((char)c);
				String glyph = accent2glyph_.get(key);
				if (glyph!=null) sb.append(glyph); else sb.append((char)c).append((char)paccent);
			} else sb.append((char)c);
			paccent=-1;

		} else if (((c=buf[boff++])>='a' && c<='z') || c==',') {   // braced == alphabetic + "@," == nestable
			int cmdoff=boff-1;
			while (buf[boff++]!='{') {}
			String scmd = new String(buf, cmdoff, boff-cmdoff-1);  // >=1 or wouldn't get here
//System.out.println("span |"+scmd+"|");
			int cmd = getCmd(scmd);
			String appendme=null, pbrace=null;
			switch (cmd) {
			// font
			case R: appendme="<span behavior='multivalent.std.span.PlainSpan'>"; pbrace="</span>"; break;
			case B: appendme="<b>"; pbrace="</b>"; break;
			case I: appendme="<i>"; pbrace="</i>"; break;
			case FILE: // fall through to T
			case T: appendme="<tt>"; pbrace="</tt>"; break;
			case SC:
				//appendme="<small>"; pbrace="</small>"; break;
				boff += parseArgs(buf, boff, endoff, sb);
				if (argc_>=1) sb.append("<small>").append(args_[0].toUpperCase()).append("</small>");
				break;
			case TITLEFONT: appendme="<font size=+2>"; pbrace="</font>"; break;
			case W: appendme="<nobr>"; pbrace="</nobr>"; break;
			case ACRONYM: appendme="<acronym>"; pbrace="</acronym>"; break;
			case COMMAND: // fall through to CODE
			case CODE: appendme="<code>"; pbrace="</code>"; break;
			case CITE: appendme="<cite>"; pbrace="</cite>"; break;
			case DFN: appendme="<dfn>"; pbrace="</dfn>"; break;
			case DMN: break;
			case EMPH: appendme="<em>"; pbrace="</em>"; break;
			case STRONG: appendme="<strong>"; pbrace="</strong>"; break;
			case KBD: appendme="<i><tt>"; pbrace="</tt></i>"; break;	// though HTML does have KBD
			case KBDINPUTSTYLE:
			case KEY: appendme="&lt;"; pbrace="&gt;"; break;
			case OPTION: // fall through to SAMP
			case SAMP: appendme="`<samp>"; pbrace="</samp>'"; break;
			case ENV: // fall thorugh to VAR
			case VAR: appendme="<var>"; pbrace="</var>"; break;
			case MATH: appendme="<tt>"; pbrace="</tt>"; break;
			case ASIS: break;

			// accents (also non-alpha/non-braced, below)
			case ',': paccent=','; break;
			case TIEACCENT: paccent='['; break;
			case RINGACCENT: paccent='*'; break;
			case DOTACCENT: paccent='.'; break;
			case UBARACCENT: paccent='_'; break;
			case UDOTACCENT: paccent='.'; break;
			case HUMLAUT: paccent='"'; break;
			case BREVE: paccent='('; break;
			case HACEK: paccent='<'; break;
			case DOTLESS: paccent='o'; break;

			case CHAR_AA: appendme="&Aring;"; break;
			case CHAR_aa: appendme="&aring;"; break;
			case CHAR_AE: appendme="&AElig;"; break;
			case CHAR_ae: appendme="&aelig;"; break;
			case CHAR_OE: appendme="&OElig;"; break;
			case CHAR_oe: appendme="&oelig;"; break;
			case CHAR_L: appendme="/L"; break;
			case CHAR_l: appendme="/l"; break;
			case CHAR_O: appendme="&Oslash;"; break;
			case CHAR_o: appendme="&oslash;"; break;
			case CHAR_ss: appendme="&szlig;"; break;
			case BULLET: appendme="&#149;"; break;	// &bull; is in 8xxx -- out of 0-255
			case COPYRIGHT: appendme="&copy;"; break;
			case MINUS: appendme="<code>-</code>"; break;
			case EXCLAMDOWN: appendme="&iexcl;"; break;
			case QUESTIONDOWN: appendme="&iquest;"; break;
			case DOTS: appendme="<small>...</small>"; break;
			case ENDDOTS: appendme="<small>...</small>."; break;
			case POUNDS: appendme="&pound;"; break;
			case TeX: appendme="T<sub>E</sub>X"; break;
			case RESULT: appendme="=>"; break;
			case EXPANSION: appendme="==>"; break;
			case PRINT: appendme="-|"; break;
			case ERROR: appendme="error-->"; break;
			case EQUIV: appendme="=="; break;
			case POINT: appendme="-!-"; break;
			case TODAY: appendme="[TODAY]"; break;


			case ANCHOR:
				// LATER
				break;
			case REF:
			case XREF:
			case PXREF:
				String txt;
				if (cmd==XREF) txt="See "; else if (cmd==PXREF) txt="see"; else txt="";
				// LATER
				break;
			case INFOREF:
				// LATER
				break;
			case URL:
				boff += parseArgs(buf, boff, endoff, sb);// -1;    // -1 => immediately return from '}'
				if (argc_>=1) sb.append("<a href='").append(args_[0]).append('>').append(args_[argc_>=2?1:0]).append("</a>");
				break;
			case UREF: appendme="&lt;"; pbrace="&gt;"; break;
			case EMAIL:
				boff += parseArgs(buf, boff, endoff, sb);// -1;
				if (argc_>=1) sb.append("<a href='mailto:").append(args_[0]).append('>').append(args_[argc_>=2?1:0]).append("</a>");
				break;
			case IMAGE:
				// LATER
				// TeX wants xxx.eps, PDFTeX wants xxx.pdf, info wants xxx.txt
				break;

			}
			if (appendme!=null) sb.append(appendme);

			// process interior, which may have nested commands
			boff += markupLine(buf, boff, endoff, sb);

			// close up
			if (pbrace!=null) sb.append(pbrace);
			//} else sb.append('}');	// unescaped "}" -- error?

//System.out.println("text cmd: |@"+cmd+"|");
		} else { //if (/*c=='@' && */c<' ') {	// end braced ('@}' for li		  } els(c=buf[boff++]) if (c<' ') {   // unbraced
			// unbraced
			switch (c) {
			case '"':
			case '\'':
			case ',':
			case '=':
			case '^':
			case '`':
			case '~':
				paccent = c;
				break;
			//case ',':   // exception: process interior -- handled with bracketed above
			case '.': case '!': case '?':	// after capital letter that really does end a sentence
			case '}': case '{': case '@':
				sb.append((char)c);
				break;
			case '-':	// discretionary hyphen
			case ':':	// no extra space
				// ignore
				break;
			case ' ': case '\t': case '\r': case '\n': sb.append(' '); break;
			case '*': sb.append("<br>\n"); break;
			}
		}
	}

	sb.append(' '); // eol==whitespace
	if (c=='\r' && buf[boff]=='\n') boff++;
	return (boff-offset);
  }

  /** Process regions, dispatch for content. */
  private void markup2(byte[] buf, int offset, int endoff, StringBuffer sb) {
	byte c;
	int inx;
	boolean inpara=false;
	String end=null;	// for non-nestable
	//String[] rendv=new String[20];	// regions nest
	//int[] rendi=new int[rendv.length];
	//int rendc=0;

	for (int boff=offset; boff<endoff; ) {
//System.out.println("parsing "+new String(buf, boff, 50));
		int cmdoff=boff;
		c = buf[boff++];
		// ignore non-command lines while indexing
//System.out.println("main loop, c="+(char)c);
//System.out.println("main loop |"+new String(buf, cmdoff, 50)+"|");

		// ignore line?
		if (end!=null && (c!='@' || (buf[boff]!='e' || buf[boff+1]!='n' || buf[boff+2]!='d'))) {
			while (c!='\n' && c!='\r') c=buf[boff++];
			if (c=='\r' && buf[boff]=='\n') boff++;
			continue;
		}


		// blank line
		if (c=='\n' || c=='\r') {
			if (inpara) sb.append("</p>\n");
			inpara=false;
			if (c=='\r' && buf[boff]=='\n') boff++;
			continue;
		}


		// now could be either command or text line,
		// text line can look like command by starting with span command
		boolean fregion = (c=='@'); // possible region command
		fregion = fregion && (Character.isLetter((char)buf[boff++]));  // ' ' and some accents
		for ( ; fregion && !Character.isWhitespace((char)(c=buf[boff])); boff++) {
			if (c=='{') fregion=false;
		}
		int cmd; String scmd;

		// line of text
		if (!fregion || (cmd=getCmd(scmd=new String(buf,cmdoff+1,boff-cmdoff-1)))>=B) {
			if (!inpara) { sb.append("<p>"); inpara=true; }
//System.out.println("line |"+new String(buf, cmdoff, 50)+"|");
			boff = cmdoff + markupLine(buf, cmdoff, endoff, sb);
//System.out.println("after |"+new String(buf, boff, 50)+"|");
			continue;
		}

		// region command
		String arg=null; int argoff;
		arg=scmd;
//		  scmd=new String(buf, cmdoff+1, boff-cmdoff-1);
//System.out.println("len="+(boff-cmdoff)+", "+scmd);
		while (((c=buf[boff])==' ' || c=='\t')) boff++;
		// collect arg, if any (and eat to end of line)
		argoff=boff;
		while ((c=buf[boff])!='\n' && c!='\r') boff++;
		if (argoff<boff) arg=new String(buf, argoff, boff-argoff);
		boff++; if (c=='\r' && buf[boff]=='\n') boff++;

		// process commands
//if (cmd==-1) System.out.println("ignored cmd = "+new String(buf,cmdoff+1,boff-cmdoff-1));  //cmds_[cmd]);

		if (end!=null) { //cmd==END && !on) {
			if (cmd==END && end.equals(arg)) end=null;	   // if off, don't nest
			//if (end!=null && end.equals(arg)) { end=null; on=true; }	// nested regions allowed? YES

		} else {
			String appendme=null;
			/*if (on)*/ switch (cmd) {
			//String rend=null;
			// see these again when index marks up its content
			// structural for outline
			case TITLEPAGE: case SHORTTITLEPAGE:
			case CHAPTER: case CENTERCHAP: case CHAPHEADING: case MAJORHEADING: case UNNUMBERED: case APPENDIX:
			case SECTION: case HEADING: case UNNUMBEREDSEC: case APPENDIXSEC:
			case SUBSECTION: case SUBHEADING: case UNNUMBEREDSUBSEC: case APPENDIXSUBSEC:
			case SUBSUBSECTION: case SUBSUBHEADING: case UNNUMBEREDSUBSUBSEC: case APPENDIXSUBSUBSEC:
				break;

	//System.out.println("|"+scmd+"|	 |"+arg+"|	@"+boff);

			// control flow
			case SET:	// vars_ can change during reading
				String name=arg, val=name;
				inx=arg.indexOf(' '); if (inx==-1) inx=arg.indexOf('\t');
				if (inx!=-1) { name=arg.substring(0,inx); val=arg.substring(inx+1).trim(); }
				vars_.put(name, val);
				break;
			case CLEAR: vars_.remove(arg); break;
			case IFCLEAR: if (vars_.get(arg)!=null) end="ifclear"; break;
			case IFSET: if (vars_.get(arg)==null) end="ifset"; break;
			case TEX: end="tex"; break;
			case HTML: end="html"; break;
			case IFTEX: end="iftex"; break;
			case IFHTML: end="ifhtml"; break;
			case IFINFO: break;	// pretend we're info (old manuals don't write to HTML)
			case IGNORE: end="ignore"; break;
			case IFNOTTEX: break;
			case IFNOTHTML: break;
			case IFNOTINFO: end="ifnotinfo"; break;
			case MENU: end="menu"; break;
			case INCLUDE:  // includes must be well formed structural units?
			/*
				if (arg!=null) {
					try {
						//markup(new URL(url, arg));
					} catch (MalformedURLException male) {
						System.err.println("bad URL: "+url+" + "+arg+" => "+male);
					}
				}*/
				break;
			case BYE:
				return;


			// translation to HTML, finally
			// @REGION .. @end REGION
			case TABLE: // HTML DL
			case FTABLE: // + findex
			case VTABLE: // + vindex
			case MULTITABLE: // HTML table-ish, more at roff table
				break;
			case TAB:
			case ENUMERATE: // LATER: arg gives start (e.g., '3') or type ("a' or 'C')
				appendme="<ol>\n";
				break;
			case ITEMIZE: appendme="<ul>\n"; break;
			// item for itemize, enumerate, table, multitable
			case ITEM: sb.append("\n<li>"); inpara=true; markupLine(buf, boff, endoff, sb); break;
			case ITEMX: sb.append("<br>\n"); inpara=true; markupLine(buf, boff, endoff, sb); break;
			case EXDENT:
			case NOINDENT:
			case FOOTNOTE:
			case FOOTNOTESTYLE: // 'end' or 'separate'
			case PRINTINDEX:
			case TITLE: sb.append("<h2>"); boff+=markupLine(buf, boff, endoff, sb); sb.append("</h2>"); break;
			//case SUBTITLE: sb.append("<p align='right'>"); boff+=markupLine(buf, boff, bend, sb); break;
			case SUBTITLE: sb.append("<h2>"); boff+=markupLine(buf, boff, endoff, sb); sb.append("</h2>"); break;
			case AUTHOR: sb.append("<br><br><br><br>"); boff+=markupLine(buf, boff, endoff, sb); break;
			case CENTER:
				sb.append("<center>");
				boff += markupLine(buf, boff, endoff, sb);
				sb.append("</center>");
			case SP:
				if (arg!=null) try {
					for (int i=0,imax=Integer.parseInt(arg); i<imax; i++) sb.append("<br>");
				} catch (NumberFormatException spnfe) {}
				break;

			case QUOTATION:
				// blockquote
				break;
			case SMALLLISP: // equivalent to LISP
			case LISP: // fall through to EXAMPLE
			case SMALLEXAMPLE: // since never printing to smallbook format, all SMALL* equivalent to non-small
			case EXAMPLE: // blockquote+pre (not filled)
				break;
			case SHORTDISPLAY:	// equivalent to DISPLAY
			case DISPLAY:	// blockquote+not filled but no change in font
				break;
			case SMALLFORMAT:	// equivalent to FORMAT
			case FORMAT:
				// no fixed-width, no blockquote
				break;
			case FLUSHLEFT:
				// pre?
				break;
			case FLUSHRIGHT:
				// pre + align=right?
				break;
			case CARTOUCHE: appendme="\n<table><tr><td>"; break;

			case END:
				// re-dispatch on arg
				// what if not well nested?
				if (arg!=null && (cmd=getCmd(arg))!=-1) switch (cmd) {
				case ENUMERATE: appendme="</ol>\n"; inpara=false; break;
				case ITEMIZE: appendme="</ul>\n"; inpara=false; break;
				case CARTOUCHE: appendme="</td></tr></table>"; break;
				}
			}
/* these handled in indexing pass
		case LOWERSECTIONS:
		case RAISESECTIONS:

		case SYNCODEINDEX:
		case SYNINDEX:
		case ALIAS:

		// ignore these
		case C:
		case COMMENT:
*/

			if (appendme!=null) sb.append(appendme);
			//if (end!=null) on=false;
			//if (rend!=null) { rendv[rendc]=rend; rendi[rendc]=cmd; rendc++; } -- no sense keeping track as not well defined if not well nested
		}
	}
//System.exit(0);
  }



	// always special pass for comments

  // REAL WORK FOLLOWS

  // index is public because may want to pre-index with external driver
  // given stream and name, abstract

  /** Cache in memory so don't have to read past other sections all the time. */
  private byte[] readFile(URI uri) throws IOException {
System.out.println("read "+uri+" vs cached "+cacheuri_);
	if (uri.equals(cacheuri_)) return cachefile_;

	InputUni iu = new InputUniURI(uri, getGlobal().getCache());
	ByteArrayOutputStream bout = new ByteArrayOutputStream(1 + (int)Math.max(iu.length(), 10*1024));
	InputStreams.copy(iu.getInputStream(), bout, true);
	bout.write('\n');	// sentinal

	cachefile_ = bout.toByteArray();
	cacheuri_ = uri;

	return cachefile_;
  }

  public boolean semanticEventAfter(SemanticEvent se, String msg) {
	Browser br = getBrowser();
	Object arg;
	if (super.semanticEventAfter(se,msg)) return true;
	else if (OutlineSpan.MSG_OPEN==msg && (arg=se.getArg())!=null) {
		Span span = (Span)arg;
		int id=Integers.parseInt(span.getAttr("id"), -1);
System.out.println("id="+id+", fault in "+Index_.get(id));
		if (id!=-1) {
			Leaf out0 = span.getStart().leaf;
			INode p=out0.getParentNode();	// h4 or whatever
			int rsibnum = 1 + p.childNum();
//System.out.println(n.getName()+"	 "+p.getName()+"/"+p.childNum()+"	"+pp.getParentNode().getName()+"/"+pp.size());
			INode pp = p.getParentNode();	// common parent of h4 and p's (headers and content blocks)
			Leaf l = pp.childAt(rsibnum).getFirstLeaf();
//System.out.println(n.getName()+" => "+l.getName());
			if (FAULT_ID.equals(l.getName())) {
				span.moveq(null);
				IndexRec rec=Index_.get(id), nextrec = id+1<Index_.size()? Index_.get(id+1): null;
//if (id+1<Index_.size()) System.out.println(" to "+Index_.get(id+1));
				try {
					Node html = markup(rec.uri, rec.offset, (nextrec!=null && rec.uri.equals(nextrec.uri)? nextrec.offset: -1));
					// => rather than various childAt()'s, use search DFS for "p" or "div" tag!
					if (html!=null) {
//html.dump();
						Node splice = html.findDFS("div");
						pp.setChildAt(splice, rsibnum);
						Leaf lastn = splice.getLastLeaf();
						span.move/*no q*/(out0,0, lastn,lastn.size());
//System.out.println("spiced into "+rsibnum);
					}
				} catch (IOException ioe) {
					Utility.warning("couldn't fault in "+rec.uri+": "+ioe);
				}
			}
		}

	} else if (Document.MSG_OPEN==msg) {	// && arg is this doc
		//cacheuri_ = cachefile_ = null;	// allow gc -- make weak ref.
	}

	return false;
  }
}
