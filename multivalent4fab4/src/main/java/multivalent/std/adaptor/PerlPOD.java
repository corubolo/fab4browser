package multivalent.std.adaptor;

import java.io.PushbackReader;
import java.io.IOException;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;
import java.net.URI;

import phelps.net.URIs;

import com.pt.io.InputUni;

import multivalent.*;



/**
	Media adaptor for Perl "Plain Old Documentation" (POD).
	Spec in Perl documentation <tt>perlpod.pod</tt>.

	<p>Simple on the fly translation into HTML.
	The disadvanages of translating to HTML are that one largely loses the structure,
	foiling behaviors that would make manipulations based on structure;
	soon one can translate on the fly into XML, largely preserving structure, and using a style sheet
	to obtain the display properties of HTML.

	@version $Revision: 1.5 $ $Date: 2002/10/14 11:46:15 $
*/
public class PerlPOD extends MediaAdaptor {
  private static final boolean DEBUG=false;


  private static final int CMD_NONE=-1,
	CMD_HEAD1=0, CMD_HEAD2=1, CMD_ITEM=2, CMD_OVER=3, CMD_BACK=4, CMD_CUT=5, CMD_POD=6, CMD_FOR=7, CMD_BEGIN=8, CMD_END=9
	;
  private static final String CMD1 = "IBSCLFXZE";	// span commands
  private static Map<String,Integer> podcmd_;
  static {
	String[] scmd = { "head1", "head2", "item", "over", "back", "cut", "pod", "for", "begin", "end" };
	podcmd_ = new HashMap<String,Integer>(scmd.length * 2);
	for (int i=0,imax=scmd.length; i<imax; i++) podcmd_.put(scmd[i], new Integer(i));

	assert podcmd_.get("for").intValue() == CMD_FOR;
  }

  private static Map<String,String> unknown_ = new HashMap<String,String>(13);    // record unknown commands


  public Object parse(INode parent) throws IOException {
	return parseHelper(toHTML(), "HTML", getLayer(), parent);
  }

  /** Returns HTML translation of document. */
  public String toHTML() throws IOException {
	PushbackReader ir = new PushbackReader(getInputUni().getReader(), 4*1024);
	StringBuffer sb=new StringBuffer(10000), csb=new StringBuffer(10), argsb=new StringBuffer(100);

	sb.append("<html>\n<head>");
	//sb.append("\t<title>").append("POD ").append(uri.getFile()).append("</title>\n");
	//sb.append("\t<base href='").append(uri).append("'>\n");
	sb.append("</head>\n");
	sb.append("<body>\n");

	// line records
	boolean sol=true;	// at start of a line?
	int cmd=CMD_NONE, listType=CMD_NONE;
	List<String> anchors = new ArrayList<String>(20);  // so can distinguish between L<name> and L<"sec"> with optional quotes, sheesh!
	Browser br = getBrowser();

	boolean inpara=false;
	for (int c=ir.read(); c!=-1; c=ir.read()) {
		char ch = (char)c;
		if (ch=='=' && sol) {	// command line
			csb.setLength(0); while (!Character.isWhitespace(ch=(char)ir.read())) csb.append(ch);
			ir.unread(ch);

			Integer ocmd = csb.length()>0? podcmd_.get(csb.toString()): null;

			// arg, if any, which gobbles through end of line
			argsb.setLength(0); while ((ch=(char)ir.read())!='\n' && ch!='\r') argsb.append(ch);
			if (ch=='\n' && (c=ir.read())!='\r') ir.unread(c);
			String arg=(argsb.length()==0? null: argsb.substring(0).trim());

			if (ocmd!=null) {
if (DEBUG) System.out.println("command = |"+csb.substring(0)+"|");
				cmd = ocmd.intValue();
				switch (cmd) {
				case CMD_HEAD1:	// can headings have spans?  (e.g., B<text>)
				case CMD_HEAD2:
					sb.append(cmd==CMD_HEAD1? "<h1>": "<h2>");
					sb.append("<a name='").append(URIs.encode(arg)).append("'>").append(arg).append("</a>");
					sb.append(cmd==CMD_HEAD1? "</h1>": "</h2>");
					anchors.add(arg);
//System.out.println("anchor |"+arg+"|");
					break;

				case CMD_ITEM:
					// analyze arg to determine type of list
					if (listType==HTML.TAG_UNKNOWN) {
						if (arg.length()==0 || "*".equals(arg) || "o".equalsIgnoreCase(arg)) { sb.append("<ul>"); listType=HTML.TAG_UL;
						} else if (Character.isDigit(arg.charAt(0))) { sb.append("<ol>"); listType=HTML.TAG_OL;
						} else { listType=HTML.TAG_DL; sb.append("<dl>"); }
//System.out.println("listType="+listType+" on "+arg);
					}
					if (listType==HTML.TAG_DL) {
						// display text and make anchor
						String name=arg;
						int inx=arg.indexOf(' '); if (inx!=-1) name=arg.substring(0,inx);
						sb.append("<dt><a name='").append(name).append("'>").append(arg).append("</a>\n");
						anchors.add(name);
//System.out.println("anchor |"+name+"|");
						inpara=false;
					} else sb.append("<li>");//.append(arg)
					break;

				case CMD_OVER: listType=HTML.TAG_UNKNOWN; break;	// revise if these can be nested
				case CMD_BACK:
					if (listType==HTML.TAG_UL) sb.append("</ul>"); else if (listType==HTML.TAG_OL) sb.append("</ol>"); else if (listType==HTML.TAG_DL) sb.append("</dl>");
					listType=HTML.TAG_UNKNOWN;
					break;

				case CMD_POD:
					break;
				case CMD_CUT:
//System.out.println("*** CUT ***");
					// switch for Perl's *compiler* (I guess): text between =pod and =cut is documentation, so don't parse
					break;

				case CMD_FOR:
					boolean isHTML = ("html".equalsIgnoreCase(arg));
					if (!isHTML) sb.append("\n<!--");
					while (true) {
						while ((ch=(char)ir.read())!='\n' && ch!='\r') sb.append(ch);
						if (ch=='\n' && (c=ir.read())=='\r') ir.unread(c);
						c=ir.read(); ir.unread(c); if ((c=ir.read())=='\n') break;
					}
					if (!isHTML) sb.append("-->\n");
					break;

				case CMD_BEGIN: 	// revise if these can be nested
					if (!("html".equalsIgnoreCase(arg))) sb.append("\n<!--");
					break;
				case CMD_END:
					if (!("html".equalsIgnoreCase(arg))) sb.append("\n-->");
					break;

				default: assert false: cmd;
				}

			} else {    // report unknown command
				String scmd = csb.toString();
				if (unknown_.get(scmd) == null) { unknown_.put(scmd,scmd); System.out.println("unknown command: ="+scmd); }
			}
			//sol=true; -- maintained

		} else if (sol && (ch==' ' || ch=='\t')) {	// indented line => PRE
			sb.append("<pre>\n").append(ch);
			// gobble entire contents here (no nested commands)
			while (true) {
				while ((ch=(char)ir.read())!='\n') if (ch=='<') sb.append("&lt;"); else if (ch=='&') sb.append("&amp;"); else sb.append(ch);	  // munch line
				sb.append("\n"); if ((c=ir.read())!='\r') ir.unread(c);	// tidy up lineend
				c=ir.read(); ir.unread(c); if (c!=' ' && c!='\t') break;	  // check next line
			}
			sb.append("</pre>\n");
			inpara=false;
			//sol=true; -- maintained

		} else if (ch=='\n' || ch=='\r') {
//System.out.println("eol");
			if (ch=='\n' && (c=ir.read())!='\r') ir.unread(c);

			if (sol) { // blank line => paragraph division
				if (inpara) sb.append("</p>\n"); else inpara=false;
			} else sb.append(' ');	// whitespace between words
			sol=true;

		} else {	// process spans and ordinary characters
			sol=false;
			if (!inpara) {
				sb.append(listType==HTML.TAG_DL? "<dd>": "<p>");
				inpara=true;
			}
//char sc=sb.charAt(sb.length()-1);
//if (ch=='<') System.out.println("span with "+sc+"?  "+CMD1.indexOf(sc));

			// span?
			int sblen, scmd;
			if (ch=='<' && (sblen=sb.length())>0 && CMD1.indexOf(scmd = sb.charAt(sblen-1))!=-1) {
				sb.setLength(sblen-1);
				// gobble contents of span.  Nested spans possible?  LATER: handle "<<"..">>"
				csb.setLength(0);
				for (int dcnt=1; dcnt>0; ) {	 // delimit
					if ((ch=(char)ir.read())=='>') dcnt--; else if (ch=='<') dcnt++;
					if (dcnt>0) { if (ch=='<') csb.append("&lt;"); else if (ch=='&') csb.append("&amp;"); else csb.append(ch); }	// including nested '<' and '>'
				}
				String arg=(csb.length()==0? "": csb.substring(0).trim());

//System.out.println(((char)cmd)+"	|"+csb+"|");
				switch (scmd) {
				case 'I': sb.append("<i>").append(arg).append("</i>"); break;
				case 'B': sb.append("<b>").append(arg).append("</b>"); break;
				case 'S': // non-breaking spaces
					for (int i=0,imax=csb.length(); i<imax; i++) if (csb.charAt(i)==' ') csb.setCharAt(i,(char)160);
					break;
				case 'F': // filenames
					// fall through
				case 'C': sb.append("<tt>").append(arg).append("</tt>"); break;
				case 'L': // link
					// split out display-only text, if any
					String txt = arg;
					int dinx=txt.indexOf('|');
					String dtxt=txt; if (dinx!=-1) { dtxt=txt.substring(0,dinx); txt=txt.substring(dinx+1); }

					// split out man page (any man page or just Perl?) vs ref
					int pinx=txt.indexOf('/');
					String page = (pinx==-1? txt: txt.substring(0,pinx));
					String ref = (pinx==-1? txt: txt.substring(pinx+1));
					if (pinx==-1) { if (ref.startsWith("\"") || anchors.indexOf(ref)!=-1) page=null; else ref=null;
					} else { if (page.length()==0) page=null; if (ref.length()==0) ref=null; }
					if (ref!=null && ref.startsWith("\"") && ref.endsWith("\"")) ref=ref.substring(1,ref.length()-1);	// strip quotes

					// write out results
					// page can be any man page, not just from Perl documentation, and fortunately we have a man page viewer built in
//System.out.println("page=|"+page+"|, ref=|"+ref+"|");
					// don't check anchors.indexOf because may be a forward reference
					if (br!=null && (page==null && ref!=null /*&& anchors.indexOf(ref)!=-1*/) || (page!=null && br.callSemanticEvent(ManualPageVolume.MSG_EXISTS, page)!=Boolean.FALSE)) {
						sb.append("<a href='");
						if (page!=null) sb.append("manpage:").append(page);
						if (ref!=null) sb.append('#').append(URIs.encode(ref));
// event openDocument manpage:$node(").append(vol).append(")'>\n"
						sb.append("'>").append(dtxt).append("</a>");
						//sb.append("<a href='http://www.cs.berkeley.edu/eatme.html'>eat me</a>");
					} else {
						sb.append("<s>").append(arg).append("</s>");
					}
					break;
				case 'X': // index -- only used in perlfunc.pod
					break;
				case 'Z': // zero-width character -- only used twice
					break;
				case 'E': // escape
					if (arg.length()>0) {
						if ("sol".equals(arg)) sb.append('/');
						else if ("verbar".equals(arg)) sb.append('|');
						else if (Character.isDigit(arg.charAt(0))) sb.append("&#").append(arg).append(';');
						else sb.append('&').append(arg).append(';');	// some HTML entity -- includes "gt" and "lt"
					}
					break;

				default: sb.append(arg);    // report unknown?
				}

			} else if (ch=='<') sb.append("&lt;"); else if (ch=='&') sb.append("&amp;"); else sb.append(ch);
		}
	}


	sb.append("\n</body></html>\n");
	ir.close();
if (DEBUG) System.out.println(sb.substring(0,1024));

	return sb.toString();
  }
}
