package phelps.lang;

import java.lang.ref.SoftReference;
import java.util.StringTokenizer;
import java.util.HashMap;
import java.util.Map;
import java.util.Comparator;



/**
	Extensions to {@link java.lang.String}.

	<ul>
	<li>possible object sharing: {@link #valueOf(String)}, {@link #valueOf(StringBuffer)}, {@link #valueOf(char)}
	<li>sorting in dictionary order: {@link #DICTIONARY_ORDER}, {@link #DICTIONARY_CASE_INSENSITIVE_ORDER},
		{@link #compareDictionary(String, String, boolean)}
	<li>{@link #trim(String, String)}, {@link #trimWhitespace(String)}, {@link #trimPunct(String)},
	<li>translate/convert/format: {@link #casify(String, String, Map)}, {@link #toASCII7(String)}, {@link #fromPigLatin(String)},
		raw {@link #getBytes8(String)} without character set encoding
		<!-- @link #javaString2raw(String)}, @link #raw2javaString(String)}, -->
	<li>algorithms: {@link #minEditDistance(String, String)}
	</ul>

	@version $Revision: 1.4 $ $Date: 2003/06/01 07:58:08 $
*/
public class Strings {
  public static String[] STRING0 = new String[0];

  public static String PUNCT = ".?!,:;()[]'\"";

  // list of stop words doesn't have to be complete as -- seems there should be a standard list somewhere
  /**
	A list of common English words, useful in several applications.
	=> put this in text file.  App-specific.
	@see multivalent.std.adaptor.ManualPage
  public static final String[] STOPWORDS = {
	"a", "an", "the",
	"who", "I", "we", "us", "he", "him", "his", "she", "her", "them", "they", "it", "this", "that", "these", "those",
	"be", "being", "am", "was", "were", "is", "are",
	"did", "do", "done", "doing", "make", "made", "making", "take", "took",
	"what", "why", "well", "since", "still", "together", "toward", "while",
	"of", "from", "to", "for", "in", "on", "into", "at", "by", "about",
	"if", "when", "then", "and", "or", "either", "neither", "but", "with", "without", "that", "which", "because",
	"where", "here", "there", "everywhere", "somewhere", "above", "below", "around",
	"one", "two", "three", "four", "five", "six", "seven", "eight", "nine", "ten", "hundred", "thousand", "million",
	"some", "also", "many", "few", "most", "each", "every", "another", "again",
	"yes", "no", "maybe", "true", "false",
  };
  */

  /**
	A list of common computer-related words.
	=> put this in text file.  App-specific.
  public static final String[] COMPUTERWORDS = {
	"computer", "user", "Java", "Sun", "Solaris", "Apple", "Macintosh", "Microsoft", "Windows",
	"kilo", "mega", "giga",
	"WWW", "HTML", "HTTP", "homepage",
  };
  */

  /**
	Canonical array of single-character strings.
	Since we're putting each word into a separate Node, we can save a little by not allocating many copies of "a", "i", "1", ....
	Would like to do this with common words too, but how to be efficient as new String() 3x faster than hash lookup.
	Would like to put this in INode.setName()/Leaf.setName(), but by the time that's called, already created new String, so saves memory (after gc!) but not time.
  */
  private static String[] SHARED = new String[0x100];
  static { for (int i=0,imax=SHARED.length; i<imax; i++) SHARED[i] = String.valueOf((char)i).intern(); }


  private static final int OFF_ACCENT = 160, OFF_GREEK = 913;
  private static final String[] UNICODE2ASCII = new String[376+1 - OFF_ACCENT],
	LIGATURE2ASCII = { "ff", "fi", "fl", "ffi", "ffl" }, // fb00..fb04
	QUOTE2ASCII = { "'", "'", "'", "'", "\"", "\"", "\"", }/*, // 2018..201e
	GREEK2ASCII = new String[977+1 - OFF_GREEK]/*, ascii2greek_ = new String['Z'+1]*/;
  static {
	String[] str = {
	// 7-bit ASCII
	"160, ,!,c,L,o,Y,|,s,XXX,(C),a,<<,-,XXX,(R),-,o,+-,2,3,XXX,u,P,.,XXX,1,o,>>,1/4,1/2,3/4,?" +
		",A,A,A,A,A,A,A,C,E,E,E,E,I,I,I,I,D,N,O,O,O,O,O,x,O,U,U,U,U,Y,th,ss" +
		",a,a,a,a,a,a,a,c,e,e,e,e,i,i,i,i,d,n,o,o,o,o,o,/,o,u,u,u,u,y,th,y",
	"338,OE,oe",
	"352,S,s",
	"376,Y",

	// Greek
	/*"913,A,B,G,D,E,Z,E,Th,I,K,L,M,N,Ks,O,P,R,S,T,U,F,Ch,Ps,O",
	"945,a,b,g,d,e,z,e,th,i,k,l,m,n,ks,o,p,r,s,s,t,u,f,ch,ps,o",
	//"977", "thetasym","", "upsih","", "982", "piv","",*/
	};

	for (int i=0,imax=str.length; i<imax; i++) {
		StringTokenizer st = new StringTokenizer(str[i], ",");
		int x = Integer.parseInt(st.nextToken());
		while (st.hasMoreTokens()) {
			String sub = valueOf(st.nextToken());
			if ("XXX".equals(sub)) {}	// skip -- can't have empty token
			else if (x<=376) UNICODE2ASCII[x - OFF_ACCENT] = sub;
			//else { GREEK2ASCII[x - OFF_GREEK] = sub; /*ascii2greek_[sub.charAt(0)] = valueOf(x);*/ }
			x++;
		}
	}
  }

  /** Comparator, as for use by {@link java.util.Arrays}, that sorts in dictionary order. */
  public static final Comparator<String> DICTIONARY_ORDER = new Comparator<String>() {
	public int compare(String s1, String s2) { return compareDictionary(s1, s2, false); }
  };

  /** Comparator, as for use by {@link java.util.Arrays}, that sorts in dictionary order, with case insensitively. */
  public static final Comparator<String> DICTIONARY_CASE_INSENSITIVE_ORDER = new Comparator<String>() {
	public int compare(String s1, String s2) { return compareDictionary(s1, s2, true); }
  };


  private static SoftReference<Map<String,String>> sharedref_ = null;	//new SoftReference<Map<String,String>>(null); -- gjc 2.2 doesn't like this



  private Strings() {}

  /**
	Returns byte array of low byte of each character.
	Like {@link java.lang.String#getBytes()} but no encoding,
	and {@link java.lang.String#getBytes(int, int, byte[], int)} but not deprecated.
  */
  public static byte[] getBytes8(String s) {
	if (s==null) s="";
	int len = s.length();
	byte[] b = new byte[len];
	for (int i=0; i<len; i++) b[i] = (byte)s.charAt(i);
	return b;
  }

  /**
	Compares one String to another in "dictionary order", which means
	alphabetics compared lexicographically and embedded numbers numerically.
  */
  public static int compareDictionary(String s1, String s2, boolean caseinsensitive) {
	if (s1==s2) return 0; else if (s1==null) return -1; else if (s2==null) return 1;
	int len1=s1.length(), len2=s2.length();

	for (int i1=0, i2=0; i1<len1 && i2<len2; ) {
		char ch1=s1.charAt(i1), ch2=s2.charAt(i2);
		if (caseinsensitive) { ch1=Character.toLowerCase(ch1); ch2=Character.toLowerCase(ch2); }
		if ('0'<=ch1&&ch1<='9' && '0'<=ch2&&ch2<='9') {	// compare as individual digits -- numbers can be of any length
			// scan to end of digits
			int e1=i1+1; while (e1<len1 && '0'<=(ch1=s1.charAt(e1))&&ch1<='9') e1++;
			int e2=i2+1; while (e2<len2 && '0'<=(ch2=s2.charAt(e2))&&ch2<='9') e2++;

			// skip over initial 0s
			while (s1.charAt(i1)=='0' && i1+1<e1) i1++;
			while (s2.charAt(i2)=='0' && i2+1<e2) i2++;

			int l1=e1-i1, l2=e2-i2; if (l1 != l2) return l1 - l2;	// different number of digits => longer larger

			for ( ; i1<e1 /*&& i2<e2 -- known equals*/; i1++, i2++) {
				assert i2 < e2;
				ch1=s1.charAt(i1); ch2=s2.charAt(i2);
				if (ch1 != ch2) return ch1 - ch2;
			}
			assert i1==e1 && i2==e2;

		} else if (ch1 != ch2) return ch1-ch2;
		else /*ch1==ch2*/ { i1++; i2++; }
	}

	return len1 - len2;	// equal as far as they both go, so shorter one first
  }


  /*public static String parseString(String val, String defaultval) { => behaviors use getAttr
	return (val!=null? val: defaultval);
  }*/

  /**
	Canonicalizes {@link java.lang.String} instances of a single character &lt;= u00ff and those created recently.
  */
  public static String valueOf(String str) {
	String s;
	if (str==null) s=null;
	else if (str.length()==0) s="";
	else if (str.length()==1 && str.charAt(0)<SHARED.length) s = SHARED[str.charAt(0)];
	else {
		Map<String,String> smap = sharedref_!=null? sharedref_.get(): null;
		if (smap==null) { smap=new HashMap<String,String>(2000); sharedref_ = new SoftReference<Map<String,String>>(smap); }
		String sstr = smap.get(str);
		if (sstr!=null) s=sstr; else { s=str; smap.put(s,s); }
	}
	return s;
  }

  /**
	Return possibly shared String.
	If String is 1-character long and char<256, then guaranteed shared.
  */
  public static String valueOf(StringBuffer sb) {
	String s;
	if (sb==null) s=null;
	else if (sb.length()==0) s="";
	else if (sb.length()==1 && sb.charAt(0)<SHARED.length) s = SHARED[sb.charAt(0)];
	else s = valueOf(sb.substring(0));	// vs .toString() -- space or speed?
	return s;
  }

  public static String valueOf(char ch) { return ch < SHARED.length? SHARED[ch]: String.valueOf(ch); }
  //public static String valueOf(int ch) { return ch < SHARED.length? SHARED[ch]: String.valueOf((char)ch); }
  //public static String valueOf(String val, Map canon) {}

  public static String valueOf(byte[] b) { return StringBuffers.valueOf(b).toString(); }

  public static String join(String[] strs, String join) {
	if (strs.length==0) return "";
	int len=0; for (String s: strs) len = len + s.length() + join.length();
	StringBuffer sb = new StringBuffer(len - join.length());
	sb.append(strs[0]);
	for (int i=0+1; i<strs.length; i++) sb.append(join).append(strs[i]);
	return sb.toString();
  }


  /** Trim letters in passed chars from ends of word. */
  public static String trim(String txt, String chars) { return trim(txt, chars, 0, txt.length()-1); }
  public static String trim(String txt, String chars, int start, int end) {
	//if (txt==null) return null; -- go boom
	while (start<end && chars.indexOf(txt.charAt(start))!=-1) start++;
	while (end>=start && chars.indexOf(txt.charAt(end))!=-1) end--;
	return (start<=end? txt.substring(start,end+1): "");
  }

  public static String trimWhitespace(String txt) { return trimWhitespace(txt, 0, txt.length()-1); }
  /** Can save a String create over String.trim(). */
  public static String trimWhitespace(String txt, int start, int end) {
	assert txt!=null;
	while (start<end && Character.isWhitespace(txt.charAt(start))) start++;
	while (end>=start && Character.isWhitespace(txt.charAt(end))) end--;
//System.out.println("|"+txt.substring(start,end+1)+"|");
	return (start<=end? txt.substring(start,end+1): "");
  }

  //public static final String PUNCTUATION = ".?!,:;()@#$%^&*-_=+\\|]}[{;/'\"";

  /** Trim off punctuation (actually, non-letter or -digit) from ends of txt. */
  public static String trimPunct(String txt) {
	int s=0, e=txt.length();
	while (s<e && !Character.isLetterOrDigit(txt.charAt(s))) s++;
	while (e>s && !Character.isLetterOrDigit(txt.charAt(e-1))) e--;
	return (s<e? txt.substring(s,e): "");
  }

  /** Returns string which has all whitespace characters from <var>txt</var>. */
  public static String removeWhitespace(String txt) {
	if (txt==null) return null;
   	// find first whitespace
	int wsi = -1;
	for (int i=0,imax=txt.length(); i<imax; i++) if (Character.isWhitespace(txt.charAt(i))) { wsi=i; break; }
	if (wsi==-1) return txt;	// if no whitespace, same as incoming

	StringBuffer sb = new StringBuffer(txt.length());
	if (wsi>0) sb.append(txt.substring(0,wsi));
	for (int i=wsi+1,imax=txt.length(); i<imax; i++) {
		char ch = txt.charAt(i);
		if (!Character.isWhitespace(ch)) sb.append(ch);
	}
	return sb.toString();
  }

  public static String escape(String str, String esc, char with) {
	if (str==null) return str;
	int len = str.length();
	StringBuffer sb = new StringBuffer(len + 5);
	for (int i=0; i<len; i++) {
		char ch=str.charAt(i);
		if (esc.indexOf(ch)!=-1) sb.append(with);
		sb.append(ch);
	}
	return sb.toString();
  }

  /** Like that for String, but if start with StringBuffer saves making one String.
  public static String trim(StringBuffer sb, String chars, int start, int end) {
	if (txt==null) return null;
	int s=0, e=txt.length();
	while (s<e && chars.indexOf(txt.charAt(s))!=-1) s++;
	while (e>s && chars.indexOf(txt.charAt(e-1))!=-1) e--;
	return (s<e? txt.substring(s,e): "");
  }

  public static String trimWhitespace(StringBuffer sb, int start, int end) {
	while (start<end && Character.isWhitespace(sb.charAt(start))) start++;	// sentinal ':'
	while (end>=start && Character.isWhitespace(sb.charAt(end))) end--;
	return sb.substring(start,end+1);
  }*/


  /** Returns String that is <var>n</var> concatenated copies of <var>s</var>. 
  public static String copies(String s, int n) {
	if (n<=0) return ""; else if (n==1) return s;


	StringBuffer sb = new StringBuffer(s.length() * n);
	for (int i=0; i<n; i++) sb.append(s);
	return sb.toString();
  }*/


// DATA TRANSLATIONS
/*  public static final String javaString2rawXXX(String cstring) {
  static final String octalDigits = "01234567";
  static final String hexDigits = "0123456789abcdefABCDEF";
  static final String escChars = "\n\t\b\r\f\\\'\"";
  static final String unescChars = "ntbrf\\'\"";

  /**
	Translate from Java programming language source literal String to internal version, converting Java escape characters to Unicode.
  * /
	StringBuffer sb = new StringBuffer(cstring.length());
	int val;
	int unesc;

	for (int i=0; i<cstring.length(); i++) {
	  char ch = cstring.charAt(i);

	  if (ch=='\\') {
		i++;
		ch = cstring.charAt(i);

		if (ch>='0' && ch<='7') {
		  val=0;
		  for (int j=i; j-i<3 && octalDigits.indexOf(ch=cstring.charAt(j))!=-1; j++) {
			val = val*8 + (((int)ch)-'0');
		  }
		  ch = (char)val;
		  i+=3-1;
		} else if (ch=='u') {
		  i++;
		  val=0;
		  for (int j=i; j-i<4; j++) {
			ch=cstring.charAt(j);
			if (hexDigits.indexOf(ch)==-1) error("invalid Unicode digit "+ch+" (must have exactly four hex digits");
			val *= 16;
			if (Character.isDigit(ch)) val += (((int)ch)-'0');
			else if (Character.isLowerCase(ch)) val += (((int)ch)-'a');
			else val += (((int)ch)-'A');
		  }
		  i+=4-1;
		  ch = (char)val;
		} else if ((unesc=unescChars.indexOf(ch))!=-1) {
		  ch = escChars.charAt(unesc);
		} else error("invalid escape character \\"+ch);
	  }

	  sb.append(ch);	// usually have some translated character to append now
	}

	return sb.substring(0);
  }


  public static final String raw2javaStringXXX(String raw) {
	StringBuffer sb = new StringBuffer(raw.length()*2);
	int unesc;

	for (int i=0; i<raw.length(); i++) {
	  char ch = raw.charAt(i);
	  int ich = (int)ch;

	  if ((unesc=escChars.indexOf(ch))!=-1) {
		sb.append('\\'); sb.append(unescChars.charAt(unesc));
	  } else if (ch<' ' || ich>=0x7f /*|| ich>0xff* /) {	// not printable or Unicode
		sb.append("\\u");
// update this
		String intString = Integer.toHexString(ich);
		sb.append("0000".substring(intString.length()));
		//sb.append(sprintf("%04s", Integer.toHexString(ich)));
		sb.append(intString);
	  } else {
		sb.append(ch);
	  }
	}

	return sb.substring(0);
  }
*/


  /**
	Transform strings of ALL UPPERCASE into Mixed-Case version,
	with each character after a space kept uppercase,
	and given table of <var>exceptions</var>.
	If a word has any lowercase initially, no case is changed in that word.

	<p>For example, UNIX manual pages typically have all uppercase section titles,
	so this method transforms them into something more easily readable, passing
	as exceptions a list of odd computer-industry capitalization.

<!--
	To do: use canonical String if word one-letter long.
-->
	@see multivalent.std.adaptor.ManualPage
  */
  public static String casify(String words, String wordbreak, Map<String,String> exceptions) {	// like String.toUpperCase()
	StringBuffer sb = new StringBuffer(words.length());
//	int start=0, end=-1;
	String newword; char ch=' ';
	for (int start=0, end=start, len=words.length(); end<len; end++, start=end) {
		// split into words.  (.SH argument supposed to be big long quoted string.)
		//if ((end=words.indexOf(' ',start))==-1) end=len;
		for (end=start; end<len; end++) if (wordbreak.indexOf(ch=words.charAt(end))!=-1) break;
		if (end==start) { sb.append(ch); continue; }	// consecutive split chars
		String word=words.substring(start,end);

//		if (start>0) sb.append(' ');
//System.out.println(word+" => "+lcexceptions.get(word));
		boolean alluc=true; for (int i=0,imax=word.length(); alluc && i<imax; i++) if (Character.isLowerCase(word.charAt(i))) alluc=false;
		if (!alluc) sb.append(word);	// leave alone words with any lowercase
		else if (exceptions!=null && (newword=(String)exceptions.get(word))!=null) sb.append(newword);
		else if (word.length()==1) sb.append(word);
		else sb.append(word.charAt(0)).append(word.substring(1).toLowerCase());

//		if (end!=-1) while (end<len && Character.isWhitespace(words.charAt(end))) end++;
//		if (end==-1 || end==len) break; else start=end;
		if (end<len) sb.append(ch);
	}
	return sb.substring(0);
  }


  /**
	Returns Unicode translation to 7-bit Latin-1 ASCII by 
	<ul>
	<li>keeping 7-bit characters (<code>0 <= <var>char</var> <= 127</code>) as is,
	<li>removing accents (e.g., "&Aacute;" => "A"),
	<li>splitting ligatures (e.g, "fi" single glyph => "f" and "i" as separate characters),
	<li>replacing curly quotes with straight quotes,
	<li>and making other character substitutions (e.g., "&copy;" => "(C)").
	</ul>
  */
  public static String toASCII7(String txt) {	// like toUpperCase()
	for (int i=0,imax=txt.length(); i<imax; i++) {
		char ch = txt.charAt(i);
		if (ch >= 128) {
			String[] u2a = UNICODE2ASCII; int end = OFF_ACCENT + u2a.length;
			StringBuffer sb = new StringBuffer(txt.length());
			for (int j=0,jmax=txt.length(); j<jmax; j++) {
				ch = txt.charAt(j);
				if (ch < 128) sb.append(ch);
				else if (OFF_ACCENT <= ch && ch < end && u2a[ch-OFF_ACCENT]!=null) sb.append(u2a[ch-OFF_ACCENT]);
				else if ('\ufb00' <=ch&&ch <= '\ufb04') sb.append(LIGATURE2ASCII[ch-0xfb00]);
				else if ('\u2018' <=ch&&ch <= '\u201e') sb.append(QUOTE2ASCII[ch-0x2018]);
				else if ('\u2013' <=ch&&ch <= '\u2014') sb.append("--");	// endash and emdash
				//else skip char
			}
			txt = sb.toString();
			break;
		}
	}
	return txt;
  }

  /**
	Converts Greek Unicode letters to Latin. => don't handle accents

	@see <a href='http://www.ibiblio.org/koine/greek/lessons/alphabet.html'>Little Greek 101</a>
  public static String fromGreek(String txt) {
	for (int i=0,imax=txt.length(), end = OFF_GREEK + GREEK2ASCII.length; i<imax; i++) {
		char ch = txt.charAt(i);
		if (OFF_GREEK<=ch && ch < end) {
			String[] g2a = GREEK2ASCII; final int END = OFF_GREEK + g2a.length;
			StringBuffer sb = new StringBuffer(txt.length());
			for (int j=0,jmax=txt.length(); j<jmax; j++) {
				ch = txt.charAt(j);
				if (OFF_GREEK <= ch && ch < END && g2a[ch-OFF_GREEK]!=null) sb.append(g2a[ch-OFF_GREEK]);
				else sb.append(ch);	// different from toASCII7 in that unknown characters are let alone
			}
			txt = sb.toString();
		}
	}
	return txt;
  }
  */


  /** Translate word from Pig Latin. */
  public static String fromPigLatin(String str) {
	String pig = str;

	// retain ending punctuation
	int strlen = str.length(), pend = strlen;
	while (pend>0 && phelps.lang.Strings.PUNCT.indexOf(str.charAt(pend-1))!=-1) pend--;
	int wend = pend;
//System.out.println(wend+"/"+str+"/"+(wend>2? Character.toLowerCase(str.charAt(wend-2)): ' ')+"/"+(wend>2? Character.toLowerCase(str.charAt(wend-1)): ' '));
	if (wend > 2 && Character.toLowerCase(str.charAt(wend-2))=='a' && Character.toLowerCase(str.charAt(wend-1))=='y') {
		wend -= 2;	// strip off -ay suffix
		StringBuffer sb = new StringBuffer(wend);
		// letters moved to end
		int hy = str.lastIndexOf('-');
		if (hy!=-1 && hy<wend) {
			char c0 = str.charAt(hy+1);
			if (hy+1<wend) sb.append(Character.isUpperCase(str.charAt(0))? Character.toUpperCase(c0): c0);
			for (int i=hy+2; i<wend; i++) sb.append(str.charAt(i));
			wend = hy;
		}
		// letters at start
		sb.append(Character.toLowerCase(str.charAt(0)));
		for (int i=1; i<wend; i++) sb.append(str.charAt(i));
		// trailing punctuation
		for (int i=pend; i<strlen; i++) sb.append(str.charAt(i));
		pig = sb.toString();
	}

	return pig;
  }



  /**
	Returns the minimum number of operations to transform one string into the other.
	An operation is insert character, delete character, substitute character.
	Useful to determine if two strings "almost match", as in <code>Strings.minEditDistance("Krzysztof", "Krystof") <= 3</code>.
  */
  public static int minEditDistance(String a, String b) {
	if (a==b) return 0;
	else if (a==null || a.equals("")) return b.length(); else if (b==null || b.equals("")) return a.length();
	else if (a.equals(b)) return 0;

	// dynamic programming
	int alen = a.length()+1, blen = b.length()+1;
	int[][] c = new int[alen][blen];

	for (int i=0; i<alen; i++) c[i][0] = i;
	for (int j=0; j<blen; j++) c[0][j] = j;

	for (int i=1; i<alen; i++) {
		for (int j=1; j<blen; j++) {
			int scost = c[i-1][j-1] + (a.charAt(i-1)==b.charAt(j-1)? 0: 1);
			int dcost = 1 + c[i-1][j];
			int icost = 1 + c[i][j-1];
			//int xcost = ;	// interchange

			c[i][j] = Math.min(scost, Math.min(dcost, icost));
		}
	}

	//if (DEBUG) for (int i=0; i<alen; i++) { for (int j=0; j<blen; j++) System.out.print(c[i][j]+" ");  System.out.println(); }

	return c[alen-1][blen-1];
  }
}
