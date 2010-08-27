package multivalent.std.adaptor;

import java.io.*;
import java.util.Map;
import java.util.HashMap;

import multivalent.ESISNode;
import multivalent.MediaAdaptor;



/**
	Abstract superclass for XML and HTML.

	@version $Revision: 1.7 $ $Date: 2003/06/02 05:39:52 $
*/
public abstract class ML extends MediaAdaptor {
  //static final boolean DEBUG = false;

  /** Minimal set of entity references. */
  private static Map<String,String> entityML_ = new HashMap<String,String>(10);
  static {
	String[] en = {
		"amp","&", "lt","<", "gt",">", "quot","\"", "apos","'", "nbsp",String.valueOf((char)160),
	};
	for (int i=0,imax=en.length; i<imax; i+=2) entityML_.put(en[i], en[i+1]);
  }


  protected boolean keepComments = false;   // retain comments -- should probably always be true, as need full information in order to write document back out
  protected boolean keepWhitespace=false;
  protected int errcnt;
  protected Map<String,String> entity_;


  /** Subclasses should set entity references table. */
  public ML() {
	// these should really come from DTD, or at least the relevant media adaptor, where they can be static
	entity_ = entityML_;	// default.  HTML overrides, for instance.
  }

  protected PushbackReader ir_ = null;

  protected Reader getReader() throws IOException {
	if (ir_==null) ir_ = new PushbackReader(getInputUni().getReader(), 10);
	return ir_;
  }



  /*
   * DEBUGGING
   */
/*
  static void validate(boolean ok, String errmsg) {
	// should recover gracefully, but for now go boom
	assert(ok, errmsg);
  }
  static void error(String errmsg) {
	System.out.println(errmsg);
	System.exit(1);
  }
*/


  /* *************************
   * FUNCTIONALITY
   **************************/

  protected String getEntity(String key) {
	return (String)entity_.get(key);
  }
  protected void setEntity(String key, String val) {
	// don't force keys to upper/lowercase!
	entity_.put(key, val);
  }


  protected char readChar() throws IOException {
	int ch = ir_.read();
//if (ch>0x7f) System.out.println("unusual character |"+((char)ch)+"|, val="+ch);
	if (ch==-1) throw new IOException();	// rather than return -1 because file should be self-delimited
//System.out.print((char)ch);
	return (char)ch;
  }


  // merge readString with getContent
  protected String readString() throws IOException {
	return readString('\0', '\0', "<", '&');
  }
  protected String readString(char open, char close) throws IOException {
	return readString(open, close, "");
  }
  protected String readString(char open, char close, String altend) throws IOException {
	return readString(open, close, altend, '&');
  }


  // delimiters given by open and close; if they're optional, altend lists terminating delimiters
  protected String readString(char open, char close, String altend, char escchar) throws IOException {
	char ch = readChar();

	boolean match = (ch==open);
//if (match) System.out.println("match "+ch+" .. "+close);
	boolean mandatory = altend.equals("");
	String ends = altend;
	if (mandatory || match) {
		//validate(match, "missing opening character "+open);
		ends = String.valueOf(close);
	} else ir_.unread(ch);

	String content = readStringInternal(ends, escchar);

	try { ch=readChar(); if (ch!=close) ir_.unread(ch); } catch (IOException ioe) {}
	//validate(!match || (ch=readChar())==close, "missing closing character "+close+" after "+content+" (match="+match+" on "+open+")");

	//System.out.println("readString => "+content);
	return content;
  }

  protected String readStringInternal(String stop, char escchar) throws IOException {
	StringBuffer sb = new StringBuffer(200);

	boolean lastws = false;
	char ch='X';
	String eoe = (escchar=='&'? "; \n\t&=<>": " ,|&\"\n\t");
//	String eoe = (escchar=='&'? "; \n\t&<>": " ,|&\"\n\t");

	// if quoted, read until other quote
	while (true) {
		try { ch = readChar(); } catch (IOException e) { if (sb.length()>0) return sb.substring(0); else throw e; }
		if (stop.indexOf(ch)!=-1) break;
//if (sb.length()==100) System.out.println("long attribute "+sb.charAt(0)+"  "+sb.substring(0));

		if (ch==escchar && ch!='%') {	// FIX!
			String ref, trans;
			StringBuffer refsb = new StringBuffer(20);  // longest valid HTML entity is 8, and quickly disposed of
			while (stop.indexOf(ch=readChar())==-1 && eoe.indexOf(ch)==-1) refsb.append(ch);
			//if (escchar=='%') in.unread(ch);	  // FIX
			ref = refsb.toString()/*share*/;	// X .toLowerCase(); -- NO!  Entities are case sensitive: Aacute vs aacute
			if (escchar=='&') {
				if (ch!=';') { /*sb.append('&'); trans=ref;*/ ir_.unread(ch); }	// user mistakenly wrote '&' instead of '&amp;' -- I guess this is an error case in straight SGML
				if (ref.length()==0) trans="&";    // user mistakenly wrote '&' instead of '&amp;' -- I guess this is an error case in straight SGML
				else if (ref.charAt(0)=='#')
					if (ref.charAt(1)=='x' || ref.charAt(1)=='X') try { trans = String.valueOf((char)Integer.parseInt(ref.substring(2),16)); } catch (NumberFormatException e) { trans=ref; }
					else try { trans=String.valueOf((char)Integer.parseInt(ref.substring(1))); } catch (NumberFormatException e) { trans=ref; }
				else trans=getEntity(ref);
//System.out.println(ref+" => "+trans+"/"+((int)trans.charAt(0)));
//if (trans==null) System.out.println("No definition for ENTITY "+ref); => happens all the time in URL href attributes that are queries
//System.out.println("*** char "+ref+" => |"+trans+"|");  //+", "+(int)trans.charAt(0));
			} else trans="XXX"; //else trans=getParameter(ref);
			//validate(trans!=null, "entity "+ref+" not defined");
			if (trans==null) { sb.append('&').append(ref); /*if (ch==escchar) sb.append(';');*/ } //)trans="&"+ref+";";   //"?"; -- standard practice is to show invalid entity references
			else sb.append(trans);
			lastws = false;
//	  } else if (ch=='\r') { // ignore MS-DOS
		} else if (!keepWhitespace && Character.isWhitespace(ch) /*<=NEED TO KEEP RETURN for PRE*/) {
			// normalize whitespace
			if (ch=='\n') { sb.append(' '); lastws=true; /*linenum++;*/ }
			if (!lastws) sb.append(' ');
			lastws = true;
		} else {
			sb.append(ch);
			lastws = false;
		}
	}
	ir_.unread(ch);

	return phelps.lang.Strings.valueOf(sb);
  }


  protected boolean ispace=false;
  protected void eatSpace() throws IOException {
	char ch;
	ispace=false;
	// need isSpace not isSpaceChar as isSpaceChar doesn't pick up ^J?
	while (Character.isWhitespace(ch=readChar())) { ispace=true; /*if (ch=='\n') linenum++*/; }   // stupid MSIE doesn't classify C-m as whitespace
	ir_.unread(ch);
  }

  // can centralize this now that PushbackInputStream has more than one character of pushback
  // need to save comment in doc tree so can write back out
  /**
	Reads past SGML/HTML/XML comments: <tt>&lt;!-- .. --&gt;</tt>.
  */
  protected void eatComment() throws IOException {
	// centralized comment handling: <!--..-->
	int ch,ch1,ch2,ch3;

//	eatSpace();

	// when collect in StringBuffer, can simlpy gobble until see '>', at which point check previous two characters
	// => mark/reset! but not supported by PushbackInputStream
	StringBuffer sb = new StringBuffer(80 * 5);     // 5 80-char lines
	if ((ch=readChar())=='<') {
	  if ((ch1=readChar())=='!') {
		if ((ch2=readChar())=='-') {
		  if ((ch3=readChar())=='-') {

				// got comment, find end
//System.out.print("eat: ");
				while (true) {
					if ((ch=readChar())=='-') {
						if ((ch1=readChar())=='-') {
							if ((ch2=readChar())=='>') {
//								eatSpace(); // for invariant
//								eatComment();	// comment following comment
								return;
							}
							ir_.unread(ch2);
						}
						ir_.unread(ch1);
					}
					sb.append(ch);
//System.out.print((char)ch);
				}
		  }
		  ir_.unread(ch3);
		}
		ir_.unread(ch2);
	  }
	  ir_.unread(ch1);
	}
	ir_.unread(ch);

//	eatSpace();
	// return sb.substring(0);  // when retain in tree
  }


  // invariant: ch = first *unprocessed* char in stream, such as '/'
  //protected ESISNode tag = new ESISNode("");	// token to pass back to clients (used to create new one each time)

  protected ESISNode getTag(/*ESISNode tag*/) throws IOException { return getTag(/*tag,*/ readChar()); }

  /**
	ESISNode returned is reused, so clients should extract all they want before calling again.
	Attributes without values (e.g., lone <tt>BORDER</tt> in HTML, as opposed to <tt>BORDER=5</tt>)
	are given a value that is the same is the name (e.g., long <tt>BORDER</tt> is that same as <tt>BORDER=BORDER</tt>);
	when saved, the attribute returned to the short form.
	In other words, they're expanded on read, shortened on write, and getAttr()!=null can test for existence.
  */
  protected ESISNode getTag(/*ESISNode tag,*/ char ch) throws IOException {
	char ch1, ch2;

	// NAME
	StringBuffer sb = new StringBuffer(20);

	// peek to see if open or close tag
	//eatSpace();
	//ch=readChar();
	//if (ch=='/') { tag.open=false; eatSpace(); } else { tag.open=true; sb.append(ch); }
	char ptype = Character.toLowerCase(ch);  // ! ? letter
	if (Character.isLetter(ch)) sb.append(ch); else eatSpace();

	// problem case: <href="http://www.mlesk.com/mlesk/">
	//while (Character.isLetterOrDigit(ch=readChar()) || ch=='_') sb.append(ch);	// is this right?
	while (!Character.isWhitespace(ch=readChar()) && ch!='>') sb.append(ch);	// is this right?
	ir_.unread(ch);
//	ESISNode tag = new ESISNode(sb.toString());	// not forced to all uppercase here
	// worthwhile to check intern String table?  Java's intern() way slow
	ESISNode tag = new ESISNode(sb.substring(0));
	tag.ptype = ptype;
	//tag.setGI(sb.substring(0));
//System.out.println("tag = "+tag.getGI());

	// ATTRIBUTES
	eatSpace();
	tag.attrs = null; // maybe attrs.clear() and rely on clients to copy attrs as desired... but almost always want to retain if exist?
	tag.empty=false;

	// collect attributes
	// attribute syntax:  (<name>[<space>][=[<space>]["|']value["|'][<space>])]+>

	while ((ch=readChar()) != '>') {
		if (ch=='/' || ch=='?') {
			tag.empty=true;
			eatSpace();
			continue;	// must be last element in tag... but be robust and continue to munch until see '>'
		}

		ir_.unread(ch);
		Object val = null;

		// get attribute name
		String name = readString('\0', '\0', ">= \t\n" /*">="*/, '%'/*?*/);
		eatSpace();
		if ("\"".equals(name)) continue;	// clean up old bug from old hubs

		// get attribute value, if any (tag with null value meaningful in boolean existence test)
		ch = readChar();
		if (ch=='=') {
			eatSpace();
			char delim = '\0';
			ch=readChar(); if (ch=='"' || ch=='\'') delim=ch; ir_.unread(ch);
			val = (Object)readString(delim, delim, "> \t\r\n", '&'/*'%'/*? -- not '&'*/);
//System.out.println(name+" = "+(String)val);
		} else ir_.unread(ch);

		//validate(val!=null, "null attribute value for "+name);
/*if (val!=null) {
	String dval = URIs.decode((String)val);
	if (!val.equals(dval)) {
		System.out.println("*** "+name+" = "+(String)val+" => "+dval);
		System.out.println("\tsecond decode = "+URIs.decode(dval));
	}
}*/
		if (val==null) val = name;   // was: DEFINED;
//		  else val=decode((String)val);
//System.out.println("attr "+name+"="+val);
		tag.putAttr(name, val);

		eatSpace();
	}

	return tag;
  }

  // may want to store open/close status in boolean separate from tag,
  // hide that choice now with this method

  public static boolean pairsWith(ESISNode t1, ESISNode t2) {
	//return pairsWith(t1.getGI(), t2.getGI());
	return t1.getGI().equals(t2.getGI()) && ((t1.ptype=='/') ^ (t2.ptype=='/'));  // exactly one is close
  }
  /*
  public static boolean pairsWithX(String gi1, String gi2) {
	//if (gi1.charAt(0)=='/') return gi2.equals(gi1.substring(1));	// expensive, probably
	//else if (gi2.charAt(0)=='/') return gi1.equals(gi2.substring(1));
	//else return false;
	return gi1.equals(gi2);
  }*/


  // more methods to grab content and build tree (approach parser for well behaived SGML: no syntax errors, tags fully instantiated, no syntax mods in DTD)
  // later, read DTD so can handle missing end tags and can validate

/* if want to time, use profiler or an observer
  // actually call here and not SGML
  protected int linenum=-1;
  public ESISNode parse(InputStream urlin) throws Exception {
	setInputStream(urlin);
//	  validate(in!=null, "must set InputStream before use");

	linenum = 1;
	errcnt=0;
	long msstart = System.currentTimeMills();
	try { eatSpace(); } catch (IOException e) {}
//	ESISNode root = parse();
	long msend = System.currentTimeMills();
	if (debug) System.out.println("parsed SGML in "+(msend-msstart)+"ms");
	try { in.close(); } catch (Exception e) {}
	if (errcnt>0) { error(""+errcnt+" errors"); }

	return root;
  }
*/

  public void close() throws IOException {
	if (ir_!=null) { ir_.close(); ir_=null; }
	super.close();
  }
}
