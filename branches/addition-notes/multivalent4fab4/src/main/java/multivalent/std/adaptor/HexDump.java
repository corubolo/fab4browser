package multivalent.std.adaptor;

import java.io.*;
import java.net.URI;

import multivalent.*;
import multivalent.gui.VMenu;
import multivalent.gui.VRadiobox;
import multivalent.gui.VRadiogroup;



/**
	Like <tt>od</tt> and <tt>strings</tt>, simple hex/ASCII dump catch all for general binary data that the user nevertheless wants to view.

	<p>LATER: different data formats like od

	@version $Revision: 1.3 $ $Date: 2002/10/16 09:12:37 $
*/
public class HexDump extends MediaAdaptor {
  public static final String PREF_DISPLAY="HexDump/DisplayAs";
  public static final String VALUE_HEX="Hex", VALUE_STRINGS="Strings", VALUE_MIXED="Hex/Ascii";

  static final String[] HEX2FF = new String[256];
  static { for (int i=0,imax=HEX2FF.length; i<imax; i++) HEX2FF[i]=Integer.toHexString(i); }


  public Object parse(INode parent) throws Exception {
	return parseHelper(toHTML(), "HTML", getLayer(), parent);
  }

  public String toHTML() {
	URI uri = getURI();

	String type = getPreference(PREF_DISPLAY, VALUE_MIXED);
	if (!VALUE_STRINGS.equals(type) && !VALUE_HEX.equals(type) && !VALUE_MIXED.equals(type)) type = VALUE_MIXED;


	// maybe sort in Java 2 .. by name|size|date ... so have to collect everything first => just use table sort
	StringBuffer sb = new StringBuffer(20000);
	sb.append("<html>\n<head>");
	int headi = sb.length();
	//sb.append("<style>body { font-family: monospace; }</style>");	//font-size: 10pts;
	//sb.append("\t<title>").append("Directory listing of ").append(urifile).append("</title>\n");
	sb.append("</head>\n");
	sb.append("<body>\n");

	try {
		if (VALUE_STRINGS.equals(type)) {
			int sbi = sb.length();
			int cnt = showStrings(sb);
			if (cnt==0) sb.insert(sbi, "No strings found."); else if (cnt>10) sb.insert(sbi, cnt+" strings found.");
		} else {
			sb.insert(headi, "<style>body { font-family: monospace; font-size: 10pts; }</style>");
			showMixed(sb, VALUE_MIXED.equals(type));
		}
	} catch (IOException ioe) { sb.append("Trouble reading ").append(uri).append(": ").append(ioe); }

	sb.append("</body></html>\n");

	return sb.toString();
  }

  int showStrings(StringBuffer sb) throws IOException {
	int len=0, minlen=5, cnt=0;
	int[] buf = new int[minlen];
	InputStream is = getInputUni().getInputStream();
	for (int c, addr=0; (c=is.read())!=-1; ) {
		// looking for string
		//c = c&0x7f; // 7-bit ASCII
		if (c==' ' || (c>='A' && c<='Z') || (c>='a' && c<='z')) buf[len++]=c; else { addr+=len+1; len=0; }

		// found one: write out saved, run out length
		if (len>=minlen) {
			cnt++;
			sb.append("<p><tt>").append(Integer.toHexString(addr)).append("</tt> ");
			for (int i=0; i<len; i++) sb.append((char)buf[i]);
			addr += len + 1;
			len=0;
			for ( ; (c=is.read())!=-1; addr++) {
				//if (c==' ' || (c>='A' && c<='Z') || (c>='a' && c<='z')) sb.append((char)c); else break;
				// more lenient once in string
				if (c>=' ' && c<='~') {
					if (c=='<') sb.append("&lt;"); else if (c=='&') sb.append("&amp;"); else sb.append((char)c);
				} else break;
			}
		}
	}
	return cnt;
  }

  void showMixed(StringBuffer sb, boolean ascii) throws IOException {
	int perrow = (ascii? 0x10: 0x20);
	int[] val = new int[perrow];
	sb.append("<table width='95%'><tr><td>0<td>");
	int lastsbi = sb.length();
	boolean skip=false, bigskip=false;
	InputStream is = getInputUni().getInputStream();
	for (int c=is.read(), addr=0, i=0; true; addr++, i++, c=is.read(), skip=false) {
		if (c==-1 || i==perrow) {
			// finish previous
			if (skip) {
				sb.setLength(lastsbi);
				if (!bigskip) { sb.append("<tr><td>*</tr>\n"); bigskip=true; }
			} else if (addr>0) {
				bigskip=false;
				if (ascii) {
					sb.append("<td>");
//System.out.println("i="+i+", val.length="+val.length);
					for (int j=0; j<i; j++) {
						char ch = (char)val[j];
						if (ch<=26) sb.append("&oslash;"); else if (ch=='<') sb.append("&lt;"); else if (ch=='&') sb.append("&amp;"); else sb.append(ch);
						//sb.append(' ');
					}
				}
				sb.append("</tr>\n");
			}

			if (c==-1) {
				if (i==0) sb.setLength(lastsbi);	// started new line, but turned out empty
				sb.append("</table>\n");
				break;
			}

			// start next
			sb.append("\n<tr><td>").append(addr%(perrow*4)==0? Integer.toHexString(addr): "").append("<td>");
			lastsbi=sb.length();
			i=0;
		}
		val[i]=c;
		skip = skip && (c==0);
		if (c<0x10) sb.append('0');
		sb.append(HEX2FF[c]).append(' ');
	}
  }

  /** Choose among hex only, ASCII only, and mixed displays. */
  public boolean semanticEventAfter(SemanticEvent se, String msg) {
	if (super.semanticEventAfter(se, msg)) return true;

	else if (VMenu.MSG_CREATE_VIEW==msg) {
		String type = getPreference(PREF_DISPLAY, VALUE_MIXED);
		String[] titles = { "Mixed Hex/ASCII", "ASCII strings only", "Hex dump only" };
		String[] vals = { VALUE_MIXED, VALUE_STRINGS, VALUE_HEX };
		VRadiogroup rg = new VRadiogroup();
		Browser br = getBrowser();
		for (int i=0,imax=titles.length; i<imax; i++) {
			VRadiobox radio = (VRadiobox)createUI("radiobox", titles[i], new SemanticEvent(br, PREF_DISPLAY, vals[i]), (INode)se.getOut(), VMenu.CATEGORY_MEDIUM, false);
			radio.setRadiogroup(rg);
			if (type.equals(vals[i])) rg.setActive(radio);
//System.out.println("state = "+type.equals(vals[i])+"/"+radio.getState());
		}

	} else if (PREF_DISPLAY==msg) {
		String type = getPreference(PREF_DISPLAY, VALUE_MIXED);
		Object o = se.getArg();
		if (o instanceof String) {
			String newtype = ((String)o).intern();
			if (newtype!=type && (newtype==VALUE_HEX || newtype==VALUE_STRINGS || newtype==VALUE_MIXED)) {
				putPreference(PREF_DISPLAY, newtype);
				getBrowser().eventq(Document.MSG_RELOAD, null);
			}
		}
	}
	return false;
  }
}
