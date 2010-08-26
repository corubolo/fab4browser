package phelps.lang;

import java.io.BufferedReader;
import java.io.InputStreamReader;



/**
	Extensions to {@link java.lang.Character}.

	<ul>
	<li>Unicode: {@link #getName(int) name}, {@link #strType(int) type as string}, {@link #toUTF8(int) UTF-8 representation}
	<li>{@link #isHexDigit(char)}
	</ul>

	@version $Revision: 1.4 $ $Date: 2003/10/01 13:21:29 $
*/
public class Characters {
  private static String[] TYPE = {
	// order parallel to java.lang.Character numbering
	"Cn", "Other, not assigned",
	"Lu", "Letter, uppercase",
	"Ll", "Letter, lowercase",
	"Lt", "Letter, titlecase",
	"Lm", "Letter, modifier",
	"Lo", "Letter, other",
	"Mn", "Mark, nonspacing",
	"Me", "Mark, enclosing",
	"Mc", "Mark, spacing combining",
	"Nd", "Number, decimal digit",
	"Nl", "Number, letter",
	"No", "Number, other",
	"Zs", "Separator, space",
	"Zl", "Separator, line",
	"Zp", "Separator, paragraph",
	"Cc", "Other, control",
	"Cf", "Other, format",
	"", "",
	"Co", "Other, private use",
	"Cs", "Other, surrogate",
	"Pd", "Punctuation, dash",
	"Ps", "Punctuation, open",
	"Pe", "Punctuation, close",
	"Pc", "Punctuation, connector",
	"Po", "Punctuation, other",
	"Sm", "Symbol, math",
	"Sc", "Symbol, currency",
	"Sk", "Symbol, modifier",
	"So", "Symbol, other",
	"Pi", "Punctuation, initial quote",
	"Pf", "Punctuation, final quote",
  };

  //public static final char UNICODE_SURROGATE_HIGH_MIN = '\ud800', UNICODE_SURROGATE_HIGH_MAX = '\udfff';

  private static String[] CHARNAME = null;
  private static Character[] SHARED = null;


  private Characters() {}


  /** Returns <code>true</code> iff <var>ch</var> is in [0-9a-fA-F]. */
  public static boolean isHexDigit(int ch) {
	return ('0' <=ch&&ch <= '9') || ('a' <= ch&&ch <= 'f') || ('A' <= ch&&ch <= 'F');
  }

  public static Character valueOf(char ch) {
	if (SHARED==null) {
		SHARED = new Character[0x100];
		for (int i=0,imax=SHARED.length; i<imax; i++) SHARED[i] = new Character((char)i); 
	}
	return ch < SHARED.length? SHARED[ch]: new Character(ch);
  }


  /** Returns two-character string type of {@link java.lang.Character#getType()}. */
  public static String strType(int type) {
	return TYPE[type*2];
  }

  /** Returns type of {@link java.lang.Character#getType()} in words. */
  public static String strTypeFull(int type) {
	return TYPE[type*2+1];
  }

  /**
	Returns Unicode name of <var>codepoint</var>.
	For example, <code>A</code> is <code>LATIN CAPITAL LETTER A</a>.
  */
  public static String getName(int codepoint) {
	if (CHARNAME==null) try {
		int max = 0x10000;	// nobody uses trans-BMP yet, no CJK names
		CHARNAME = new String[max];	// 65K * 4 bytes + strings * 2 bytes/char = 256K + 2MB
		BufferedReader r = new BufferedReader(new InputStreamReader(Characters.class.getResourceAsStream("/org/unicode/UnicodeData.txt")));
		String last = "";
		for (String line; (line = r.readLine()) != null; ) {
			String[] rec = line.split(";");
			String name = rec[1]; if (name.equals(last)) name = last;
			int cp = -1; try { cp = Integer.parseInt(rec[0], 16); } catch (NumberFormatException nfe) {}
			if (cp < max) CHARNAME[cp] = name;
//System.out.println("["+cp+"] = "+name);
			last = name;
		}
		r.close();

	} catch (Exception ioe) { ioe.printStackTrace(); }

	return 0 < codepoint&&codepoint < CHARNAME.length? CHARNAME[codepoint]: null;
  }

	/*
  public String toUTF8(int codepoint, StringBuffer sb) {
	StringBuffer sb = new StringBuffer(3);
	toUTF8(codepoint, sb);
	return sb.toString();
  }*/

  public static String toUTF8(int codepoint) {
	StringBuffer sb = new StringBuffer(3);
	if (codepoint <= 0x7f) sb.append((char)codepoint);
	else if (codepoint <= 0x7ff) sb.append((char)(0xc0 | (codepoint>>6))).append((char)(0x80 | (codepoint&0x3f)));
	else if (codepoint <= 0xffff) sb.append((char)(0xe0 | (codepoint>>12))).append((char)(0x80 | ((codepoint>>6)&0x3f))).append((char)(0x80 | (codepoint&0x3f)));
	else sb.append((char)(0xf0 | (codepoint>>18))).append((char)(0x80 | ((codepoint>>12)&0x3f))).append((char)(0x80 | ((codepoint>>6)&0x3f))).append((char)(0x80 | (codepoint&0x3f)));
	return sb.toString();
  }

}
