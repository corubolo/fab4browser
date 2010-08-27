package multivalent.std.adaptor;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.net.URI;

import phelps.lang.Strings;
import phelps.lang.Booleans;

import multivalent.*;
import multivalent.node.LeafUnicode;
import multivalent.node.IParaBox;
import multivalent.node.IVBox;
import multivalent.node.LeafText;
import multivalent.std.span.RelPointSpan;
import multivalent.std.span.HyperlinkSpan;
import multivalent.std.span.ScriptSpan;
import multivalent.std.ui.DocumentPopup;



/**
	Media adaptor for UNIX manual pages (directly from roff -man source).
	Along with HTML and ASCII, example of a media adaptor directly building the tree rather than first translating to a big HTML String.
	No need for nroff, rman, or caching formatted pages.
	Parsing is much faster than TkMan, because TkMan can't parse in Tcl (waaay to slow)	and rman costs and exec and generating a big string and processing a big string by Tk text widget.
	In fact, this is so fast that there is no need for preformatted cat versions of pages,
	or for progressive rendering as manual pages are usually local and even is bash short enough (200K) to be fast (4 seconds, with decompression while playing MP3 on 500MHz Pentium III).

	@see multivalent.std.adaptor.ManualPageVolume
	@see multivalent.std.Outliner <!-- outlining is added externally -->

	@version $Revision: 1.17 $ $Date: 2005/02/10 05:47:56 $
*/
public class ManualPage extends MediaAdaptor {
  static final boolean DEBUG = false && multivalent.Meta.DEVEL;


  private static final Map<String,Object> SPANATTRS = new CHashMap<Object>(5);
  static {
	SPANATTRS.put(ScriptSpan.ATTR_SCRIPT, "event "+Document.MSG_OPEN+" manpage:$node");
	SPANATTRS.put(ScriptSpan.ATTR_TITLE, "Search for man page");
  }

  private static final String MANVALID = "._-+:";	// characters which can appear within man page name (e.g., "Stream::open"), in addition to alphanum

  private static Map<String,String> lcexceptions_;
  static {
	String[] ex = {
// new rule: double consonants == UC?
	// articles, verbs, conjunctions, prepositions, pronouns
	// for these, could use list in Utility.COMMONWORDS */
	"a", "an", "the",
	"am", "are", "is", "were",
	"and", "or",
	"as", "by", "for", "from", "in", "into", "it", "of", "on", "to", "with",
	"that", "this",

	// terms
	"ASCII", "API", "CD", "GUI", "UI", /*I/O=>I/O already*/ "ID", "IDs", "OO",
	"IOCTLS", "IPC", "RPC",

	// systems
	"AWK", "cvs", "rcs", "GL", "vi", "PGP", "QuickTime",
	"NFS", "NIS", "NIS+", "AFS",
	"UNIX", "SysV",
	"XFree86", "ICCCM",
	"MH", "MIME",
	"TeX", "LaTeX", "PicTeX",
	"PostScript", "EPS", "EPSF", "EPSI",
	"WWW", "HTML", "URL", "URI",

	// institutions
	"ANSI", "CERN", "GNU", "ISO", "POSIX", "NCSA",

	// Sun-specific
	"MT-Level", "SPARC",
	};

	lcexceptions_ = new HashMap<String,String>(2*ex.length);
	for (String x: ex) lcexceptions_.put(x.toUpperCase(), x);
  }


  // if macro system specific, put in this table rather than case statement so that it can be overridden
  static final HashMap<String,String>/*cloned*/ strings_, macros_;
  static {
	// built-in strings
	String[] str = {
	// math
	"pl","+", "mi","-", "eq","=", "**","*", "sc","S", "aa","'", "ul","_", "sl","/",
	"sr","/", "rn","-", ">=",">=", "<=","<=", "==","==", "~=","~=", "ap","~", "!=","~=",
	"->","->", "<-","<-", "ua","^", "da","v", "mu","x", "di","/", "+-","+-", "cu","U",
	"ca","intersection", "sb","C", "sp","superset", "ib","improper subset", "ip","improper superset",
	"if","oo", "pd","pd", "gr","gr", "no","-", "is","S", "pt","oc", "es","0", "mo","E", "br","|", "|","|",
	"dd","++", "rh","=>", "lh","<=", "or","|", "ci","O",
	"lt","/", "lb","\\", "rt","\\", "rb","/", "lk","{", "rk","}", "bv","|", "lf","[", "rf","]", "lc","[", "rc","]",

	// Greek
	// LATER (usually not used in man pages because can't display those glyphs on tty)

	// other
	"en","-", "em","--", "bu","o", "sq","[]", "ru","_", "14","\u00bc", "12","\u00bd", "34","\u00be",  //"14","1/4", "12","1/2", "34","3/4",
	"fi","fi", "fl","fl", "ff","ff", "Fi","ffi", "Fl","ffl",
	"de","o", "dg","+", "fm","'", "ct","\u00a2", "rg","\u00ae"/*"(R)"*/, "co","\u00a9"/*"(C)"*/
	};
	strings_ = new HashMap<String,String>(str.length);
	for (int i=0,imax=str.length; i<imax; i+=2) strings_.put(str[i],str[i+1]);


	// built-in macros
	String[] mac = {
	// Tcl/Tk built-in?  could read from its man.macros or rely on installed pages.  slightly slows down non-Tcl/Tk pages
	"BS","", "BE","",	// box is lame
	"VS","", "VE","",	// version change bars => sidebar span?
	"DS",".RS\n.nf\n.sp\n", "DE",".fi\n.RE\n.sp\n",
	"SO",".SH \"STANDARD OPTIONS\"\n.LP\n.nf\n.ta 4c 8c 12c\n.ft B\n",
	"SE",".fi\n.ft R\n.LP\nSee the \\fBoptions\\fR manual entry for details on the standard options.\n",
	//"OP",".LP\n.nf\n.ta 4c\nCommand-Line Name:	\\fB\\$1\\fR\nDatabase Name:	\\fB\\$2\\fR\nDatabase Class:	\\fB\\$3\\fR\n.fi\n.IP\n",
	"CS",".RS\n.nf\n.ta .25i .5i .75i 1i\n", "CE",".fi\n.RE\n",
	//"UL","\\$1\l'|0\(ul'\\$2\n"

	// BSD
	//"Dt", "", "Dd", "", "Os", "",

	// Solaris
	"IX", "",
	};
	macros_ = new HashMap<String,String>(mac.length);
	for (int i=0,imax=mac.length; i<imax; i+=2) macros_.put(mac[i],mac[i+1]);
  }

  private static String[] SPACES = new String[80];	// initial spaces in successive indentations
  static {
	StringBuffer sb=new StringBuffer(80); for (int i=0; i<80; i++) sb.append(' ');
	String sp80=sb.toString(); for (int i=0; i<80; i++) SPACES[i] = sp80.substring(0,i);	// share char[]
  }


	// instance vars
  private PushbackInputStream is_;


  /** Parse *roff, translate to document tree. */
  public Object parse(INode root) throws Exception {
	Document doc = root.getDocument();
	doc.getStyleSheet().getContext(null, null).zoom = getZoom();
	PushbackInputStream is = is_ = new PushbackInputStream(getInputUni().getInputStream(), 4*1024);

	//String title = doc.getAttr(Document.ATTR_TITLE);
	URI uri = getURI();
	String title = uri.toString();
	int inx = title.lastIndexOf('/');
	if (inx!=-1) doc.putAttr(Document.ATTR_TITLE, title.substring(inx+1)+" man page");


	int ch;
	// maybe need to gobble up initial blank lines
	do { ch=is.read(); } while (Character.isWhitespace((char)ch));
	is.unread(ch);

	if (ch==-1) {
		return new LeafUnicode("Empty file or symlink, which Java doesn't support yet.",null, root);
	} else if (ch!='.' && ch!='\'') {
		return new LeafUnicode("Looks like a formatted man page; try with man page SOURCE. "+ch,null, root);
	}


	// for now, build new tree from lines
	INode sects = new IVBox("ManualPage",null, doc); // always return this, maybe with no children

	// links to directory, volume, all volumes
	IParaBox hpara = new IParaBox("links",null, sects);
	if (uri!=null) {
		String sfile = uri.getPath();
		if ("file".equals(uri.getScheme()) && Booleans.parseBoolean(getPreference(ManualPageVolume.PREF_DIR, "true"), true)) {
//System.out.println("dir = "+new File(sfile).getParentFile().toURL());
			try { link(hpara, "corresponding directory, ", 2, new File(sfile).getParentFile().toURL());	} catch (IOException e) {}
		}

		inx = sfile.lastIndexOf('.');
		if (inx!=-1 && inx>sfile.lastIndexOf('/')) {
			String vol = sfile.substring(inx+1);
			link(hpara, "volume "+vol+", ", 2, "manpagevol:"+vol);
		}
	}
	link(hpara, "all volumes", 0, "manpagevol:ALL");
//hpara.dump();

	// each page can zap the strings as it sees fit -- just clone a proto instance()
	Map<String,String> strings=(Map<String,String>)strings_.clone(), macros=(Map<String,String>)macros_.clone();
	Map<String,String> unrecognized = new HashMap<String,String>(10); // report each unrecognized macro exactly once

	parse2(sects, strings, macros, unrecognized, ch);
//sects.childAt(0).dump();
	return sects;
  }


  /** Re-entrant portion for .so */
  private void parse2(INode sects, Map<String,String> strings, Map<String,String> macros, Map<String,String> unrecognized, int ch) throws Exception {
	Browser br = getBrowser();
	PushbackInputStream is = is_;

	String str;

	// sect [- subsect] - paras
	IVBox sect = null;

	StringBuffer parasb = new StringBuffer(80*2);
	StringBuffer argsb = new StringBuffer(80);
	String sarg=null;
	String[] args = new String[10+100];	// max 10 args, numbered 0..9, BUT user error produces .SH with any number of words
	StringBuffer pushbacksb = new StringBuffer(1024);
	int argcnt=0;
	List<Span> spans = new ArrayList<Span>(20);
	String pendingURL=null;
	int fstart=-1,sstart=-1,ustart=-1, ssize=-1;	// roff spans of same type don't nest (f=font, s=size, u=url)
	boolean pendingBR = false;
	char ftype='X';
	//int sval=-1;

	//int linecnt=0;
	int closecnt=-1;
	int cmd=-1;
	int ignore=-1;
	//int paracnt = 1;

	// roff flags
	boolean fill=true;
	boolean ifcond=false, ifnot;	// should be arrays since these can be nested?
	boolean fSEEALSO=false;
	//int sccnt=0;


	while (true) {
		// invariant: at start of line
		int pushbacklen = pushbacksb.length();
		if (pushbacklen>0) {
			for (int i=pushbacklen-1; i>=0; i--) is.unread(pushbacksb.charAt(i));
			pushbacksb.setLength(0);
		}

		if (ignore!=-1) {
			// ignore.charAt(ignore.length()-1)==(cmd&0xff) && (ignore.length()==1 || (ignore.charAt(ignore.length()-2)==((cmd>>8)&0xff)))
			//if (ignore==cmd) ignore=-1;
			if (is.read()=='.' && ((ignore&0xff00)==0 || is.read()==((ignore>>8)&0xff)) && (is.read()==(ignore&0xff))) ignore=-1;
//System.out.println("ignore");
//if (ignore==-1) System.out.println("=> done");
			eatLine();
			continue;
		} else if (!fill && parasb.length()>0) ch='\n';
		else if (closecnt<0) ch=is.read();
		else if (closecnt==0) ch='.';


		// blank line => fill text, adding spans and checking for man page refs
		if (closecnt>0 || ch=='\n' || ch=='\r' || ch==-1 || (pendingBR && parasb.length()>0)) {
			if (pendingBR) is.unread(ch);
			else if (ch=='\r' && (ch=is.read())!='\n') is.unread(ch);

			if (parasb.length()>0) {
// split off into method
				// reverse order of spans for more efficient removal (swap endpoints)
				for (int j=0,jlast=spans.size()-1,jmax=spans.size()/2; j<jmax; j++, jlast--) {
					Span tmp=spans.get(j); spans.set(j, spans.get(jlast)); spans.set(jlast, tmp);
				}

				// process paragraph into word-leaves and spans -- LATER: split into own method
				String gi = (pendingBR? "tp": fill? "para": "line");	// (fill?"para":"line")
				IParaBox para = new IParaBox(gi,null, sect);
				//doesn't work if (++paracnt % 10 == 0) { br.repaint(); System.out.println("progressive"); Thread.currentThread().yield(); }	// progressive rendering
				Leaf node=null, prevnode=null;
				parasb.append(' ');	// sentinal
				String paratxt = parasb.substring(0); // big char array, words point into this
				//for (int i=0,imax=paratxt.length(); i<imax; i++,prevnode=node) {
				int wordStart = 0, len=parasb.length();
				if (!fill) {	// retain initial spaces
					while (wordStart<len && Character.isWhitespace(paratxt.charAt(wordStart))) wordStart++;
					if (wordStart>0 && wordStart<len) new LeafUnicode(SPACES[wordStart-1/*pick up one from interword spacing*/],null, para);
				}
				for (int i=paratxt.indexOf(' ',wordStart); i!=-1; wordStart=i+1,prevnode=node, i=paratxt.indexOf(' ',wordStart)) {	// guaranteed no tabs
					if (i==wordStart) continue;	// runs of spaces
					ch = paratxt.charAt(wordStart);
//if (i-wordStart==1 && ch<0x100) System.out.println("single char string "+(++sccnt));
					String word = (i-wordStart==1? Strings.valueOf(paratxt.charAt(wordStart)): paratxt.substring(wordStart,i));
					node = new LeafUnicode(word,null, para);
					// now that all macro transformations taken care of, postpass looking for man page references
//					for (int lp=word.indexOf('('),rp=-1; lp!=-1; lp=rp+1) { -- maybe have ref within parentheses -- later, after below is working
					int lp=word.indexOf('('),rp;
					// parens => check further
					// handle both "ls(1)" and "ls (1)"
					//boolean dicey = (lp==0);	// could match if consider previous word
					String manname=null; int li=-1;
					if (lp>0) { manname=word; li=lp-1; }
					else if (lp==0 && prevnode!=null && (manname=prevnode.getName())!=null) li=manname.length()-1;
					// in determining man page links, if right parenthesis is immediately followed by non-whitespace/non-punctuation, cancel it
					if (manname!=null /*lp!=-1 /*&& implied by lp>0*/ && (rp=word.indexOf(')',lp))!=-1 && rp>lp+1 && (rp+1==word.length() || !Character.isLetter(word.charAt(rp+1)))) {	// && rp-lp<10 -- probably not good
						// validate: only alphanum within parentheses, '(' preceeded alnum, ')' followed by letter (but not one that implies a plural)
						boolean alnumsect = true;
						for (int j=lp+1,jmax=rp; j<jmax; j++) if (!Character.isLetterOrDigit(word.charAt(j))) { alnumsect=false; break; }
						if (alnumsect && (Character.isLetterOrDigit((char)(ch=manname.charAt(li))) || MANVALID.indexOf(ch)!=-1) && (Character.isLetterOrDigit((char)(ch=word.charAt(lp+1))) && ch!='s' && ch!='e' && ch!='i')) {
							// track back to beginning of ref
							//int li=lp-1; -- set above according to this or prev word
							while (li>=0 && (Character.isLetterOrDigit((char)(ch=manname.charAt(li))) || MANVALID.indexOf(ch)!=-1)) li--;
							li++;

							// add span
							//HyperlinkSpan hspan = (HyperlinkSpan)Behavior.getInstance("HyperlinkSpan",null, getLayer());
							//hspan.putAttr("script", SCRIPT);
							//hspan.setTarget(new URL("manualpage:"+word.substring(li,rp+1)));
							// if (looks like good span) OR (dicey reference AND page by that name), add search link
							// //else (dicey reference AND no page by that name) so do nothing
//System.out.println(manname+".substring("+li+","+(lp>0?lp:manname.length())+"), lp="+lp);
//System.out.println("\t"+manname.substring(li,(lp>0?lp:manname.length())));
							// check to see if valid name.	more permissive in asking, but still not that many (10 in Tk's large canvas.n)
							boolean exists = br!=null && (br.callSemanticEvent(ManualPageVolume.MSG_EXISTS, manname.substring(li,(lp>0?lp:manname.length()))) != Boolean.FALSE);
							boolean validsect = br==null || (br.callSemanticEvent(ManualPageVolume.MSG_SECTION_VALID, String.valueOf(word.charAt(lp+1))) != Boolean.FALSE);
							if (exists || fSEEALSO || (lp>0 && lp>=2 && validsect)) { // additional screening before overstrike because may be a function call: section is valid || in See Also section
								Span hspan = (Span)Behavior.getInstance("manref", (exists? "ScriptSpan": "OverstrikeSpan"), SPANATTRS, getLayer());
								hspan.moveq((lp>0?null:prevnode),(lp>0?li+wordStart:li), null,rp+wordStart+1);
								spans.add(hspan);
							}
//System.out.println("possible man page ref "+word.substring(li,rp+1));
						}
					}

					// first build up text+b/i/tt spans, then second pass to generate word hunks
					// any span transitions at this point?	make this more efficient
					// traverse from top to bottom (cancelling when start>current position?)
					for (int j=spans.size()-1; j>=0; j--) {
						Span span = spans.get(j);
						Mark s = span.getStart();
						if (s.leaf==null && s.offset<=i) { s.leaf=node; s.offset=Math.max(s.offset-wordStart,0); }
						Mark e = span.getEnd();
						if (e.leaf==null/*<=must be true?*/ && e.offset<=i) {
							Leaf hack=s.leaf; s.leaf=null;	// probably don't need hack anymore
							if (e.offset>=wordStart || prevnode==null) span.moveq(hack,s.offset, node,Math.max(e.offset-wordStart,0));
							else span.moveq(hack,s.offset, prevnode,prevnode.size());
//System.out.println("setting span "+s+".."+e);
							spans.remove(j);

							// could check bold, italic words for man page refs => too many false positives, not many valid positives
							//if (span.getName()=="BoldSpan" || span.getName()=="ItalicSpan") System.out.print(" "+hack.getName());
						}
					}
				}

				if (para.size()==0) {
					if (DEBUG) System.out.println("parasb.length()>0 but no leaves produced: |"+parasb.toString()+"|");
					para.remove();
				}
				parasb.setLength(0); //spans.setSize(0); <= let spans spill across arbitrary amount, as when .ft change font
				//pendingBR = false; -- moved out of 'if' else infinite loop on man1/eqn.man
			}
			pendingBR = false;
			if (closecnt>0) closecnt--;
			if (ch==-1) break;	// EOF: end parsing (done here so can process final paragraph)

		} else if (ch=='.') {	// command
			// collect command
			boolean newcmd=true;
			if (closecnt==0) { closecnt--; newcmd=false; }

			if (newcmd) {
				cmd=0;	//cmd=is.read(); -- Tcl's fconfigure.n has some lines with just '.' -- which apparently should just be ignored
				//is.mark(80);
				ch = is.read();
				if (!Character.isWhitespace((char)ch)) {
					cmd = ch;	// it's a command
					ch=is.read();
					if (!Character.isWhitespace((char)ch)) { cmd = (cmd<<8) + ch; ch=is.read(); }	// two-letter command
				}
				//while (!Character.isWhitespace((char)(ch=is.read()))) cmd = ((cmd<<8) + ch); // all macros within 4 bytes (in fact, 2 bytes, in think) -- I hope
//System.out.println("cmd = "+(char)(cmd>>8)+(char)(cmd&0x7f));
				while (ch==' ' || ch=='\t'/*(?)*/) ch=is.read();
				is.unread(ch);	// first char of arg, or \n
			}

			// commands to ignore, without args, args that shouldn't be parsed here, or ignored commands where we'd waste time parsing
			switch (cmd) {
			// if, if-else, else too? need first arg, but then just pass through the rest

			// handle commands we ignore right away, so don't waste time building arg list
			case (('\\'<<8)+'"'):	// comment
				// fall through to "eatLine(); continue;"

			// ignore all these, at least in online display

			// not applicable
			case (('n'<<8)+'h'):	// turn off hyphenation
			case (('h'<<8)+'y'):	// turn on hyphenation
			case (('h'<<8)+'c'):	// explicit hyphenation character

			case (('p'<<8)+'l'):	// page length
			case (('p'<<8)+'o'):	// page offset
			case (('b'<<8)+'p'):	// begin page
			case (('p'<<8)+'n'):	// set page number
			case (('p'<<8)+'c'):	// page number character

			case (('l'<<8)+'l'):	// line length (width)
			case (('l'<<8)+'s'):	// line spacing
			case (('n'<<8)+'e'):	// need XX lines

			case (('r'<<8)+'d'):	// read from standard input
			case (('t'<<8)+'m'):	// print to terminal
			case (('s'<<8)+'y'):	// execute system command
			case (('p'<<8)+'i'):	// pipe output
			case (('p'<<8)+'m'):	// print macros to stderr

			// not supported
			case (('n'<<8)+'m'):	// line numbering
			case (('n'<<8)+'n'):	// no line numbering
			case (('n'<<8)+'s'):	// no space mode
			case (('n'<<8)+'x'):	// next file
			case (('o'<<8)+'s'):	// output saved vertical space
			case (('m'<<8)+'k'):	// mark vertical place
			case (('r'<<8)+'t'):	// return to marked vertical place
			case (('s'<<8)+'s'):	// space character size
			case (('s'<<8)+'v'):	// save a block of space
			case (('t'<<8)+'r'):	// character translation
			case (('w'<<8)+'h'):	// set or remove a trap
			case (('P'<<8)+'D'):	// distance between paragraphs
			case (('l'<<8)+'g'):	// ligature mode

			// bogus
			case 0:	// line just has period (as found in Tcl's fconfigure)

			// not supported but should be
			case (('T'<<8)+'H'):	// begin reference page
				// .TH name section date
			case (('H'<<8)+'P'):	// hanging indent
			case (('n'<<8)+'r'):	// number register
			case (('r'<<8)+'r'):	// remove number register
			case (('p'<<8)+'s'):	// set point size (like \s)
			case (('t'<<8)+'a'):	// tab stops
			case (('t'<<8)+'c'):	// tab repetition character
			case (('D'<<8)+'T'):	// restore default tabs
			case (('v'<<8)+'s'):	// vertical space
			case (('a'<<8)+'d'):	// adjust: l=left, r=right, c=center, b/n=both, (absent)=unchanged
			case (('t'<<8)+'i'):	// temporary indent

				eatLine();
				continue;


			// ALSO handle commands that want to parse own arguments
			case (('d'<<8)+'s'):	// define string
			case (('a'<<8)+'s'):	// append to string
			case (('r'<<8)+'s'):	// remove string
				str = getarg12();	// string name
				if (cmd==(('d'<<8)+'s') || cmd==(('a'<<8)+'s')) {	// string body
					argsb.setLength(0);
					if (cmd==(('a'<<8)+'s') && (sarg=(String)strings.get(str))!=null) argsb.append(sarg);
					ch=is.read(); if (ch!='"') argsb.append((char)ch);
					//for (boolean fesc=false; (ch=is.read())!='\n' && ch!='\r'; ) {
					while ((ch=is.read())!='\n' && ch!='\r') {
						/*if (ch=='\\') fesc=!fesc;	// can have escapes within
						if (!fesc)*/
						if (ch=='\\' && argsb.length()>0 && argsb.charAt(argsb.length()-1)=='\\') {/*SKIP*/}	// WRONG
						else argsb.append((char)ch);
					}
					if (ch=='\r' && (ch=is.read())!='\n') is.unread(ch);
					strings.put(str, argsb.substring(0));
				} else strings.remove(str);

				while (ch!='\n' && ch!='\r') ch=is.read();	if (ch=='\r' && (ch=is.read())!='\n') is.unread(ch);
//System.out.println("defined string "+str+" = |"+argsb.toString()+"|");
				continue;


			case (('d'<<8)+'e'):	// define macro
			case (('a'<<8)+'m'):	// append to macro
				str = getarg12();	// string name
				eatLine();
				int sblen;
				argsb.setLength(0);
				while ((ch=is.read())!=-1) {	// gobble text until ".." line
					if ((ch=='\n' || ch=='\r') && (sblen=argsb.length())>=2 && argsb.charAt(sblen-1)=='.' && argsb.charAt(sblen-2)=='.' && (sblen==2 || argsb.charAt(sblen-3)=='\n' || argsb.charAt(sblen-3)=='\r')) {
						is.unread(ch);	// leave for eatLine()
						argsb.setLength(sblen-2);	// chop off terminating ".."
						argsb.append('\n');
//System.out.println("DEFINE "+str+"\n"+argsb.toString());
						break;
					}
					argsb.append((char)ch);
				}
				eatLine();
//if (cmd==(('d'<<8)+'e') && macros.get(str)!=null) System.out.println("redefining "+str);
				if (cmd==(('a'<<8)+'m') && (sarg=(String)macros.get(str))!=null) { argsb.insert(0,"\n"); argsb.insert(0, sarg); }
				macros.put(str, argsb.substring(0));
				continue;
			case (('r'<<8)+'n'):	// rename macro
				str = getarg12();	// string name
				macros.put(getarg12(), macros.get(str));
				macros.remove(str);
				continue;
			case (('r'<<8)+'m'):	// remove macro
				str = getarg12();	// string name
				macros.remove(str);
				continue;

			default: //System.out.println("ignored "+(char)(ch>>8)+" "+(char)(ch&0xff));	// fall through to arg collection and dispatch
			}

			if (newcmd) {
			// collect args (space between, double quote groups)
			argcnt=0; argsb.setLength(0); //int argsmax=args.length;
			boolean dq=false;
			while (true) {
				ch=is.read();
				/*if (argcnt==argsmax) {
					// something's probably wrong--language can only refer to 10 args => often .SH with more than 10 words
					eatLine();
					break;
				} else */if (ch=='\\' /*&& argsb.length()==0*/) {
					if ((ch=is.read())=='"') break;	// rest of line is comment
					else { is.unread(ch); argsb.append('\\'); }
				} else if ((ch=='"' && dq)) {	 // end of quoted arg
					args[argcnt++]=argsb.substring(0);	// do it even if 0-length
//System.out.println("quoted arg = |"+args[argcnt-1]+"|");
					argsb.setLength(0);
					while ((ch=is.read())==' ' || ch=='\t') {/*eat*/}	is.unread(ch);
				} else if (ch=='"' /*&& !dq*/) {
					dq=true;	// arg until next quote
				} else if (ch=='\n' || ch=='\r') {
					break;
				} else if (!dq && (ch==' ' || ch=='\t')) {
					// not quoted, end of arg
					args[argcnt++]=argsb.substring(0);
//System.out.println("arg = |"+args[argcnt-1]+"|");
					argsb.setLength(0);
					while ((ch=is.read())==' ' || ch=='\t') {/*eat*/}	is.unread(ch);
				} else {
					// add to current arg
					argsb.append((char)ch);
				}
			}

			if (argsb.length()>0) args[argcnt++]=argsb.substring(0);	// package last arg
			// gobble rest of line (as after comment)
			while (ch!='\n' && ch!='\r') ch=is.read();
			if (ch=='\r' && (ch=is.read())!='\n') is.unread(ch);
//System.out.println("end arg = |"+(argcnt>0? args[argcnt-1]: "[no args]")+"|");
			}



			// commands
			switch (cmd) {

			// STRUCTURAL
			case (('S'<<8)+'H'):
			//case (('S'<<8)+'h'):	// BSD
			case (('S'<<8)+'S'):	// for now don't nest subsections, which are rare anyhow
			//case (('S'<<8)+'s'):	// BSD
				if (parasb.length()>0) { closecnt=1; continue; }
				fill=true;	// people forget to turn off .nf
				sect = new IVBox("section",null, sects);	 // HBox?
				if (argcnt>0) { // Linux sync.8 has ".SH\nDESCRIPTION"
					IParaBox para = new IParaBox("secthead",null, sect);
					//*for (int i=0; i<argcnt; i++)*/ new LeafUnicode(casify(args[0]),null, para);	// should just process first, but people leave quotes off multiple words
					for (int i=0; i<argcnt; i++) new LeafUnicode(Strings.casify(args[i], " ./_", lcexceptions_),null, para); //System.out.println(casify(args[i]));
				}
				fSEEALSO = (sect.size()==2 && "See".equals(sect.childAt(0).getName()) && "Also".equals(sect.childAt(1).getName()));
				break;
			case (('P'<<8)+'P'):	// start new paragraph
			case (('L'<<8)+'P'):
			case ('P'):
			//case (('P'<<8)+'p'):	// BSD
				if (parasb.length()>0) { closecnt=1; continue; }
				break;
			case (('T'<<8)+'P'):
				if (parasb.length()>0) { closecnt=1; continue; }
				// hanging indent according to argument
				// newline after next line
				pendingBR = true;
				break;
			case (('I'<<8)+'P'):
				if (parasb.length()>0) { closecnt=1; continue; }
				// hanging indent according to argument
				// newline after next line
				//if (argcnt>0 && para!=null) new LeafUnicode(args[0]);
				//parasb.append(args[0]).append(' ');	// doesn't handle escapes
				if (argcnt>=1) pushbacksb.append(args[0]).append(' ');  	 // want more space
				break;
			case (('b'<<8)+'r'):	// break
			case (('s'<<8)+'p'):	// vertical space -- ok fake
				if (parasb.length()>0) { closecnt=1; continue; }
				break;

			case (('R'<<8)+'S'):	// relative indent... with insets span, INDENT/null structure (probably is structurally significant)?
				if (parasb.length()>0) { closecnt=1; continue; }
				sect = new IVBox("indent",null, sect);	// RSes nest within each other
				break;
			case (('R'<<8)+'E'):
				if (parasb.length()>0) { closecnt=1; continue; }
				if (sect!=null) sect = (IVBox)sect.getParentNode();
				break;

			case (('T'<<8)+'S'):	// table (don't have a tbl around, but tables not so hard)
				eatLine();	// specs
				fill=false; // for now
				break;
			case (('T'<<8)+'E'):
				fill=true;
				break;

			case (('n'<<8)+'f'):
				if (parasb.length()>0) { closecnt=1; continue; }
				fill=false;
				break;
			case (('f'<<8)+'i'):
				fill=true;	// need to preserve whitespace during this?  troff doesn't and we're modelling that (nroff uses to format tables)
				break;




			// FONTS
			// convert font changes to escape versions -- convert to macro?  less code but probably slower
			case (('f'<<8)+'t'):
				if (argcnt>=1 && args[0]!=null) pushbacksb.append("\\f").append(args[0].charAt(0)).append(" \n");
//				if (argcnt>=1 && args[0]!=null) pushbacksb.append("\\f").append(args[0]).append(" \n");
// for now, just handle font changes within paragraph, not
// .ft B
// <some text>
// .ft R
// also .ft #, as in .ft 2 (I think)
				break;
			case ('B'):
				addAllArgs(argcnt, args, pushbacksb, "\\fB", null);
				break;
			case ('I'):
				addAllArgs(argcnt, args, pushbacksb, "\\fI", null);
				break;

			// all these RB/RI/BR/IB/BI are lame -- macro for these?
			case (('R'<<8)+'B'):
				addAllArgs(argcnt, args, pushbacksb, "\\fR", "\\fB");
				break;
			case (('R'<<8)+'I'):
				addAllArgs(argcnt, args, pushbacksb, "\\fR", "\\fI");
				break;
			case (('B'<<8)+'R'):
				addAllArgs(argcnt, args, pushbacksb, "\\fB", "\\fR");
				break;
			case (('I'<<8)+'R'):
				addAllArgs(argcnt, args, pushbacksb, "\\fI", "\\fR");
				break;
			case (('I'<<8)+'B'):
				addAllArgs(argcnt, args, pushbacksb, "\\fI", "\\fB");
				break;
			case (('B'<<8)+'I'):
				addAllArgs(argcnt, args, pushbacksb, "\\fB", "\\fI");
				break;

			case (('S'<<8)+'M'):	// small caps
				addAllArgs(argcnt, args, pushbacksb, "\\fS", null);
				break;
			case (('S'<<8)+'B'):	// small and bold
				addAllArgs(argcnt, args, pushbacksb, "\\fS\\fB", null);
				break;



			// CONTROL FLOW
			case (('s'<<8)+'o'):
				// later -- have to support because used as softlink mechanism
//System.out.println(".so to "+args[0]+", uri="+uri+"	  "+getAttr("uri"));
//System.out.println("**********************.so");
				PushbackInputStream oldin = is;
				try {
//System.out.println("try reading from "+new URL(uri, "../"+args[0]));
					// check args[0] to make sure not including yourself, which happens in Linux pages
					// ...
					//ir_ = new PushbackReader(new BufferedReader(new InputStreamReader(new URL(uri, "../"+args[0]).openStream())), 1024);
					is = new PushbackInputStream(new BufferedInputStream(getURI().resolve("../"+args[0]).toURL().openStream(),2048), 1024);
					parse2(sects, strings, macros, unrecognized, is.read()); // should be current parent
				} catch (IOException ex) {
					//System.err.println("****** "+ex);	 // often can't find -man macros
				}
				is = oldin;
				break;

			case (('i'<<8)+'f'):	// if
			case (('i'<<8)+'e'):	// if-else
				// evaluate condition
				ifnot = args[0].startsWith("!");
				String cond = (ifnot? args[0].substring(1): args[0]);
				if ("n".equals(cond)) ifcond=false;	// nroff
				else if ("t".equals(cond)) ifcond=true; // we're masquerading as troff... maybe should be nroff
				else if ("o".equals(cond)) ifcond=true; // odd page -- and we're always page one
				else if ("e".equals(cond)) ifcond=false;	// even page
				//else if (<number>)
				//else if (cond.beginsWith("'")) string comparison

				if (ifnot) ifcond=!ifcond;
//System.out.println("if "+args[0]+"=>"+ifcond+" then "+args[1]+"...");
				if (ifcond) {
					// put rest of args back on input to evaluate
					for (int i=0+1,imax=argcnt; i<imax; i++) {
						if (args[i].indexOf(' ')!=-1) pushbacksb.append('"').append(args[i]).append('"').append(' '); // ok to lose quotes on args?
						else pushbacksb.append(args[i]);
//System.out.print("   "+args[i]);
					}
//System.out.println();
					pushbacksb.append('\n');
				}
				break;
			case (('e'<<8)+'l'):	// else
				if (!ifcond) {
					for (int i=0,imax=argcnt; i<imax; i++) {
						if (args[i].indexOf(' ')!=-1) pushbacksb.append('"').append(args[i]).append('"').append(' '); // ok to lose quotes on args?
						else pushbacksb.append(args[i]);
					}
					pushbacksb.append('\n');
				}
				break;

			case (('i'<<8)+'g'):	// ignore
				// gobble until specified end or ".."
				if (argcnt==0 || args[0]==null || args[0].length()==0) ignore='.';
				else { ignore=0; for (int i=0,imax=args[0].length(); i<imax; i++) ignore=((ignore<<8)+args[0].charAt(i)); }
//System.out.println("ignoring until |"+(argcnt==0?"..":args[0])+"|");
				break;



			// SYSTEM-SPECIFIC -- these can be bad because they override new definitions, so define in default macro table if possible
			// Solaris
			//case (('I'<<8)+'X'):	// indexing -- redefine as '\ in default macro table
			//	break;

			// groff
			// URLs-in-man pages: only used twice in standard Linux pages, and one of those a demo!
			case (('U'<<8)+'R'):	// <a href='arg'>, except if arg==":" in which case do nothing
				if (argcnt>=1) {
					pendingURL = args[0];
					ustart = parasb.length();
				}
				break;
			case (('U'<<8)+'E'):	// </a>
				if (!":".equals(pendingURL)) {
					HyperlinkSpan hspan = (HyperlinkSpan)Behavior.getInstance("hyperlink","HyperlinkSpan",null, null, getLayer());
					hspan.setTarget(pendingURL);
					// check isSeen() -- pretty much has to be full URL
					int pointnow = parasb.length()-1;	// -1 to strip previous space
					if (ustart < pointnow) hspan.moveq(null,ustart, null,pointnow);	// can screw up if URL spans paragraphs
					spans.add(hspan);
				}
				pendingURL = null;
				break;
			case (('U'<<8)+'N'):	// <a name='arg' id='arg'></a>
				break;

			// Tcl/Tk... make check and incorporate flag into dispatch? (what about non-specific macros that still want to pick up?)
			// => low level roff good enough to just read Tcl/Tk macro file, or perhaps seed the macro table, so can be overridden by Tcl/Tk?
			case (('O'<<8)+'P'):
				if (parasb.length()>0) { closecnt=1; continue; }
				if (argcnt>=3) {
				pushbacksb.append(".nf\nCommand-Line Name:	\\fB").append(args[0]);
				pushbacksb.append("\\fR\nDatabase Name:   \\fB").append(args[1]);
				pushbacksb.append("\\fR\nDatabase Class:   \\fB").append(args[2]);
				pushbacksb.append("\\fR\n.fi\n");
				}
				break;

			// BSD
			case (('S'<<8)+'h'):
			case (('S'<<8)+'s'):
			case (('D'<<8)+'d'):
			case (('D'<<8)+'t'):
			case (('O'<<8)+'s'):
				new LeafUnicode("BSD macros not supported",null, sects);
				return;	// end parsing now!


			// newly defined macro? (should actually check this first in case override built-in)
			default:
				String key=""+(char)((cmd>>8)&0xff)+(char)(cmd&0xff);
				String def=(String)macros.get(key);
				if (def!=null) {
//System.out.println("macro "+key);//+" => "+def);
					// need to do arg substitution
					pushbacksb.append(def);
				} else {
					// ignore unrecognized ones (perhaps report this)
					if (unrecognized.get(key)==null) {
						//java.awt.Toolkit.getDefaultToolkit().beep();
						if (DEBUG) {
							System.err.print("*** macro not recognized: |"+key+"|");
							for (int i=0,imax=key.length(); i<imax; i++) System.out.print("  "+(int)key.charAt(i));
							System.out.println();
						}
						unrecognized.put(key, key);
					}
					// write out as if not a macro -- not entirely accurate if had quoted words, but it's your mistake in the first place
					pushbacksb.append("\\.");
					if (key.charAt(0)!=0) pushbacksb.append(key.charAt(0));
					pushbacksb.append(key.charAt(1));
					for (int i=0,imax=argcnt; i<imax; i++) pushbacksb.append(' ').append(args[i]);
				}

			}


		} else if (ch=='\'') {
			// whole line is comment -- so why is verbose and awkward .\" more frequently used by far?
			eatLine();

		} else {	// text
			while (ch!='\n' && ch!='\r' && ch!=-1) {
				// collect text
//System.out.println("prev cmd = "+cmd+" vs .TP="+(('T'<<8)+'P'));

				// escapes -- I think all text is supposed to be filtered through escapes
				if (ch=='\\') {
					switch (ch=is.read()) {
					// first build up text+b/i/tt spans, then second pass to generate word hunks
					case 'f':	// font changes: \fB, \fI, \fR, \f0, ...
						// close previous span, if any (in roff, spans of same type don't nest)
						int posn=parasb.length();
//if (fstart==posn) System.err.println("fstart==posn=="+posn);
//ch=is.read(); System.err.println("font escape on "+(char)ch); is.unread(ch);
						if (fstart!=-1	&& fstart!=posn) {
							String spanname = ftype=='B'?"BoldSpan": ftype=='I'?"ItalicSpan": ftype=='S'?"RelPointSpan": ftype=='C'?"MonospacedSpan": null;	// good-looking idiom
							if (spanname!=null) {
								Span span = (Span)Behavior.getInstance(Strings.valueOf((char)(ftype-'A'+'a')),spanname,null, getLayer());
								if (ftype=='S') ((RelPointSpan)span).setDelta(-1);
//System.out.println("new "+spanname+/*" on "+parasb.toString().substring(fstart,posn+1)+*/", "+fstart+".."+posn);
								span.moveq(null,fstart, null,posn);
								spans.add(span);
							}
							fstart=-1;
						}

						switch (is.read()) {
						case 'R': case 'P': case '0': case '1': break;	// plain is the default
						case 'B': case '3': fstart=posn; ftype='B'; break;
						case 'I': case '2': fstart=posn; ftype='I'; break;
						case 'C': ftype='C'; break;
						case 'S': fstart=posn; ftype='S'; break;	// fabricated small caps
						default: ftype='X';
//System.out.println("unknown f escape ");
						}
//System.out.println("ftype = "+ftype);
						break;

					case 's':	// font size changes: \s<int>, such as \s0, \s-1, \s2
						if (sstart!=-1) {	// close previous
							Span span = (Span)Behavior.getInstance("relpt","RelPointSpan",null, getLayer());
							((RelPointSpan)span).setDelta(ssize);
							span.moveq(null,sstart, null,parasb.length());
							spans.add(span);
							sstart=-1;
						}
						ch=is.read();
						if (ch=='-') ssize=-(is.read()-'0'); else ssize=ch-'0';
						if (ssize!=0) sstart = parasb.length();
						break;

					case '(':	// predefined character named xx
						str = (String)strings.get(getarg12());
						if (str!=null) {
							//is.unread(' '); // sometimes yes, sometimes no
							for (int i=str.length()-1; i>=0; i--) is.unread(str.charAt(i));
						}
						break;
					case '*':	// value of string with one- or two-character name
						if ((ch=is.read())!='(') is.unread(ch);
						str = (String)strings.get(getarg12());
//System.out.println("ref string |"+str+"|");
						if (str!=null) {
							//is.unread(' ');
							for (int i=str.length()-1; i>=0; i--) is.unread(str.charAt(i));
						}
						break;
					case 'n':	// value of number register
						break;

					case '"':	// comment until end of line -- starting from wherever
//System.out.print("'\\\"");
						eatLine();
						is.unread('\n');	// mark end of line for enclosing loop
						break;

					case '-':
					case '\\':
					case '\'': case '`':
					case '.': case ' ':
						parasb.append((char)ch);
						break;
					case 'e':
						parasb.append('\\');
						break;
					case '0': case '|': case '^':
						parasb.append(' ');
						break;
					case '&':	// 0-width space => nothing!  just a parsing hack
						parasb.append((char)160);	// nbsb -- gotta have something for possible spans (as in zip.1: ".IR \& .c")
						break;

					// eat arg
					case 'h':	// horizontal movement
					case 'l':	// horizontal line
					case 'L':	// vertical line
					case 'v':	// move down
					case 'x':	// extra line-space
					case 'D':	// draw line/circle/cllipse/arc/B-spline
					case 'H':	// set character height independent of width
					case 'S':	// slant
					case 'w':	// width of string
						is.read(); // first '\''
						while ((ch=is.read())!=-1 && ch!='\'') {}
						break;

					case 'k':	// horizontal register
					case 'r':	// 1 em upward vertical motion
					case 'u':	// 1/2 em upward vertical motion

					case '%':	// hyphenation point
					case '\n':	// newline -- line continuation
					case '\r':
					case '{':	// begin conditional input
					case '}':	// end conditional input
						break;

					// unknown ones seen in practice
					case 'c':
						// I think this means to eat following linebreak immediately following
						ch=is.read();
						break;

					default:
						//System.out.println("unknown escape "+(char)ch);
					}

				} else if (ch=='\t') parasb.append(' ');
				else parasb.append((char)ch); // plain character

				ch=is.read();
			}

			// end of line
			//parasb.append(' ');
			if (parasb.length()>0 /*&& !Character.isWhitespace(parasb.charAt(parasb.length()-1))*/) {
				parasb.append(' ');
				//if (pendingBR) { parasb.append("\t \t "); pendingBR=false; }
			}
			if (ch=='\r' && (ch=is.read())!='\n') is.unread(ch);
		}
	}

  }


  private void eatLine() throws IOException {
	int ch;
	PushbackInputStream is = is_;
	while ((ch=is.read())!='\n' && ch!='\r' && ch!=-1) {/*eat*/}
	if (ch=='\r' && (ch=is.read())!='\n') is.unread(ch);	// Doze
  }

  /** Get 1- or 2-character argument.  (Commands never read as strings, always as int's so can directly switch(). */
  private String getarg12() throws IOException {
	String str;
	PushbackInputStream is = is_;
	int ch1=is.read(), ch2=is.read();
//	if (Character.isWhitespace((char)ch2)) str=""+(char)ch1;
	if (/*!Character.isLetterOrDigit((char)ch1) ||--allow "^B" */ !Character.isLetterOrDigit((char)ch2)) str = Strings.valueOf((char)ch1);	// guaranteed to be within range as reading bytes
	else { str=""+(char)ch1+(char)ch2; ch2=is.read(); }
	while (ch2==' ' || ch2=='\t') ch2=is.read();
	is.unread(ch2);
	return str;
  }

  /** Add the passed text, making it a hyperlink. */
  private void link(INode p, String txt, int eoff, Object target) {
	Leaf l = new LeafUnicode(txt,null, p);
	HyperlinkSpan hspan = (HyperlinkSpan)Behavior.getInstance("uri","HyperlinkSpan",null, null, getLayer());
	hspan.setTarget(target);
	hspan.moveq(l,0, l,l.size()-eoff);
  }

  /** Add all words in passed array, separated by spaces, to passed StringBuffer. */
  private void addAllArgs(int argcnt, String[] args, StringBuffer sb, String even, String odd) {
//int sbin = sb.length();
	if (argcnt>=1 && args[0]!=null) {
		boolean toggle = (even!=odd && odd!=null);
		sb.append(even);
		for (int i=0,imax=argcnt; i<imax; i+=2) {
			if (args[i]!=null) {
				if (i>0) { sb.append(' '); if (toggle) sb.append(even); }
				//if (toggle) sb.append(even); else if (i>0) sb.append(' ');
				sb.append(args[i]);
			}
			if (i+1<imax && args[i+1]!=null) {
				sb.append(' ');
				if (toggle) sb.append(odd);
				sb.append(args[i+1]);
			}
		}
		sb.append("\\fR \n");
	}
//System.out.println(sb.substring(sbin));
  }



  /** Since not all man page references are recognizable as such, have docpopup choice to treat current word as man page ref. */
  public boolean semanticEventBefore(SemanticEvent se, String msg) {
	if (super.semanticEventBefore(se,msg)) return true;
	else if (DocumentPopup.MSG_CREATE_DOCPOPUP==msg && se.getIn()!=getBrowser().getSelectionSpan()) {	//null)) { -- just plain-doc popup
//System.out.println("**** f/b createMenu "+super.toString()+", in="+se.getIn());
		// maybe move into ManualPageVolume
		Browser br = getBrowser();
		Node curn = br.getCurNode();
		if (curn instanceof LeafText) {
			String name = Strings.trimPunct(curn.getName());
			INode menu = (INode)se.getOut();
			createUI("button", "Search for \""+name+"\" as man page", "event "+Document.MSG_OPEN+" manpage:"+name, menu, "SPECIFIC", false);
		}
	}
	return false;
  }
}
