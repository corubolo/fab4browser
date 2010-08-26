package multivalent.std.adaptor;

import java.io.IOException;
import java.io.InputStream;

import multivalent.*;



/**
	Media adaptor for Apple II AppleWorks 3.0 Word Processor (AWP)
	Throws out pagination, tabstops, spacing, margins.
	Can also be run as an application to write HTML conversion to stdout.

	<p>Developer notes:
	Specification at <a href="http://www.umich.edu/~archive/apple2/technotes/ftn/FTN.1A.xxxx">http://www.umich.edu/~archive/apple2/technotes/ftn/FTN.1A.xxxx</a>.
	(Where's a spec for AppleWorks 5.1?)
	Example of how to translate some simpler concrete format into HTML internally.
	Revise that to a translation to XML, as HTML drops semantic information.
	Written as fast as could type in from spec--2 hours! (Though didn't implement most commands.)

	@version $Revision: 1.5 $ $Date: 2005/01/01 14:39:06 $
*/
public class AppleWorksWP extends MediaAdaptor {
  private static final boolean DEBUG=false;

  /** Syntax for running as an application. */
  private static final String USAGE = "USAGE: java a2.adaptor.AppleWorksWP <file> [<file>...]";


  public Object parse(INode parent) throws IOException {
	Document doc = parent.getDocument();
	return parseHelper(toHTML(), "HTML", getLayer(), parent);
  }

  /** Return HTML translation of document. */
  public String toHTML() throws IOException {
	InputStream is = getInputUni().getInputStream();

	// header
	for (int i=0; i<=3; i++) is.read();	// unused
	int id = is.read();
	//if (id != 0x4e) throw new IOException("Not an Apple II AppleWorks file");


	for (int i=5; i<=84; i++) is.read();	// tabstops -- read later
	is.read();	// zoom switch
	for (int i=86; i<=89; i++) is.read();	// unused
	is.read();	// show page breaks?
	is.read();	// left margin
	is.read();	// any mail merge?
	for (int i=93; i<=175; i++) is.read();	// reserved
	is.read();	// multiple rulers?
	for (int i=177; i<=182; i++) is.read();	// ruler tracking
	int minvers = is.read();	// min version of AppleWorks required (00 = <3.0, 30 (decimal) = 3.0)
if (DEBUG) System.out.println("min AW = "+minvers);
	for (int i=184; i<=249; i++) is.read();	// reserved
	for (int i=250; i<=299; i++) is.read();	// user space

	// "If SFMinVers is non-zero, the first line record (two bytes long) is invalid and should be skipped."
	int lineno=1, pageno=1;
	if (minvers>=30) { is.read(); is.read(); lineno++; }


	StringBuffer docsb = new StringBuffer(10000);	// estimate from file size, if available
	StringBuffer sb = docsb;
	sb.append("<html>\n<head>");
	//sb.append("\t<title>").append("AppleWorks ").append(url.getFile()).append("</title>\n");
	//sb.append("\t<base href='").append(url).append("'>\n");
	sb.append("\t<!-- ").append(getURI()/*.getTail()*/).append(" -->");
	sb.append("</head>\n");
	sb.append("<body>\n");
	boolean inpara=false;


	// line records
	//int skipcnt=0;	// treat skipped lines as comments?
	StringBuffer headersb=new StringBuffer(80*3), footersb=new StringBuffer(80*3);
	for (int type0=is.read(), type1=is.read(); type1!=0xff && type1!=-1; type0=is.read(), type1=is.read(), lineno++) {
		if (!inpara) {
			int sblen=sb.length();
			if (sb.charAt(sblen-1)=='>' && sb.charAt(sblen-2)=='p' && sb.charAt(sblen-3)=='<') sb.append("&nbsp;");	// don't have structural w/o children
			sb.append("\n<p>");
			inpara=true;
		}

		switch (type1) {
		case 0xd0:	// cr line, +0=horizontal char position of CR
			inpara=false;
			break;
		case 0xd1:	// undefined
		case 0xd2:
		case 0xd3:
			break;
		case 0xd4:	// reserved
			break;
		case 0xd5:	// page header end
			sb = docsb;
System.out.println("header = "+headersb.substring(0));
			break;
		case 0xd6:	// page footer end
System.out.println("footer = "+footersb.substring(0));
			sb = docsb;
			break;
		case 0xd7:	// right justified
			// <div align='right'>
			break;
		case 0xd8:	// platen width, +0=10ths of an inch
			// n/a
			break;
		case 0xd9:	// left margin, +0=10ths of an inch
			break;
		case 0xda:	// right margin, +0=10ths of an inch
			break;
		case 0xdb:	// chars per inch
			break;
		case 0xdc:	// proportional-1
			break;
		case 0xdd:	// proportional-2
			break;
		case 0xde:	// indent
			break;
		case 0xdf:	// justify
			// <div align='justify'>
			break;
		case 0xe0:	// unjustify
			break;
		case 0xe1:	// center
			// <div align='center'>
			break;
		case 0xe2:	// paper length, +0=10ths of an inch
			break;
		case 0xe3:	// top margin, +0=10ths of an inch
			break;
		case 0xe4:	// bottom margin, +0=10ths of an inch
			break;
		case 0xe5:	// lines per inch
			break;
		case 0xe6:	// single space
			break;
		case 0xe7:	// double space
			break;
		case 0xe8:	// triple space
			break;
		case 0xe9:	// new page
			pageno++;
			inpara=false;
			break;
		case 0xea:	// group begin
			break;
		case 0xeb:	// group end
			break;
		case 0xec:	// page header
			sb = headersb;
System.out.println("start page header");
			break;
		case 0xed:	// page footer
			//sb = footersb;
System.out.println("start page footer");
			break;
		case 0xee:	// skip lines, +0=count
			break;
		case 0xef:	// page number
			sb.append(pageno);
			break;
		case 0xf0:	// pause each page
		case 0xf1:	// pause here
			// no-op
			break;
		case 0xf2:	// set marker, +0=marker number
			break;
		case 0xf3:	// page number, +0=(add 256)
			break;
		case 0xf4:	// page break, +0=page number
			break;
		case 0xf5:	// page break, +0=(add 256)
			break;
		case 0xf6:	// page break, (break in middle of paragraph)
			break;
		case 0xf7:	// page break, (add 256 in middle of paragraph)
			pageno++;
			inpara=false;
			break;
		case 0xf8:
		case 0xf9:
		case 0xfa:
		case 0xfb:
		case 0xfc:
		case 0xfd:
		case 0xfe:
			break;
		case 0xff:	// end of file -- handled in loop test before getting here
			break;
		default:	// < 0xd0 => text line, +0=number of bytes in line
			/*int tab =*/ is.read();
			int crlen = is.read();
			boolean cr = (crlen&0x80)!=0;
			int len = (crlen&0x7f);
			for (int i=0; i<len; i++) {	// text characters
				int ch=is.read();
				switch (ch) {
				case 0x00:	// undefined
				case 0x19:
				case 0x1a:
				case 0x1b:
				case 0x1c:
				case 0x1d:
				case 0x1e:
				case 0x1f:
					break;
				case 0x01:	sb.append("<b>"); break;	// boldface begin
				case 0x02:	sb.append("</b>"); break;	// boldface end
				case 0x03:	sb.append("<sup>"); break;	// superscript begin
				case 0x04:	sb.append("</sup>"); break;	// superscript end
				case 0x05:	sb.append("<sub>"); break;	// subscript begin
				case 0x06:	sb.append("</sub>"); break;	// subscript end
				case 0x07:	sb.append("<u>"); break;	// underline begin
				case 0x08:	sb.append("</u>"); break;	// underlinen end
				case 0x09:	// print page number
					// not supported -- just a big scroll
					break;
				case 0x0a:	// enter keyboard
					// not supported
					break;
				case 0x0b:	// sticky space
					sb.append((char)160);
					break;
				case 0x0c:	// begin mail merge
					// not supported
					break;
				case 0x0d:	// reserved
					break;
				case 0x0e:	// print date
					sb.append("DATE");	// set to conversion time or keep as variable?
					break;
				case 0x0f:	// print time
					sb.append("TIME");	// set to conversion time or keep as variable?
					break;
				case 0x10:	// special code 1
				case 0x11:	// special code 2
				case 0x12:	// special code 3
				case 0x13:	// special code 4
				case 0x14:	// special code 5
				case 0x15:	// special code 6
					break;
				case 0x16:	// tab character
					sb.append("\t");
					break;
				case 0x17:	// tab fill character
					break;
				case 0x18:	// reserved
					break;
				default:	// ASCII character
					if (ch=='<') sb.append("&lt;");	// escape meta characters
					else if (ch=='>') sb.append("&gt;");
					else if (ch=='&') sb.append("&amp;");
					else sb.append((char)ch);
				}
			}
			if (cr) sb.append("<br />\n");
		}
	}

	// file tags (after FF FF)	-- defined by Beagle Bros, OK to ignore
	int tagcnt=0;
	for (int type0=is.read(), type1=is.read(); ; type0=is.read(), type1=is.read(), tagcnt++) {
		int tag0=is.read(), tag1=is.read();
		if (tag0==-1 || (tag0==tagcnt && tag1==0xff)) break;
		int len = tag0+(tag1<<8);
		for (int i=0; i<=len; i++) is.read();
	}

	if (inpara) sb.setLength(sb.length()-"\n<p>".length());	// invariant always has open paragraph
	sb.append("\n</body></html>\n");
	is.close();
if (DEBUG) System.out.println(sb.substring(0,1024));

	return sb.substring(0);
  }



  /**
	When run as application, output HTML conversion.
  */
  public static void main(String[] argv) {
	if (argv.length==0) { System.out.println(USAGE); System.exit(0); }
	AppleWorksWP awp = new AppleWorksWP();

	for (int i=0,imax=argv.length; i<imax; i++) {
		String file = argv[i];
		try {
			awp.setInput(new com.pt.io.InputUniFile(file));
			String txt = awp.toHTML();
			awp.close();
			System.out.println(txt);
		} catch (IOException ioe) {
			System.out.println(ioe+": "+file);
		}
	}
  }
}
